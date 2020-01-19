package heartbeat.tracking;

import api.wynn.WynnApi;
import api.wynn.structs.GuildLeaderboard;
import app.Bot;
import db.model.guildXpLeaderboard.GuildXpLeaderboard;
import db.repository.GuildLeaderboardRepository;
import db.repository.GuildXpLeaderboardRepository;
import heartbeat.base.TaskBase;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GuildLeaderboardTracker implements TaskBase {
    private final Logger logger;
    private final WynnApi wynnApi;
    private final GuildLeaderboardRepository guildLeaderboardRepository;
    private final GuildXpLeaderboardRepository guildXpLeaderboardRepository;

    public GuildLeaderboardTracker(Bot bot) {
        this.logger = bot.getLogger();
        this.wynnApi = new WynnApi(bot.getLogger(), bot.getProperties().wynnTimeZone);
        this.guildLeaderboardRepository = bot.getDatabase().getGuildLeaderboardRepository();
        this.guildXpLeaderboardRepository = bot.getDatabase().getGuildXpLeaderboardRepository();
    }

    @Override
    public void run() {
        GuildLeaderboard leaderboard = this.wynnApi.getGuildLeaderboard();
        if (leaderboard == null) {
            return;
        }

        List<db.model.guildLeaderboard.GuildLeaderboard> current = convertLeaderboard(leaderboard);
        List<db.model.guildLeaderboard.GuildLeaderboard> last = this.guildLeaderboardRepository.getLatestLeaderboard();
        if (last == null) {
            return;
        }

        current.sort(Comparator.comparingInt(db.model.guildLeaderboard.GuildLeaderboard::getNum));
        last.sort(Comparator.comparingInt(db.model.guildLeaderboard.GuildLeaderboard::getNum));

        if (leaderboardEquals(current, last)) {
            return;
        }

        if (!checkIntegrity(current, last)) {
            this.logger.log(-1, "Guild Leaderboard Tracker: Retrieved leaderboard failed to pass integrity check");
            return;
        }

        this.guildLeaderboardRepository.createAll(current);
        this.clearOldData();
        this.updateXpLeaderboard();
    }

    /**
     * Clears old data from database.
     */
    private void clearOldData() {
        Date newest = this.guildLeaderboardRepository.getNewestDate();
        Date oldest = this.guildLeaderboardRepository.getOldestDate();
        if (newest == null || oldest == null) {
            return;
        }

        Date oneDayBefore = new Date(newest.getTime() - TimeUnit.DAYS.toMillis(1));

        Date border = this.guildLeaderboardRepository.getNewestDateBetween(oldest, oneDayBefore);
        if (border == null) {
            return;
        }

        this.guildLeaderboardRepository.deleteAllOlderThan(border);
    }

    @NotNull
    private static List<db.model.guildLeaderboard.GuildLeaderboard> convertLeaderboard(GuildLeaderboard leaderboard) {
        List<db.model.guildLeaderboard.GuildLeaderboard> ret = new ArrayList<>();
        Date updatedAt = new Date(leaderboard.getRequest().getTimestamp() * 1000);

        for (GuildLeaderboard.Guild datum : leaderboard.getData()) {
            ret.add(new db.model.guildLeaderboard.GuildLeaderboard(
                    datum.getName(),
                    datum.getPrefix(),
                    datum.getXp(),
                    datum.getLevel(),
                    datum.getNum(),
                    datum.getTerritories(),
                    datum.getMembersCount(),
                    updatedAt
            ));
        }
        return ret;
    }

    /**
     * Checks if both leaderboards are equal.
     * <br>For each 'num', comparing name, level, and xp. If any of them were different, returns false.
     * @param sortedCurrent Sorted current one by 'num' field.
     * @param sortedLast Sorted last one by 'num' field.
     * @return true if yes.
     */
    private static boolean leaderboardEquals(List<db.model.guildLeaderboard.GuildLeaderboard> sortedCurrent,
                                             List<db.model.guildLeaderboard.GuildLeaderboard> sortedLast) {
        if (sortedCurrent.size() != sortedLast.size()) {
            return false;
        }

        for (int i = 0; i < sortedCurrent.size(); i++) {
            db.model.guildLeaderboard.GuildLeaderboard c = sortedCurrent.get(i);
            db.model.guildLeaderboard.GuildLeaderboard l = sortedLast.get(i);
            if (!c.getName().equals(l.getName()) ||
                    c.getLevel() != l.getLevel() || c.getXp() != l.getXp()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the current one is 'newer' than the last one.
     * <br>Returns false if any of the guilds included in both leaderboards, have decreased xp / level.
     * @param current Current leaderboard.
     * @param last Last leaderboard.
     * @return true if yes.
     */
    private static boolean checkIntegrity(List<db.model.guildLeaderboard.GuildLeaderboard> current,
                                          List<db.model.guildLeaderboard.GuildLeaderboard> last) {
        Set<String> lastGuilds = last.stream()
                .map(db.model.guildLeaderboard.GuildLeaderboard::getName).collect(Collectors.toSet());
        Set<String> guildsContainedInBoth = current.stream()
                .map(db.model.guildLeaderboard.GuildLeaderboard::getName).filter(lastGuilds::contains)
                .collect(Collectors.toSet());
        Map<String, db.model.guildLeaderboard.GuildLeaderboard> currentMap = current.stream()
                .collect(Collectors.toMap(db.model.guildLeaderboard.GuildLeaderboard::getName, g -> g));
        Map<String, db.model.guildLeaderboard.GuildLeaderboard> lastMap = last.stream()
                .collect(Collectors.toMap(db.model.guildLeaderboard.GuildLeaderboard::getName, g -> g));

        for (String guildName : guildsContainedInBoth) {
            db.model.guildLeaderboard.GuildLeaderboard c = currentMap.get(guildName);
            db.model.guildLeaderboard.GuildLeaderboard l = lastMap.get(guildName);
            if (c.getLevel() < l.getLevel()) {
                // If current level is lower than the last level
                return false;
            } else if (c.getLevel() == l.getLevel() && c.getXp() < l.getXp()) {
                // Or, level is the same but xp decreased
                return false;
            }
        }
        return true;
    }

    /**
     * Updates `guild_xp_leaderboard` table.
     */
    private void updateXpLeaderboard() {
        List<db.model.guildLeaderboard.GuildLeaderboard> all = this.guildLeaderboardRepository.findAll();
        if (all == null) {
            return;
        }

        class Leaderboard {
            private List<db.model.guildLeaderboard.GuildLeaderboard> data;

            private Leaderboard(List<db.model.guildLeaderboard.GuildLeaderboard> data) {
                this.data = data;
            }
        }

        Map<String, Leaderboard> guildHistory = new HashMap<>();
        List<Date> dates = all.stream().map(db.model.guildLeaderboard.GuildLeaderboard::getUpdatedAt)
                .distinct().sorted(Comparator.comparingLong(Date::getTime)).collect(Collectors.toList());
        if (dates.size() == 0) return;
        Date newest = dates.get(dates.size() - 1);
        Date oldest = dates.get(0);

        all.forEach(g -> {
            if (guildHistory.containsKey(g.getName())) {
                guildHistory.put(g.getName(), new Leaderboard(new ArrayList<>()));
            }
            guildHistory.get(g.getName()).data.add(g);
        });

        guildHistory.forEach((guild, history) -> history.data.sort(Comparator.comparingLong(g -> g.getUpdatedAt().getTime())));

        // guilds contained in both oldest and newest leaderboard
        Set<String> guildsContainedInBoth = new HashSet<>();
        guildHistory.forEach((guild, history) -> {
            if (history.data.size() == 0) return;
            db.model.guildLeaderboard.GuildLeaderboard first = history.data.get(0);
            db.model.guildLeaderboard.GuildLeaderboard last = history.data.get(history.data.size() - 1);
            if (first.getUpdatedAt().getTime() == oldest.getTime()
            && last.getUpdatedAt().getTime() == newest.getTime()) {
                guildsContainedInBoth.add(guild);
            }
        });

        Function<Leaderboard, Long> getXpDiff = leaderboard -> {
            db.model.guildLeaderboard.GuildLeaderboard first = leaderboard.data.get(0);
            db.model.guildLeaderboard.GuildLeaderboard last = leaderboard.data.get(leaderboard.data.size() - 1);
            // If the level stayed the same, simple case
            if (first.getLevel() == last.getLevel()) {
                return last.getXp() - first.getXp();
            }
            // Else, go through the history to calculate xp diff
            long ret = 0;
            for (int i = 0; i < leaderboard.data.size() - 1; i++) {
                db.model.guildLeaderboard.GuildLeaderboard old = leaderboard.data.get(i);
                db.model.guildLeaderboard.GuildLeaderboard next = leaderboard.data.get(i + 1);
                if (old.getLevel() == next.getLevel()) {
                    ret += (next.getXp() - old.getXp());
                } else if (old.getLevel() < next.getLevel()) {
                    // On level up, we do not know the max xp for that level so just calculate from the newer xp
                    // this is one reason the xp leaderboard is not accurate
                    ret += next.getXp();
                }
            }
            return ret;
        };

        List<GuildXpLeaderboard> xpLeaderboard = new ArrayList<>();
        for (String guildName : guildsContainedInBoth) {
            Leaderboard leaderboard = guildHistory.get(guildName);
            db.model.guildLeaderboard.GuildLeaderboard first = leaderboard.data.get(0);
            db.model.guildLeaderboard.GuildLeaderboard last = leaderboard.data.get(leaderboard.data.size() - 1);
            xpLeaderboard.add(new GuildXpLeaderboard(
                    guildName, last.getPrefix(), last.getLevel(), last.getXp(),
                    getXpDiff.apply(leaderboard), first.getUpdatedAt(), last.getUpdatedAt()
            ));
        }

        boolean res = this.guildXpLeaderboardRepository.truncateTable();
        if (!res) {
            this.logger.log(0, "Guild Leaderboard Tracker: Failed to truncate guild xp leaderboard");
        }
        res = this.guildXpLeaderboardRepository.createAll(xpLeaderboard);
        if (!res) {
            this.logger.log(0, "Guild Leaderboard Tracker: Failed to update guild xp leaderboard");
        }
    }

    @Override
    public long getFirstDelay() {
        return TimeUnit.MINUTES.toMillis(1);
    }

    @Override
    public long getInterval() {
        return TimeUnit.MINUTES.toMillis(5);
    }
}

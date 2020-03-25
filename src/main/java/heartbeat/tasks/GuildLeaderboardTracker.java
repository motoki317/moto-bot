package heartbeat.tasks;

import api.wynn.WynnApi;
import api.wynn.structs.WynnGuildLeaderboard;
import app.Bot;
import db.model.guildLeaderboard.GuildLeaderboard;
import db.model.guildXpLeaderboard.GuildXpLeaderboard;
import db.repository.base.GuildLeaderboardRepository;
import db.repository.base.GuildXpLeaderboardRepository;
import heartbeat.base.TaskBase;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
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

    @NotNull
    @Override
    public String getName() {
        return "Guild Leaderboard Tracker";
    }

    @Override
    public void run() {
        WynnGuildLeaderboard leaderboard = this.wynnApi.mustGetGuildLeaderboard();
        if (leaderboard == null) {
            return;
        }

        List<GuildLeaderboard> current = convertLeaderboard(leaderboard);
        List<GuildLeaderboard> last = this.guildLeaderboardRepository.getLatestLeaderboard();
        if (last == null) {
            return;
        }

        current.sort(Comparator.comparingInt(GuildLeaderboard::getNum));
        last.sort(Comparator.comparingInt(GuildLeaderboard::getNum));

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
    private static List<GuildLeaderboard> convertLeaderboard(WynnGuildLeaderboard leaderboard) {
        List<GuildLeaderboard> ret = new ArrayList<>();
        Date updatedAt = new Date(leaderboard.getRequest().getTimestamp() * 1000);

        for (WynnGuildLeaderboard.Guild datum : leaderboard.getData()) {
            ret.add(new GuildLeaderboard(
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
    private static boolean leaderboardEquals(List<GuildLeaderboard> sortedCurrent,
                                             List<GuildLeaderboard> sortedLast) {
        if (sortedCurrent.size() != sortedLast.size()) {
            return false;
        }

        for (int i = 0; i < sortedCurrent.size(); i++) {
            GuildLeaderboard c = sortedCurrent.get(i);
            GuildLeaderboard l = sortedLast.get(i);
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
    private static boolean checkIntegrity(List<GuildLeaderboard> current,
                                          List<GuildLeaderboard> last) {
        Set<String> lastGuilds = last.stream()
                .map(GuildLeaderboard::getName).collect(Collectors.toSet());
        Set<String> guildsContainedInBoth = current.stream()
                .map(GuildLeaderboard::getName).filter(lastGuilds::contains)
                .collect(Collectors.toSet());
        Map<String, GuildLeaderboard> currentMap = current.stream()
                .collect(Collectors.toMap(GuildLeaderboard::getName, g -> g));
        Map<String, GuildLeaderboard> lastMap = last.stream()
                .collect(Collectors.toMap(GuildLeaderboard::getName, g -> g));

        for (String guildName : guildsContainedInBoth) {
            GuildLeaderboard c = currentMap.get(guildName);
            GuildLeaderboard l = lastMap.get(guildName);
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

    private static class GuildHistory {
        private Map<String, List<GuildLeaderboard>> history;

        GuildHistory() {
            this.history = new HashMap<>();
        }

        private void add(GuildLeaderboard l) {
            if (!history.containsKey(l.getName())) {
                history.put(l.getName(), new ArrayList<>());
            }
            history.get(l.getName()).add(l);
        }

        private void sortByAscendingTime() {
            history.forEach((guild, leaderboardHistory) -> leaderboardHistory.sort(Comparator.comparingLong(l -> l.getUpdatedAt().getTime())));
        }

        private long getXpDiff(String guildName) {
            List<GuildLeaderboard> leaderboardHistory = history.get(guildName);
            if (leaderboardHistory == null || leaderboardHistory.isEmpty()) {
                return 0L;
            }

            GuildLeaderboard first = leaderboardHistory.get(0);
            GuildLeaderboard last = leaderboardHistory.get(leaderboardHistory.size() - 1);

            // If the level stayed the same, simple case
            if (first.getLevel() == last.getLevel()) {
                return last.getXp() - first.getXp();
            }
            // Else, go through the history to calculate xp diff
            long xp = 0;
            for (int i = 0; i < leaderboardHistory.size() - 1; i++) {
                GuildLeaderboard old = leaderboardHistory.get(i);
                GuildLeaderboard next = leaderboardHistory.get(i + 1);
                if (old.getLevel() == next.getLevel()) {
                    xp += (next.getXp() - old.getXp());
                } else if (old.getLevel() < next.getLevel()) {
                    // On level up, we do not know the max xp for that level so just calculate from the newer xp
                    // this is one reason the xp leaderboard is not accurate
                    xp += next.getXp();
                }
            }
            return xp;
        }
    }

    /**
     * Updates `guild_xp_leaderboard` table.
     */
    private void updateXpLeaderboard() {
        List<GuildLeaderboard> all = this.guildLeaderboardRepository.findAll();
        if (all == null) {
            return;
        }

        GuildHistory guildHistory = new GuildHistory();
        all.forEach(guildHistory::add);
        guildHistory.sortByAscendingTime();

        List<GuildXpLeaderboard> xpLeaderboard = new ArrayList<>();
        for (String guildName : guildHistory.history.keySet()) {
            long xpDiff = guildHistory.getXpDiff(guildName);
            if (xpDiff == 0L) {
                continue;
            }

            List<GuildLeaderboard> leaderboard = guildHistory.history.get(guildName);
            GuildLeaderboard first = leaderboard.get(0);
            GuildLeaderboard last = leaderboard.get(leaderboard.size() - 1);
            xpLeaderboard.add(new GuildXpLeaderboard(
                    guildName, last.getPrefix(), last.getLevel(), last.getXp(),
                    xpDiff, first.getUpdatedAt(), last.getUpdatedAt()
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

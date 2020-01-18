package heartbeat.tracking;

import api.wynn.WynnApi;
import api.wynn.structs.GuildLeaderboard;
import app.Bot;
import db.repository.GuildLeaderboardRepository;
import heartbeat.base.TaskBase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GuildLeaderboardTracker implements TaskBase {
    private final WynnApi wynnApi;
    private final GuildLeaderboardRepository guildLeaderboardRepository;

    public GuildLeaderboardTracker(Bot bot) {
        this.wynnApi = new WynnApi(bot.getLogger(), bot.getProperties().wynnTimeZone);
        this.guildLeaderboardRepository = bot.getDatabase().getGuildLeaderboardRepository();
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

        this.guildLeaderboardRepository.createAll(current);
        this.clearOldData();
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

    @Override
    public long getFirstDelay() {
        return TimeUnit.MINUTES.toMillis(1);
    }

    @Override
    public long getInterval() {
        return TimeUnit.MINUTES.toMillis(5);
    }
}

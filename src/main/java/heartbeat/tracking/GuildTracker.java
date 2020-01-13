package heartbeat.tracking;

import db.repository.GuildRepository;
import heartbeat.base.TaskBase;

import java.util.concurrent.TimeUnit;

public class GuildTracker implements TaskBase {
    private final Object dbLock;
    private final GuildRepository guildRepository;

    public GuildTracker(Object dbLock, GuildRepository guildRepository) {
        this.dbLock = dbLock;
        this.guildRepository = guildRepository;
    }

    private static final long GUILD_TRACKER_DELAY = TimeUnit.HOURS.toMillis(1);

    @Override
    public long getFirstDelay() {
        return GUILD_TRACKER_DELAY;
    }

    @Override
    public long getInterval() {
        return GUILD_TRACKER_DELAY;
    }

    @Override
    public void run() {
        // TODO: implement me
    }
}

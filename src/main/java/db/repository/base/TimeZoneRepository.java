package db.repository.base;

import db.ConnectionPool;
import db.model.timezone.CustomTimeZone;
import db.model.timezone.CustomTimeZoneId;
import log.Logger;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public abstract class TimeZoneRepository extends Repository<CustomTimeZone, CustomTimeZoneId> {
    protected TimeZoneRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Retrieves custom timezone.
     * If more than one IDs are given, later ones are more prioritized.
     * @param ids List of ids.
     * @return Custom timezone. If no custom timezone is set, returns default.
     */
    @NotNull
    public abstract CustomTimeZone getTimeZone(long... ids);

    /**
     * Retrieves custom timezone, in order of guild (if exists) -> channel -> user -> default (fallback).
     * Earlier ones are more prioritized if exists.
     * @param event Discord message received event.
     * @return Custom timezone.
     */
    @NotNull
    public abstract CustomTimeZone getTimeZone(MessageReceivedEvent event);
}

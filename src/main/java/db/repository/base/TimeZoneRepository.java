package db.repository.base;

import db.model.timezone.CustomTimeZone;
import db.model.timezone.CustomTimeZoneId;
import db.repository.Repository;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public interface TimeZoneRepository extends Repository<CustomTimeZone, CustomTimeZoneId> {
    /**
     * Retrieves custom timezone.
     * If more than one IDs are given, later ones are more prioritized.
     * @param ids List of ids.
     * @return Custom timezone. If no custom timezone is set, returns default.
     */
    @NotNull CustomTimeZone getTimeZone(long... ids);

    /**
     * Retrieves custom timezone, in order of guild (if exists) -> channel -> user -> default (fallback).
     * Earlier ones are more prioritized if exists.
     * @param event Discord message received event.
     * @return Custom timezone.
     */
    @NotNull CustomTimeZone getTimeZone(MessageReceivedEvent event);
}

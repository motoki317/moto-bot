package db.repository.base;

import commands.event.CommandEvent;
import db.model.timezone.CustomTimeZone;
import db.model.timezone.CustomTimeZoneId;
import db.repository.Repository;
import org.jetbrains.annotations.NotNull;

public interface TimeZoneRepository extends Repository<CustomTimeZone, CustomTimeZoneId> {
    /**
     * Retrieves custom timezone.
     * If more than one IDs are given, later ones are more prioritized.
     *
     * @param ids List of ids.
     * @return Custom timezone. If no custom timezone is set, returns default.
     */
    @NotNull CustomTimeZone getTimeZone(long... ids);

    /**
     * Retrieves custom timezone, in order of guild (if exists) -> channel -> user -> default (fallback).
     * Earlier ones are more prioritized if exists.
     *
     * @param event Discord command event.
     * @return Custom timezone.
     */
    default @NotNull CustomTimeZone getTimeZone(CommandEvent event) {
        if (event.isFromGuild()) {
            return this.getTimeZone(
                    event.getGuild().getIdLong(),
                    event.getChannel().getIdLong(),
                    event.getAuthor().getIdLong()
            );
        } else {
            return this.getTimeZone(
                    event.getChannel().getIdLong(),
                    event.getAuthor().getIdLong()
            );
        }
    }
}

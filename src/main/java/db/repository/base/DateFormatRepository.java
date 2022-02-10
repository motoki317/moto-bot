package db.repository.base;

import commands.event.CommandEvent;
import db.model.dateFormat.CustomDateFormat;
import db.model.dateFormat.CustomDateFormatId;
import db.repository.Repository;
import org.jetbrains.annotations.NotNull;

public interface DateFormatRepository extends Repository<CustomDateFormat, CustomDateFormatId> {
    /**
     * Retrieves custom date format.
     * If more than one IDs are given, the later ones are more prioritized.
     *
     * @param ids List of discord IDs.
     * @return Custom date format.
     */
    @NotNull CustomDateFormat getDateFormat(long... ids);

    /**
     * Retrieves custom date format, in order of guild (if exists) -> channel -> user -> default (fallback).
     * Earlier ones are more prioritized.
     *
     * @param event discord command event.
     * @return Custom date format.
     */
    default @NotNull CustomDateFormat getDateFormat(CommandEvent event) {
        if (event.isFromGuild()) {
            return this.getDateFormat(
                    event.getGuild().getIdLong(),
                    event.getChannel().getIdLong(),
                    event.getAuthor().getIdLong()
            );
        } else {
            return this.getDateFormat(
                    event.getChannel().getIdLong(),
                    event.getAuthor().getIdLong()
            );
        }
    }
}

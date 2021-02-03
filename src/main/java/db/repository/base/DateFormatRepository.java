package db.repository.base;

import db.model.dateFormat.CustomDateFormat;
import db.model.dateFormat.CustomDateFormatId;
import db.repository.Repository;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public interface DateFormatRepository extends Repository<CustomDateFormat, CustomDateFormatId> {
    /**
     * Retrieves custom date format.
     * If more than one IDs are given, the later ones are more prioritized.
     * @param ids List of discord IDs.
     * @return Custom date format.
     */
    @NotNull CustomDateFormat getDateFormat(long... ids);

    /**
     * Retrieves custom date format, in order of guild (if exists) -> channel -> user -> default (fallback).
     * Earlier ones are more prioritized.
     * @param event discord message received event.
     * @return Custom date format.
     */
    @NotNull CustomDateFormat getDateFormat(MessageReceivedEvent event);
}

package db.repository.base;

import db.ConnectionPool;
import db.model.dateFormat.CustomDateFormat;
import db.model.dateFormat.CustomDateFormatId;
import log.Logger;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public abstract class DateFormatRepository extends Repository<CustomDateFormat, CustomDateFormatId> {
    protected DateFormatRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Retrieves custom date format.
     * If more than one IDs are given, the later ones are more prioritized.
     * @param ids List of discord IDs.
     * @return Custom date format.
     */
    @NotNull
    public abstract CustomDateFormat getDateFormat(long... ids);

    /**
     * Retrieves custom date format, in order of guild (if exists) -> channel -> user -> default (fallback).
     * Earlier ones are more prioritized.
     * @param event discord message received event.
     * @return Custom date format.
     */
    @NotNull
    public abstract CustomDateFormat getDateFormat(MessageReceivedEvent event);
}

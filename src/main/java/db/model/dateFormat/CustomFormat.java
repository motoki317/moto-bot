package db.model.dateFormat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public enum CustomFormat {
    TWELVE_HOUR(
            new SimpleDateFormat("yyyy/MM/dd a h:mm"),
            new SimpleDateFormat("yyyy/MM/dd a h:mm:ss")
    ),
    TWENTY_FOUR_HOUR(
            new SimpleDateFormat("yyyy/MM/dd HH:mm"),
            new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    );

    // minute-wise format
    private DateFormat minuteFormat;
    // second-wise format
    private DateFormat secondFormat;

    CustomFormat(DateFormat minuteFormat, DateFormat secondFormat) {
        this.minuteFormat = minuteFormat;
        this.secondFormat = secondFormat;
    }

    public DateFormat getMinuteFormat() {
        return minuteFormat;
    }

    public DateFormat getSecondFormat() {
        return secondFormat;
    }

    @Override
    public String toString() {
        return this.name();
    }
}

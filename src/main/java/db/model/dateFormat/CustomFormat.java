package db.model.dateFormat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public enum CustomFormat {
    TWELVE_HOUR(
            "12h",
            new SimpleDateFormat("yyyy/MM/dd a h:mm"),
            new SimpleDateFormat("yyyy/MM/dd a h:mm:ss")
    ),
    TWENTY_FOUR_HOUR(
            "24h",
            new SimpleDateFormat("yyyy/MM/dd HH:mm"),
            new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    );

    private String shortName;
    // minute-wise format
    private DateFormat minuteFormat;
    // second-wise format
    private DateFormat secondFormat;

    CustomFormat(String shortName, DateFormat minuteFormat, DateFormat secondFormat) {
        this.shortName = shortName;
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

    public String getShortName() {
        return shortName;
    }
}

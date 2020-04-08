package net.motobot.wrapper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public enum CustomTimeFormat {

    TWELVE_HOUR(new SimpleDateFormat("yyyy/MM/dd a h:mm"),
            new SimpleDateFormat("yyyy/MM/dd a h:mm:ss"),
            new SimpleDateFormat("yyyy/MM/dd E a h:mm"),
            new SimpleDateFormat("yyyy/MM/dd E a h:mm:ss"),
            "12-hour"),
    TWENTYFOUR_HOUR(new SimpleDateFormat("yyyy/MM/dd HH:mm"),
            new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
            new SimpleDateFormat("yyyy/MM/dd E HH:mm"),
            new SimpleDateFormat("yyyy/MM/dd E HH:mm:ss"),
            "24-hour");

    private DateFormat minFormat;
    private DateFormat secFormat;
    private DateFormat minFormatWDay;
    private DateFormat secFormatWDay;
    private String str;

    CustomTimeFormat(DateFormat minFormat, DateFormat secFormat, DateFormat minFormatWDay, DateFormat secFormatWDay, String str) {
        this.minFormat = minFormat;
        this.secFormat = secFormat;
        this.minFormatWDay = minFormatWDay;
        this.secFormatWDay = secFormatWDay;
        this.str = str;
    }

    public DateFormat getMinFormat () {
        return this.minFormat;
    }

    public DateFormat getSecFormat() {
        return secFormat;
    }

    public DateFormat getMinFormatWDay() {
        return minFormatWDay;
    }

    public DateFormat getSecFormatWDay() {
        return secFormatWDay;
    }

    @Override
    public String toString() {
        return this.str;
    }
}

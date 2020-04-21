package music;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MusicUtils {
    private final static Pattern youtubeVideo = Pattern.compile("https://www\\.youtube\\.com/watch\\?v=(.+)");
    private final static String YOUTUBE_THUMBNAIL = "http://i.ytimg.com/vi/%s/default.jpg";

    /**
     * Tries to retrieve thumbnail URL for the given audio source URL.
     * @param sourceURL Audio source URL.
     * @return Thumbnail URL if found.
     */
    @Nullable
    static String getThumbnailURL(String sourceURL) {
        Matcher m = youtubeVideo.matcher(sourceURL);
        if (m.matches()) {
            return String.format(YOUTUBE_THUMBNAIL, m.group(1));
        }
        return null;
    }

    /**
     * Formats time length.
     * @param milliseconds Length in milliseconds.
     * @return Formatted string such as "4:33".
     */
    static String formatLength(long milliseconds) {
        long seconds = milliseconds / 1000L;

        long hours = seconds / 3600L;
        seconds -= hours * 3600L;
        long minutes = seconds / 60L;
        seconds -= minutes * 60L;

        if (hours > 0L) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
}

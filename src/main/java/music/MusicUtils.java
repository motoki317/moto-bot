package music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import db.model.musicSetting.MusicSetting;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicUtils {
    private final static Pattern YOUTUBE_VIDEO = Pattern.compile("https://(www\\.)?youtube\\.com/watch\\?v=(.+)");
    private final static String YOUTUBE_THUMBNAIL = "http://i.ytimg.com/vi/%s/default.jpg";

    /**
     * Tries to retrieve thumbnail URL for the given audio source URL.
     * @param sourceURL Audio source URL.
     * @return Thumbnail URL if found.
     */
    @Nullable
    public static String getThumbnailURL(String sourceURL) {
        Matcher m = YOUTUBE_VIDEO.matcher(sourceURL);
        if (m.matches()) {
            return String.format(YOUTUBE_THUMBNAIL, m.group(2));
        }
        return null;
    }

    /**
     * Formats time length.
     * @param milliseconds Length in milliseconds.
     * @return Formatted string such as "4:33".
     */
    public static String formatLength(long milliseconds) {
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

    private final static Pattern lengthHourPattern = Pattern.compile("(\\d+):(\\d{2}):(\\d{2})");
    private final static Pattern lengthMinutePattern = Pattern.compile("(\\d{1,2}):(\\d{2})");

    /**
     * Parses time length.
     * @param length Length string, such as "1:23:45" or "5:34".
     * @return Length in milliseconds.
     * @throws IllegalArgumentException If the given length is not a valid pattern.
     */
    public static long parseLength(String length) throws IllegalArgumentException {
        Matcher hour = lengthHourPattern.matcher(length);
        if (hour.matches()) {
            long hours = Integer.parseInt(hour.group(1));
            long minutes = Integer.parseInt(hour.group(2));
            long seconds = Integer.parseInt(hour.group(3));
            return TimeUnit.HOURS.toMillis(hours) +
                    TimeUnit.MINUTES.toMillis(minutes) +
                    TimeUnit.SECONDS.toMillis(seconds);
        }
        Matcher minute = lengthMinutePattern.matcher(length);
        if (minute.matches()) {
            long minutes = Integer.parseInt(minute.group(1));
            long seconds = Integer.parseInt(minute.group(2));
            return TimeUnit.MINUTES.toMillis(minutes) +
                    TimeUnit.SECONDS.toMillis(seconds);
        }
        throw new IllegalArgumentException("Failed to parse length");
    }

    /**
     * Formats "now playing" message.
     * @param track Audio track.
     * @param user User who requested this track.
     * @param setting Music setting.
     * @param botAvatarURL Bot avatar URL.
     * @param showPosition If {@code true}, displays current position.
     * @return Embed message
     */
    @NotNull
    public static MessageEmbed formatNowPlaying(AudioTrack track, @Nullable User user,
                                                MusicSetting setting, String botAvatarURL,
                                                boolean showPosition) {
        AudioTrackInfo info = track.getInfo();
        String title = info.title;
        RepeatState repeat = setting.getRepeat();

        String length = info.isStream ? "LIVE" : formatLength(info.length);
        String lengthField = showPosition
                ? String.format("%s / %s", formatLength(track.getPosition()), length)
                : length;

        return new EmbedBuilder()
                .setAuthor(String.format("♪ Now playing%s", repeat == RepeatState.OFF ? "" : " (" + repeat.getMessage() + ")"),
                        null, botAvatarURL)
                .setTitle("".equals(title) ? "(No title)" : title, track.getInfo().uri)
                .addField("Length", lengthField, true)
                .addField("Player volume", setting.getVolume() + "%", true)
                .setThumbnail(getThumbnailURL(info.uri))
                .setFooter(String.format("Requested by %s",
                        user != null ? user.getName() + "#" + user.getDiscriminator() : "Unknown User"),
                        user != null ? user.getEffectiveAvatarUrl() : null)
                .setTimestamp(Instant.now())
                .build();
    }
}

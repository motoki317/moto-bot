package app;

import java.awt.*;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class Properties {
    private final java.util.Properties properties;

    public final String version;

    public final String herokuReleaseVersion;
    public final Date releaseDate;
    public final Date lastReboot;

    final String botAccessToken;
    public final String botDiscordId;

    public final Map<Integer, Long> logChannelId;
    public final long playerTrackerChannelId;
    final long botRestartChannelId;

    public final String prefix;
    final int shards;

    private final String mainColor;
    public final String githubImagesUrl;
    public final String botServerInviteUrl;
    public final String botInviteUrl;
    public final String wynnIconUrl;
    public final String guildBannerUrl;

    final TimeZone logTimeZone;
    public final TimeZone wynnTimeZone;

    public Properties() throws IOException, ParseException {
        this.properties = new java.util.Properties();
        this.properties.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));

        this.version = getProperty("version");
//        String artifactId = getProperty("artifactId");

        String herokuVersionEnv = getEnv("HEROKU_RELEASE_VERSION");
        this.herokuReleaseVersion = herokuVersionEnv == null ? "" : herokuVersionEnv;

        String herokuReleaseDateStr = getEnv("HEROKU_RELEASE_CREATED_AT");
        DateFormat herokuReleaseDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        this.releaseDate = herokuReleaseDateStr == null || herokuReleaseDateStr.equals("")
                ? new Date()
                : herokuReleaseDateFormat.parse(herokuReleaseDateStr);
        this.lastReboot = new Date();

        this.botAccessToken = getEnv("DISCORD_ACCESS_TOKEN");
        this.botDiscordId = getEnv("BOT_DISCORD_ID");

        int logChannelsMax = getPropertyInt("botLogChannelsMax");
        this.logChannelId = new HashMap<>();
        for (int i = 0; i <= logChannelsMax; i++) {
            this.logChannelId.put(i, Long.parseLong(getEnv("BOT_LOG_CHANNEL_" + i)));
        }
        this.playerTrackerChannelId = Long.parseLong(getEnv("PLAYER_TRACKER_CHANNEL"));
        this.botRestartChannelId = Long.parseLong(getEnv("BOT_RESTART_CHANNEL"));

        this.prefix = getProperty("prefix");
        this.shards = getPropertyInt("shards");

        this.mainColor = getProperty("mainColor");
        this.githubImagesUrl = getProperty("githubImagesUrl");
        this.botServerInviteUrl = getProperty("botServerInviteUrl");
        this.botInviteUrl = String.format(
                "https://discordapp.com/oauth2/authorize?client_id=%s&scope=bot&permissions=93248",
                // bot discord id is the same as bot client id
                this.botDiscordId
        );
        this.wynnIconUrl = getProperty("wynnIconUrl");
        this.guildBannerUrl = getProperty("guildBannerUrl");

        this.logTimeZone = TimeZone.getTimeZone(getProperty("logTimeZone"));
        this.wynnTimeZone = TimeZone.getTimeZone(getProperty("wynnTimeZone"));
    }

    private String getEnv(String name) {
        return System.getenv(name);
    }

    private String getProperty(String name) {
        return this.properties.getProperty(name);
    }

    private int getPropertyInt(String name) {
        return Integer.parseInt(this.properties.getProperty(name));
    }

    /**
     * Converts hex color string "#FFFFFF" to java color instance.
     * @return Color instance.
     */
    public Color getMainColor() {
        return new Color(
                Integer.valueOf( this.mainColor.substring( 1, 3 ), 16 ),
                Integer.valueOf( this.mainColor.substring( 3, 5 ), 16 ),
                Integer.valueOf( this.mainColor.substring( 5, 7 ), 16 )
        );
    }
}

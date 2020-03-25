package heartbeat.tasks;

import api.wynn.WynnApi;
import api.wynn.structs.GuildList;
import api.wynn.structs.WynnGuild;
import app.Bot;
import db.model.guild.Guild;
import db.model.timezone.CustomTimeZone;
import db.model.track.TrackChannel;
import db.model.track.TrackType;
import db.repository.base.DateFormatRepository;
import db.repository.base.GuildRepository;
import db.repository.base.TimeZoneRepository;
import db.repository.base.TrackChannelRepository;
import heartbeat.base.TaskBase;
import log.Logger;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GuildTracker implements TaskBase {
    private final Logger logger;
    private final ShardManager manager;
    private final WynnApi wynnApi;
    private final GuildRepository guildRepository;
    private final TrackChannelRepository trackChannelRepository;
    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;

    public GuildTracker(Bot bot) {
        this.logger = bot.getLogger();
        this.manager = bot.getManager();
        this.wynnApi = new WynnApi(bot.getLogger(), bot.getProperties().wynnTimeZone);
        this.guildRepository = bot.getDatabase().getGuildRepository();
        this.trackChannelRepository = bot.getDatabase().getTrackingChannelRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
    }

    private static final long GUILD_TRACKER_DELAY = TimeUnit.HOURS.toMillis(1);
    private static final long GUILD_STATS_RETRIEVAL_DELAY = TimeUnit.SECONDS.toMillis(5);

    @Override
    public long getFirstDelay() {
        return TimeUnit.MINUTES.toMillis(1);
    }

    @Override
    public long getInterval() {
        return GUILD_TRACKER_DELAY;
    }

    @NotNull
    @Override
    public String getName() {
        return "Guild Tracker";
    }

    @Override
    public void run() {
        GuildList guildList = this.wynnApi.mustGetGuildList();
        if (guildList == null) {
            return;
        }
        Set<String> retrievedGuildNames = new HashSet<>(guildList.getGuilds());

        List<Guild> guildsInDb = this.guildRepository.findAll();
        if (guildsInDb == null) {
            return;
        }
        Map<String, Guild> guildNamesInDb = guildsInDb.stream().collect(Collectors.toMap(Guild::getName, g -> g));

        for (String retrievedGuildName : retrievedGuildNames) {
            if (!guildNamesInDb.containsKey(retrievedGuildName)) {
                // Guild created
                handleGuildCreation(retrievedGuildName);
                // Sleep so it doesn't spam Wynn API
                try {
                    Thread.sleep(GUILD_STATS_RETRIEVAL_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        for (String existingGuildName : guildNamesInDb.keySet()) {
            if (!retrievedGuildNames.contains(existingGuildName)) {
                // Guild deleted
                handleGuildDeletion(existingGuildName);
            }
        }
    }

    /**
     * Handles guild creation.
     * <br>1. Tries to retrieve guild stats. If impossible to do so, skip handling.
     * <br>2. Inserts guild (prefix) data into DB.
     * <br>3. Sends tracking.
     * @param guildName Guild name.
     */
    private void handleGuildCreation(String guildName) {
        // Retrieve guild stats
        WynnGuild guild = this.wynnApi.mustGetGuildStats(guildName);
        if (guild == null) {
            return;
        }

        // Insert guild data into DB
        boolean res = this.guildRepository.create(
                new Guild(guildName, guild.getPrefix(), guild.getCreated())
        );
        if (!res) {
            return;
        }

        // Tracking
        List<TrackChannel> trackChannels = this.trackChannelRepository.findAllOfType(TrackType.GUILD_CREATE);
        if (trackChannels == null) {
            return;
        }

        String messageBase1 = String.format("Guild `%s` `[%s]` created.",
                guildName, guild.getPrefix()
        );
        String ownerName = guild.getOwnerName();
        String messageBase2 = String.format("    Owner `%s`, `%s` members",
                ownerName != null ? ownerName : "(Unknown owner)",
                guild.getMembers().size()
        );

        for (TrackChannel trackChannel : trackChannels) {
            DateFormat dateFormat = this.dateFormatRepository.getDateFormat(
                    trackChannel.getGuildId(),
                    trackChannel.getChannelId()
            ).getDateFormat().getSecondFormat();
            CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(
                    trackChannel.getGuildId(),
                    trackChannel.getChannelId()
            );
            dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

            String message = String.format(
                    "%s\n    Created At: `%s` (%s)\n%s",
                    messageBase1,
                    dateFormat.format(guild.getCreated()), customTimeZone.getFormattedTime(),
                    messageBase2
            );

            TextChannel channel = this.manager.getTextChannelById(trackChannel.getChannelId());
            if (channel == null) {
                continue;
            }
            channel.sendMessage(message).queue();
        }
    }

    /**
     * Handles guild deletion.
     * <br>1. Deletes guild (prefix) data from DB.
     * <br>2. Sends tracking.
     * @param guildName Guild name.
     */
    private void handleGuildDeletion(String guildName) {
        Guild guild = this.guildRepository.findOne(() -> guildName);
        if (guild == null) {
            this.logger.log(0, "Guild Tracker: Failed to retrieve guild from db");
            return;
        }
        boolean res = this.guildRepository.delete(guild);
        if (!res) {
            return;
        }

        List<TrackChannel> trackChannels = this.trackChannelRepository.findAllOfType(TrackType.GUILD_DELETE);
        if (trackChannels == null) {
            return;
        }

        String message = String.format("Guild `%s` `[%s]` deleted.", guildName, guild.getPrefix());
        for (TrackChannel trackChannel : trackChannels) {
            TextChannel channel = this.manager.getTextChannelById(trackChannel.getChannelId());
            if (channel == null) {
                continue;
            }
            channel.sendMessage(message).queue();
        }
    }
}

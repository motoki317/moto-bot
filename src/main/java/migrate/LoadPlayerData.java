package migrate;

import app.Bot;
import commands.base.GenericCommand;
import db.model.dateFormat.CustomDateFormat;
import db.model.dateFormat.CustomFormat;
import db.model.guildList.GuildListEntry;
import db.model.ignoreChannel.IgnoreChannel;
import db.model.musicSetting.MusicSetting;
import db.model.prefix.Prefix;
import db.model.serverLog.ServerLogEntry;
import db.model.territoryList.TerritoryListEntry;
import db.model.timezone.CustomTimeZone;
import db.model.track.TrackChannel;
import db.model.track.TrackType;
import db.repository.base.*;
import log.Logger;
import music.RepeatState;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.motobot.DiscordBot.PlayerData;
import net.motobot.music.MusicSettings;
import net.motobot.wrapper.CustomTimeFormat;
import net.motobot.wrapper.TrackEntry;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class LoadPlayerData extends GenericCommand {
    private final ShardManager manager;
    private final Logger logger;
    private final TrackChannelRepository trackChannelRepository;
    private final TimeZoneRepository timeZoneRepository;
    private final PrefixRepository prefixRepository;
    private final DateFormatRepository dateFormatRepository;
    private final IgnoreChannelRepository ignoreChannelRepository;
    private final TerritoryListRepository territoryListRepository;
    private final GuildListRepository guildListRepository;
    private final ServerLogRepository serverLogRepository;
    private final MusicSettingRepository musicSettingRepository;

    public LoadPlayerData(Bot bot) {
        this.manager = bot.getManager();
        this.logger = bot.getLogger();
        this.trackChannelRepository = bot.getDatabase().getTrackingChannelRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.prefixRepository = bot.getDatabase().getPrefixRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.ignoreChannelRepository = bot.getDatabase().getIgnoreChannelRepository();
        this.territoryListRepository = bot.getDatabase().getTerritoryListRepository();
        this.guildListRepository = bot.getDatabase().getGuildListRepository();
        this.serverLogRepository = bot.getDatabase().getServerLogRepository();
        this.musicSettingRepository = bot.getDatabase().getMusicSettingRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"migrate"}, {"playerData"}};
    }

    @Override
    public @NotNull String syntax() {
        return "migrate playerData";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Migrate player data.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(this.shortHelp()).build();
    }

    @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        File file = new File("data/PlayerData");
        PlayerData data;
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fis);
            data = (PlayerData) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            respondError(event, "an exception occurred while opening file");
            return;
        }

        if (args.length <= 2) {
            // debug
            // to add data to db
            respond(event, "" + data);
            return;
        }

        switch (args[2]) {
            case "tracking":
                migrateTracking(data);
                respond(event, "Successfully migrated track channels!");
                return;
            case "all":
                migrateTimezone(data);
                respond(event, "Successfully migrated timezones!");
                migratePrefix(data);
                respond(event, "Successfully migrated prefixes!");
                migrateTimeFormat(data);
                respond(event, "Successfully migrated date formats!");
                migrateIgnoreChannels(data);
                respond(event, "Successfully migrated ignore channels!");
                migrateTerritoryList(data);
                respond(event, "Successfully migrated territory lists!");
                migrateGuildList(data);
                respond(event, "Successfully migrated guild lists!");
                migrateServerLog(data);
                respond(event, "Successfully migrated server log channels!");
                migrateMusicSettings(data);
                respond(event, "Successfully migrated music settings!");
                return;
        }

        respondException(event, "Unknown operation.");
    }

    private void migrateTracking(PlayerData data) {
        long now = System.currentTimeMillis();

        for (TrackEntry e : data.trackings) {
            TextChannel channel = this.manager.getTextChannelById(e.getChannelId());
            if (channel == null) {
                this.logger.log(0,
                        String.format("Failed to get text channel for ID: %d, skipping", e.getChannelId()));
                continue;
            }

            TrackType newType = TrackType.valueOf(e.getType().name());
            Date expireDate = new Date(now + newType.getDefaultExpireTime());

            TrackChannel newEntry = new TrackChannel(
                    newType,
                    channel.getGuild().getIdLong(),
                    e.getChannelId(),
                    // user id unknown
                    0L,
                    expireDate
            );
            boolean res = this.trackChannelRepository.create(newEntry);
            if (!res) {
                throw new RuntimeException("Failed to migrate track channels");
            }
        }
    }

    private void migrateTimezone(PlayerData data) {
        List<CustomTimeZone> toAdd = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : data.guildTimeZones.entrySet()) {
            long id = entry.getKey();
            int offset = entry.getValue();
            toAdd.add(new CustomTimeZone(id, String.format("GMT%+d", offset)));
        }
        for (Map.Entry<Long, Integer> entry : data.channelTimeZones.entrySet()) {
            long id = entry.getKey();
            int offset = entry.getValue();
            toAdd.add(new CustomTimeZone(id, String.format("GMT%+d", offset)));
        }
        for (Map.Entry<Long, Integer> entry : data.userTimeZones.entrySet()) {
            long id = entry.getKey();
            int offset = entry.getValue();
            toAdd.add(new CustomTimeZone(id, String.format("GMT%+d", offset)));
        }

        for (CustomTimeZone customTimeZone : toAdd) {
            boolean res = this.timeZoneRepository.create(customTimeZone);
            if (!res) {
                throw new RuntimeException("Failed to migrate timezones");
            }
        }
    }

    private void migratePrefix(PlayerData data) {
        List<Prefix> toAdd = new ArrayList<>();

        for (Map.Entry<Long, String> entry : data.guildPrefixes.entrySet()) {
            long id = entry.getKey();
            String prefix = entry.getValue();
            toAdd.add(new Prefix(id, prefix));
        }
        for (Map.Entry<Long, String> entry : data.channelPrefixes.entrySet()) {
            long id = entry.getKey();
            String prefix = entry.getValue();
            toAdd.add(new Prefix(id, prefix));
        }
        for (Map.Entry<Long, String> entry : data.userPrefixes.entrySet()) {
            long id = entry.getKey();
            String prefix = entry.getValue();
            toAdd.add(new Prefix(id, prefix));
        }

        for (Prefix prefix : toAdd) {
            boolean res = this.prefixRepository.create(prefix);
            if (!res) {
                throw new RuntimeException("Failed to migrate prefixes");
            }
        }
    }

    private CustomFormat migrateDateFormat(CustomTimeFormat oldFormat) {
        switch (oldFormat) {
            case TWELVE_HOUR:
                return CustomFormat.TWELVE_HOUR;
            case TWENTYFOUR_HOUR:
                return CustomFormat.TWENTY_FOUR_HOUR;
            default:
                throw new RuntimeException("Unknown format");
        }
    }

    private void migrateTimeFormat(PlayerData data) {
        List<CustomDateFormat> toAdd = new ArrayList<>();

        for (Map.Entry<Long, CustomTimeFormat> entry : data.guildTimeFormats.entrySet()) {
            long id = entry.getKey();
            CustomDateFormat dateFormat = new CustomDateFormat(id, migrateDateFormat(entry.getValue()));
            toAdd.add(dateFormat);
        }
        for (Map.Entry<Long, CustomTimeFormat> entry : data.channelTimeFormats.entrySet()) {
            long id = entry.getKey();
            CustomDateFormat dateFormat = new CustomDateFormat(id, migrateDateFormat(entry.getValue()));
            toAdd.add(dateFormat);
        }
        for (Map.Entry<Long, CustomTimeFormat> entry : data.userTimeFormats.entrySet()) {
            long id = entry.getKey();
            CustomDateFormat dateFormat = new CustomDateFormat(id, migrateDateFormat(entry.getValue()));
            toAdd.add(dateFormat);
        }

        for (CustomDateFormat customDateFormat : toAdd) {
            boolean res = this.dateFormatRepository.create(customDateFormat);
            if (!res) {
                throw new RuntimeException("Failed to migrate date formats");
            }
        }
    }

    private void migrateIgnoreChannels(PlayerData data) {
        for (long channelId : data.ignoredChannels) {
            IgnoreChannel channel = new IgnoreChannel(channelId);
            boolean res = this.ignoreChannelRepository.create(channel);
            if (!res) {
                throw new RuntimeException("Failed to migrate ignore channels");
            }
        }
    }

    private void migrateTerritoryList(PlayerData data) {
        List<TerritoryListEntry> toAdd = new ArrayList<>();
        for (Map.Entry<Long, Map<String, List<String>>> entry : data.customTerrList.entrySet()) {
            long userId = entry.getKey();
            Map<String, List<String>> lists = entry.getValue();
            for (Map.Entry<String, List<String>> list : lists.entrySet()) {
                String listName = list.getKey();
                List<String> territories = list.getValue();

                for (String territory : territories) {
                    toAdd.add(new TerritoryListEntry(
                            userId, listName, territory
                    ));
                }
            }
        }

        for (TerritoryListEntry t : toAdd) {
            boolean res = this.territoryListRepository.create(t);
            if (!res) {
                throw new RuntimeException("Failed to migrate custom territory lists");
            }
        }
    }

    private void migrateGuildList(PlayerData data) {
        List<GuildListEntry> toAdd = new ArrayList<>();

        for (Map.Entry<Long, Map<String, List<String>>> entry : data.customGuildList.entrySet()) {
            long userId = entry.getKey();
            Map<String, List<String>> lists = entry.getValue();
            for (Map.Entry<String, List<String>> list : lists.entrySet()) {
                String listName = list.getKey();
                List<String> guilds = list.getValue();

                for (String guild : guilds) {
                    toAdd.add(new GuildListEntry(
                            userId, listName, guild
                    ));
                }
            }
        }

        for (GuildListEntry g : toAdd) {
            boolean res = this.guildListRepository.create(g);
            if (!res) {
                throw new RuntimeException("Failed to migrate custom guild lists");
            }
        }
    }

    private void migrateServerLog(PlayerData data) {
        for (Map.Entry<Long, Long> entry : data.serverLogChannels.entrySet()) {
            long guildId = entry.getKey();
            long channelId = entry.getValue();

            boolean res = this.serverLogRepository.create(new ServerLogEntry(
                    guildId, channelId
            ));
            if (!res) {
                throw new RuntimeException("Failed to migrate server log channels");
            }
        }
    }

    private static MusicSetting migrateMusicSetting(long guildId, MusicSettings s) {
        return new MusicSetting(
                guildId,
                s.getVolume(),
                RepeatState.valueOf(s.getRepeat().name()),
                s.isShowNp(),
                s.getRestrictChannel() != 0 ? s.getRestrictChannel() : null
        );
    }

    private void migrateMusicSettings(PlayerData data) {
        for (Map.Entry<Long, MusicSettings> entry : data.musicSettings.entrySet()) {
            Long guildId = entry.getKey();
            MusicSettings settings = entry.getValue();
            MusicSetting newSettings = migrateMusicSetting(guildId, settings);
            boolean res;
            if (this.musicSettingRepository.exists(() -> guildId)) {
                res = this.musicSettingRepository.update(newSettings);
            } else {
                res = this.musicSettingRepository.create(newSettings);
            }
            if (!res) {
                throw new RuntimeException("Failed to migrate music settings");
            }
        }
    }
}

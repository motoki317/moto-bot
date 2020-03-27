package commands;

import api.mojang.MojangApi;
import app.Bot;
import commands.base.GuildCommand;
import db.Database;
import db.model.dateFormat.CustomDateFormat;
import db.model.timezone.CustomTimeZone;
import db.model.track.TrackChannel;
import db.model.track.TrackType;
import db.repository.base.DateFormatRepository;
import db.repository.base.TimeZoneRepository;
import db.repository.base.TrackChannelRepository;
import log.Logger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import utils.UUID;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Track extends GuildCommand {
    private final Logger logger;
    private final TrackChannelRepository trackChannelRepository;
    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;
    private final MojangApi mojangApi;

    public Track(Bot bot) {
        this.logger = bot.getLogger();
        Database db = bot.getDatabase();
        this.trackChannelRepository = db.getTrackingChannelRepository();
        this.dateFormatRepository = db.getDateFormatRepository();
        this.timeZoneRepository = db.getTimeZoneRepository();
        this.mojangApi = new MojangApi(bot.getLogger());
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"track"}};
    }

    @NotNull
    @Override
    public String syntax() {
        return "track [server|guild|war|territory|update|refresh]";
    }

    @NotNull
    @Override
    public String shortHelp() {
        return "Manages server, war, and territory tracking.";
    }

    @NotNull
    @Override
    public Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("Track Command Help")
                .setDescription("Running this command in a guild channel will make the bot to send messages when a certain track event occurs. " +
                        "Type the same command to enable/disable tracking.\n" +
                        "Each tracking option has default expiration time, so you will either have to reset the " +
                        "tracking or use `track <update|refresh>` command to update the expiration time.")
                .addField("Syntax",
                        String.join("\n",
                                "`" + this.syntax() + "`",
                                "First argument specifies the type of action. " +
                                        "Use without arguments to view all tracking enabled in this channel."
                                ),
                        false)
                .addField("Server Tracking Syntax",
                        String.join("\n",
                                "`track server <start|close> [all]`",
                                "`<start|close>` specifies when to send message.",
                                "\"start\" : When a server starts",
                                "\"close\" : When a server closes.",
                                "`[all]` : On which server start/close the bot should send messages.",
                                "Without all argument, the bot sends messages only when main servers (WC and EU) start/close.",
                                "With all argument, the bot sends messages when every server, but excluding WAR servers, start/close."
                                ),
                        false)
                .addField("Guild Creation/Deletion Tracking Syntax",
                        String.join("\n",
                                "`track guild <create|delete>`",
                                "Guild tracking will send a message when a guild is created or deleted."
                        ),
                        false)
                .addField("War Tracking Syntax",
                        String.join("\n",
                        "`track war <all|guild|player> [name]`",
                                "`<all|guild|player>` specifies the war track type.",
                                "\"all\" : When any guild starts a war.",
                                "\"guild\" : When a specified guild starts a war.",
                                "\"player\" : When a specified player starts a war.",
                                "`[name]` : Must be specified if you choose guild or player track type."
                        ),
                        false)
                .addField("Territory Tracking Syntax",
                        String.join("\n",
                                "`track territory <all|guild> [name]`",
                                "`<all|guild> specifies the territory track type.`",
                                "\"all\" : When any guild acquires a territory.",
                                "\"guild\" : When a specified guild acquires a territory.",
                                "`[name]` : Must be specified if you choose guild track type."
                                ),
                        false)
                .addField("Update the Expiration Time",
                        String.join("\n",
                        "Type `track <update|refresh>` to update expiration time for all tracking enabled in " +
                                "this channel."),
                        false)
                .addField("Examples",
                        String.join("\n",
                                "`track server close all`",
                                "`track server start`",
                                "`track war guild WynnContentTeam`",
                                "`track territory all`"
                                ),
                        false)
                .build()
        ).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            sendCurrentTrackingMessage(event);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "update":
            case "refresh":
                if (refreshTracking(event)) {
                    respond(event, ":white_check_mark: Successfully refreshed expiration time!");
                }
                return;
        }

        TrackType type = getCorrespondingTrackType(args);
        if (type == null) {
            respond(event, "Invalid arguments. Please refer to help.");
            return;
        }

        Date expiresAt = new Date(System.currentTimeMillis() + type.getDefaultExpireTime());
        TrackChannel entity = new TrackChannel(
                type, event.getGuild().getIdLong(), event.getChannel().getIdLong(),
                event.getAuthor().getIdLong(), expiresAt
        );

        switch (type) {
            case WAR_SPECIFIC:
            case TERRITORY_SPECIFIC:
                String guildName = getName(args);
                if (guildName == null) {
                    respond(event, "Invalid arguments: guild name was not specified.");
                    return;
                }

                entity.setGuildName(guildName);
                break;
            case WAR_PLAYER:
                String playerName = getName(args);
                if (playerName == null) {
                    respond(event, "Invalid arguments: player name was not specified.");
                    return;
                }

                UUID uuid = this.mojangApi.mustGetUUIDAtTime(playerName, new Date().getTime());
                if (uuid == null) {
                    respond(event, String.format("Failed to retrieve player UUID for `%s`... " +
                            "Check the spelling, and make sure the player with that name exists.", playerName));
                    return;
                }

                entity.setPlayerUUID(uuid.toStringWithHyphens());
                break;
        }

        if (this.trackChannelRepository.exists(entity)) {
            // Disable tracking
            if (this.trackChannelRepository.delete(entity)) {
                respond(event, ":mute: Successfully **disabled** " + entity.getDisplayName() + " for this channel.");
                this.logger.log(0, ":mute: Tracking has been **disabled**:\n" + entity.toString());
            } else {
                respondError(event, "Something went wrong while saving data.");
            }
        } else {
            // Enable tracking

            // Check conflicting types
            List<TrackChannel> conflicting = getConflictingEntities(type, event);
            if (conflicting == null) {
                respondError(event, "Something went wrong while retrieving data.");
                return;
            }
            if (!conflicting.isEmpty()) {
                String message = "You have conflicting type of tracking enabled in this channel to enable the one you just specified.\n" +
                        "Below is the list of that.\n";
                message += conflicting.stream().map(TrackChannel::getDisplayName).collect(Collectors.joining("\n"));
                respond(event, message);
                return;
            }

            if (this.trackChannelRepository.create(entity)) {
                respond(event, ":loud_sound: Successfully **enabled** " + entity.getDisplayName() + " for this channel!");
                this.logger.log(0, ":loud_sound: Tracking has been **enabled**:\n" + entity.toString());
            } else {
                respondError(event, "Something went wrong while saving data.");
            }
        }
    }

    /**
     * Formats message to view all tracking enabled in the channel and sends it.
     * @param event Guild Message received event.
     */
    private void sendCurrentTrackingMessage(MessageReceivedEvent event) {
        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannel().getIdLong();
        List<TrackChannel> tracks = this.trackChannelRepository.findAllOf(guildId, channelId);
        if (tracks == null) {
            respondError(event, "Something went wrong while retrieving data...");
            return;
        }

        if (tracks.size() == 0) {
            respond(event, "You do not seem to have any tracking enabled in this channel yet. " +
                    "See help with `track help` for more.");
            return;
        }

        List<String> ret = new ArrayList<>();
        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);
        DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

        ret.add("**List of enabled tracking for this channel**");
        ret.add(String.format("You have %s tracking(s) enabled for this channel.", tracks.size()));
        ret.add("");

        for (TrackChannel track : tracks) {
            ret.add(String.format("`%s` : Expires at `%s` (%s)",
                    track.getDisplayName(),
                    dateFormat.format(track.getExpiresAt()),
                    customTimeZone.getFormattedTime()
            ));
        }

        respond(event, String.join("\n", ret));
    }

    /**
     * Refreshes tracking in the channel.
     * @param event Discord guild message received event.
     * @return {@code true} if success. Else, sends error message.
     */
    private boolean refreshTracking(MessageReceivedEvent event) {
        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannel().getIdLong();
        List<TrackChannel> tracks = this.trackChannelRepository.findAllOf(guildId, channelId);
        if (tracks == null) {
            respondError(event, "Something went wrong while retrieving data...");
            return false;
        }

        if (tracks.isEmpty()) {
            respond(event, "You do not seem to have any tracking enabled in this channel yet. " +
                    "See help with `track help` for more.");
            return false;
        }

        long now = System.currentTimeMillis();
        for (TrackChannel track : tracks) {
            Date newExpirationTime = new Date(now + track.getType().getDefaultExpireTime());
            if (track.getExpiresAt().getTime() < newExpirationTime.getTime()) {
                track.setExpiresAt(newExpirationTime);
            }
            // calls update method n times here, but shouldn't be a big problem...
            if (!this.trackChannelRepository.update(track)) {
                respondError(event, "Something went wrong while updating data...");
                return false;
            }
        }
        return true;
    }

    @Nullable
    private List<TrackChannel> getConflictingEntities(TrackType type, MessageReceivedEvent event) {
        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannel().getIdLong();
        Set<TrackType> conflictTypes = type.getConflictTypes();
        List<TrackChannel> possible = this.trackChannelRepository.findAllOf(guildId, channelId);
        if (possible == null) {
            return null;
        }
        return possible.stream().filter(e -> conflictTypes.contains(e.getType())).collect(Collectors.toList());
    }

    @Nullable
    private static TrackType getCorrespondingTrackType(String[] args) {
        if (args.length <= 2) {
            return null;
        }

        switch (args[1].toLowerCase()) {
            case "server":
                boolean all = false;
                if (args.length > 3) {
                    if ("all".equals(args[3].toLowerCase())) {
                        all = true;
                    }
                }
                switch (args[2].toLowerCase()) {
                    case "start":
                        return all ? TrackType.SERVER_START_ALL : TrackType.SERVER_START;
                    case "close":
                        return all ? TrackType.SERVER_CLOSE_ALL : TrackType.SERVER_CLOSE;
                    default:
                        return null;
                }
            case "guild":
                switch (args[2].toLowerCase()) {
                    case "create":
                        return TrackType.GUILD_CREATE;
                    case "delete":
                        return TrackType.GUILD_DELETE;
                    default:
                        return null;
                }
            case "war":
                switch (args[2].toLowerCase()) {
                    case "all":
                        return TrackType.WAR_ALL;
                    case "guild":
                        return TrackType.WAR_SPECIFIC;
                    case "player":
                        return TrackType.WAR_PLAYER;
                    default:
                        return null;
                }
            case "territory":
                switch (args[2].toLowerCase()) {
                    case "all":
                        return TrackType.TERRITORY_ALL;
                    case "guild":
                        return TrackType.TERRITORY_SPECIFIC;
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    @Nullable
    private static String getName(String[] args) {
        if (args.length <= 3) {
            return null;
        }
        return String.join(" ", Arrays.copyOfRange(args, 3, args.length));
    }
}

package commands;

import api.mojang.MojangApi;
import app.Bot;
import commands.base.GuildCommand;
import commands.guild.GuildNameResolver;
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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import utils.UUID;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Track extends GuildCommand {
    private final Logger logger;
    private final TrackChannelRepository trackChannelRepository;
    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;
    private final MojangApi mojangApi;
    private final GuildNameResolver guildNameResolver;

    public Track(Bot bot) {
        this.logger = bot.getLogger();
        Database db = bot.getDatabase();
        this.trackChannelRepository = db.getTrackingChannelRepository();
        this.dateFormatRepository = db.getDateFormatRepository();
        this.timeZoneRepository = db.getTimeZoneRepository();
        this.mojangApi = new MojangApi(bot.getLogger());
        this.guildNameResolver = new GuildNameResolver(bot.getResponseManager(), db.getGuildRepository());
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

    @NotNull
    @Override
    protected Permission[] getRequiredPermissions() {
        return new Permission[]{Permission.MANAGE_CHANNEL};
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            sendCurrentTrackingMessage(event);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "update", "refresh" -> {
                if (refreshTracking(event)) {
                    respond(event, ":white_check_mark: Successfully refreshed expiration time!");
                }
                return;
            }
        }

        TrackType type = getCorrespondingTrackType(args);
        if (type == null) {
            respond(event, "Invalid arguments. Please refer to help.");
            return;
        }

        // Expiry date = current time + default duration for each track type
        Date expiresAt = new Date(System.currentTimeMillis() + type.getDefaultExpireTime());
        TrackChannel entity = new TrackChannel(
                type, event.getGuild().getIdLong(), event.getChannel().getIdLong(),
                event.getAuthor().getIdLong(), expiresAt
        );

        // Resolve guild name or player UUID
        try {
            resolveGuildOrPlayer(
                    event, args, entity,
                    () -> saveTrackData(event, entity, true),
                    () -> saveTrackData(event, entity, false)
            );
        } catch (IllegalArgumentException e) {
            respondException(event, e.getMessage());
        }
    }

    /**
     * Saves the final track channel entity.
     * @param event Message received event.
     * @param entity Track entity.
     * @param safeGuildResolve {@code true} if the guild name was safely resolved.
     *                         If {@code false}, only tries to delete the existing track.
     */
    private void saveTrackData(MessageReceivedEvent event, TrackChannel entity, boolean safeGuildResolve) {
        if (this.trackChannelRepository.exists(entity)) {
            disableTracking(event, entity);
        } else {
            // Prevent the track from being registered if an unknown guild has been resolved.
            if (!safeGuildResolve) {
                respondException(event, String.format(
                        "Guild with name `%s` not found. Check the spells, or try again later.",
                        entity.getGuildName()
                ));
                return;
            }
            enableTracking(event, entity);
        }
    }

    /**
     * Resolves guild name or player UUID depending on the track type.
     * @param event Message Received event.
     * @param args Command arguments.
     * @param entity Track channel entity.
     * @param onResolve To be called on successful resolve.
     * @param unknownGuildResolved To be called on unknown guild name resolve.
     * @throws IllegalArgumentException On bad user input.
     */
    private void resolveGuildOrPlayer(MessageReceivedEvent event, @NotNull String[] args, TrackChannel entity, Runnable onResolve, Runnable unknownGuildResolved) throws IllegalArgumentException {
        switch (entity.getType()) {
            case WAR_SPECIFIC, TERRITORY_SPECIFIC -> {
                String specified = getName(args);
                if (specified == null) {
                    throw new IllegalArgumentException("Invalid arguments: guild name was not specified.");
                }
                this.guildNameResolver.resolve(
                        specified, event.getTextChannel(), event.getAuthor(),
                        (guildName, prefix) -> {
                            entity.setGuildName(guildName);
                            if (prefix == null) {
                                unknownGuildResolved.run();
                            } else {
                                onResolve.run();
                            }
                        },
                        reason -> respondException(event, reason)
                );
            }
            case WAR_PLAYER -> {
                String playerName = getName(args);
                if (playerName == null) {
                    throw new IllegalArgumentException("Invalid arguments: player name was not specified.");
                }
                UUID uuid = this.mojangApi.mustGetUUIDAtTime(playerName, new Date().getTime());
                if (uuid == null) {
                    throw new IllegalArgumentException(String.format("Failed to retrieve player UUID for `%s`... " +
                            "Check the spelling, and make sure the player with that name exists.", playerName));
                }
                entity.setPlayerUUID(uuid.toStringWithHyphens());
                onResolve.run();
            }
            default -> onResolve.run();
        }
    }

    private void disableTracking(@NotNull MessageReceivedEvent event, TrackChannel entity) {
        if (this.trackChannelRepository.delete(entity)) {
            respond(event, ":mute: Successfully **disabled** " + entity.getDisplayName() + " for this channel.");
            this.logger.log(0, ":mute: Tracking has been **disabled**:\n" + entity);
        } else {
            respondError(event, "Something went wrong while saving data.");
        }
    }

    private void enableTracking(@NotNull MessageReceivedEvent event, TrackChannel entity) {
        // Check conflicting types
        List<TrackChannel> conflicting = getConflictingEntities(entity.getType(), event);
        if (conflicting == null) {
            respondError(event, "Something went wrong while retrieving data.");
            return;
        }
        if (!conflicting.isEmpty()) {
            String message = "You have conflicting type of tracking enabled in this channel." +
                    " Use a different channel, or remove the below before enabling the one you just specified:\n" +
                    conflicting.stream().map(TrackChannel::getDisplayName).collect(Collectors.joining("\n"));
            respond(event, message);
            return;
        }

        if (this.trackChannelRepository.create(entity)) {
            respond(event, ":loud_sound: Successfully **enabled** " + entity.getDisplayName() + " for this channel!");
            this.logger.log(0, ":loud_sound: Tracking has been **enabled**:\n" + entity);
        } else {
            respondError(event, "Something went wrong while saving data.");
        }
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

    @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
    @Nullable
    private static TrackType getCorrespondingTrackType(String[] args) {
        if (args.length <= 2) {
            return null;
        }

        switch (args[1].toLowerCase()) {
            case "server":
                boolean all = false;
                if (args.length > 3) {
                    if ("all".equalsIgnoreCase(args[3])) {
                        all = true;
                    }
                }
                return switch (args[2].toLowerCase()) {
                    case "start" -> all ? TrackType.SERVER_START_ALL : TrackType.SERVER_START;
                    case "close" -> all ? TrackType.SERVER_CLOSE_ALL : TrackType.SERVER_CLOSE;
                    default -> null;
                };
            case "guild":
                return switch (args[2].toLowerCase()) {
                    case "create" -> TrackType.GUILD_CREATE;
                    case "delete" -> TrackType.GUILD_DELETE;
                    default -> null;
                };
            case "war":
                return switch (args[2].toLowerCase()) {
                    case "all" -> TrackType.WAR_ALL;
                    case "guild" -> TrackType.WAR_SPECIFIC;
                    case "player" -> TrackType.WAR_PLAYER;
                    default -> null;
                };
            case "territory":
                return switch (args[2].toLowerCase()) {
                    case "all" -> TrackType.TERRITORY_ALL;
                    case "guild" -> TrackType.TERRITORY_SPECIFIC;
                    default -> null;
                };
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

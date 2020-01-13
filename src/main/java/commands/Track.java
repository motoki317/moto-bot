package commands;

import commands.base.GuildCommand;
import db.Database;
import db.model.track.TrackChannel;
import db.model.track.TrackType;
import db.repository.TrackChannelRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Track extends GuildCommand {
    private final Database db;

    public Track(Database db) {
        this.db = db;
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"track"}};
    }

    @NotNull
    @Override
    public String syntax() {
        return "track <server|guild|war|territory>";
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
                        "Type the same command to enable/disable tracking.")
                .addField("Syntax",
                        String.join("\n",
                                "`" + this.syntax() + "`",
                                "First argument specifies the type of tracking."
                                ),
                        false)
                .addField("Server Tracking Syntax",
                        String.join("\n",
                                "`track server <start|close> [all]`",
                                "`<start|close>` specifies when to send message.",
                                "\"start\" sends message when a server starts",
                                "\"close\" sends message when a server closes.",
                                "`[all]` optional argument specifies on which server start/close the bot should send messages.",
                                "Without all argument, the bot sends messages only when main servers (WC and EU) start/close.",
                                "With all argument, the bot sends messages when every server, but excluding WAR servers, starts/closes."
                                ),
                        false)
                .addField("Guild Tracking Syntax",
                        String.join("\n",
                                "`track guild <create|delete>",
                                "Guild tracking will send a message to channel when the bot detects new guild creation, " +
                                        "or deletion.",
                                "`<create|delete>` specifies when to send message."
                        ),
                        false)
                .addField("War Tracking Syntax",
                        String.join("\n",
                        "`track war <all|guild|player> [name]`",
                                "`<all|guild|player>` specifies the war track type.",
                                "\"all\" sends message when any guild starts a war.",
                                "\"guild\" sends message when a specified guild starts a war.",
                                "\"player\" sends message when a specified player starts a war.",
                                "`[name]` optional argument must be specified if you choose guild or player track type."
                        ),
                        false)
                .addField("Territory Tracking Syntax",
                        String.join("\n",
                                "`track territory <all|guild> [name]`",
                                "`<all|guild> specifies the territory track type.`",
                                "\"all\" sends message when any guild acquires a territory.",
                                "\"guild\" sends message when a specified guild acquires a territory.",
                                "`[name]` optional argument must be specified if you choose guild track type."
                                ),
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
        TrackType type = getCorrespondingTrackType(args);
        if (type == null) {
            respond(event, "Invalid arguments. Please refer to help.");
            return;
        }

        TrackChannel entity = new TrackChannel(type, event.getGuild().getIdLong(), event.getChannel().getIdLong());

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

                entity.setPlayerName(playerName);
                break;
        }

        TrackChannelRepository repo = this.db.getTrackingChannelRepository();

        if (repo.exists(entity)) {
            // Disable tracking
            if (repo.delete(entity)) {
                respond(event, ":mute: Successfully **disabled** " + entity.getDisplayName() + " for this channel.");
            } else {
                respondError(event, "Something went wrong while saving data.");
            }
        } else {
            // Enable tracking

            // Check conflicting types
            List<TrackChannel> conflicting = getConflictingEntities(repo, type, event);
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

            if (repo.create(entity)) {
                respond(event, ":loud_sound: Successfully **enabled** " + entity.getDisplayName() + " for this channel!");
            } else {
                respondError(event, "Something went wrong while saving data.");
            }
        }
    }

    @Nullable
    private static List<TrackChannel> getConflictingEntities(TrackChannelRepository repo, TrackType type, MessageReceivedEvent event) {
        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannel().getIdLong();
        Set<TrackType> conflictTypes = type.getConflictTypes();
        List<TrackChannel> possible = repo.findAllOf(guildId, channelId);
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

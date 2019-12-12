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

public class ServerTrack extends GuildCommand {
    private final Database db;

    public ServerTrack(Database db) {
        this.db = db;
    }

    @Override
    public String[] names() {
        return new String[]{"servertrack"};
    }

    @Override
    public String shortHelp() {
        return "Sends messages to a channel when a Wynncraft server starts or closes.";
    }

    @Override
    public Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("Server Track Command Help")
                .setDescription("Running this command in a guild channel will make the bot to send messages when a Wynncraft server starts or closes. " +
                        "Type the same command to enable/disable tracking.")
                .addField("Syntax",
                        "`servertrack <start/close> [all]`\n" +
                                "<start/close> specifies when to send the message. " +
                                "\"start\" means to send the message when a server starts, and " +
                                "\"close\" means to send the message when a server closes.\n" +
                                "[all] optional argument specifies on which server start/close the bot should send the messages. " +
                                "Without all argument, the bot sends messages only when main servers (WC and EU) start/close. " +
                                "With all argument, the bot sends messages when every server, including lobby, YT, and GM, starts/closes.",
                        false)
                .addField("Examples",
                        "`servertrack close`\n" +
                                "`servertrack start all`",
                        false)
                .build()
        ).build();
    }

    @Override
    public void process(MessageReceivedEvent event, String[] args) {
        if (args.length <= 1) {
            respond(event, this.longHelp());
            return;
        }

        boolean startSpecified;
        switch (args[1].toLowerCase()) {
            case "start":
                startSpecified = true;
                break;
            case "close":
                startSpecified = false;
                break;
            default:
                respond(event, this.longHelp());
                return;
        }

        boolean allSpecified = false;
        if (args.length > 2) {
            if ("all".equals(args[2].toLowerCase())) {
                allSpecified = true;
            }
        }

        TrackType type = getCorrespondingTrackType(startSpecified, allSpecified);
        TrackChannelRepository repo = this.db.getTrackingChannelRepository();
        TrackChannel entity = new TrackChannel(type, event.getGuild().getIdLong(), event.getChannel().getIdLong());

        if (repo.exists(entity)) {
            // Disable tracking
            if (repo.delete(entity)) {
                respond(event, "Successfully disabled " + type.getDisplayName() + " for this channel.");
            } else {
                respond(event, "Something went wrong while disabling tracking, please contact the bot owner.");
            }
        } else {
            // Enable tracking
            if (repo.create(entity)) {
                respond(event, "Successfully enabled " + type.getDisplayName() + " for this channel!");
            } else {
                respond(event, "Something went wrong while enabling tracking, please contact the bot owner.");
            }
        }
    }

    private static TrackType getCorrespondingTrackType(boolean start, boolean all) {
        if (start) {
            return all ? TrackType.SERVER_START_ALL : TrackType.SERVER_START;
        } else {
            return all ? TrackType.SERVER_CLOSE_ALL : TrackType.SERVER_CLOSE;
        }
    }
}

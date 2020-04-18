package commands;

import app.Bot;
import commands.base.GenericCommand;
import db.model.timezone.CustomTimeZone;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import utils.BotUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DiscordIdInfo extends GenericCommand {
    private final TimeZoneRepository timeZoneRepository;

    public DiscordIdInfo(Bot bot) {
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"idInfo"}};
    }

    @Override
    public @NotNull String syntax() {
        return "idInfo <discord id>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Given a number discord ID, parse and display its creation time. " +
                "Can be used to extract user / channel / guild creation time.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(this.shortHelp()).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(event, this.longHelp());
            return;
        }

        long id;
        try {
            id = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            respond(event, "Please input a valid number (discord ID).");
            return;
        }

        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

        long createdAt = BotUtils.getIdCreationTime(id);
        respond(event, new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("ID Info")
                .setDescription(String.valueOf(id))
                .addField("ID Created At",
                        String.format("%s (%s)", dateFormat.format(new Date(createdAt)), customTimeZone.getFormattedTime()),
                        false)
                .build()
        ).build());
    }
}

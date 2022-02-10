package commands;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import db.model.timezone.CustomTimeZone;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import utils.BotUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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
    public @NotNull String[] slashName() {
        return new String[]{"idinfo"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.INTEGER, "id", "Discord ID (such as user, channel, guild id)", true)
        };
    }

    @Override
    public @NotNull String syntax() {
        return "idInfo <discord id>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Given a discord ID, parse and display its creation time.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(this.shortHelp() + "\n" +
                "Can be used to extract creation time of ID (and its object).").build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            event.reply(this.longHelp());
            return;
        }

        long id;
        try {
            id = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            event.reply("Please input a valid number (discord ID).");
            return;
        }

        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

        long createdAt = BotUtils.getIdCreationTime(id);
        event.reply(new MessageBuilder(
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

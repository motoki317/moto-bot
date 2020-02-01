package commands;

import commands.base.GenericCommand;
import db.model.dateFormat.CustomDateFormat;
import db.model.dateFormat.CustomFormat;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import utils.MinecraftColor;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Date;

public class DateFormatCmd extends GenericCommand {
    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;

    public DateFormatCmd(DateFormatRepository dateFormatRepository, TimeZoneRepository timeZoneRepository) {
        this.dateFormatRepository = dateFormatRepository;
        this.timeZoneRepository = timeZoneRepository;
    }

    @Override
    protected @NotNull String[][] names() {
        return new String[][]{{"dateFormat", "timeFormat", "dateTimeFormat", "dateTime"}};
    }

    @Override
    public @NotNull String syntax() {
        return "timeformat <12h|24h> [guild|channel|user]";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Configures time format for a guild, a channel or an user, to 12-hour or 24-hour format.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                .setColor(MinecraftColor.BLUE.getColor())
                .setAuthor("Time Format Help")
                .setDescription(this.shortHelp() + " Use without arguments to view current settings.")
                .addField("Syntax",
                        String.join("\n",
                                "`" + this.syntax() + "`",
                                "`<12h|24h>` is a required argument and specifies setting.",
                                "`[guild|channel|user]` is an optional argument and specifies guild, channel or user setting."
                        ),
                        false
                )
                .build()
        ).build();
    }

    private enum Type {
        Guild,
        Channel,
        User;

        private long getDiscordId(MessageReceivedEvent event) {
            switch (this) {
                case Guild:
                    return event.getGuild().getIdLong();
                case Channel:
                    return event.getChannel().getIdLong();
                case User:
                    return event.getAuthor().getIdLong();
                default:
                    return 0L;
            }
        }
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(event, getStatus(event));
            return;
        }

        CustomFormat specified;
        switch (args[1].toLowerCase()) {
            case "12h":
                specified = CustomFormat.TWELVE_HOUR;
                break;
            case "24h":
                specified = CustomFormat.TWENTY_FOUR_HOUR;
                break;
            default:
                respond(event, "Please specify either \"12h\" or \"24h\".");
                return;
        }

        // type
        Type type = Type.Channel;
        if (args.length > 2) {
            switch (args[2].toLowerCase()) {
                case "guild":
                    type = Type.Guild;
                    break;
                case "user":
                    type = Type.User;
                    break;
            }
        }

        // save settings
        CustomDateFormat dateFormat = new CustomDateFormat(type.getDiscordId(event), specified);
        boolean res;
        if (this.dateFormatRepository.exists(dateFormat)) {
            res = this.dateFormatRepository.update(dateFormat);
        } else {
            res = this.dateFormatRepository.create(dateFormat);
        }

        if (res) {
            respond(event, ":white_check_mark: Successfully updated settings!");
        } else {
            respondError(event, "Something went wrong while saving settings...");
        }
    }

    private Message getStatus(MessageReceivedEvent event) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(MinecraftColor.BLUE.getColor())
                .setAuthor("Time Format Settings", null, event.getAuthor().getEffectiveAvatarUrl())
                .setTitle("Current Settings")
                .setDescription("Settings are prioritized in the order of: User (most prioritized) > Channel > Guild > Default (least prioritized).");

        Date now = new Date();
        CustomTimeZone timeZone = this.timeZoneRepository.getTimeZone(event);

        CustomDateFormat defaultFormat = CustomDateFormat.getDefault();
        DateFormat defaultFormatter = defaultFormat.getDateFormat().getSecondFormat();
        defaultFormatter.setTimeZone(timeZone.getTimeZoneInstance());
        eb.addField(
                "Default",
                String.format("%s `%s` (%s)", defaultFormat.getDateFormat().getShortName(),
                        defaultFormatter.format(now), timeZone.getFormattedTime()),
                false
        );

        if (event.isFromGuild()) {
            addField(eb, "Guild", event.getGuild().getIdLong(), now, timeZone);
        }

        addField(eb, "Channel", event.getChannel().getIdLong(), now, timeZone);
        addField(eb, "User", event.getAuthor().getIdLong(), now, timeZone);

        eb.setTimestamp(Instant.ofEpochMilli(now.getTime()));

        return new MessageBuilder(
                eb.build()
        ).build();
    }

    private void addField(EmbedBuilder eb, String name, long id, Date now, CustomTimeZone timeZone) {
        CustomDateFormat customDateFormat = this.dateFormatRepository.findOne(() -> id);
        DateFormat formatter;
        if (customDateFormat != null) {
            formatter = customDateFormat.getDateFormat().getSecondFormat();
        } else {
            formatter = CustomDateFormat.getDefault().getDateFormat().getSecondFormat();
        }
        formatter.setTimeZone(timeZone.getTimeZoneInstance());
        eb.addField(name,
                String.format("%s `%s` (%s) %s",
                        customDateFormat != null
                                ? customDateFormat.getDateFormat().getShortName()
                                : CustomDateFormat.getDefault().getDateFormat().getShortName(),
                        formatter.format(now),
                        timeZone.getFormattedTime(),
                        customDateFormat != null ? "" : "default"),
                false
        );
    }
}

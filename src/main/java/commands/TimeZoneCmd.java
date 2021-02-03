package commands;

import commands.base.GenericCommand;
import db.model.timezone.CustomTimeZone;
import db.model.timezone.CustomTimeZoneId;
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
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeZoneCmd extends GenericCommand {
    private final TimeZoneRepository timeZoneRepository;
    private final DateFormatRepository dateFormatRepository;

    public TimeZoneCmd(TimeZoneRepository timeZoneRepository, DateFormatRepository dateFormatRepository) {
        this.timeZoneRepository = timeZoneRepository;
        this.dateFormatRepository = dateFormatRepository;
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"timezone"}};
    }

    @NotNull
    @Override
    public String syntax() {
        return "timezone <num|timezone|reset> [guild|channel|user]";
    }

    @NotNull
    @Override
    public String shortHelp() {
        return "Sets local timezone for a guild, a channel or an user, for easier view of time stamps.";
    }

    @NotNull
    @Override
    public Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setColor(MinecraftColor.DARK_GREEN.getColor())
                        .setAuthor("TimeZone Command Help")
                        .setDescription(this.shortHelp() + " Use without arguments to view current settings.")
                        .addField("Syntax",
                                String.join("\n",
                                        "`" + this.syntax() + "`",
                                        "`<num|timezone|reset>` argument specifies the timezone.",
                                        "Specify a number between -23 ~ +23 to set offset in hours.",
                                                "Specify a 4-digit number (e.g. \"+0930\") to set a custom minute-wise offset. " +
                                                "You can also specify a timezone name (e.g. \"EST\", \"America/New_York\"), " +
                                                "based on Java-8 TimeZone class.",
                                        "Specify \"reset\" to reset the setting.",
                                        "`[guild|channel|user]` optional argument will set the timezone for guild, channel, or user (yourself). " +
                                                "Default is channel."
                                ),
                                false)
                        .build()
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(event, getStatus(event));
            return;
        }

        Type type = Type.Channel;
        if (args.length >= 3) {
            switch (args[2].toLowerCase()) {
                case "guild":
                    type = Type.Guild;
                    break;
                case "channel":
                    // redundant but just writing it
                    // type = Type.Channel;
                    break;
                case "user":
                    type = Type.User;
                    break;
                default:
                    respond(event, "Specified 2nd argument is invalid: please specify one of `guild`, `channel`, or `user`.");
                    return;
            }
        }

        if (type == Type.Guild && !event.isFromGuild()) {
            respond(event, "You cannot set a guild settings from a DM.");
            return;
        }

        String timezoneId = args[1];

        Matcher m1;
        if ((m1 = custom1.matcher(timezoneId)).matches()) {
            int hours = Integer.parseInt(m1.group(1));
            if (args[1].startsWith("-")) {
                hours *= -1;
            }

            if (hours < -23 || 23 < hours) {
                respond(event, "Specified 1st argument is invalid: please specify a number between -23 ~ +23.");
                return;
            }

            String id = String.format("GMT%+d", hours);
            addSetting(event, id, type);
            return;
        }

        Matcher m2;
        if ((m2 = custom2.matcher(timezoneId)).matches()) {
            String sign = m2.group(1);
            if (sign.isEmpty()) {
                sign = "+";
            }
            int hours = Integer.parseInt(m2.group(2));
            int minutes = Integer.parseInt(m2.group(3));

            if (hours < 0 || 23 < hours
            || minutes < 0 || 59 < minutes) {
                respond(event, "Specified 1st argument is invalid: please specify a 4-digit number indicating " +
                        "24-hour time. e.g. `+0930`");
                return;
            }

            String id = String.format("GMT%s%02d%02d", sign, hours, minutes);
            addSetting(event, id, type);
            return;
        }

        if ("reset".equalsIgnoreCase(timezoneId)) {
            resetSetting(event, type);
            return;
        }

        if (timeZoneExists(timezoneId)) {
            addSetting(event, timezoneId, type);
        } else {
            respond(event, String.format("Couldn't find a timezone matching your input: `%s`...", args[1]));
        }
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

    private static final Pattern custom1 = Pattern.compile("[+-]?(\\d{1,2})");
    private static final Pattern custom2 = Pattern.compile("([+-]?)(\\d{2})(\\d{2})");

    private static boolean timeZoneExists(@NotNull String timezone) {
        for (String availableID : TimeZone.getAvailableIDs()) {
            if (availableID.equals(timezone)) {
                return true;
            }
        }
        return false;
    }

    private void resetSetting(MessageReceivedEvent event, Type type) {
        CustomTimeZoneId id = () -> type.getDiscordId(event);
        boolean success = !this.timeZoneRepository.exists(id) || this.timeZoneRepository.delete(id);
        if (success) {
            respond(event, ":white_check_mark: Successfully reset the setting!");
        } else {
            respondError(event, "Something went wrong while saving your settings...");
        }
    }

    private void addSetting(MessageReceivedEvent event, String timezoneId, Type type) {
        CustomTimeZone customTimeZone = new CustomTimeZone(type.getDiscordId(event), timezoneId);
        boolean success = this.timeZoneRepository.exists(customTimeZone)
                ? this.timeZoneRepository.update(customTimeZone)
                : this.timeZoneRepository.create(customTimeZone);
        if (success) {
            respond(event, ":white_check_mark: Successfully saved your settings!");
        } else {
            respondError(event, "Something went wrong while saving your settings...");
        }
    }

    private Message getStatus(MessageReceivedEvent event) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(MinecraftColor.DARK_GREEN.getColor())
                .setAuthor("TimeZone Settings", null, event.getAuthor().getEffectiveAvatarUrl())
                .setTitle("Current Settings")
                .setDescription("Settings are prioritized in the order of: User (most prioritized) > Channel > Guild > Default (least prioritized).");

        Date now = new Date();
        DateFormat dateFormat = this.dateFormatRepository.getDateFormat(event).getDateFormat().getSecondFormat();
        dateFormat.setTimeZone(TimeZone.getDefault());
        eb.addField(
                "Default",
                String.format("`%s` (+0)", dateFormat.format(now)),
                false
        );

        if (event.isFromGuild()) {
            addField(eb, "Guild", event.getGuild().getIdLong(), now, dateFormat);
        }

        addField(eb, "Channel", event.getChannel().getIdLong(), now, dateFormat);
        addField(eb, "User", event.getAuthor().getIdLong(), now, dateFormat);

        eb.setTimestamp(Instant.ofEpochMilli(now.getTime()));

        return new MessageBuilder(
                eb.build()
        ).build();
    }

    private void addField(EmbedBuilder eb, String name, long id, Date now, DateFormat dateFormat) {
        CustomTimeZone customTimeZone = this.timeZoneRepository.findOne(() -> id);
        if (customTimeZone != null) {
            dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());
        }
        eb.addField(name,
                String.format("`%s` (%s) `%s`",
                        dateFormat.format(now),
                        customTimeZone != null ? customTimeZone.getFormattedTime() : "+0",
                        customTimeZone != null ? customTimeZone.getTimezone() : "default"),
                false
        );
    }
}

package commands;

import commands.base.GenericCommand;
import db.model.prefix.Prefix;
import db.model.prefix.PrefixId;
import db.repository.base.PrefixRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import utils.MinecraftColor;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class PrefixCmd extends GenericCommand {
    private final String defaultPrefix;
    private final PrefixRepository prefixRepository;

    public PrefixCmd(String defaultPrefix, PrefixRepository prefixRepository) {
        this.defaultPrefix = defaultPrefix;
        this.prefixRepository = prefixRepository;
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"prefix"}};
    }

    @Override
    public @NotNull String syntax() {
        return "prefix <new prefix> [guild|channel|user]";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Sets new prefix for bot commands.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setColor(MinecraftColor.DARK_GREEN.getColor())
                        .setAuthor("Prefix Command Help")
                        .setDescription(this.shortHelp())
                        .addField("Syntax",
                                String.join("\n",
                                        "`" + this.syntax() + "`",
                                        "Use without arguments to check the current settings for guild/channel/user.",
                                        "`<new prefix|reset>` argument specifies the new prefix. " +
                                                "Prefix should be as short as possible and no longer than " + PREFIX_MAX_LENGTH + " characters.",
                                        "Specify \"reset\" to reset the setting.",
                                        "`[guild|channel|user]` optional argument will set the prefix for guild, channel, or user (yourself). " +
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

    private static final int PREFIX_MAX_LENGTH = 10;

    /**
     * Checks if the member has enough permissions to change the given type prefix.
     * @param type Prefix type.
     * @param member Guild member.
     * @return {@code true} if the guild member has enough permissions.
     */
    private static boolean hasPermissions(Type type, @NotNull Member member) {
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        }
        switch (type) {
            case User:
                return true;
            case Channel:
                return member.hasPermission(Permission.MANAGE_CHANNEL);
            case Guild:
                return member.hasPermission(Permission.MANAGE_SERVER);
            default:
                return false;
        }
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

        if (event.isFromGuild()) {
            Member member = event.getMember();
            if (member == null) {
                respondError(event, "Something went wrong while retrieving your permissions...");
                return;
            }
            if (!hasPermissions(type, member)) {
                respond(event, "You do not have enough permissions to change this prefix.");
                return;
            }
        }

        String newPrefix = args[1];
        if ("reset".equalsIgnoreCase(newPrefix)) {
            resetSetting(event, type);
            return;
        }

        if (newPrefix.length() <= PREFIX_MAX_LENGTH) {
            addSetting(event, newPrefix, type);
        } else {
            respond(event, "Given new prefix is too long, a prefix must be shorter than " + PREFIX_MAX_LENGTH + " characters.");
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

    private void resetSetting(MessageReceivedEvent event, Type type) {
        PrefixId id = () -> type.getDiscordId(event);
        boolean success = !this.prefixRepository.exists(id) || this.prefixRepository.delete(id);
        if (success) {
            respond(event, ":white_check_mark: Successfully reset the setting!");
        } else {
            respondError(event, "Something went wrong while saving your settings...");
        }
    }

    private void addSetting(MessageReceivedEvent event, String prefix, Type type) {
        Prefix p = new Prefix(type.getDiscordId(event), prefix);
        boolean success = this.prefixRepository.exists(p)
                ? this.prefixRepository.update(p)
                : this.prefixRepository.create(p);
        if (success) {
            respond(event, ":white_check_mark: Successfully saved your settings!");
        } else {
            respondError(event, "Something went wrong while saving your settings...");
        }
    }

    private Message getStatus(MessageReceivedEvent event) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(MinecraftColor.DARK_GREEN.getColor())
                .setAuthor("Prefix Settings", null, event.getAuthor().getEffectiveAvatarUrl())
                .setTitle("Current Settings")
                .setDescription("Settings are prioritized in the order of: User (most prioritized) > Channel > Guild > Default (least prioritized).");

        eb.addField(
                "Default",
                String.format("`%s`", this.defaultPrefix),
                false
        );

        if (event.isFromGuild()) {
            addField(eb, "Guild", event.getGuild().getIdLong());
        }

        addField(eb, "Channel", event.getChannel().getIdLong());
        addField(eb, "User", event.getAuthor().getIdLong());

        eb.setTimestamp(Instant.now());

        return new MessageBuilder(
                eb.build()
        ).build();
    }

    private void addField(EmbedBuilder eb, String name, long id) {
        Prefix p = this.prefixRepository.findOne(() -> id);
        eb.addField(name,
                String.format("`%s`%s",
                        p != null ? p.getPrefix() : this.defaultPrefix,
                        p != null ? "" : " (default)"),
                false
        );
    }
}

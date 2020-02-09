package listeners;

import app.Bot;
import commands.*;
import commands.base.BotCommand;
import commands.guild.*;
import db.model.commandLog.CommandLog;
import db.model.prefix.Prefix;
import db.repository.base.CommandLogRepository;
import db.repository.base.IgnoreChannelRepository;
import db.repository.base.PrefixRepository;
import log.Logger;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import utils.BotUtils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;

public class MessageListener extends ListenerAdapter {
    private final List<BotCommand> commands;
    private final Map<String, BotCommand> commandNameMap;
    private int maxArgumentsLength;

    private final Bot bot;
    private final Logger logger;
    private final String defaultPrefix;

    private final CommandLogRepository commandLogRepository;
    private final PrefixRepository prefixRepository;
    private final IgnoreChannelRepository ignoreChannelRepository;

    public MessageListener(Bot bot) {
        this.commands = new ArrayList<>();
        this.commandNameMap = new HashMap<>();
        this.maxArgumentsLength = 1;

        this.bot = bot;
        this.logger = bot.getLogger();
        this.defaultPrefix = bot.getProperties().prefix;

        this.commandLogRepository = bot.getDatabase().getCommandLogRepository();
        this.prefixRepository = bot.getDatabase().getPrefixRepository();
        this.ignoreChannelRepository = bot.getDatabase().getIgnoreChannelRepository();

        Consumer<BotCommand> addCommand = (command) -> {
            for (String commandName : command.getNames()) {
                commandName = commandName.toLowerCase();

                if (this.commandNameMap.containsKey(commandName)) {
                    throw new Error("FATAL: Command name conflict: " + commandName + "\n" +
                            "Command " + command.getClass().getName() + " is not being added.");
                }

                this.commandNameMap.put(commandName, command);
            }
            this.commands.add(command);
            this.maxArgumentsLength = Math.max(this.maxArgumentsLength, command.getArgumentsLength());
        };

        addCommand.accept(new Help(bot, this.commands, this.commandNameMap, () -> this.maxArgumentsLength));
        addCommand.accept(new Ping(bot));
        addCommand.accept(new Info(bot));
        addCommand.accept(new ServerList(bot));
        addCommand.accept(new Track(bot));
        addCommand.accept(new TimeZoneCmd(bot.getDatabase().getTimeZoneRepository(), bot.getDatabase().getDateFormatRepository()));
        addCommand.accept(new PrefixCmd(bot.getProperties().prefix, bot.getDatabase().getPrefixRepository()));
        addCommand.accept(new DateFormatCmd(bot.getDatabase().getDateFormatRepository(), bot.getDatabase().getTimeZoneRepository()));
        addCommand.accept(new Ignore(bot.getDatabase().getIgnoreChannelRepository()));
        addCommand.accept(new Find(bot));
        addCommand.accept(new PlayerStats(bot));

        addCommand.accept(new GuildCmd(bot));
        addCommand.accept(new GuildStats(bot));

        addCommand.accept(new GuildRank(bot));
        addCommand.accept(new GainedXpRank(bot));

        addCommand.accept(new CurrentWars(bot));

        addCommand.accept(new GuildWarStats(bot));
        addCommand.accept(new PlayerWarStats(bot));

        addCommand.accept(new PlayerWarLeaderboardCmd(bot));
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        // Do not process if the shard is not loaded
        int shardId = this.bot.getShardId(event.getJDA());
        if (!bot.isConnected(shardId)) return;

        // Do not respond to webhook/bot messages
        if (event.isWebhookMessage() || event.getAuthor().isBot()) return;

        // Ignored channel
        boolean channelIsIgnored = this.ignoreChannelRepository.exists(
                () -> event.getChannel().getIdLong()
        );

        // Check prefix
        Set<String> prefixes = getPrefix(event);
        String rawMessage = event.getMessage().getContentRaw();
        // Repeat for each prefix
        for (String prefix : prefixes) {
            if (!rawMessage.startsWith(prefix)) continue;

            String commandMessage = rawMessage.substring(prefix.length());
            String[] args = commandMessage.split(" ");

            // Do not process command for ignored channel, unless it was ignore command itself
            if (channelIsIgnored && !commandMessage.toLowerCase().startsWith("ignore")) {
                continue;
            }

            // Process command from the most 'specific' (e.g. g pws) to most 'generic' (e.g. guild)
            for (int argLength = this.maxArgumentsLength; argLength > 0; argLength--) {
                if (args.length < argLength) continue;

                String cmdBase = String.join(" ", Arrays.copyOfRange(args, 0, argLength));
                // Command name match
                if (this.commandNameMap.containsKey(cmdBase.toLowerCase())) {
                    BotCommand command = this.commandNameMap.get(cmdBase.toLowerCase());

                    // Check guild-only command
                    if (!event.isFromGuild() && command.guildOnly()) {
                        String message = String.format("This command (%s) cannot be executed in direct messages.", cmdBase);
                        event.getChannel().sendMessage(message).queue();
                        return;
                    }

                    // Log and check spam
                    boolean isSpam = this.logger.logEvent(event);
                    if (isSpam) {
                        String message = "You are requesting commands too quickly! Please wait at least 1 second between each commands.";
                        event.getChannel().sendMessage(message).queue();
                        return;
                    }

                    // If the first argument is "help", then send full help of the command
                    // e.g. "track help"
                    if (argLength < args.length && args[argLength].equalsIgnoreCase("help")) {
                        event.getChannel().sendMessage(command.longHelp()).queue();
                        return;
                    }

                    try {
                        command.process(event, args);
                    } catch (Exception e) {
                        BotCommand.respondError(event, "Something went wrong while processing your command...");
                        this.logger.logException("Something went wrong while processing a user command", e);
                    }

                    addCommandLog(cmdBase, commandMessage, event);
                    return;
                }
            }
        }
    }

    /**
     * Retrieves prefix for the received event.
     * @param event Message received event
     * @return Resolved set of prefixes
     */
    private Set<String> getPrefix(@NotNull MessageReceivedEvent event) {
        Set<String> ret = new HashSet<>();
        ret.add(this.defaultPrefix);
        if (event.isFromGuild()) {
            Prefix guild = this.prefixRepository.findOne(() -> event.getGuild().getIdLong());
            if (guild != null) {
                ret.add(guild.getPrefix());
            }
        }
        Prefix channel = this.prefixRepository.findOne(() -> event.getChannel().getIdLong());
        if (channel != null) {
            ret.add(channel.getPrefix());
        }
        Prefix user = this.prefixRepository.findOne(() -> event.getAuthor().getIdLong());
        if (user != null) {
            ret.add(user.getPrefix());
        }
        return ret;
    }

    /**
     * Adds command log to db.
     */
    private void addCommandLog(String kind, String full, MessageReceivedEvent event) {
        long discordIdTime = BotUtils.getIdCreationTime(event.getMessageIdLong());
        CommandLog entity = new CommandLog(kind, full, event.isFromGuild() ? event.getGuild().getIdLong() : null,
                event.getChannel().getIdLong(), event.getAuthor().getIdLong(), new Date(discordIdTime));
        if (!this.commandLogRepository.create(entity)) {
            this.logger.log(0, "Failed to log command to db.");
        }
    }
}

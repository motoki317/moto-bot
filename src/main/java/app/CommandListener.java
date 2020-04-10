package app;

import commands.*;
import commands.base.BotCommand;
import commands.guild.*;
import commands.guild.leaderboard.GuildWarLeaderboardCmd;
import commands.guild.leaderboard.PlayerWarLeaderboardCmd;
import db.model.commandLog.CommandLog;
import db.model.prefix.Prefix;
import db.repository.base.CommandLogRepository;
import db.repository.base.IgnoreChannelRepository;
import db.repository.base.PrefixRepository;
import log.DiscordSpamChecker;
import log.Logger;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import utils.BotUtils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandListener extends ListenerAdapter {
    private final List<BotCommand> commands;
    private final Map<String, BotCommand> commandNameMap;
    private int maxArgumentsLength;

    private final ExecutorService threadPool;

    private final Bot bot;
    private final Logger logger;
    private final String defaultPrefix;

    private final CommandLogRepository commandLogRepository;
    private final PrefixRepository prefixRepository;
    private final IgnoreChannelRepository ignoreChannelRepository;

    private final DiscordSpamChecker spamChecker;

    CommandListener(Bot bot) {
        this.commands = new ArrayList<>();
        this.commandNameMap = new HashMap<>();
        this.maxArgumentsLength = 1;

        this.threadPool = Executors.newFixedThreadPool(5);

        this.bot = bot;
        this.logger = bot.getLogger();
        this.defaultPrefix = bot.getProperties().prefix;

        this.commandLogRepository = bot.getDatabase().getCommandLogRepository();
        this.prefixRepository = bot.getDatabase().getPrefixRepository();
        this.ignoreChannelRepository = bot.getDatabase().getIgnoreChannelRepository();

        this.spamChecker = new DiscordSpamChecker();

        addCommand(new Help(bot, this.commands, this.commandNameMap, () -> this.maxArgumentsLength));
        addCommand(new Ping(bot));
        addCommand(new Info(bot));
        addCommand(new ServerList(bot));

        addCommand(new Track(bot));
        addCommand(new TimeZoneCmd(bot.getDatabase().getTimeZoneRepository(), bot.getDatabase().getDateFormatRepository()));
        addCommand(new PrefixCmd(bot.getProperties().prefix, bot.getDatabase().getPrefixRepository()));
        addCommand(new DateFormatCmd(bot.getDatabase().getDateFormatRepository(), bot.getDatabase().getTimeZoneRepository()));
        addCommand(new Ignore(bot.getDatabase().getIgnoreChannelRepository()));
        addCommand(new SetPage(bot));

        addCommand(new Find(bot));
        addCommand(new PlayerStats(bot));

        addCommand(new ItemView(bot));

        addCommand(new GuildCmd(bot));
        addCommand(new GuildStats(bot));

        addCommand(new GuildRank(bot));
        addCommand(new GuildLevelRank(bot));
        addCommand(new GainedXpRank(bot));

        addCommand(new TerritoryLogsCmd(bot));

        addCommand(new CurrentWars(bot));

        addCommand(new GuildWarStats(bot));
        addCommand(new PlayerWarStats(bot));

        addCommand(new GuildWarLeaderboardCmd(bot));
        addCommand(new PlayerWarLeaderboardCmd(bot));
    }

    private void addCommand(BotCommand command) {
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
        String prefix = getPrefix(event);
        String rawMessage = event.getMessage().getContentRaw();
        if (!rawMessage.startsWith(prefix)) return;

        String commandMessage = rawMessage.substring(prefix.length());
        String[] args = commandMessage.split(" ");

        // Do not process command for ignored channel, unless it was ignore command itself
        if (channelIsIgnored && !commandMessage.toLowerCase().startsWith("ignore")) {
            return;
        }

        // Process command from the most 'specific' (e.g. g pws) to most 'generic' (e.g. guild)
        for (int argLength = Math.min(this.maxArgumentsLength, args.length); argLength > 0; argLength--) {
            String cmdBase = String.join(" ", Arrays.copyOfRange(args, 0, argLength));
            // Command name match
            if (this.commandNameMap.containsKey(cmdBase.toLowerCase())) {
                int finalArgLength = argLength;
                this.threadPool.execute(() -> processCommand(event, commandMessage, args, finalArgLength, cmdBase));
                return;
            }
        }
    }

    private void processCommand(@Nonnull MessageReceivedEvent event, String commandMessage, String[] args, int argLength, String cmdBase) {
        BotCommand command = this.commandNameMap.get(cmdBase.toLowerCase());

        // Check guild-only command
        if (!event.isFromGuild() && command.guildOnly()) {
            String message = String.format("This command (%s) cannot be executed in direct messages.", cmdBase);
            event.getChannel().sendMessage(message).queue();
            return;
        }

        // Check spam and log event
        boolean isSpam = this.spamChecker.isSpam(event);
        this.logger.logEvent(event, isSpam);
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

        // Process command
        event.getChannel().sendTyping().queue();
        try {
            command.process(event, args);
        } catch (Throwable e) {
            BotCommand.respondError(event, "Something went wrong while processing your command...");
            this.logger.logException("Something went wrong while processing a user command", e);
        }

        addCommandLog(cmdBase, commandMessage, event);
    }

    /**
     * Retrieves the prefix for the received event with the highest priority:<ol>
     *     <li>user</li>
     *     <li>channel</li>
     *     <li>guild</li>
     *     <li>default</li>
     * </ol>
     * @param event Message received event
     * @return Resolved prefix with the highest priority found.
     */
    private String getPrefix(@NotNull MessageReceivedEvent event) {
        Prefix user = this.prefixRepository.findOne(() -> event.getAuthor().getIdLong());
        if (user != null) {
            return user.getPrefix();
        }
        Prefix channel = this.prefixRepository.findOne(() -> event.getChannel().getIdLong());
        if (channel != null) {
            return channel.getPrefix();
        }
        if (event.isFromGuild()) {
            Prefix guild = this.prefixRepository.findOne(() -> event.getGuild().getIdLong());
            if (guild != null) {
                return guild.getPrefix();
            }
        }
        return this.defaultPrefix;
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

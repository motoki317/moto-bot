package app;

import commands.*;
import commands.base.BotCommand;
import commands.event.CommandEvent;
import commands.event.MessageReceivedEventAdapter;
import commands.event.SlashCommandEventAdapter;
import commands.guild.*;
import commands.guild.leaderboard.GuildWarLeaderboardCmd;
import commands.guild.leaderboard.PlayerWarLeaderboardCmd;
import db.model.commandLog.CommandLog;
import db.model.prefix.Prefix;
import db.repository.base.CommandLogRepository;
import db.repository.base.IgnoreChannelRepository;
import db.repository.base.PrefixRepository;
import io.prometheus.client.Counter;
import log.DiscordSpamChecker;
import log.Logger;
import music.Music;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import utils.MinecraftColor;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CommandListener extends ListenerAdapter {
    private static final Counter COMMANDS_COUNTER = Counter.build()
            .name("moto_bot_commands")
            .help("Counts of moto-bot command usage per command kind.")
            .labelNames("kind")
            .register();

    private final Bot bot;

    private final CommandComplex commands;

    private final ExecutorService threadPool;

    private final Logger logger;
    private final String defaultPrefix;

    private final CommandLogRepository commandLogRepository;
    private final PrefixRepository prefixRepository;
    private final IgnoreChannelRepository ignoreChannelRepository;

    private final DiscordSpamChecker spamChecker;

    CommandListener(Bot bot) {
        this.bot = bot;

        this.threadPool = Executors.newFixedThreadPool(5);

        this.logger = bot.getLogger();
        this.defaultPrefix = bot.getProperties().prefix;

        this.commandLogRepository = bot.getDatabase().getCommandLogRepository();
        this.prefixRepository = bot.getDatabase().getPrefixRepository();
        this.ignoreChannelRepository = bot.getDatabase().getIgnoreChannelRepository();

        this.spamChecker = new DiscordSpamChecker();

        this.commands = registerCommands(bot);
    }

    @SuppressWarnings({"OverlyLongMethod", "OverlyCoupledMethod"})
    private CommandComplex registerCommands(Bot bot) {
        CommandComplex.Builder b = new CommandComplex.Builder(bot);

        b.addCommandDescription("g", "Guild related commands.");

        b.addCommand(new Help(bot, () -> this.commands));
        b.addCommand(new CommandAliases(() -> this.commands));
        b.addCommand(new Ping(bot));
        b.addCommand(new Info(bot));
        b.addCommand(new ServerList(bot));

        b.addCommand(new Track(bot));
        b.addCommand(new TimeZoneCmd(bot.getDatabase().getTimeZoneRepository(), bot.getDatabase().getDateFormatRepository()));
        b.addCommand(new PrefixCmd(bot.getProperties().prefix, bot.getDatabase().getPrefixRepository()));
        b.addCommand(new DateFormatCmd(bot.getDatabase().getDateFormatRepository(), bot.getDatabase().getTimeZoneRepository()));
        b.addCommand(new Ignore(bot.getDatabase().getIgnoreChannelRepository()));
        b.addCommand(new SetPage(bot));

        b.addCommand(new CatCmd(bot));
        b.addCommand(new DiscordIdInfo(bot));
        b.addCommand(new Purge());
        b.addCommand(new ServerLogCmd(bot));

        b.addCommand(new Find(bot));
        b.addCommand(new PlayerStats(bot));
        b.addCommand(new NameHistoryCmd(bot));

        b.addCommand(new ItemView(bot));
        b.addCommand(new IdentifyItem(bot));

        b.addCommand(new GuildCmd(bot));
        b.addCommand(new GuildStats(bot));

        b.addCommand(new GuildRank(bot));
        b.addCommand(new GuildLevelRank(bot));
        b.addCommand(new GainedXpRank(bot));

        b.addCommand(new TerritoryLogsCmd(bot));
        b.addCommand(new TerritoryListCmd(bot));
        b.addCommand(new TerritoryActivityCmd(bot));

        b.addCommand(new CurrentWars(bot));

        b.addCommand(new GuildWarStats(bot));
        b.addCommand(new PlayerWarStats(bot));

        b.addCommand(new GuildWarLeaderboardCmd(bot));
        b.addCommand(new PlayerWarLeaderboardCmd(bot));

        b.addCommand(new CustomTerritoryListCmd(bot));
        b.addCommand(new CustomGuildListCmd(bot));

        b.addCommand(new Music(bot));

        return b.build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        // Do not process if JDA is disconnected from WS
        if (!this.bot.isConnected(this.bot.getShardId(event.getJDA()))) return;

        // Do not respond to bot messages
        if (event.getUser().isBot()) return;

        // Do not respond if the bot cannot talk in the (text) channel
        // DMs are always okay
        if (event.isFromGuild() && !event.getTextChannel().canTalk()) {
            return;
        }

        String rawMessage = event.getCommandString();
        String commandMessage = rawMessage.substring(1); // strip "/" prefix
        String[] args = commandMessage.split("\\s+");

        CommandComplex.Result res = this.commands.getCommand(args);
        if (res == null) {
            return;
        }

        this.threadPool.execute(() -> {
            processCommand(new SlashCommandEventAdapter(event, this.bot), res, args);
            addCommandLog(res.base(), commandMessage, new SlashCommandEventAdapter(event, this.bot));
            COMMANDS_COUNTER.labels(res.base()).inc();
        });
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        // Do not process if JDA is disconnected from WS
        if (!this.bot.isConnected(this.bot.getShardId(event.getJDA()))) return;

        // Do not respond to webhook/bot messages
        if (event.isWebhookMessage() || event.getAuthor().isBot()) return;

        // Do not respond if the bot cannot talk in the (text) channel
        // DMs are always okay
        if (event.isFromGuild() && !event.getTextChannel().canTalk()) {
            return;
        }

        // Check prefix
        String prefix = getPrefix(event);
        String rawMessage = event.getMessage().getContentRaw();
        if (!rawMessage.startsWith(prefix)) return;

        String commandMessage = rawMessage.substring(prefix.length());
        String[] args = commandMessage.split("\\s+");

        // Ignored channel
        boolean channelIsIgnored = this.ignoreChannelRepository.exists(
                () -> event.getChannel().getIdLong()
        );
        // Do not process command for ignored channel, unless it was the 'ignore' command itself
        if (channelIsIgnored && !commandMessage.toLowerCase().startsWith("ignore")) {
            return;
        }

        CommandComplex.Result res = this.commands.getCommand(args);
        if (res == null) {
            return;
        }

        this.threadPool.execute(() -> {
            processCommand(new MessageReceivedEventAdapter(event, this.bot), res, args);
            addCommandLog(res.base(), commandMessage, new MessageReceivedEventAdapter(event, this.bot));
            COMMANDS_COUNTER.labels(res.base()).inc();
        });
    }

    private void processCommand(@Nonnull CommandEvent event, CommandComplex.Result res, String[] args) {
        BotCommand command = res.command();

        // Check guild-only command
        if (!event.isFromGuild() && command.guildOnly()) {
            event.reply(String.format("This command (%s) cannot be executed in direct messages.", res.base()));
            return;
        }

        // Check spam and log event
        boolean isSpam = this.spamChecker.isSpam(event, command.getCoolDown());
        this.logger.logEvent(event, isSpam);
        if (isSpam) {
            long userId = event.getAuthor().getIdLong();
            long remainingCoolDown = this.spamChecker.nextCoolDownExpire(userId);
            event.reply(new EmbedBuilder()
                            .setColor(MinecraftColor.RED.getColor())
                            .setTitle("Slow down!")
                            .setDescription(String.format(
                                    "Please wait at least `%s` seconds before submitting a command again.",
                                    (double) remainingCoolDown / 1000D
                            ))
                            .build(),
                    s -> s.deleteMessageAfter(10, TimeUnit.SECONDS));
            return;
        }

        // If the argument list is 2 or longer and the last argument is "help", then send full help of the command
        // e.g. "track help" but not including "help"
        if (args.length >= 2 && args[args.length - 1].equalsIgnoreCase("help")) {
            event.reply(command.longHelp());
            return;
        }

        // Check permissions to execute the command
        if (command.requirePermissions()) {
            if (!event.isFromGuild()) {
                event.reply("You cannot execute this command in DM because it requires guild permissions.");
                return;
            }
            Member member = event.getMember();
            if (!command.hasPermissions(member)) {
                event.reply("You do not have enough permissions to execute this command!");
                return;
            }
        }

        // Process command
        event.acknowledge();
        try {
            command.process(event, args);
        } catch (Throwable e) {
            event.replyError("Something went wrong while processing your command...");
            this.logger.logException("Something went wrong while processing a user command", e);
        }
    }

    /**
     * Retrieves the prefix for the received event with the highest priority:<ol>
     * <li>user</li>
     * <li>channel</li>
     * <li>guild</li>
     * <li>default</li>
     * </ol>
     *
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
    private void addCommandLog(String kind, String full, CommandEvent event) {
        CommandLog entity = new CommandLog(kind, full, event.isFromGuild() ? event.getGuild().getIdLong() : null,
                event.getChannel().getIdLong(), event.getAuthor().getIdLong(), new Date(event.getCreatedAt()));
        if (!this.commandLogRepository.create(entity)) {
            this.logger.log(0, "Failed to log command to db.");
        }
    }
}

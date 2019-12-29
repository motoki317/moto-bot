package listeners;

import app.Bot;
import commands.*;
import commands.base.BotCommand;
import db.model.commandLog.CommandLog;
import db.model.prefix.Prefix;
import db.repository.CommandLogRepository;
import db.repository.PrefixRepository;
import log.Logger;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

public class MessageListener extends ListenerAdapter {
    private final List<BotCommand> commands;
    private final Map<String, BotCommand> commandNameMap;

    private final Bot bot;
    private final Logger logger;
    private final String defaultPrefix;

    private final CommandLogRepository commandLogRepository;
    private final PrefixRepository prefixRepository;

    public MessageListener(Bot bot) {
        this.commands = new ArrayList<>();
        this.commandNameMap = new HashMap<>();

        this.bot = bot;
        this.logger = bot.getLogger();
        this.defaultPrefix = bot.getProperties().prefix;

        this.commandLogRepository = bot.getDatabase().getCommandLogRepository();
        this.prefixRepository = bot.getDatabase().getPrefixRepository();

        addCommand(new Help(bot, this.commands));
        addCommand(new Ping(bot));
        addCommand(new Info(bot));
        addCommand(new ServerList(bot.getDatabase().getWorldRepository(), bot.getReactionManager()));
        addCommand(new Track(bot.getDatabase()));
        addCommand(new TimeZoneCmd(bot.getDatabase().getCustomTimeZoneRepository()));
    }

    private void addCommand(BotCommand command) {
        for (String commandName : command.names()) {
            commandName = commandName.toLowerCase();

            if (this.commandNameMap.containsKey(commandName)) {
                throw new Error("FATAL: Command name conflict: " + commandName + "\n" +
                        "Command " + command.getClass().getName() + " is not being added.");
            }

            this.commandNameMap.put(commandName, command);
        }
        this.commands.add(command);
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        // Do not process if the shard is not loaded
        int shardId = this.bot.getShardId(event.getJDA());
        if (!bot.isConnected(shardId)) return;

        // Do not respond to webhook/bot messages
        if (event.isWebhookMessage() || event.getAuthor().isBot()) return;

        // Check prefix
        String prefix = getPrefix(event);
        String rawMessage = event.getMessage().getContentRaw();
        if (!rawMessage.startsWith(prefix)) return;

        String commandMessage = rawMessage.substring(prefix.length());
        String[] args = commandMessage.split(" ");

        // Process command
        if (args.length == 0) return;
        for (String commandName : this.commandNameMap.keySet()) {
            if (args[0].toLowerCase().equals(commandName)) {
                BotCommand command = this.commandNameMap.get(commandName);

                // Check guild-only command
                if (!event.isFromGuild() && command.guildOnly()) {
                    String message = String.format("This command (%s) cannot be executed in direct messages.", commandName);
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

                command.process(event, args);

                addCommandLog(args[0], commandMessage, event.getAuthor().getIdLong(), !event.isFromGuild());
                return;
            }
        }
    }

    /**
     * Retrieves prefix for the received event. Default prefix
     * @param event Message received event
     * @return Resolved prefix
     */
    private String getPrefix(@NotNull MessageReceivedEvent event) {
        String ret = this.defaultPrefix;
        if (event.isFromGuild()) {
            Prefix guild = this.prefixRepository.findOne(() -> event.getGuild().getIdLong());
            if (guild != null) {
                ret = guild.getPrefix();
            }
        }
        Prefix channel = this.prefixRepository.findOne(() -> event.getChannel().getIdLong());
        if (channel != null) {
            ret = channel.getPrefix();
        }
        Prefix user = this.prefixRepository.findOne(() -> event.getAuthor().getIdLong());
        if (user != null) {
            ret = user.getPrefix();
        }
        return ret;
    }

    /**
     * Adds command log to db.
     */
    private void addCommandLog(String kind, String full, long userId, boolean dm) {
        CommandLog entity = new CommandLog(kind, full, userId, dm);
        if (!this.commandLogRepository.create(entity)) {
            this.logger.log(0, "Failed to log command to db.");
        }
    }
}

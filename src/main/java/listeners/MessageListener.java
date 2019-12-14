package listeners;

import app.Bot;
import commands.*;
import commands.base.BotCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.*;

public class MessageListener extends ListenerAdapter {
    private final Bot bot;

    private final List<BotCommand> commands;
    private final Map<String, BotCommand> commandNameMap;

    public MessageListener(Bot bot) {
        this.bot = bot;
        this.commands = new ArrayList<>();
        this.commandNameMap = new HashMap<>();

        addCommand(new Help(bot, this.commands));
        addCommand(new Ping(bot));
        addCommand(new Info(bot));
        addCommand(new ServerList(bot.getDatabase().getWorldRepository()));
        addCommand(new Track(bot.getDatabase()));
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
        String prefix = this.bot.getProperties().prefix;
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
                boolean isSpam = this.bot.getLogger().logEvent(event);
                if (isSpam) {
                    String message = "You are requesting commands too quickly! Please wait at least 1 second between each commands.";
                    event.getChannel().sendMessage(message).queue();
                    return;
                }

                command.process(event, args);
                return;
            }
        }
    }
}

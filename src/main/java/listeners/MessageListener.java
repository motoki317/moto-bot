package listeners;

import app.App;
import commands.Info;
import commands.Ping;
import commands.base.BotCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import utils.Logger;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MessageListener extends ListenerAdapter {
    private final Map<String, BotCommand> commands;
    private final Set<String> commandsList;

    public MessageListener() {
        this.commands = new HashMap<>();
        this.commandsList = new HashSet<>();

        addCommand(new Ping());
        addCommand(new Info());
    }

    private void addCommand(BotCommand command) {
        for (String commandName : command.names()) {
            commandName = commandName.toLowerCase();

            if (this.commandsList.contains(commandName)) {
                throw new Error("FATAL: Command name conflict: " + commandName + "\n" +
                        "Command " + command.getClass().getName() + " is not being added.");
            }

            this.commandsList.add(commandName);
            this.commands.put(commandName, command);
        }
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        // Do not process until all shards are loaded
        if (!App.isAllConnected()) return;

        // Do not respond to webhook/bot messages
        if (event.isWebhookMessage() || event.getAuthor().isBot()) return;

        // Check prefix
        String rawMessage = event.getMessage().getContentRaw();
        if (!rawMessage.startsWith(App.BOT_PROPERTIES.prefix)) return;

        String commandMessage = rawMessage.substring(App.BOT_PROPERTIES.prefix.length());
        String[] args = commandMessage.split(" ");

        // Process command
        if (args.length == 0) return;
        for (String commandName : this.commandsList) {
            if (args[0].toLowerCase().equals(commandName)) {
                BotCommand command = this.commands.get(commandName);

                // Check guild-only command
                if (!event.isFromGuild() && command.guildOnly()) {
                    String message = String.format("This command (%s) cannot be executed in direct messages.", commandName);
                    event.getChannel().sendMessage(message).queue();
                    return;
                }

                Logger.log(event, false);
                command.process(event, args);
                return;
            }
        }
    }
}

package commands;

import app.Bot;
import commands.base.BotCommand;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Set;

public class Help extends GenericCommand {
    private final Bot bot;

    private final Set<BotCommand> commands;

    public Help(Bot bot, Set<BotCommand> commands) {
        this.bot = bot;
        this.commands = commands;
    }

    @Override
    public String[] names() {
        return new String[]{"help", "h"};
    }

    @Override
    public String shortHelp() {
        return "Calls this help. Use with arguments to view detailed help of each command. e.g. `help ping`";
    }

    @Override
    public Message longHelp() {
        return new MessageBuilder(
                "Use help command with arguments to view detailed help of each command. e.g. `help ping`"
        ).build();
    }

    @Override
    public void process(MessageReceivedEvent event, String[] args) {
        if (args.length == 1) {
            // TODO: commands list in multi-page respond
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(this.bot.getProperties().getMainColor());
            eb.setTitle("Commands List");
            for (BotCommand cmd : this.commands) {
                eb.addField(
                        this.bot.getProperties().prefix + cmd.names()[0],
                        cmd.shortHelp(),
                        false
                );
            }

            respond(event, eb.build());
            return;
        }

        // TODO: nested command (e.g. `>g stats`) help
        String specifiedCmd = args[1];
        for (BotCommand cmd : this.commands) {
            for (String cmdName : cmd.names()) {
                if (cmdName.equals(specifiedCmd)) {
                    respond(event, cmd.longHelp());
                }
            }
        }
    }
}

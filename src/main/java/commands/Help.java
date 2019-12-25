package commands;

import app.Bot;
import commands.base.BotCommand;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import update.multipage.MultipageHandler;

import java.util.List;
import java.util.function.Function;

public class Help extends GenericCommand {
    private final Bot bot;

    private final List<BotCommand> commands;

    public Help(Bot bot, List<BotCommand> commands) {
        this.bot = bot;
        this.commands = commands;
    }

    @Override
    public String[] names() {
        return new String[]{"help", "h"};
    }

    @Override
    public String syntax() {
        return "help [cmd name]";
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
            Function<Integer, Message> pages = this.pageSupplier();

            if (this.maxPage() == 0) {
                respond(event, pages.apply(0));
                return;
            }

            respond(event, pages.apply(0), message -> {
                MultipageHandler handler = new MultipageHandler(message, pages, this::maxPage);
                bot.getReactionManager().addEventListener(handler);
            });
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

    private static final int COMMANDS_PER_PAGE = 5;

    private Function<Integer, Message> pageSupplier() {
        return (page) -> {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(this.bot.getProperties().getMainColor());
            eb.setTitle("Commands List");

            int min = COMMANDS_PER_PAGE * page;
            int max = Math.min(COMMANDS_PER_PAGE * (page + 1), this.commands.size());
            for (int i = min; i < max; i++) {
                BotCommand cmd = this.commands.get(i);
                eb.addField(
                        this.bot.getProperties().prefix + cmd.syntax(),
                        cmd.shortHelp(),
                        false
                );
            }
            return new MessageBuilder(
                    eb.build()
            ).build();
        };
    }

    private int maxPage() {
        return (this.commands.size() - 1) / COMMANDS_PER_PAGE;
    }
}

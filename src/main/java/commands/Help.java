package commands;

import app.Bot;
import commands.base.BotCommand;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import update.multipage.MultipageHandler;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Help extends GenericCommand {
    private final Bot bot;

    private final List<BotCommand> commands;
    private final Map<String, BotCommand> commandNameMap;
    private final Supplier<Integer> maxArgumentsLength;

    public Help(Bot bot, List<BotCommand> commands, Map<String, BotCommand> commandNameMap, Supplier<Integer> maxArgumentsLength) {
        this.bot = bot;
        this.commands = commands;
        this.commandNameMap = commandNameMap;
        this.maxArgumentsLength = maxArgumentsLength;
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"help", "h"}};
    }

    @NotNull
    @Override
    public String syntax() {
        return "help [cmd name]";
    }

    @NotNull
    @Override
    public String shortHelp() {
        return "Calls this help. Use with arguments to view detailed help of each command. e.g. `help ping`";
    }

    @NotNull
    @Override
    public Message longHelp() {
        return new MessageBuilder(
                "Use help command with arguments to view detailed help of each command. e.g. `help ping`"
        ).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length == 1) {

            if (this.maxPage() == 0) {
                respond(event, this.getPage(0));
                return;
            }

            respond(event, this.getPage(0), message -> {
                MultipageHandler handler = new MultipageHandler(message, event.getAuthor().getIdLong(), this::getPage, this::maxPage);
                bot.getReactionManager().addEventListener(handler);
            });
            return;
        }

        // Supports nested command help (e.g. ">help guild levelRank")
        args = Arrays.copyOfRange(args, 1, args.length);
        for (int argLength = 1; argLength <= this.maxArgumentsLength.get(); argLength++) {
            if (args.length < argLength) return;

            String cmdBase = String.join(" ", Arrays.copyOfRange(args, 0, argLength));
            if (this.commandNameMap.containsKey(cmdBase.toLowerCase())) {
                BotCommand cmd = this.commandNameMap.get(cmdBase.toLowerCase());
                respond(event, cmd.longHelp());
                return;
            }
        }
    }

    private static final int COMMANDS_PER_PAGE = 5;

    private Message getPage(int page) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(this.bot.getProperties().getMainColor())
                .setTitle(String.format("Commands List : Page [%s/%s]", page + 1, maxPage() + 1))
                .setFooter("<text> means required, and [text] means optional arguments.")
                .setTimestamp(Instant.now());

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
    }

    private int maxPage() {
        return (this.commands.size() - 1) / COMMANDS_PER_PAGE;
    }
}

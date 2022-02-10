package commands;

import app.Bot;
import app.CommandComplex;
import commands.base.BotCommand;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Help extends GenericCommand {
    private final Bot bot;

    private final Supplier<CommandComplex> commands;

    public Help(Bot bot, Supplier<CommandComplex> commands) {
        this.bot = bot;
        this.commands = commands;
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"help", "h"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"help"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "command-name", "Command to get help for")
        };
    }

    @NotNull
    @Override
    public String syntax() {
        return "help [cmd name]";
    }

    @NotNull
    @Override
    public String shortHelp() {
        return "Calls help (of each command).";
    }

    @NotNull
    @Override
    public Message longHelp() {
        return new MessageBuilder(
                "Use help command with arguments to view detailed help of each command. e.g. `help ping`"
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length == 1) {
            if (this.maxPage() == 0) {
                event.reply(this.getPage(0));
                return;
            }

            event.replyMultiPage(this.getPage(0), this::getPage, this::maxPage);
            return;
        }

        // Supports nested command help (e.g. ">help guild levelRank")
        CommandComplex.Result res = this.commands.get().getCommand(
                Arrays.copyOfRange(args, 1, args.length)
        );
        if (res == null) {
            event.reply("Command not found, try `help`.");
            return;
        }

        event.reply(res.command().longHelp());
    }

    private static final int COMMANDS_PER_PAGE = 5;

    private Message getPage(int page) {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(this.bot.getProperties().getMainColor())
                .setTitle(String.format("Commands List : Page [%s/%s]", page + 1, maxPage() + 1))
                .setFooter("<text> means required, and [text] means optional arguments.")
                .setTimestamp(Instant.now());

        List<BotCommand> commands = this.commands.get().getCommands();
        int min = COMMANDS_PER_PAGE * page;
        int max = Math.min(COMMANDS_PER_PAGE * (page + 1), commands.size());
        for (int i = min; i < max; i++) {
            BotCommand cmd = commands.get(i);
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
        return (this.commands.get().getCommands().size() - 1) / COMMANDS_PER_PAGE;
    }
}

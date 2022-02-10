package commands;

import app.CommandComplex;
import commands.base.BotCommand;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CommandAliases extends GenericCommand {
    private final Supplier<CommandComplex> commands;

    public CommandAliases(Supplier<CommandComplex> commands) {
        this.commands = commands;
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"alias", "aliases"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"alias"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "command-name", "Command to get help for", true)
        };
    }

    @Override
    public @NotNull String syntax() {
        return "alias <command name>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows all aliases of each bot command.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Alias Command Help")
                        .setDescription(this.shortHelp())
                        .addField("Syntax", this.syntax(), false)
                        .addField("Example", "`alias up`\n`alias g levelRank`", false)
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            event.reply(this.longHelp());
            return;
        }

        // Supports nested command (e.g. ">alias guild levelRank")
        CommandComplex.Result res = this.commands.get().getCommand(
                Arrays.copyOfRange(args, 1, args.length)
        );
        if (res == null) {
            event.reply("Command not found, try `help`.");
            return;
        }

        event.reply(formatMessage(res.command()));
    }

    private static MessageEmbed formatMessage(BotCommand cmd) {
        return new EmbedBuilder()
                .setTitle("Command Aliases")
                .setDescription("This command has following aliases: " +
                        cmd.getNames().stream().map(n -> "`" + n + "`").collect(Collectors.joining(", ")))
                .build();
    }
}

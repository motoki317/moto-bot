package commands;

import commands.base.BotCommand;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CommandAliases extends GenericCommand {
    private final Map<String, BotCommand> commandNameMap;
    private final Supplier<Integer> maxArgumentsLength;

    public CommandAliases(Map<String, BotCommand> commandNameMap, Supplier<Integer> maxArgumentsLength) {
        this.commandNameMap = commandNameMap;
        this.maxArgumentsLength = maxArgumentsLength;
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"alias", "aliases"}};
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
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(event, this.longHelp());
            return;
        }

        // Supports nested command (e.g. ">alias guild levelRank")
        args = Arrays.copyOfRange(args, 1, args.length);
        for (int argLength = Math.min(this.maxArgumentsLength.get(), args.length); argLength > 0; argLength--) {
            String cmdBase = String.join(" ", Arrays.copyOfRange(args, 0, argLength));
            if (this.commandNameMap.containsKey(cmdBase.toLowerCase())) {
                BotCommand cmd = this.commandNameMap.get(cmdBase.toLowerCase());
                respond(event, formatMessage(cmd));
                return;
            }
        }

        respond(event, "Command not found, try `help`.");
    }

    private static MessageEmbed formatMessage(BotCommand cmd) {
        return new EmbedBuilder()
                .setTitle("Command Aliases")
                .setDescription("This command has following aliases: " +
                        cmd.getNames().stream().map(n -> "`" + n + "`").collect(Collectors.joining(", ")))
                .build();
    }
}

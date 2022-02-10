package app;

import commands.base.BotCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import javax.annotation.Nullable;
import java.util.*;

public class CommandComplex {
    private final List<BotCommand> commands;
    private final Map<String, BotCommand> commandNameMap;
    private final int maxArgumentsLength;

    private CommandComplex(List<BotCommand> commands, Map<String, BotCommand> commandNameMap, int maxArgumentsLength) {
        this.commands = commands;
        this.commandNameMap = commandNameMap;
        this.maxArgumentsLength = maxArgumentsLength;
    }

    public List<BotCommand> getCommands() {
        return commands;
    }

    public int getMaxArgumentsLength() {
        return this.maxArgumentsLength;
    }

    /**
     * Get the corresponding bot command from user arguments.
     *
     * @param args <b>Prefix-stripped</b> user arguments.
     * @return Bot command if found.
     */
    public @Nullable
    Result getCommand(String[] args) {
        // Process command from the most 'specific' (e.g. g pws) to most 'generic' (e.g. guild)
        for (int argLength = Math.min(this.maxArgumentsLength, args.length); argLength > 0; argLength--) {
            String cmdBase = String.join(" ", Arrays.copyOfRange(args, 0, argLength)).toLowerCase();
            // Command name match
            if (this.commandNameMap.containsKey(cmdBase)) {
                return new Result(this.commandNameMap.get(cmdBase), cmdBase);
            }
        }
        return null;
    }

    public record Result(BotCommand command, String base) {
    }

    public static class Builder {
        private final Bot bot;

        private final List<BotCommand> commands;
        private final Map<String, BotCommand> commandNameMap;
        private int maxArgumentsLength;

        private final Map<String, CommandData> slashCommands;
        private final Map<String, String> commandDescriptions;
        private final Map<String, String> subCommandGroupDescriptions;

        public Builder(Bot bot) {
            this.bot = bot;

            this.commands = new ArrayList<>();
            this.commandNameMap = new HashMap<>();
            this.maxArgumentsLength = 1;

            this.slashCommands = new HashMap<>();
            this.commandDescriptions = new HashMap<>();
            this.subCommandGroupDescriptions = new HashMap<>();
        }

        private void addTextCommand(BotCommand command) {
            for (String commandName : command.getNames()) {
                commandName = commandName.toLowerCase();

                if (this.commandNameMap.containsKey(commandName)) {
                    throw new Error("FATAL: Command name conflict: " + commandName + "\n" +
                            "Command " + command.getClass().getName() + " is not being added.");
                }

                this.commandNameMap.put(commandName, command);
            }
        }

        private void addSlashCommand(BotCommand command) {
            String[] slashName = command.slashName();
            switch (slashName.length) {
                case 1:
                    if (this.slashCommands.containsKey(slashName[0])) {
                        throw new Error("Slash command name conflict: " + slashName[0]);
                    }
                    this.slashCommands.put(slashName[0], new CommandData(slashName[0], command.shortHelp()).addOptions(command.slashOptions()));
                    break;
                case 2:
                    CommandData slashData = this.slashCommands.get(slashName[0]);
                    if (slashData == null) {
                        slashData = new CommandData(slashName[0], this.commandDescriptions.getOrDefault(slashName[0], slashName[0]));
                        this.slashCommands.put(slashName[0], slashData);
                    }
                    slashData.addSubcommands(new SubcommandData(slashName[1], command.shortHelp()));
                    break;
                case 3:
                    slashData = this.slashCommands.get(slashName[0]);
                    if (slashData == null) {
                        slashData = new CommandData(slashName[0], this.commandDescriptions.getOrDefault(slashName[0], slashName[0]));
                        this.slashCommands.put(slashName[0], slashData);
                    }
                    SubcommandGroupData subcommandGroup = slashData.getSubcommandGroups().stream().filter(s -> s.getName().equals(slashName[1])).findFirst().orElse(null);
                    if (subcommandGroup == null) {
                        subcommandGroup = new SubcommandGroupData(slashName[1], this.subCommandGroupDescriptions.getOrDefault(slashName[0] + " " + slashName[1], slashName[1]));
                        slashData.addSubcommandGroups(subcommandGroup);
                    }
                    subcommandGroup.addSubcommands(new SubcommandData(slashName[2], command.shortHelp()));
                    break;
                default:
                    throw new Error("Slash command name length must be >= 0 and <= 3");
            }
        }

        /**
         * Registers command description for use in root command.
         * Must be called before {@link Builder#addCommand(BotCommand)}.
         * <pre>
         * command <-- Sets description of this command
         * |
         * |__ subcommand-group
         *     |
         *     |__ subcommand
         * |
         * |__ subcommand
         * </pre>
         *
         * @param name        Root command name.
         * @param description Description.
         */
        public void addCommandDescription(String name, String description) {
            this.commandDescriptions.put(name, description);
        }

        /**
         * Registers subcommand group description for use in subcommand groups.
         * Must be called before {@link Builder#addCommand(BotCommand)}.
         * <pre>
         * command
         * |
         * |__ subcommand-group <-- Sets description of this subcommand-group
         *     |
         *     |__ subcommand
         * |
         * |__ subcommand
         * </pre>
         *
         * @param name        Root command name.
         * @param subName     Subcommand-group name.
         * @param description Description.
         */
        public void addSubcommandGroupDescription(String name, String subName, String description) {
            this.subCommandGroupDescriptions.put(name + " " + subName, description);
        }

        public void addCommand(BotCommand command) {
            this.addTextCommand(command);
            this.addSlashCommand(command);

            this.commands.add(command);
            this.maxArgumentsLength = Math.max(this.maxArgumentsLength, command.getArgumentsLength());
        }

        private Collection<CommandData> getSlashCommands() {
            return this.slashCommands.values();
        }

        /**
         * Builds {@link CommandComplex} and registers slash commands to JDA.
         *
         * @return {@link CommandComplex}
         */
        public CommandComplex build() {
            Collection<CommandData> slashCommands = this.getSlashCommands();
            for (JDA jda : bot.getManager().getShards()) {
                jda.updateCommands().addCommands(slashCommands).queue();
            }

            return new CommandComplex(this.commands, this.commandNameMap, this.maxArgumentsLength);
        }
    }
}

package commands.base;

import commands.event.CommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class BotCommand {
    public abstract boolean guildOnly();

    /**
     * Command names including aliases. Used to process command inputs.
     * Examples:
     * {{"help", "h"}} for 1-argument command.
     * {{"guild", "g"}, {"levelRank", "lRank"}} for 2-arguments command.
     *
     * @return Command names.
     */
    @NotNull
    protected abstract String[][] names();

    /**
     * Command name for slash command.
     * Command name returned by this MUST also be included by {@link BotCommand#names()}.
     * Should return valid slash command name: use lower-case letters and hyphens ("-").
     *
     * @return Command name.
     */
    @NotNull
    public abstract String[] slashName();

    /**
     * Command options used by slash command.
     *
     * @return Slash command options.
     */
    @NotNull
    public abstract OptionData[] slashOptions();

    /**
     * Get names for this command including aliases.
     * Examples:
     * {"help", "h"} for 1-argument command.
     * {"guild levelRank", "guild lRank", "g levelRank", "g lRank"} for 2-arguments command.
     *
     * @return Names. Values possibly includes spaces.
     */
    public Set<String> getNames() {
        return getNamesRec(this.names(), 0);
    }

    private static Set<String> getNamesRec(String[][] base, int i) {
        if (base.length - 1 == i) {
            return new HashSet<>(Arrays.asList(base[i]));
        }

        Set<String> ret = new HashSet<>();
        for (String latter : getNamesRec(base, i + 1)) {
            for (String current : base[i]) {
                ret.add(current + " " + latter);
            }
        }
        return ret;
    }

    /**
     * Get base arguments' length of this command.
     * Examples:
     * "help" command: 1,
     * "g levelRank" command: 2
     *
     * @return Length of base arguments.
     */
    public int getArgumentsLength() {
        return this.names().length;
    }

    /**
     * Command syntax. Used in help display.
     * Example:
     * "help [command name]"
     *
     * @return Command syntax.
     */
    @NotNull
    public abstract String syntax();

    /**
     * Short help for use in help and slash command.
     *
     * @return Short help.
     */
    @NotNull
    public abstract String shortHelp();

    /**
     * Shows long help in help (cmd name) command.
     *
     * @return Long help message.
     */
    @NotNull
    public abstract Message longHelp();

    /**
     * Get required guild permissions to execute this command.
     * All permission given by this has to be satisfied by the member.
     * If no permission is required, returns an empty list.
     *
     * @return List of permissions.
     */
    @NotNull
    protected Permission[] getRequiredPermissions() {
        return new Permission[]{};
    }

    /**
     * Checks if this command requires any guild permissions.
     *
     * @return Returns {@code true} this command requires guild perms.
     */
    public boolean requirePermissions() {
        return this.getRequiredPermissions().length > 0;
    }

    /**
     * Checks if the given member has enough permissions to execute this command.
     * Should check {@link BotCommand#requirePermissions()} first before calling this.
     *
     * @param member Guild member.
     * @return {@code true} if
     */
    public boolean hasPermissions(@NotNull Member member) {
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        }
        for (Permission p : this.getRequiredPermissions()) {
            if (!member.hasPermission(p)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retrieves command cool-down in milliseconds.
     * The user will not be able to execute any other commands for this much time, after executing this command.
     *
     * @return Cool-down in milliseconds.
     */
    public abstract long getCoolDown();

    /**
     * Process a command.
     *
     * @param event Command event.
     * @param args  Argument array, separated by space characters.
     */
    public abstract void process(@NotNull CommandEvent event, @NotNull String[] args);
}

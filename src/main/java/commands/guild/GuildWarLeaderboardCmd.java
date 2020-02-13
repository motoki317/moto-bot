package commands.guild;

import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public class GuildWarLeaderboardCmd extends GenericCommand {
    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"lb", "leaderboard", "glb", "guildLeaderboard"}};
    }

    @Override
    public @NotNull String syntax() {
        return "g lb [-t|--total]";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Guild war leaderboard. `g lb help` for more.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("Guild War Leaderboard Help")
                .setDescription(
                        "This command displays war leaderboard for guilds.\n" +
                        "They are ordered by their # of success wars by default.\n" +
                        "Note: Wars are logged and stored since around the beginning of April 2018."
                )
                .addField("Syntax",
                        this.syntax(),
                        false
                )
                .addField("Optional Arguments",
                        String.join("\n",
                                "**-t --total** : Sorts in order of # of total wars."
                                // TODO: implement range specify
                                // "**-d <days>** : Specifies the range of leaderboard, day-specifying is up to 30 days."
                        ),
                        false
                )
                .addField("Examples",
                        String.join("\n",
                                ".g lb : Displays leaderboard of all guilds ordered by # of success wars.",
                                ".g lb -t : Displays leaderboard of guilds ordered by # of total wars."
                                // ".g plb -sc -d 7 : Displays leaderboard of all players, in last 7 days, and in order of # of success rate."
                        ),
                        false
                ).build()
        ).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        // TODO impl cmd
    }
}

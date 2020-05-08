package commands;

import api.wynn.WynnApi;
import app.Bot;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import utils.InputChecker;
import utils.MinecraftColor;

public class Find extends GenericCommand {
    private final WynnApi wynnApi;

    public Find(Bot bot) {
        this.wynnApi = new WynnApi(bot.getLogger());
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"find"}};
    }

    @Override
    public @NotNull String syntax() {
        return "find <player name>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Checks if a player is online.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(this.shortHelp()).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(event, this.longHelp());
            return;
        }

        String playerName = args[1];
        if (!InputChecker.isValidMinecraftUsername(playerName)) {
            respondException(event, String.format("Given player name `%s` doesn't seem to be a valid minecraft username...",
                    playerName));
            return;
        }

        String world = this.wynnApi.mustFindPlayer(playerName);
        if (world == null) {
            respond(event,
                    new EmbedBuilder()
                    .setColor(MinecraftColor.RED.getColor())
                    .setDescription(String.format("`%s` is not online on any Wynncraft servers...", playerName))
                    .build()
            );
        } else {
            respond(event,
                    new EmbedBuilder()
                            .setColor(MinecraftColor.GREEN.getColor())
                            .setDescription(String.format("`%s` is online on %s", playerName, world))
                            .build()
            );
        }
    }
}

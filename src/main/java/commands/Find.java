package commands;

import api.wynn.WynnApi;
import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import utils.InputChecker;
import utils.MinecraftColor;

import java.util.concurrent.TimeUnit;

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
    public @NotNull String[] slashName() {
        return new String[]{"find"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "name", "Name of player to find", true)
        };
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
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            event.reply(this.longHelp());
            return;
        }

        String playerName = args[1];
        if (!InputChecker.isValidMinecraftUsername(playerName)) {
            event.replyException(String.format("Given player name `%s` doesn't seem to be a valid minecraft username...",
                    playerName));
            return;
        }

        String world = this.wynnApi.mustFindPlayer(playerName);
        if (world == null) {
            event.reply(new EmbedBuilder()
                    .setColor(MinecraftColor.RED.getColor())
                    .setDescription(String.format("`%s` is not online on any Wynncraft servers...", playerName))
                    .build());
        } else {
            event.reply(new EmbedBuilder()
                    .setColor(MinecraftColor.GREEN.getColor())
                    .setDescription(String.format("`%s` is online on %s", playerName, world))
                    .build());
        }
    }
}

package commands;

import api.thecatapi.TheCatApi;
import api.thecatapi.structs.CatResponse;
import app.Bot;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public class CatCmd extends GenericCommand {
    private final TheCatApi theCatApi;

    public CatCmd(Bot bot) {
        this.theCatApi = new TheCatApi(bot.getLogger());
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"cat"}};
    }

    @Override
    public @NotNull String syntax() {
        return "cat";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows a random cat. (Powered by thecatapi.com)";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(this.shortHelp()).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        CatResponse res = this.theCatApi.mustGetCat();
        if (res == null) {
            respondError(event, "Something went wrong while requesting The Cat API...");
            return;
        }

        respond(event, formatMessage(event, res));
    }

    private static Message formatMessage(MessageReceivedEvent event, CatResponse res) {
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor(String.format("%s, here's a random cat for you! \uD83D\uDC31",
                        event.getAuthor().getName()), null, event.getAuthor().getEffectiveAvatarUrl())
                .setImage(res.getUrl())
                .setFooter("Powered by thecatapi.com")
                .build()
        ).build();
    }
}

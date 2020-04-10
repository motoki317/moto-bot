package commands;

import api.wynn.WynnApi;
import api.wynn.structs.ItemDB;
import app.Bot;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

public class ItemView extends GenericCommand {
    private final WynnApi wynnApi;

    public ItemView(Bot bot) {
        this.wynnApi = new WynnApi(bot.getLogger(), bot.getProperties().wynnTimeZone);
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"i", "item"}};
    }

    @Override
    public @NotNull String syntax() {
        return "item <item name>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows item stats.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("Item Stats Command Help")
                .setDescription("Shows an item's stats. Give partial or full item name to the argument.")
                .build()
        ).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        ItemDB db = this.wynnApi.mustGetItemDB(false);
        if (db == null) {
            respondError(event, "Something went wrong while requesting Wynncraft API.");
            return;
        }
        respond(event, "db size: " + db.getItems().size());
    }
}

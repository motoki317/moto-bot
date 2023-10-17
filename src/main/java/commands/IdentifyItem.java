package commands;

import api.wynn.WynnApi;
import api.wynn.structs.Item;
import api.wynn.structs.ItemDB;
import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import commands.event.message.ButtonClickEventAdapter;
import commands.event.message.SentMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.Component;
import org.jetbrains.annotations.NotNull;
import update.button.ButtonClickHandler;
import update.button.ButtonClickManager;
import utils.ArgumentParser;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static commands.ItemView.*;

public class IdentifyItem extends GenericCommand {
    private final WynnApi wynnApi;
    private final String imageURLBase;
    private final ButtonClickManager buttonClickManager;

    public IdentifyItem(Bot bot) {
        this.wynnApi = new WynnApi(bot.getLogger());
        this.imageURLBase = bot.getProperties().githubImagesUrl;
        this.buttonClickManager = bot.getButtonClickManager();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"id", "identify"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"id"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "name", "Name of item to identify", true),
                new OptionData(OptionType.STRING, "re-identify", "Show re-ID buttons")
                        .addChoice("-re", "-re")
        };
    }

    @Override
    public @NotNull String syntax() {
        return "id <item name> [-re]";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Simulates identifying an item.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Identify Command Help")
                        .setDescription("Simulates identifying an item.")
                        .addField("Re-identifying",
                                "To keep identifying an item multiple times, give an argument `-re` " +
                                        "and press the reaction button on the message.", false)
                        .build()
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length < 2) {
            event.reply("Please input the item name you want to simulate identifying.");
            return;
        }

        ItemDB db = this.wynnApi.mustGetItemDB(false);
        if (db == null) {
            event.replyError("Something went wrong while requesting Wynncraft API.");
            return;
        }

        ArgumentParser parser = new ArgumentParser(Arrays.copyOfRange(args, 1, args.length));

        String input = parser.getNormalArgument();
        List<Item> matched = searchItem(input, db.getItems());

        if (matched.isEmpty()) {
            event.reply(String.format("No items matched with input `%s`.", input));
            return;
        } else if (matched.size() > 1) {
            event.reply(String.format("%d items matched with input `%s`.\n%s",
                    matched.size(), input,
                    matched.stream().limit(50).map(i -> "`" + i.getName() + "`")
                            .collect(Collectors.joining(", "))));
            return;
        }

        Item item = matched.get(0);

        boolean reIdentify = parser.getArgumentMap().containsKey("re");
        if (!reIdentify) {
            event.reply(formatItemInfo(item, this.imageURLBase, 1));
            return;
        }

        Function<Integer, Message> messageSupplier = seq ->
                new MessageBuilder(formatItemInfo(item, imageURLBase, seq))
                        .setActionRows(ActionRow.of(IDButtonHandler.actionRow()))
                        .build();
        event.reply(messageSupplier.apply(1), message ->
                message.getMessage(actualMsg -> {
                    IDButtonHandler handler = new IDButtonHandler(message, actualMsg.getIdLong(), messageSupplier);
                    this.buttonClickManager.addEventListener(handler);
                }));
    }

    private static class IDButtonHandler extends ButtonClickHandler {
        private static final String BUTTON_ID_REFRESH = "refresh";
        private static final String BUTTON_ID_CANCEL = "cancel";

        private SentMessage message;
        private int seq;
        private final Function<Integer, Message> messageSupplier;

        public IDButtonHandler(SentMessage message, long messageId, Function<Integer, Message> messageSupplier) {
            super(messageId, (event) -> false, () -> {
            });

            this.message = message;
            this.seq = 1;
            this.messageSupplier = messageSupplier;
        }

        private static Component[] actionRow() {
            return new Component[]{
                    Button.secondary(BUTTON_ID_REFRESH, Emoji.fromUnicode("\u2705")),
                    Button.danger(BUTTON_ID_CANCEL, Emoji.fromUnicode("\u274C"))
            };
        }

        @Override
        public boolean handle(ButtonClickEvent event) {
            super.handle(event);

            if (event.getButton() == null || event.getButton().getId() == null) return false;

            switch (event.getButton().getId()) {
                case BUTTON_ID_REFRESH:
                    break;
                case BUTTON_ID_CANCEL:
                    this.message = new ButtonClickEventAdapter(event);
                    return true;
            }

            this.seq++;
            event.editMessage(this.messageSupplier.apply(this.seq)).queue();

            return false;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            // Delete buttons
            this.message.editComponents();
        }
    }

    /**
     * Formats item info and returns formatted message.
     *
     * @param item         Item
     * @param imageURLBase Image URl
     * @param seq          How many times this item has been identified. Used for display.
     * @return Formatted message.
     */
    private static Message formatItemInfo(Item item, String imageURLBase, int seq) {
        EmbedBuilder eb = getEmbed(item, imageURLBase);
        if (seq > 1) {
            eb.setTitle(String.format("Lv. %s %s [%s], %s %s",
                    item.getLevel(), item.getName(), seq, item.getTier(), item.getType()));
        }
        eb.setDescription("You identified the item!");

        return new MessageBuilder(
                "```ml\n" +
                        getIDs(item) +
                        "\n```"
        ).setEmbeds(
                eb.build()
        ).build();
    }

    private static String getIDs(Item item) {
        List<Identification> availableIDs = Arrays.stream(identifications)
                .filter(i -> i.status.apply(item) != 0).toList();

        List<String> ret = new ArrayList<>();
        if (availableIDs.isEmpty()) {
            return "No Identifications";
        }

        int displayJustify = availableIDs.stream().mapToInt(i -> i.displayName.length()).max().getAsInt() + 1;
        for (Identification id : availableIDs) {
            ret.add(String.format("%s%s : %s",
                    nSpaces(displayJustify - id.displayName.length()), id.displayName,
                    getFormattedIdentified(id, item)));
        }

        return String.join("\n", ret);
    }

    private static String getFormattedIdentified(Identification id, Item item) {
        int base = id.status.apply(item);
        if (id.isIdentified() || item.isIdentified()) {
            return base + id.getSuffix();
        }

        int lo, hi, val;
        String stars = "";
        String percentage;
        Random rand = new Random();
        if (base > 0) {
            int roll = rand.nextInt(101);
            val = (int) Math.round((double) base * (roll + 30d) / 100d);
            lo = (int) Math.round((double) base * 0.3d);
            hi = (int) Math.round((double) base * 1.3d);

            if (71 <= roll && roll < 95) stars = " *";
            else if (95 <= roll && roll < 100) stars = " **";
            else if (roll == 100) stars = " ***";

            percentage = String.valueOf(roll);
        } else {
            int roll = rand.nextInt(61);
            val = (int) Math.round((double) base * (roll + 70d) / 100d);
            lo = (int) Math.round((double) base * 1.3d);
            hi = (int) Math.round((double) base * 0.7d);

            percentage = String.valueOf((int) ((double) (60 - roll) * 100d / 60d));
        }

        return String.format("%s%s%s [%s%%], %s%s ~ %s%s",
                val, id.getSuffix(), stars, percentage, lo, id.getSuffix(), hi, id.getSuffix());
    }

    private static String nSpaces(int n) {
        return String.join("", Collections.nCopies(n, " "));
    }
}

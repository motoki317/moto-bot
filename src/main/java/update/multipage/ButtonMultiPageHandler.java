package update.multipage;

import commands.event.message.ButtonClickEventAdapter;
import commands.event.message.SentMessage;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.Component;
import update.button.ButtonClickHandler;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ButtonMultiPageHandler extends ButtonClickHandler {
    private static final String ARROW_LEFT = "\u2B05";
    private static final String ARROW_RIGHT = "\u27A1";
    private static final String WHITE_CHECK_MARK = "\u2705";
    private static final String X = "\u274C";

    private static final String BUTTON_ID_LEFT_PAGE = "left_page";
    private static final String BUTTON_ID_RIGHT_PAGE = "right_page";
    private static final String BUTTON_ID_REFRESH = "refresh";
    private static final String BUTTON_ID_CANCEL = "cancel";

    private SentMessage message;
    private final Function<Integer, Message> pages;

    private final Supplier<Integer> maxPage;

    private int currentPage;

    public ButtonMultiPageHandler(SentMessage message, long messageId, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        super(messageId, (event) -> false, () -> {
        });
        this.message = message;
        this.pages = pages;
        this.maxPage = maxPage;
        this.currentPage = 0;
    }

    public static Component[] getActionRow() {
        return new Component[]{
                Button.primary(BUTTON_ID_LEFT_PAGE, Emoji.fromUnicode(ARROW_LEFT)),
                Button.primary(BUTTON_ID_RIGHT_PAGE, Emoji.fromUnicode(ARROW_RIGHT)),
                Button.secondary(BUTTON_ID_REFRESH, Emoji.fromUnicode(WHITE_CHECK_MARK)),
                Button.danger(BUTTON_ID_CANCEL, Emoji.fromUnicode(X))
        };
    }

    @Override
    public boolean handle(ButtonClickEvent event) {
        super.handle(event);

        if (event.getButton() == null || event.getButton().getId() == null) return false;

        if (BUTTON_ID_CANCEL.equals(event.getButton().getId())) {
            this.message = new ButtonClickEventAdapter(event);
            return true;
        }

        int mod = maxPage.get() + 1;
        int nextPage = switch (event.getButton().getId()) {
            case BUTTON_ID_LEFT_PAGE -> (currentPage - 1 + mod) % mod;
            case BUTTON_ID_RIGHT_PAGE -> (currentPage + 1) % mod;
            default -> currentPage;
        };

        event.editMessage(
                new MessageBuilder(pages.apply(nextPage))
                        .setActionRows(ActionRow.of(getActionRow()))
                        .build()
        ).queue();

        this.currentPage = nextPage;

        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Delete buttons
        this.message.editComponents();
    }

    public void getMessage(Consumer<Message> callback) {
        this.message.getMessage(callback);
    }

    public void setPageAndUpdate(int page) {
        int mod = this.maxPage.get() + 1;
        int nextPage = ((page % mod) + mod) % mod;

        this.message.editMessage(
                new MessageBuilder(pages.apply(nextPage))
                        .setActionRows(ActionRow.of(getActionRow()))
                        .build()
        );

        this.currentPage = nextPage;
    }
}

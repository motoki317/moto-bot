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

import java.util.function.Function;
import java.util.function.Supplier;

public class ButtonMultiPageHandler extends ButtonClickHandler {
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
                Button.primary(BUTTON_ID_LEFT_PAGE, Emoji.fromUnicode("\u2B05")),
                Button.primary(BUTTON_ID_RIGHT_PAGE, Emoji.fromUnicode("\u27A1")),
                Button.secondary(BUTTON_ID_REFRESH, Emoji.fromUnicode("\u2705")),
                Button.danger(BUTTON_ID_CANCEL, Emoji.fromUnicode("\u274C"))
        };
    }

    @Override
    public boolean handle(ButtonClickEvent event) {
        super.handle(event);

        if (event.getButton() == null || event.getButton().getId() == null) return false;

        int nextPage = currentPage;
        switch (event.getButton().getId()) {
            case BUTTON_ID_LEFT_PAGE:
                nextPage--;
                break;
            case BUTTON_ID_RIGHT_PAGE:
                nextPage++;
                break;
            case BUTTON_ID_REFRESH:
                break;
            case BUTTON_ID_CANCEL:
                this.message = new ButtonClickEventAdapter(event);
                return true;
            default:
                return false;
        }

        int mod = maxPage.get() + 1;
        nextPage = (nextPage + mod) % mod;

        this.currentPage = nextPage;
        event.editMessage(
                new MessageBuilder(pages.apply(nextPage))
                        .setActionRows(ActionRow.of(getActionRow()))
                        .build()
        ).queue();

        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Delete buttons
        this.message.editComponents();
    }
}

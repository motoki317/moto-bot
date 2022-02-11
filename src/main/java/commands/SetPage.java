package commands;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import commands.event.MessageReceivedEventAdapter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import update.button.ButtonClickManager;

import java.util.concurrent.TimeUnit;

public class SetPage extends GenericCommand {
    private final ButtonClickManager buttonClickManager;

    public SetPage(Bot bot) {
        this.buttonClickManager = bot.getButtonClickManager();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"page"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"page"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.INTEGER, "page", "New page number", true)
        };
    }

    @Override
    public @NotNull String syntax() {
        return "page <new page>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Sets page for the last multi-page message sent in the channel.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Page command help")
                        .setDescription(String.join("\n", new String[]{
                                this.shortHelp(),
                                "Can be used to skip multiple pages in long command response, like `g ws`."
                        }))
                        .addField("Example", "`page 15` skips the page to page 15.", false)
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
            event.reply(this.longHelp());
            return;
        }

        int newPage;
        try {
            newPage = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            event.reply("Please input a valid number for the argument. Example: `page 15`");
            return;
        }

        if (newPage < 1) {
            event.reply("Please input a valid page number, greater than 0.");
            return;
        }

        long channelId = event.getChannel().getIdLong();

        // from 1-indexed to 0-indexed
        if (this.buttonClickManager.setPage(channelId, newPage - 1)) {
            // delete user message as well if possible
            if (event instanceof MessageReceivedEventAdapter a) {
                try {
                    a.event().getMessage().delete().queue();
                } catch (Exception ignored) {
                }
            }

            // delete success message after 3 seconds
            event.reply(String.format("Skipped to page %s.", newPage),
                    message -> message.deleteAfter(3, TimeUnit.SECONDS));
        } else {
            event.reply("Message not found.");
        }
    }
}

package commands;

import app.Bot;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import update.reaction.ReactionManager;

import java.util.concurrent.TimeUnit;

public class SetPage extends GenericCommand {
    private final ReactionManager reactionManager;

    public SetPage(Bot bot) {
        this.reactionManager = bot.getReactionManager();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"page"}};
    }

    @Override
    public @NotNull String syntax() {
        return "page <new page>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Sets page for the last multi-page message sent in the channel, requested by you.";
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
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length < 2) {
            respond(event, this.longHelp());
            return;
        }

        int newPage;
        try {
            newPage = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            respond(event, "Please input a valid number for the argument. Example: `page 15`");
            return;
        }

        if (newPage < 1) {
            respond(event, "Please input a valid page number, greater than 0.");
            return;
        }

        long userId = event.getAuthor().getIdLong();
        long channelId = event.getChannel().getIdLong();

        // from 1-indexed to 0-indexed
        if (this.reactionManager.setPage(userId, channelId, newPage - 1)) {
            // delete user message as well if possible
            try {
                event.getMessage().delete().queue();
            } catch (Exception ignored) {}

            // delete success message after 3 seconds
            respond(event, String.format("Skipped to page %s.", newPage),
                    message -> message.delete().queueAfter(3, TimeUnit.SECONDS));
        } else {
            respond(event, "Message not found.");
        }
    }
}

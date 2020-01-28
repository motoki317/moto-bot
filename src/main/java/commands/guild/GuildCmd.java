package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class GuildCmd extends GenericCommand {
    private final GuildStats.Handler handler;

    public GuildCmd(Bot bot) {
        this.handler = new GuildStats.Handler(bot);
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild", "gStats", "guildStats"}};
    }

    @Override
    public @NotNull String syntax() {
        return "g <guild name>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows a guild's general information. Alias for g stats or gstats.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Guild Command Help")
                        .setDescription(this.shortHelp())
                        .addField(
                                "Syntax",
                                "`" + this.syntax() + "`",
                                false
                        )
                        .build()
        ).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(event, this.longHelp());
            return;
        }

        String guildName = String.join("\n", Arrays.copyOfRange(args, 1, args.length));
        handler.handle(event, guildName);
    }
}

package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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
    public @NotNull String[] slashName() {
        return new String[]{"gs"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "guild", "Name or prefix of a guild", true)
        };
    }

    @Override
    public @NotNull String syntax() {
        return "g <guild name>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows a guild's information.";
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
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(3);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            event.reply(this.longHelp());
            return;
        }

        String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        handler.handle(event, guildName);
    }
}

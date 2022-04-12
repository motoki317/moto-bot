package commands;

import app.Bot;
import app.Properties;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Info extends GenericCommand {
    private final Bot bot;

    public Info(Bot bot) {
        this.bot = bot;
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"info"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"info"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{};
    }

    @NotNull
    @Override
    public String syntax() {
        return "info";
    }

    @NotNull
    @Override
    public String shortHelp() {
        return "Shows meta info of the bot.";
    }

    @NotNull
    @Override
    public Message longHelp() {
        return new MessageBuilder(
                "Shows info of the bot, such as version, bot invite link, privacy policy, and bot support server link."
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        event.reply(getInfo().build());
    }

    private EmbedBuilder getInfo() {
        EmbedBuilder eb = new EmbedBuilder();
        Properties properties = this.bot.getProperties();

        eb.setAuthor("Bot Info", "https://forums.wynncraft.com/threads/223425/",
                properties.githubImagesUrl + "moto-icon.png");
        eb.setColor(properties.getMainColor());

        int serverNum = this.bot.getManager().getGuilds().size();

        List<String> description = new ArrayList<>();
        description.add("**A Discord Bot for Wynncraft Utility Commands.**");
        description.add("**Author:** motoki1#8508");
        description.add("**Servers:** " + serverNum);
        description.add("**Bot Server:** " + properties.botServerInviteUrl);

        description.add("**Version:** " + properties.version);
        description.add("**Commit:** `" + properties.gitCommitShort + "`");
        description.add("**Repository:** " + properties.repositoryUrl);

        description.add("");
        description.add("Some concepts inspired by:");
        description.add("[WynnBot](https://forums.wynncraft.com/threads/231863/) by Wurst#1783");
        description.add("[WynnStats Bot](https://forums.wynncraft.com/threads/214023/) by Infernis#6699");

        eb.setDescription(String.join("\n", description));

        eb.addField("About your message content",
                """
                        The bot may collect and log your message content for the following purposes.
                        1. The bot will log your command message to improve the bot by analyzing command uses. Only the bot author can access the command usage data.
                        2. The bot will collect all message content it can see in order to log message edits/deletes in the server log feature (`>serverlog`). This is **disabled by default**. Only admins of the server can enable this. The bot author cannot access this message content data.
                        For more and if you have any questions, join the bot support server via the invite link above, or DM the bot author.
                        """,
                false);

        User botUser = this.bot.getManager().getUserById(properties.botDiscordId);
        if (botUser != null) {
            eb.setThumbnail(botUser.getEffectiveAvatarUrl());
        }

        eb.setTitle("Click here to invite the bot!", properties.botInviteUrl);
        eb.setFooter("Last Reboot", properties.wynnIconUrl);
        eb.setTimestamp(Instant.ofEpochMilli(properties.lastReboot.getTime()));

        return eb;
    }
}

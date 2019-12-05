package commands;

import app.Bot;
import app.Properties;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Info extends GenericCommand {
    private final Bot bot;

    public Info(Bot bot) {
        this.bot = bot;
    }

    @Override
    public String[] names() {
        return new String[]{"info"};
    }

    @Override
    public void process(MessageReceivedEvent event, String[] args) {
        event.getChannel().sendMessage(getInfo().build()).queue();
    }

    private EmbedBuilder getInfo() {
        DateFormat outFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
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

        String versionStr = properties.version;
        if (!properties.herokuReleaseVersion.equals("")) {
            versionStr += "_" + properties.herokuReleaseVersion;
        }
        description.add("**Version:** " + versionStr);

        description.add("**Last Version Release Date:**");
        description.add(outFormat.format(properties.releaseDate) + " UTC");
        description.add("");
        description.add("Some concepts inspired by:");
        description.add("[WynnBot](https://forums.wynncraft.com/threads/231863/) by Wurst#1783");
        description.add("[WynnStats Bot](https://forums.wynncraft.com/threads/214023/) by Infernis#6699");

        eb.setDescription(String.join("\n", description));

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

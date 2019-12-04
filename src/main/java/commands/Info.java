package commands;

import app.App;
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
    @Override
    public String[] names() {
        return new String[]{"info"};
    }

    @Override
    public void process(MessageReceivedEvent event, String[] args) {
        event.getChannel().sendMessage(getInfo().build()).queue();
    }

    private static EmbedBuilder getInfo() {
        DateFormat outFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        EmbedBuilder eb = new EmbedBuilder();

        eb.setAuthor("Bot Info", "https://forums.wynncraft.com/threads/223425/",
                App.BOT_PROPERTIES.githubImagesUrl + "moto-icon.png");
        eb.setColor(App.BOT_PROPERTIES.getMainColor());

        int serverNum = App.MANAGER.getGuilds().size();

        List<String> description = new ArrayList<>();
        description.add("**A Discord Bot for Wynncraft Utility Commands.**");
        description.add("**Author:** motoki1#8508");
        description.add("**Servers:** " + serverNum);
        description.add("**Bot Server:** " + App.BOT_PROPERTIES.botServerInviteUrl);

        String versionStr = App.BOT_PROPERTIES.version;
        if (!App.BOT_PROPERTIES.herokuReleaseVersion.equals("")) {
            versionStr += "_" + App.BOT_PROPERTIES.herokuReleaseVersion;
        }
        description.add("**Version:** " + versionStr);

        description.add("**Last Version Release Date:**");
        description.add(outFormat.format(App.BOT_PROPERTIES.releaseDate) + " UTC");
        description.add("");
        description.add("Some concepts inspired by:");
        description.add("[WynnBot](https://forums.wynncraft.com/threads/231863/) by Wurst#1783");
        description.add("[WynnStats Bot](https://forums.wynncraft.com/threads/214023/) by Infernis#6699");

        eb.setDescription(String.join("\n", description));

        User botUser = App.MANAGER.getUserById(App.BOT_PROPERTIES.botDiscordId);
        if (botUser != null) {
            eb.setThumbnail(botUser.getEffectiveAvatarUrl());
        }

        eb.setTitle("Click here to invite the bot!", App.BOT_PROPERTIES.botInviteUrl);
        eb.setFooter("Last Reboot", App.BOT_PROPERTIES.wynnIconUrl);
        eb.setTimestamp(Instant.ofEpochMilli(App.BOT_PROPERTIES.lastReboot.getTime()));

        return eb;
    }
}

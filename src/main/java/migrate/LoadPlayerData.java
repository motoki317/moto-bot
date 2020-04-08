package migrate;

import app.Bot;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.motobot.DiscordBot.PlayerData;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class LoadPlayerData extends GenericCommand {
    public LoadPlayerData(Bot bot) {
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"migrate"}, {"playerData"}};
    }

    @Override
    public @NotNull String syntax() {
        return "migrate playerData";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Migrate player data.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(this.shortHelp()).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        File file = new File("data/PlayerData");
        PlayerData data;
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fis);
            data = (PlayerData) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            respondError(event, "an exception occurred while opening file");
            return;
        }

        // debug
        // to add data to db
        respond(event, "" + data);
    }
}

package migrate;

import app.Bot;
import commands.base.GenericCommand;
import db.model.musicSetting.MusicSetting;
import db.repository.base.MusicSettingRepository;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.motobot.DiscordBot.PlayerData;
import net.motobot.music.MusicSettings;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

public class LoadPlayerData extends GenericCommand {
    private final MusicSettingRepository musicSettingRepository;

    public LoadPlayerData(Bot bot) {
        this.musicSettingRepository = bot.getDatabase().getMusicSettingRepository();
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

        if (args.length <= 2) {
            // debug
            // to add data to db
            respond(event, "" + data);
            return;
        }

        switch (args[2]) {
            case "musicSetting":
                migrateMusicSettings(data);
                respond(event, "Successfully migrated music settings!");
                return;
            default:
                respondException(event, "Unknown operation.");
                return;
        }
    }

    private static MusicSetting formatToNewSetting(long guildId, MusicSettings s) {
        return new MusicSetting(
                guildId,
                s.getVolume(),
                s.getRepeat().toString(),
                s.isShowNp(),
                s.getRestrictChannel() != 0 ? s.getRestrictChannel() : null
        );
    }

    private void migrateMusicSettings(PlayerData data) {
        for (Map.Entry<Long, MusicSettings> entry : data.musicSettings.entrySet()) {
            Long guildId = entry.getKey();
            MusicSettings settings = entry.getValue();
            MusicSetting newSettings = formatToNewSetting(guildId, settings);
            boolean res;
            if (this.musicSettingRepository.exists(() -> guildId)) {
                res = this.musicSettingRepository.update(newSettings);
            } else {
                res = this.musicSettingRepository.create(newSettings);
            }
            if (!res) {
                throw new RuntimeException("Failed to migrate music settings");
            }
        }
    }
}

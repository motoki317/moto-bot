package music.handlers;

import app.Bot;
import db.model.musicSetting.MusicSetting;
import db.repository.base.MusicSettingRepository;
import music.MusicState;
import music.RepeatState;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.MinecraftColor;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static commands.base.BotCommand.*;

public class MusicSettingHandler {
    private final Map<Long, MusicState> states;

    private final ShardManager manager;
    private final MusicSettingRepository musicSettingRepository;

    public MusicSettingHandler(Bot bot, Map<Long, MusicState> states) {
        this.states = states;
        this.manager = bot.getManager();
        this.musicSettingRepository = bot.getDatabase().getMusicSettingRepository();
    }

    /**
     * Retrieves setting for the guild.
     * @param guildId Guild ID.
     * @return Music setting. Default if not found.
     */
    @NotNull
    private MusicSetting getSetting(long guildId) {
        MusicState state;
        synchronized (states) {
            state = states.getOrDefault(guildId, null);
        }
        if (state != null) {
            return state.getSetting();
        }
        MusicSetting setting = this.musicSettingRepository.findOne(() -> guildId);
        return setting != null ? setting : MusicSetting.getDefault(guildId);
    }

    /**
     * Saves setting for the guild.
     * @param setting Setting.
     * @return {@code true} if success.
     */
    private boolean saveSetting(MusicSetting setting) {
        return this.musicSettingRepository.exists(setting)
                ? this.musicSettingRepository.update(setting)
                : this.musicSettingRepository.create(setting);
    }

    private final static int MAX_VOLUME = 150;

    private static MessageEmbed volumeHelp(int currentVolume, String botAvatarURL) {
        return new EmbedBuilder()
                .setAuthor("Volume Setting", null, botAvatarURL)
                .setDescription("Adjusts the player's volume.")
                .addField("Current", String.format("%s%%", currentVolume), false)
                .addField("Update", "`m volume <percentage>`\ne.g. `m volume 50`", false)
                .build();
    }

    /**
     * Handles "volume" command.
     * @param event Event.
     * @param args Command arguments.
     */
    public void handleVolume(MessageReceivedEvent event, String[] args) {
        long guildId = event.getGuild().getIdLong();
        MusicState state;
        synchronized (states) {
            state = states.getOrDefault(guildId, null);
        }
        MusicSetting setting = state != null ? state.getSetting() : getSetting(guildId);
        int oldVolume = setting.getVolume();

        if (args.length <= 2) {
            respond(event, volumeHelp(oldVolume, event.getJDA().getSelfUser().getEffectiveAvatarUrl()));
            return;
        }

        int newVolume;
        try {
            // accepts formats such as "50%" and "50"
            Matcher m = Pattern.compile("(\\d+)%").matcher(args[2]);
            if (m.matches()) {
                newVolume = Integer.parseInt(m.group(1));
            } else {
                newVolume = Integer.parseInt(args[2]);
            }
            if (newVolume < 0 || MAX_VOLUME < newVolume) {
                respond(event, String.format("Please input a number between 0 and %s for the new volume!", MAX_VOLUME));
                return;
            }
        } catch (NumberFormatException e) {
            respondException(event, "Please input a valid number for the new volume!");
            return;
        }

        setting.setVolume(newVolume);
        if (state != null) {
            state.getPlayer().setVolume(newVolume);
        }

        if (this.saveSetting(setting)) {
            respond(event, new EmbedBuilder()
                    .setColor(MinecraftColor.DARK_GREEN.getColor())
                    .setDescription(String.format("Set volume from `%s%%` to `%s%%`!", oldVolume, newVolume))
                    .build());
        } else {
            respondError(event, "Something went wrong while saving setting...");
        }
    }

    private static MessageEmbed repeatHelp(RepeatState current, String botAvatarURL) {
        return new EmbedBuilder()
                .setAuthor("Repeat Setting", null, botAvatarURL)
                .setDescription("Sets the repeat mode.")
                .addField("Current", current.name(), false)
                .addField("Update", "`m repeat <mode>`", false)
                .addField("Available Modes",
                        Arrays.stream(RepeatState.values())
                                .map(r -> String.format("**%s** : %s", r.name(), r.getDescription()))
                                .collect(Collectors.joining("\n")),
                        false)
                .build();
    }

    /**
     * Handles "repeat" command.
     * @param event Event.
     * @param args Command arguments.
     */
    public void handleRepeat(MessageReceivedEvent event, String[] args) {
        MusicSetting setting = getSetting(event.getGuild().getIdLong());
        RepeatState oldState = setting.getRepeat();

        if (args.length <= 2) {
            respond(event, repeatHelp(oldState, event.getJDA().getSelfUser().getEffectiveAvatarUrl()));
            return;
        }

        RepeatState newState;
        try {
            newState = RepeatState.valueOf(args[2].toUpperCase());
        } catch (IllegalStateException e) {
            respondException(event, "Invalid repeat type.");
            return;
        }

        setting.setRepeat(newState);

        if (this.saveSetting(setting)) {
            respond(event, new EmbedBuilder()
                    .setColor(MinecraftColor.DARK_GREEN.getColor())
                    .setDescription(String.format("Set repeat mode from `%s` to `%s`!", oldState.name(), newState.name()))
                    .build());
        } else {
            respondError(event, "Something went wrong while saving setting...");
        }
    }

    private MessageEmbed settingHelp(MusicSetting current, String botAvatarURL) {
        TextChannel restrict = current.getRestrictChannel() != null
                ? this.manager.getTextChannelById(current.getRestrictChannel())
                : null;

        return new EmbedBuilder()
                .setAuthor("Music Other Settings", null, botAvatarURL)
                .setDescription("Use the command written below each option to set options.")
                .addField("Show Now Playing Messages",
                        String.format("Current: **%s**\n`m setting shownp <ON/OFF>`", current.isShowNp() ? "ON" : "OFF"),
                        true)
                .addField("Channel Restriction",
                        String.format("Current: %s\n`m setting restrict <ON/OFF>`",
                                restrict != null ? restrict.getAsMention() :
                                        (current.getRestrictChannel() != null ? "ID: " + current : "None")),
                        true)
                .build();
    }

    /**
     * Handles "setting" command.
     * @param event Event.
     * @param args Command arguments.
     */
    public void handleSetting(MessageReceivedEvent event, String[] args) {
        MusicSetting setting = getSetting(event.getGuild().getIdLong());

        if (args.length <= 2) {
            respond(event, settingHelp(setting, event.getJDA().getSelfUser().getEffectiveAvatarUrl()));
            return;
        }

        switch (args[2].toLowerCase()) {
            case "shownp":
                handleShowNP(event, setting, args);
                return;
            case "restrict":
            case "restriction":
                handleRestriction(event, setting, args);
                return;
        }

        respond(event, "Unknown music setting.");
    }

    private static MessageEmbed showNPHelp(boolean current, String botAvatarURL) {
        return new EmbedBuilder()
                .setAuthor("Music Other Settings - Show Now Playing Messages", null, botAvatarURL)
                .setDescription("If enabled, it will send a message of the song info on each start.\nDefault: ON")
                .addField("Current", current ? "ON" : "OFF", false)
                .addField("Update", "`m setting shownp <ON/OFF>`", false)
                .build();
    }

    /**
     * Handles "setting shownp" command.
     * @param event Event.
     * @param setting Current setting.
     * @param args Command arguments.
     */
    private void handleShowNP(MessageReceivedEvent event, MusicSetting setting, String[] args) {
        if (args.length <= 3) {
            respond(event, showNPHelp(setting.isShowNp(), event.getJDA().getSelfUser().getEffectiveAvatarUrl()));
            return;
        }

        boolean newValue;
        switch (args[3].toUpperCase()) {
            case "ON":
                newValue = true;
                break;
            case "OFF":
                newValue = false;
                break;
            default:
                respond(event, "Input either `ON` OR `OFF` for the new value!");
                return;
        }

        setting.setShowNp(newValue);

        if (this.saveSetting(setting)) {
            respond(event, new EmbedBuilder()
                    .setColor(MinecraftColor.DARK_GREEN.getColor())
                    .setDescription(String.format("The bot will %s display now playing messages!", newValue ? "now" : "no longer"))
                    .build());
        } else {
            respondError(event, "Something went wrong while saving setting...");
        }
    }

    private MessageEmbed restrictionHelp(@Nullable Long current, String botAvatarURL) {
        TextChannel restrict = current != null ? this.manager.getTextChannelById(current) : null;

        return new EmbedBuilder()
                .setAuthor("Music Other Settings - Channel Restriction", null, botAvatarURL)
                .setDescription("If enabled, users will only be able to use music commands in this channel.")
                .addField("Current", restrict != null ? restrict.getAsMention()
                        : (current != null ? "ID: " + current : "None"), false)
                .build();
    }

    /**
     * Handles "setting restrict" command.
     * @param event Event.
     * @param setting Current setting.
     * @param args Command arguments.
     */
    private void handleRestriction(MessageReceivedEvent event, MusicSetting setting, String[] args) {
        if (args.length <= 3) {
            respond(event, restrictionHelp(setting.getRestrictChannel(), event.getJDA().getSelfUser().getEffectiveAvatarUrl()));
            return;
        }

        Long newValue;
        switch (args[3].toUpperCase()) {
            case "ON":
                newValue = event.getChannel().getIdLong();
                break;
            case "OFF":
                newValue = null;
                break;
            default:
                respond(event, "Input either `ON` OR `OFF` for the new value!");
                return;
        }

        setting.setRestrictChannel(newValue);

        if (this.saveSetting(setting)) {
            String newSetting = newValue != null ? event.getTextChannel().getAsMention() : "None";
            respond(event, new EmbedBuilder()
                    .setColor(MinecraftColor.DARK_GREEN.getColor())
                    .setDescription(String.format("Successfully set channel restriction to: %s!", newSetting))
                    .build());
        } else {
            respondError(event, "Something went wrong while saving setting...");
        }
    }
}

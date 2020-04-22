package music;

import app.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import commands.base.GuildCommand;
import db.model.musicSetting.MusicSetting;
import db.repository.base.MusicSettingRepository;
import music.handlers.MusicManagementHandler;
import music.handlers.MusicPlayHandler;
import music.handlers.MusicSettingHandler;
import music.handlers.SearchSite;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Music extends GuildCommand {
    private static final Map<Long, MusicState> states;
    private static final AudioPlayerManager playerManager;

    static {
        states = new HashMap<>();
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    private final Map<String, BiConsumer<MessageReceivedEvent, String[]>> commands;

    // Command handlers
    private final MusicPlayHandler playHandler;
    private final MusicManagementHandler managementHandler;
    private final MusicSettingHandler settingHandler;

    private final ShardManager manager;
    private final MusicSettingRepository musicSettingRepository;

    public Music(Bot bot) {
        this.commands = new HashMap<>();
        this.manager = bot.getManager();
        this.musicSettingRepository = bot.getDatabase().getMusicSettingRepository();
        this.playHandler = new MusicPlayHandler(bot, states, playerManager);
        this.managementHandler = new MusicManagementHandler(bot, states);
        this.settingHandler = new MusicSettingHandler(bot, states);

        this.registerCommands();
    }

    @SuppressWarnings("OverlyLongMethod")
    private void registerCommands() {
        // Join and leave handlers
        commands.put("j", (event, args) -> this.playHandler.handleJoin(event));
        commands.put("join", (event, args) -> this.playHandler.handleJoin(event));

        commands.put("l", (event, args) -> this.playHandler.handleLeave(event, true));
        commands.put("leave", (event, args) -> this.playHandler.handleLeave(event, true));
        commands.put("stop", (event, args) -> this.playHandler.handleLeave(event, true));

        commands.put("c", (event, args) -> this.playHandler.handleLeave(event, false));
        commands.put("clear", (event, args) -> this.playHandler.handleLeave(event, false));

        // Play handlers
        commands.put("p", (event, args) -> this.playHandler.handlePlay(event, args, false, SearchSite.YouTube));
        commands.put("play", (event, args) -> this.playHandler.handlePlay(event, args, false, SearchSite.YouTube));

        commands.put("pa", (event, args) -> this.playHandler.handlePlay(event, args, true, SearchSite.YouTube));
        commands.put("playall", (event, args) -> this.playHandler.handlePlay(event, args, true, SearchSite.YouTube));

        commands.put("sc", (event, args) -> this.playHandler.handlePlay(event, args, false, SearchSite.SoundCloud));
        commands.put("soundcloud", (event, args) -> this.playHandler.handlePlay(event, args, false, SearchSite.SoundCloud));

        // Player management handlers
        commands.put("np", (event, args) -> this.managementHandler.handleNowPlaying(event));
        commands.put("nowplaying", (event, args) -> this.managementHandler.handleNowPlaying(event));

        commands.put("q", (event, args) -> this.managementHandler.handleQueue(event));
        commands.put("queue", (event, args) -> this.managementHandler.handleQueue(event));

        commands.put("pause", (event, args) -> this.managementHandler.handlePause(event, true));

        commands.put("resume", (event, args) -> this.managementHandler.handlePause(event, false));

        commands.put("s", this.managementHandler::handleSkip);
        commands.put("skip", this.managementHandler::handleSkip);

        commands.put("seek", this.managementHandler::handleSeek);

        commands.put("shuffle", (event, args) -> this.managementHandler.handleShuffle(event));

        commands.put("purge", (event, args) -> this.managementHandler.handlePurge(event));

        // Setting handlers
        commands.put("v", this.settingHandler::handleVolume);
        commands.put("vol", this.settingHandler::handleVolume);
        commands.put("volume", this.settingHandler::handleVolume);

        commands.put("r", this.settingHandler::handleRepeat);
        commands.put("repeat", this.settingHandler::handleRepeat);

        commands.put("setting", this.settingHandler::handleSetting);
        commands.put("settings", this.settingHandler::handleSetting);
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"m", "music"}};
    }

    @Override
    public @NotNull String syntax() {
        return "music";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Music commands! `music help` for more.";
    }

    @Override
    public @NotNull Message longHelp() {
        JDA jda = this.manager.getShards().stream().findAny().orElse(null);
        String selfUserImage = jda != null
                ? jda.getSelfUser().getEffectiveAvatarUrl()
                : null;
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("♪ Music Help", null, selfUserImage)
                .addField("Join and Leave", String.join("\n",
                        "**m join** : Joins the voice channel.",
                                "**m leave** : Leaves the voice channel (saves the current queue).",
                                "**m clear** : Leaves the voice channel (does NOT save the current queue)."
                ), false)
                .addField("Play Songs", String.join("\n",
                        "**m play <keyword / URL>** : Searches and plays from the keyword / URL.",
                                "**m playAll <keyword / URL>** : Plays all search results from the keyword / URL.",
                                "**m soundcloud <keyword / URL>** : Searches SoundCloud with keyword / URL."
                ), false)
                .addField("Player Management", String.join("\n",
                        "**m nowPlaying** : Shows the current song.",
                                "**m queue** : Shows the queue.",
                                "**m pause** : Pauses the song.",
                                "**m resume** : Resumes the song.",
                                "**m skip [num]** : Skips the current song. Append a number to skip multiple songs at once.",
                                "**m seek <time>** : Seeks the current song to the specified time. e.g. `m seek 1:50`",
                                "**m shuffle** : Shuffles the queue.",
                                "**m purge** : Purges all waiting songs in the queue. Does not stop the current song."
                ), false)
                .addField("Settings", String.join("\n",
                        "**m volume <percentage>** : Sets the volume. e.g. `m volume 50`",
                                "**m repeat <mode>** : Sets the repeat mode.",
                                "**m settings** : Other settings. Settings will be saved per guild if changed."
                ), false)
        ).build();
    }

    /**
     * Retrieves music setting for the guild.
     * @param guildId Guild ID.
     * @return Music setting. Default if not found.
     */
    @NotNull
    private MusicSetting getSetting(long guildId) {
        MusicSetting setting = this.musicSettingRepository.findOne(() -> guildId);
        return setting != null ? setting : MusicSetting.getDefault(guildId);
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(event, this.longHelp());
            return;
        }

        // Check bound channel ID
        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannel().getIdLong();
        MusicState state = states.getOrDefault(guildId, null);
        if (state != null && state.getBoundChannelId() != channelId) {
            TextChannel channel = this.manager.getTextChannelById(state.getBoundChannelId());
            respond(event, String.format("Music commands are currently bound to %s!",
                    channel != null ? channel.getAsMention() : "ID: " + state.getBoundChannelId()));
            return;
        }

        // Check music channel restriction setting
        MusicSetting setting = state != null ? state.getSetting() : getSetting(guildId);
        if (setting.getRestrictChannel() != null && setting.getRestrictChannel() != channelId) {
            TextChannel channel = this.manager.getTextChannelById(setting.getRestrictChannel());
            respond(event, String.format("Music commands are only allowed in %s!",
                    channel != null ? channel.getAsMention() : "ID: " + setting.getRestrictChannel()));
            return;
        }

        BiConsumer<MessageReceivedEvent, String[]> handler = this.commands.getOrDefault(args[1], null);
        if (handler != null) {
            handler.accept(event, args);
            return;
        }

        respond(event, "Unknown music command. Try `m help`!");
    }
}

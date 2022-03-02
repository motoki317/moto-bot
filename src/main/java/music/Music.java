package music;

import app.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import commands.base.GuildCommand;
import commands.event.CommandEvent;
import db.model.musicSetting.MusicSetting;
import db.repository.base.MusicSettingRepository;
import heartbeat.HeartBeatTask;
import io.prometheus.client.Gauge;
import log.Logger;
import music.handlers.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Music extends GuildCommand {
    public static final Gauge MUSIC_PLAYER_GAUGE = Gauge.build()
            .name("moto_bot_music_players")
            .help("Count of music players set up.")
            .register();

    private static final Map<Long, MusicState> states;
    private static final AudioPlayerManager playerManager;

    static {
        states = new HashMap<>();
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    private final Map<String, BiConsumer<CommandEvent, String[]>> commands;

    // Command handlers
    private final MusicPlayHandler playHandler;
    private final MusicManagementHandler managementHandler;
    private final MusicSettingHandler settingHandler;

    // Music related heartbeat
    private final HeartBeatTask heartbeat;

    private final Logger logger;
    private final ShardManager manager;
    private final MusicSettingRepository musicSettingRepository;

    public Music(Bot bot) {
        this.commands = new HashMap<>();
        this.logger = bot.getLogger();
        this.manager = bot.getManager();
        this.musicSettingRepository = bot.getDatabase().getMusicSettingRepository();
        this.playHandler = new MusicPlayHandler(bot, states, playerManager);
        this.managementHandler = new MusicManagementHandler(bot);
        this.settingHandler = new MusicSettingHandler(bot);

        this.registerCommands();

        // Rejoin interrupted guilds on shutdown
        this.playHandler.rejoinInterruptedGuilds();

        // Register music related heartbeat
        this.logger.debug("Starting music heartbeat...");
        MusicAutoLeaveChecker autoLeaveChecker = new MusicAutoLeaveChecker(bot, states, this.playHandler);
        this.heartbeat = new HeartBeatTask(bot.getLogger(), new MusicHeartBeat(bot, autoLeaveChecker));
        this.heartbeat.start();

        // Save all players on shutdown to be re-joined above
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.logger.log(-1, "Stopping music heartbeat and clearing up...");
            this.heartbeat.clearUp();
            autoLeaveChecker.forceShutdownAllGuilds();
            this.logger.log(-1, "Cleared up music feature!");
        }));
    }

    private interface MusicSubCommandRequireMusicState {
        void handle(CommandEvent event, String[] args, MusicState state);
    }

    /**
     * Requires the guild to have a player set up, AND requires the user to be in the same voice channel
     * as the bot is in.
     *
     * @param next Next handler.
     * @return Wrapped handler.
     */
    private BiConsumer<CommandEvent, String[]> requireMusicState(MusicSubCommandRequireMusicState next) {
        return (event, args) -> {
            MusicState state;
            synchronized (states) {
                state = states.getOrDefault(event.getGuild().getIdLong(), null);
            }
            if (state == null) {
                event.reply("This guild doesn't seem to have a music player set up.");
                return;
            }

            // Require user to be in the same voice channel
            // null chaining
            long userVCId = Optional.of(event)
                    .map(CommandEvent::getGuild)
                    .map(g -> g.getMember(event.getAuthor()))
                    .map(Member::getVoiceState)
                    .map(GuildVoiceState::getChannel)
                    .map(ISnowflake::getIdLong)
                    .orElse(0L);
            if (state.getVoiceChannelId() != userVCId) {
                event.reply("You have to be in the same voice channel as the bot is in to use this command.");
                return;
            }

            next.handle(event, args, state);
        };
    }

    private interface MusicSettingSubCommand {
        void handle(CommandEvent event, String[] args, MusicSetting setting);
    }

    /**
     * Requires music setting.
     * If the guild currently has a player set up, requires the user to be in the same voice channel
     * as the bot is in.
     * If not, returns the guild music setting (or default if the guild doesn't have one).
     *
     * @param next Next handler.
     * @return Wrapped handler.
     */
    private BiConsumer<CommandEvent, String[]> requireMusicSetting(MusicSettingSubCommand next) {
        return (event, args) -> {
            long guildId = event.getGuild().getIdLong();
            MusicState state;
            synchronized (states) {
                state = states.getOrDefault(guildId, null);
            }

            if (state != null) {
                // Require user to be in the same voice channel
                // null chaining
                long userVCId = Optional.of(event)
                        .map(CommandEvent::getGuild)
                        .map(g -> g.getMember(event.getAuthor()))
                        .map(Member::getVoiceState)
                        .map(GuildVoiceState::getChannel)
                        .map(ISnowflake::getIdLong)
                        .orElse(0L);
                if (state.getVoiceChannelId() != userVCId) {
                    event.reply("You have to be in the same voice channel as the bot is in to use this command," +
                            " while the player is set up.");
                    return;
                }
            }

            MusicSetting setting = state != null ? state.getSetting() : getSetting(guildId);
            next.handle(event, args, setting);
        };
    }

    @SuppressWarnings("OverlyLongMethod")
    private void registerCommands() {
        // Join and leave handlers
        commands.put("j", (event, args) -> this.playHandler.handleJoin(event));
        commands.put("join", (event, args) -> this.playHandler.handleJoin(event));

        commands.put("l", requireMusicState(
                (event, args, state) -> this.playHandler.handleLeave(event, state, true)
        ));
        commands.put("leave", requireMusicState(
                (event, args, state) -> this.playHandler.handleLeave(event, state, true)
        ));
        commands.put("stop", requireMusicState(
                (event, args, state) -> this.playHandler.handleLeave(event, state, true)
        ));

        commands.put("c", requireMusicState(
                (event, args, state) -> this.playHandler.handleLeave(event, state, false)
        ));
        commands.put("clear", requireMusicState(
                (event, args, state) -> this.playHandler.handleLeave(event, state, false)
        ));

        // Play handlers
        commands.put("p", (event, args) -> this.playHandler.handlePlay(event, args, false, SearchSite.YouTube));
        commands.put("play", (event, args) -> this.playHandler.handlePlay(event, args, false, SearchSite.YouTube));

        commands.put("pa", (event, args) -> this.playHandler.handlePlay(event, args, true, SearchSite.YouTube));
        commands.put("playall", (event, args) -> this.playHandler.handlePlay(event, args, true, SearchSite.YouTube));

        commands.put("sc", (event, args) -> this.playHandler.handlePlay(event, args, false, SearchSite.SoundCloud));
        commands.put("soundcloud", (event, args) -> this.playHandler.handlePlay(event, args, false, SearchSite.SoundCloud));

        // Player management handlers
        commands.put("np", requireMusicState(
                (event, args, state) -> this.managementHandler.handleNowPlaying(event, state)
        ));
        commands.put("nowplaying", requireMusicState(
                (event, args, state) -> this.managementHandler.handleNowPlaying(event, state)
        ));

        commands.put("q", requireMusicState(
                (event, args, state) -> this.managementHandler.handleQueue(event, state)
        ));
        commands.put("queue", requireMusicState(
                (event, args, state) -> this.managementHandler.handleQueue(event, state)
        ));

        commands.put("pause", requireMusicState(
                (event, args, state) -> this.managementHandler.handlePause(event, state, true)
        ));

        commands.put("resume", requireMusicState(
                (event, args, state) -> this.managementHandler.handlePause(event, state, false)
        ));

        commands.put("s", requireMusicState(this.managementHandler::handleSkip));
        commands.put("skip", requireMusicState(this.managementHandler::handleSkip));

        commands.put("seek", requireMusicState(this.managementHandler::handleSeek));

        commands.put("shuffle", requireMusicState(
                (event, args, state) -> this.managementHandler.handleShuffle(event, state)
        ));

        commands.put("purge", requireMusicState(
                (event, args, state) -> this.managementHandler.handlePurge(event, state)
        ));

        Function<CommandEvent, @Nullable MusicState> getOptionalMusicState = event -> {
            MusicState state;
            synchronized (states) {
                state = states.getOrDefault(event.getGuild().getIdLong(), null);
            }
            return state;
        };
        // Setting handlers
        commands.put("v", requireMusicSetting(
                (event, args, setting) ->
                        this.settingHandler.handleVolume(event, args, setting, getOptionalMusicState.apply(event))));
        commands.put("vol", requireMusicSetting(
                (event, args, setting) ->
                        this.settingHandler.handleVolume(event, args, setting, getOptionalMusicState.apply(event))));
        commands.put("volume", requireMusicSetting(
                (event, args, setting) ->
                        this.settingHandler.handleVolume(event, args, setting, getOptionalMusicState.apply(event))));

        commands.put("r", requireMusicSetting(this.settingHandler::handleRepeat));
        commands.put("repeat", requireMusicSetting(this.settingHandler::handleRepeat));

        commands.put("setting", requireMusicSetting(this.settingHandler::handleSetting));
        commands.put("settings", requireMusicSetting(this.settingHandler::handleSetting));
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"m", "music"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"m"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{};
    }

    @Override
    public @NotNull BaseCommand<CommandData> slashCommand() {
        return new CommandData("m", "Music commands.")
                .addSubcommands(
                        new SubcommandData("join", "Joins the VC."),
                        new SubcommandData("leave", "Leaves the VC."),
                        new SubcommandData("stop", "Stops the player and leave VC."),
                        new SubcommandData("clear", "Stops the player, clear queue, and leave VC."),

                        new SubcommandData("play", "Searches and plays from keyword / URL.")
                                .addOption(OptionType.STRING, "keyword", "Keyword or URL", true),
                        new SubcommandData("playall", "Plays all search results from the keyword / URL.")
                                .addOption(OptionType.STRING, "keyword", "Keyword or URL", true),
                        new SubcommandData("soundcloud", "Searches SoundCloud with keyword / URL.")
                                .addOption(OptionType.STRING, "keyword", "Keyword or URL", true),

                        new SubcommandData("np", "Shows now-playing song."),
                        new SubcommandData("queue", "Shows the queue."),
                        new SubcommandData("pause", "Pauses the player."),
                        new SubcommandData("resume", "Resumes the player."),
                        new SubcommandData("skip", "Skips the current song.")
                                .addOption(OptionType.INTEGER, "num", "Number of songs to skip"),
                        new SubcommandData("seek", "Seek to a specified time.")
                                .addOption(OptionType.STRING, "time", "Time to seek to. e.g. `4:33`", true),
                        new SubcommandData("shuffle", "Shuffles the queue."),
                        new SubcommandData("purge", "Purges the queue. Does not stop the current song."),

                        new SubcommandData("volume", "Sets the volume.")
                                .addOption(OptionType.INTEGER, "volume", "Volume in percentage.", true),
                        new SubcommandData("repeat", "Sets the repeat mode.")
                                .addOptions(new OptionData(OptionType.STRING, "mode", "Repeat mode")
                                        .addChoice("off", "off")
                                        .addChoice("one", "one")
                                        .addChoice("queue", "queue")
                                        .addChoice("random", "random")
                                        .addChoice("random_repeat", "random_repeat")))
                .addSubcommandGroups(
                        new SubcommandGroupData("setting", "Other settings.")
                                .addSubcommands(
                                        new SubcommandData("shownp", "Whether to send a song info on start.")
                                                .addOptions(new OptionData(OptionType.STRING, "show", "Show or not")
                                                        .addChoice("on", "on")
                                                        .addChoice("off", "off")),
                                        new SubcommandData("restrict", "Restrict music commands to this channel.")
                                                .addOptions(new OptionData(OptionType.STRING, "restriction", "Restrict or not")
                                                        .addChoice("on", "on")
                                                        .addChoice("off", "off"))));
    }

    @Override
    public @NotNull String syntax() {
        return "music";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Music commands.";
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

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    /**
     * Retrieves music setting for the guild.
     *
     * @param guildId Guild ID.
     * @return Music setting. Default if not found.
     */
    @NotNull
    private MusicSetting getSetting(long guildId) {
        MusicSetting setting = this.musicSettingRepository.findOne(() -> guildId);
        return setting != null ? setting : MusicSetting.getDefault(guildId);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            event.reply(this.longHelp());
            return;
        }

        // Check bound channel ID
        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannel().getIdLong();
        MusicState state;
        synchronized (states) {
            state = states.getOrDefault(guildId, null);
        }
        if (state != null && state.getBoundChannelId() != channelId) {
            TextChannel channel = this.manager.getTextChannelById(state.getBoundChannelId());
            event.reply(String.format("Music commands are currently bound to %s!",
                    channel != null ? channel.getAsMention() : "ID: " + state.getBoundChannelId()));
            return;
        }

        // Check music channel restriction setting
        MusicSetting setting = state != null ? state.getSetting() : getSetting(guildId);
        if (setting.getRestrictChannel() != null && setting.getRestrictChannel() != channelId) {
            TextChannel channel = this.manager.getTextChannelById(setting.getRestrictChannel());
            event.reply(String.format("Music commands are only allowed in %s!",
                    channel != null ? channel.getAsMention() : "ID: " + setting.getRestrictChannel()));
            return;
        }

        BiConsumer<CommandEvent, String[]> handler = this.commands.getOrDefault(
                // case-insensitive sub commands
                args[1].toLowerCase(),
                null
        );
        if (handler != null) {
            handler.accept(event, args);
            return;
        }

        event.reply("Unknown music command. Try `m help`!");
    }
}

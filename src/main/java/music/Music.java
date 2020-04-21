package music;

import app.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import commands.base.GuildCommand;
import db.model.musicQueue.MusicQueueEntry;
import db.model.musicSetting.MusicSetting;
import db.repository.base.MusicQueueRepository;
import db.repository.base.MusicSettingRepository;
import log.Logger;
import music.exception.DuplicateTrackException;
import music.exception.QueueFullException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import update.response.Response;
import update.response.ResponseManager;
import utils.MinecraftColor;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static music.MusicUtils.formatLength;
import static music.MusicUtils.getThumbnailURL;

public class Music extends GuildCommand {
    private static final Map<Long, MusicState> states;
    private static final AudioPlayerManager playerManager;

    static {
        states = new HashMap<>();
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    private final ShardManager manager;
    private final Logger logger;
    private final MusicSettingRepository musicSettingRepository;
    private final MusicQueueRepository musicQueueRepository;
    private final ResponseManager responseManager;

    public Music(Bot bot) {
        this.manager = bot.getManager();
        this.logger = bot.getLogger();
        this.musicSettingRepository = bot.getDatabase().getMusicSettingRepository();
        this.musicQueueRepository = bot.getDatabase().getMusicQueueRepository();
        this.responseManager = bot.getResponseManager();
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
                                "**m purge** : Purges all songs in the queue, except the currently playing one."
                ), false)
                .addField("Settings", String.join("\n",
                        "**m volume <percentage>** : Sets the volume. e.g. `m volume 50`",
                                "**m repeat <mode>** : Sets the repeat mode.",
                                "**m settings** : Other settings. Settings will be saved per guild if changed."
                ), false)
        ).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(event, this.longHelp());
            return;
        }

        switch (args[1].toLowerCase()) {
            // Join and leave handlers
            case "j":
            case "join":
                handleJoin(event);
                return;
            case "l":
            case "leave":
                handleLeave(event, true);
                return;
            case "c":
            case "clear":
                handleLeave(event, false);
                return;
            // Play handlers
            case "p":
            case "play":
                handlePlay(event, args, false, SearchSite.YouTube);
                return;
            case "pa":
            case "playall":
                handlePlay(event, args, true, SearchSite.YouTube);
                return;
            case "sc":
            case "soundcloud":
                handlePlay(event, args, false, SearchSite.SoundCloud);
        }
    }

    /**
     * Retrieves voice channel the user is in.
     * @param event Event.
     * @return Voice channel. null if not found.
     */
    @Nullable
    private static VoiceChannel getVoiceChannel(@NotNull MessageReceivedEvent event) {
        Member member = event.getMember();
        if (member == null) {
            return null;
        }
        GuildVoiceState voiceState = member.getVoiceState();
        if (voiceState == null) {
            return null;
        }
        return voiceState.getChannel();
    }

    /**
     * Get music setting for the guild.
     * @param guildId Guild ID.
     * @return Music setting. Default setting if not found.
     */
    @NotNull
    private MusicSetting getSetting(long guildId) {
        MusicSetting setting = this.musicSettingRepository.findOne(() -> guildId);
        if (setting != null) {
            return setting;
        }
        return MusicSetting.getDefault(guildId);
    }

    /**
     * Prepares music state for the guild.
     * (Sets up audio players, but does not join the VC)
     * @param event Event.
     * @return Music state
     */
    @NotNull
    private MusicState prepareMusicState(@NotNull MessageReceivedEvent event) {
        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannel().getIdLong();
        Logger logger = this.logger;
        ShardManager manager = this.manager;

        MusicSetting setting = getSetting(guildId);

        AudioPlayer player = playerManager.createPlayer();
        TrackScheduler scheduler = new TrackScheduler(new TrackScheduler.SchedulerGateway() {
            @Override
            public void sendMessage(Message message) {
                TextChannel channel = manager.getTextChannelById(channelId);
                if (channel == null) {
                    logger.log(-1, "Music: Failed to retrieve text channel for id " + channelId);
                    return;
                }
                channel.sendMessage(message).queue();
            }

            @Override
            public @NotNull MusicSetting getSetting() {
                return setting;
            }

            @Override
            public String getBotAvatarURL() {
                return event.getJDA().getSelfUser().getEffectiveAvatarUrl();
            }

            @Nullable
            @Override
            public User getUser(long userId) {
                return manager.getUserById(userId);
            }

            @Override
            public void setLastInteract() {
                MusicState state = states.getOrDefault(guildId, null);
                if (state != null) {
                    state.setLastInteract(System.currentTimeMillis());
                }
            }

            @Override
            public void playTrack(AudioTrack track) {
                player.playTrack(track);
            }
        });
        player.addListener(scheduler);

        // TODO: enqueue all saved musics on player setup
        MusicState state = new MusicState(player, scheduler, System.currentTimeMillis());
        states.put(guildId, state);
        return state;
    }

    /**
     * Tries to connect to the VC the user is in.
     * @param event Event.
     * @return {@code true} if success.
     */
    private boolean connect(MessageReceivedEvent event) {
        VoiceChannel channel = getVoiceChannel(event);
        if (channel == null) {
            respondException(event, "Please join in a voice channel before you use this command!");
            return false;
        }

        // Prepare whole music logic state
        MusicState state = prepareMusicState(event);

        try {
            // Join the Discord VC and prepare audio send handler
            AudioManager audioManager = channel.getGuild().getAudioManager();
            audioManager.openAudioConnection(channel);
            // AudioPlayerSendHandler handles audio sending from LavaPlayer to Discord (JDA)
            audioManager.setSendingHandler(new AudioPlayerSendHandler(state.getPlayer()));
        } catch (InsufficientPermissionException e) {
            respondException(event, "The bot couldn't join your voice channel. Please make sure the bot has sufficient permissions to do so!");
            return false;
        }

        return true;
    }

    /**
     * Retrieves music state if the guild already has a music player set up,
     * or creates a new state if the guild doesn't.
     * @param event Event.
     * @return Music state. null if failed to join in a vc.
     */
    @Nullable
    private MusicState getStateOrConnect(MessageReceivedEvent event) {
        long guildId = event.getGuild().getIdLong();
        MusicState state = states.getOrDefault(guildId, null);
        if (state != null) {
            return state;
        }
        boolean res = connect(event);
        if (!res) {
            return null;
        }
        return states.get(guildId);
    }

    /**
     * Handles "join" command.
     * @param event Event.
     */
    private void handleJoin(MessageReceivedEvent event) {
        if (!connect(event)) {
            return;
        }

        respond(event, new EmbedBuilder()
                .setColor(MinecraftColor.DARK_GREEN.getColor())
                .setDescription("Successfully connected to your voice channel!")
                .build());
    }

    /**
     * handles "leave" command.
     * @param event Event.
     * @param saveQueue {@code true} if the bot should save the current queue, and use it next time.
     */
    private void handleLeave(@NotNull MessageReceivedEvent event, boolean saveQueue) {
        long guildId = event.getGuild().getIdLong();
        MusicState state = states.getOrDefault( guildId, null);
        if (state == null) {
            respond(event, "This guild doesn't seem to have a music player set up.");
            return;
        }

        boolean saveResult = !saveQueue || saveQueue(guildId, state);

        state.stopPlaying();
        state.getPlayer().destroy();
        event.getGuild().getAudioManager().closeAudioConnection();

        if (!saveResult) {
            respondError(event, "Something went wrong while saving the queue...");
            return;
        }

        respond(event, new EmbedBuilder()
                .setDescription(String.format("Player stopped. (%s)",
                        saveQueue ? "Saved the queue" : "Queue cleared"))
                .build());
    }

    /**
     * Saves the current queue.
     * @param guildId Guild ID.
     * @param state Music state.
     * @return {@code true} if success.
     */
    private boolean saveQueue(long guildId, @NotNull MusicState state) {
        QueueState queue = state.getCurrentQueue();
        List<QueueEntry> tracks = new ArrayList<>(queue.getQueue());
        if (tracks.isEmpty()) {
            return true;
        }

        boolean res = this.musicQueueRepository.deleteGuildMusicQueue(guildId);
        if (!res) {
            return false;
        }

        List<MusicQueueEntry> toSave = new ArrayList<>(tracks.size());
        for (int i = 0; i < tracks.size(); i++) {
            QueueEntry track = tracks.get(i);
            toSave.add(new MusicQueueEntry(
                    guildId,
                    i,
                    track.getUserId(),
                    track.getTrack().getInfo().uri,
                    i == 0 ? queue.getPosition() : 0L
            ));
        }
        return this.musicQueueRepository.saveGuildMusicQueue(toSave);
    }

    private static boolean isURL(@NotNull String possibleURL) {
        return possibleURL.startsWith("http://") || possibleURL.startsWith("https://");
    }

    private enum SearchSite {
        YouTube("ytsearch: "),
        SoundCloud("scsearch: ");

        private String prefix;

        SearchSite(String prefix) {
            this.prefix = prefix;
        }
    }

    /**
     * Handles "play" command.
     * @param event Event.
     * @param args Command arguments.
     * @param playAll If the bot should enqueue all the search results directly.
     * @param site Which site to search from.
     */
    private void handlePlay(MessageReceivedEvent event, String[] args, boolean playAll, SearchSite site) {
        if (args.length <= 2) {
            respond(event, "Please input a keyword or URL you want to search or play!");
            return;
        }

        // Delete message if possible
        try {
            event.getMessage().delete().queue();
        } catch (Exception ignored) {
        }

        String input = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (isURL(input)) {
            playAll = true;
        } else {
            input = site.prefix + input;
        }

        MusicState state = getStateOrConnect(event);
        if (state == null) {
            return;
        }

        boolean finalPlayAll = playAll;
        playerManager.loadItem(input, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                enqueueSong(event, state, audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                if (audioPlaylist.getTracks().isEmpty()) {
                    respondException(event, "Received an empty play list.");
                    return;
                }

                if (audioPlaylist.getTracks().size() == 1) {
                    enqueueSong(event, state, audioPlaylist.getTracks().get(0));
                    return;
                }

                if (!audioPlaylist.isSearchResult() || finalPlayAll) {
                    enqueueMultipleSongs(event, state, audioPlaylist.getTracks());
                    return;
                }

                selectSong(event, state, audioPlaylist);
            }

            @Override
            public void noMatches() {
                respond(event, "No results found.");
            }

            @Override
            public void loadFailed(FriendlyException e) {
                respondException(event, "Something went wrong while loading tracks... : " + e.getMessage());
            }
        });
    }

    private void selectSong(MessageReceivedEvent event, MusicState state, AudioPlaylist playlist) {
        List<AudioTrack> tracks = playlist.getTracks();
        List<String> desc = new ArrayList<>();
        int max = Math.min(10, tracks.size());
        for (int i = 0; i < max; i++) {
            AudioTrack track = tracks.get(i);
            AudioTrackInfo info = track.getInfo();
            desc.add(String.format("%s. %s `[%s]`",
                    i + 1,
                    info.title,
                    info.isStream ? "LIVE" : formatLength(info.length)));
        }
        desc.add("");
        desc.add(String.format("all: Play all (%s songs)", tracks.size()));
        desc.add("");
        desc.add("c: Cancel");

        MessageEmbed eb = new EmbedBuilder()
                .setAuthor(String.format("%s, Select a song!", event.getAuthor().getName()),
                        null, event.getAuthor().getEffectiveAvatarUrl())
                .setDescription(String.join("\n", desc))
                .setFooter(String.format("Type '1' ~ '%s', 'all' to play all, or 'c' to cancel.", max))
                .build();
        respond(event, eb, message -> {
            Response handler = new Response(event.getChannel().getIdLong(), event.getAuthor().getIdLong(),
                    res -> chooseTrack(event, message, state, tracks, res)
            );
            this.responseManager.addEventListener(handler);
        });
    }

    private boolean chooseTrack(MessageReceivedEvent event, Message botMsg,
                                MusicState state, List<AudioTrack> tracks, MessageReceivedEvent res) {
        String msg = res.getMessage().getContentRaw();
        if ("all".equalsIgnoreCase(msg)) {
            this.enqueueMultipleSongs(event, state, tracks);
            return true;
        }
        if ("c".equalsIgnoreCase(msg)) {
            respond(event, "Cancelled.", m -> m.delete().queueAfter(3, TimeUnit.SECONDS));
            return false;
        }

        try {
            int max = Math.min(10, tracks.size());
            int index = Integer.parseInt(msg);
            if (index < 1 || max < index) {
                respond(event, String.format("Please input a number between 1 and %s!", max),
                        m -> m.delete().queueAfter(3, TimeUnit.SECONDS));
                return false;
            }
            // Delete bot and response message if possible
            try {
                botMsg.delete().queue();
                res.getMessage().delete().queue();
            } catch (Exception ignored) {
            }
            this.enqueueSong(event, state, tracks.get(index - 1));
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private void enqueueSong(MessageReceivedEvent event, MusicState state, AudioTrack audioTrack) {
        long userId = event.getAuthor().getIdLong();
        boolean toShowQueuedMsg = !state.getCurrentQueue().getQueue().isEmpty();
        int queueSize = state.getCurrentQueue().getQueue().size();
        long remainingLength = state.getRemainingLength();

        try {
            state.enqueue(new QueueEntry(audioTrack, userId));
        } catch (DuplicateTrackException | QueueFullException e) {
            respondException(event, e.getMessage());
            return;
        }

        if (toShowQueuedMsg) {
            AudioTrackInfo info = audioTrack.getInfo();
            respond(event, new EmbedBuilder()
                    .setColor(MinecraftColor.DARK_GREEN.getColor())
                    .setAuthor("✔ Queued 1 song.", null, event.getAuthor().getEffectiveAvatarUrl())
                    .setTitle(("".equals(info.title) ? "(No title)" : info.title), info.uri)
                    .setThumbnail(getThumbnailURL(info.uri))
                    .addField("Length", info.isStream ? "LIVE" : formatLength(info.length), true)
                    .addField("Position in queue", String.valueOf(queueSize), true)
                    .addField("Estimated time until playing", formatLength(remainingLength), true)
                    .build());
        }
    }

    private void enqueueMultipleSongs(MessageReceivedEvent event, MusicState state, List<AudioTrack> tracks) {
        long userId = event.getAuthor().getIdLong();
        long remainingLength = state.getRemainingLength();
        int queueSize = state.getCurrentQueue().getQueue().size();

        int success = 0;
        long queuedLength = 0;
        for (AudioTrack track : tracks) {
            try {
                state.enqueue(new QueueEntry(track, userId));
                success++;
                queuedLength += track.getDuration();
            } catch (DuplicateTrackException | QueueFullException ignored) {
            }
        }

        respond(event, new EmbedBuilder()
                .setColor(MinecraftColor.DARK_GREEN.getColor())
                .setAuthor(String.format("✔ Queued %s song%s.", success, success == 1 ? "" : "s"),
                        null, event.getAuthor().getEffectiveAvatarUrl())
                .addField("Length", formatLength(queuedLength), true)
                .addField("Position in queue", String.valueOf(queueSize), true)
                .addField("Estimated time until playing", formatLength(remainingLength), true)
                .build());

        if (success != tracks.size()) {
            respondException(event, String.format("Failed to queue %s song%s due to duplicated track(s) or queue being full.",
                    tracks.size() - success, (tracks.size() - success) == 1 ? "" : "s"));
        }
    }
}

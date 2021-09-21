package music.handlers;

import app.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import db.model.musicInterruptedGuild.MusicInterruptedGuild;
import db.model.musicQueue.MusicQueueEntry;
import db.model.musicSetting.MusicSetting;
import db.repository.base.MusicInterruptedGuildRepository;
import db.repository.base.MusicQueueRepository;
import db.repository.base.MusicSettingRepository;
import log.Logger;
import music.*;
import music.exception.DuplicateTrackException;
import music.exception.QueueFullException;
import net.dv8tion.jda.api.EmbedBuilder;
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
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static commands.base.BotCommand.*;
import static music.Music.MUSIC_PLAYER_GAUGE;
import static music.MusicUtils.formatLength;
import static music.MusicUtils.getThumbnailURL;

public class MusicPlayHandler {
    private final Map<Long, MusicState> states;
    private final AudioPlayerManager playerManager;

    private final ShardManager manager;
    private final Logger logger;
    private final MusicSettingRepository musicSettingRepository;
    private final MusicQueueRepository musicQueueRepository;
    private final MusicInterruptedGuildRepository interruptedGuildRepository;
    private final ResponseManager responseManager;

    public MusicPlayHandler(Bot bot, Map<Long, MusicState> states, AudioPlayerManager playerManager) {
        this.states = states;
        this.playerManager = playerManager;
        this.manager = bot.getManager();
        this.logger = bot.getLogger();
        this.musicSettingRepository = bot.getDatabase().getMusicSettingRepository();
        this.musicQueueRepository = bot.getDatabase().getMusicQueueRepository();
        this.interruptedGuildRepository = bot.getDatabase().getMusicInterruptedGuildRepository();
        this.responseManager = bot.getResponseManager();
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
     * Rejoin to all interrupted guilds.
     */
    public void rejoinInterruptedGuilds() {
        List<MusicInterruptedGuild> guilds = this.interruptedGuildRepository.findAll();
        if (guilds == null) {
            return;
        }
        this.interruptedGuildRepository.deleteAll();

        for (MusicInterruptedGuild guild : guilds) {
            long guildId = guild.getGuildId();
            long channelId = guild.getChannelId();
            long vcId = guild.getVoiceChannelId();

            TextChannel channel = this.manager.getTextChannelById(channelId);
            VoiceChannel vc = this.manager.getVoiceChannelById(vcId);
            if (channel == null || vc == null) {
                this.logger.log(0, String.format(
                        "Music rejoin: Failed to retrieve text channel or voice channel for ID: %d, %d", channelId, vcId));
                continue;
            }

            MusicState state = prepareMusicState(channel, vc.getIdLong());

            try {
                // Join the Discord VC and prepare audio send handler
                AudioManager audioManager = channel.getGuild().getAudioManager();
                audioManager.openAudioConnection(vc);
                // AudioPlayerSendHandler handles audio sending from LavaPlayer to Discord (JDA)
                audioManager.setSendingHandler(new AudioPlayerSendHandler(state.getPlayer()));
            } catch (InsufficientPermissionException e) {
                respondException(channel, "The bot couldn't join your voice channel. " +
                        "Please make sure the bot has sufficient permissions to do so!");
                synchronized (states) {
                    states.remove(guildId);
                    MUSIC_PLAYER_GAUGE.dec();
                }
            }
        }
    }

    /**
     * Prepares music state for the guild.
     * (Sets up audio players, but does not join the VC)
     * @param channel Guild text channel.
     * @param voiceChannelId Voice channel ID. For data cache.
     * @return Music state
     */
    @NotNull
    private MusicState prepareMusicState(@NotNull TextChannel channel, long voiceChannelId) {
        long guildId = channel.getGuild().getIdLong();
        long channelId = channel.getIdLong();
        Logger logger = this.logger;
        ShardManager manager = this.manager;

        MusicSetting setting = getSetting(guildId);

        AudioPlayer player = playerManager.createPlayer();
        player.setVolume(setting.getVolume());

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
                return channel.getJDA().getSelfUser().getEffectiveAvatarUrl();
            }

            @Nullable
            @Override
            public User getUser(long userId) {
                return manager.getUserById(userId);
            }

            @Override
            public void setLastInteract() {
                MusicState state;
                synchronized (states) {
                    state = states.getOrDefault(guildId, null);
                }
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

        MusicState state = new MusicState(player, scheduler, setting, guildId, channelId, voiceChannelId);
        synchronized (states) {
            states.put(guildId, state);
            MUSIC_PLAYER_GAUGE.inc();
        }

        Guild guild = channel.getGuild();
        this.logger.log(3, String.format("Preparing music player (%s) for guild %s (%s members, ID: %s)",
                states.size(), guild.getName(), guild.getMemberCount(), guild.getIdLong()
        ));

        enqueueSavedQueue(channel, guildId, state);
        return state;
    }

    /**
     * Enqueues all saved previous queue.
     * @param guildId Guild ID.
     * @param state State.
     */
    private void enqueueSavedQueue(MessageChannel channel, long guildId, MusicState state) {
        List<MusicQueueEntry> queue = this.musicQueueRepository.getGuildMusicQueue(guildId);
        boolean deleteRes = this.musicQueueRepository.deleteGuildMusicQueue(guildId);
        if (queue == null || queue.isEmpty() || !deleteRes) {
            return;
        }

        List<Future<Void>> futures = new ArrayList<>(queue.size());

        String firstURL = queue.get(0).getUrl();
        long position = queue.get(0).getPosition();

        Map<String, MusicQueueEntry> urlMap = queue.stream().collect(Collectors.toMap(MusicQueueEntry::getUrl, q -> q));

        for (MusicQueueEntry e : queue) {
            Future<Void> f = playerManager.loadItemOrdered(state.getPlayer(), e.getUrl(), new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack audioTrack) {
                    // If it's the first one, set the position
                    AudioTrackInfo info = audioTrack.getInfo();
                    if (info.uri.equals(firstURL)) {
                        audioTrack.setPosition(position);
                    }
                    MusicQueueEntry e = urlMap.getOrDefault(info.uri, null);
                    try {
                        state.enqueue(new QueueEntry(audioTrack, e != null ? e.getUserId() : 0L));
                    } catch (DuplicateTrackException | QueueFullException ex) {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void playlistLoaded(AudioPlaylist audioPlaylist) {
                    // Should probably not be reached because we're supplying a URL for each song
                    for (AudioTrack track : audioPlaylist.getTracks()) {
                        try {
                            AudioTrackInfo info = track.getInfo();
                            MusicQueueEntry e = urlMap.getOrDefault(info.uri, null);
                            state.enqueue(new QueueEntry(track, e != null ? e.getUserId() : 0L));
                        } catch (DuplicateTrackException | QueueFullException ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                @Override
                public void noMatches() {
                    logger.debug("Music: Loading old queue: No match for URL " + e.getUrl());
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    logger.debug("Music: Loading old queue: Load failed :" + e.getMessage());
                }
            });
            futures.add(f);
        }

        // Use asynchronous logic to cancel loading in case the user wants it
        String desc = String.format("Loading `%s` song%s from the previous queue...",
                queue.size(), queue.size() == 1 ? "" : "s");

        CompletableFuture<Void> all = CompletableFuture.runAsync(() -> {
            for (Future<Void> f : futures) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });

        respond(channel,
                new EmbedBuilder()
                .setColor(MinecraftColor.DARK_GREEN.getColor())
                .setDescription(desc + "\nThis might take a while. `m purge` or `m clear` to stop loading.")
                .build(),
                message -> sendFinishEnqueueSaved(message, state, futures, all, desc)
        );
    }

    private void sendFinishEnqueueSaved(Message message, MusicState state, List<Future<Void>> futures,
                                        CompletableFuture<Void> all, String desc) {
        all.thenRun(() -> message.editMessage(
                new EmbedBuilder()
                .setColor(MinecraftColor.DARK_GREEN.getColor())
                .setDescription(desc + "\nFinished loading!")
                .build()
        ).queue());

        state.setOnStopLoadingCache(() -> {
            if (all.isDone()) {
                return;
            }
            all.cancel(false);
            futures.forEach(f -> {
                if (f.isDone()) return;
                f.cancel(false);
            });
            message.editMessage(
                    new EmbedBuilder()
                    .setColor(MinecraftColor.RED.getColor())
                    .setDescription(desc + "\nCancelled loading.")
                    .build()
            ).queue();
        });
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
        MusicState state = prepareMusicState(event.getTextChannel(), channel.getIdLong());

        try {
            // Join the Discord VC and prepare audio send handler
            AudioManager audioManager = channel.getGuild().getAudioManager();
            audioManager.openAudioConnection(channel);
            // AudioPlayerSendHandler handles audio sending from LavaPlayer to Discord (JDA)
            audioManager.setSendingHandler(new AudioPlayerSendHandler(state.getPlayer()));
        } catch (InsufficientPermissionException e) {
            respondException(event, "The bot couldn't join your voice channel. Please make sure the bot has sufficient permissions to do so!");
            synchronized (states) {
                states.remove(event.getGuild().getIdLong());
                MUSIC_PLAYER_GAUGE.dec();
            }
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
        synchronized (states) {
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
    }

    /**
     * Handles "join" command.
     * @param event Event.
     */
    public void handleJoin(MessageReceivedEvent event) {
        synchronized (states) {
            if (states.containsKey(event.getGuild().getIdLong())) {
                respond(event, "This guild already has a music player set up!");
                return;
            }
        }

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
     * @param state Current music state of the guild.
     * @param saveQueue {@code true} if the bot should save the current queue, and use it next time.
     */
    public void handleLeave(@NotNull MessageReceivedEvent event, @NotNull MusicState state, boolean saveQueue) {
        try {
            this.shutdownPlayer(saveQueue, state);
        } catch (RuntimeException e) {
            respondError(event, e.getMessage());
            return;
        }

        synchronized (states) {
            states.remove(state.getGuildId());
            MUSIC_PLAYER_GAUGE.dec();
        }

        respond(event, new EmbedBuilder()
                .setDescription(String.format("Player stopped. (%s)",
                        saveQueue ? "Saved the queue" : "Queue cleared"))
                .build());
    }

    /**
     * Shuts down the music player for the guild.
     * @param saveQueue If the bot should save the current queue.
     * @param state Music state.
     * @throws RuntimeException If something went wrong.
     */
    void shutdownPlayer(boolean saveQueue, MusicState state) throws RuntimeException {
        // Stop loading from cache if it was running
        state.stopLoadingCache();

        // Retrieve the current queue before it is cleared
        QueueState queue = state.getCurrentQueue();

        // Stop playing, and destroy LavaPlayer audio player (this call clears the queue inside MusicState)
        state.stopPlaying();
        state.getPlayer().destroy();

        // Try to disconnect from the Discord voice channel
        long guildId = state.getGuildId();
        try {
            Guild guild = this.manager.getGuildById(guildId);
            if (guild != null) {
                this.logger.log(3, String.format("Shutting down music player (%s) for guild %s (%s members, ID: %s)",
                        states.size(), guild.getName(), guild.getMemberCount(), guild.getIdLong()
                ));

                guild.getAudioManager().closeAudioConnection();
            } else {
                this.logger.log(0, "Failed to get guild info for id: " + guildId);
            }
        } catch (RejectedExecutionException e) {
            // Expected to be thrown on shutdown, because JDA is no longer connected
            this.logger.logException("Failed to close audio connection for guild id " + guildId, e);
        }

        // Save the current queue
        boolean saveResult = !saveQueue || saveQueue(guildId, queue);
        if (!saveResult) {
            throw new RuntimeException("Something went wrong while saving the queue...");
        }

        // Save setting if the current differs from the default
        boolean saveSetting = true;
        if (!state.getSetting().equals(MusicSetting.getDefault(guildId))) {
            saveSetting = this.musicSettingRepository.exists(() -> guildId)
                    ? this.musicSettingRepository.update(state.getSetting())
                    : this.musicSettingRepository.create(state.getSetting());
        }
        if (!saveSetting) {
            throw new RuntimeException("Something went wrong while saving settings for this guild...");
        }
    }

    /**
     * Saves the current queue.
     * @param guildId Guild ID.
     * @param queue Music queue.
     * @return {@code true} if success.
     */
    private boolean saveQueue(long guildId, @NotNull QueueState queue) {
        List<QueueEntry> tracks = new ArrayList<>(queue.getQueue());
        if (tracks.isEmpty()) {
            return true;
        }

        List<MusicQueueEntry> toSave = new ArrayList<>(tracks.size());
        long now = System.currentTimeMillis();
        for (int i = 0; i < tracks.size(); i++) {
            QueueEntry track = tracks.get(i);
            toSave.add(new MusicQueueEntry(
                    guildId,
                    i,
                    track.getUserId(),
                    track.getTrack().getInfo().uri,
                    i == 0 ? queue.getPosition() : 0L,
                    new Date(now)
            ));
        }
        return this.musicQueueRepository.saveGuildMusicQueue(toSave);
    }

    private static boolean isURL(@NotNull String possibleURL) {
        return possibleURL.startsWith("http://") || possibleURL.startsWith("https://");
    }

    /**
     * Handles "play" command.
     * @param event Event.
     * @param args Command arguments.
     * @param playAll If the bot should enqueue all the search results directly.
     * @param site Which site to search from.
     */
    public void handlePlay(MessageReceivedEvent event, String[] args, boolean playAll, SearchSite site) {
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
            input = site.getPrefix() + input;
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
                respondException(event, "Something went wrong while loading tracks:\n" + e.getMessage());
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
            try {
                // Delete bot and response message if possible
                botMsg.delete().queue();
                res.getMessage().delete().queue();
            } catch (Exception ignored) {
            }
            this.enqueueMultipleSongs(event, state, tracks);
            return true;
        }
        if ("c".equalsIgnoreCase(msg)) {
            try {
                // Delete bot and response message if possible
                botMsg.delete().queue();
                res.getMessage().delete().queue();
            } catch (Exception ignored) {
            }
            respond(event, "Cancelled.", m -> m.delete().queueAfter(3, TimeUnit.SECONDS));
            return true;
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
            // Ignore normal messages
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

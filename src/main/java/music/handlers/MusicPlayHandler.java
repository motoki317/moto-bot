package music.handlers;

import app.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import commands.event.*;
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
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ComponentLayout;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import update.button.ButtonClickHandler;
import update.button.ButtonClickManager;
import update.response.Response;
import update.response.ResponseManager;
import utils.MinecraftColor;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private final ButtonClickManager buttonClickManager;

    public MusicPlayHandler(Bot bot, Map<Long, MusicState> states, AudioPlayerManager playerManager) {
        this.states = states;
        this.playerManager = playerManager;
        this.manager = bot.getManager();
        this.logger = bot.getLogger();
        this.musicSettingRepository = bot.getDatabase().getMusicSettingRepository();
        this.musicQueueRepository = bot.getDatabase().getMusicQueueRepository();
        this.interruptedGuildRepository = bot.getDatabase().getMusicInterruptedGuildRepository();
        this.responseManager = bot.getResponseManager();
        this.buttonClickManager = bot.getButtonClickManager();
    }

    /**
     * Retrieves voice channel the user is in.
     *
     * @param event Event.
     * @return Voice channel. null if not found.
     */
    @Nullable
    private static VoiceChannel getVoiceChannel(@NotNull CommandEvent event) {
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
     *
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
                channel.sendMessage("The bot couldn't join your voice channel. " +
                        "Please make sure the bot has sufficient permissions to do so!").queue();
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
     *
     * @param channel        Guild text channel.
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
                MusicState state;
                synchronized (states) {
                    state = states.getOrDefault(guildId, null);
                }
                if (state != null && state.getMessageToEdit() != null) {
                    state.getMessageToEdit().editMessage(message);
                    state.setMessageToEdit(null);
                    return;
                }

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
     *
     * @param guildId Guild ID.
     * @param state   State.
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

        channel.sendMessageEmbeds(
                new EmbedBuilder()
                        .setColor(MinecraftColor.DARK_GREEN.getColor())
                        .setDescription(desc + "\nThis might take a while. `m purge` or `m clear` to stop loading.")
                        .build()
        ).queue(message -> sendFinishEnqueueSaved(message, state, futures, all, desc));
    }

    private void sendFinishEnqueueSaved(Message message, MusicState state, List<Future<Void>> futures,
                                        CompletableFuture<Void> all, String desc) {
        all.thenRun(() -> message.editMessageEmbeds(
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
            message.editMessageEmbeds(
                    new EmbedBuilder()
                            .setColor(MinecraftColor.RED.getColor())
                            .setDescription(desc + "\nCancelled loading.")
                            .build()
            ).queue();
        });
    }

    /**
     * Tries to connect to the VC the user is in.
     *
     * @param event Event.
     * @return {@code true} if success.
     */
    private boolean connect(CommandEvent event) {
        VoiceChannel channel = getVoiceChannel(event);
        if (channel == null) {
            event.replyException("Please join in a voice channel before you use this command!");
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
            event.replyException("The bot couldn't join your voice channel. Please make sure the bot has sufficient permissions to do so!");
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
     *
     * @param event Event.
     * @return Music state. null if failed to join in a vc.
     */
    @Nullable
    private MusicState getStateOrConnect(CommandEvent event) {
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
     *
     * @param event Event.
     */
    public void handleJoin(CommandEvent event) {
        synchronized (states) {
            if (states.containsKey(event.getGuild().getIdLong())) {
                event.reply("This guild already has a music player set up!");
                return;
            }
        }

        if (!connect(event)) {
            return;
        }

        event.reply(new EmbedBuilder()
                .setColor(MinecraftColor.DARK_GREEN.getColor())
                .setDescription("Successfully connected to your voice channel!")
                .build());
    }

    /**
     * handles "leave" command.
     *
     * @param event     Event.
     * @param state     Current music state of the guild.
     * @param saveQueue {@code true} if the bot should save the current queue, and use it next time.
     */
    public void handleLeave(@NotNull CommandEvent event, @NotNull MusicState state, boolean saveQueue) {
        try {
            this.shutdownPlayer(saveQueue, state);
        } catch (RuntimeException e) {
            event.replyError(e.getMessage());
            return;
        }

        synchronized (states) {
            states.remove(state.getGuildId());
            MUSIC_PLAYER_GAUGE.dec();
        }

        event.reply(new EmbedBuilder()
                .setDescription(String.format("Player stopped. (%s)",
                        saveQueue ? "Saved the queue" : "Queue cleared"))
                .build());
    }

    /**
     * Shuts down the music player for the guild.
     *
     * @param saveQueue If the bot should save the current queue.
     * @param state     Music state.
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
     *
     * @param guildId Guild ID.
     * @param queue   Music queue.
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
     *
     * @param event   Event.
     * @param args    Command arguments.
     * @param playAll If the bot should enqueue all the search results directly.
     * @param site    Which site to search from.
     */
    public void handlePlay(CommandEvent event, String[] args, boolean playAll, SearchSite site) {
        if (args.length <= 2) {
            event.reply("Please input a keyword or URL you want to search or play!");
            return;
        }

        // Delete message if possible
        if (event instanceof MessageReceivedEventAdapter a) {
            try {
                a.event().getMessage().delete().queue();
            } catch (Exception ignored) {
            }
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
        String finalInput = input;
        event.reply(new EmbedBuilder().setDescription("Searching for songs...").build(), s ->
                playerManager.loadItem(finalInput, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack audioTrack) {
                        enqueueSong(event, s, state, audioTrack);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist audioPlaylist) {
                        if (audioPlaylist.getTracks().isEmpty()) {
                            event.replyException("Received an empty play list.");
                            return;
                        }

                        if (audioPlaylist.getTracks().size() == 1) {
                            enqueueSong(event, s, state, audioPlaylist.getTracks().get(0));
                            return;
                        }

                        if (!audioPlaylist.isSearchResult() || finalPlayAll) {
                            enqueueMultipleSongs(event, s, state, audioPlaylist.getTracks());
                            return;
                        }

                        selectSong(event, s, state, audioPlaylist);
                    }

                    @Override
                    public void noMatches() {
                        s.editMessage(new EmbedBuilder().setDescription("No results found.").build());
                    }

                    @Override
                    public void loadFailed(FriendlyException e) {
                        s.editMessage(new EmbedBuilder()
                                .setColor(MinecraftColor.RED.getColor())
                                .setDescription("Something went wrong while loading tracks:\n" + e.getMessage())
                                .build());
                    }
                }));
    }

    private void selectSong(CommandEvent event, SentMessage s, MusicState state, AudioPlaylist playlist) {
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

        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(String.format("%s, Select a song!", event.getAuthor().getName()),
                        null, event.getAuthor().getEffectiveAvatarUrl())
                .setDescription(String.join("\n", desc));

        if (event instanceof MessageReceivedEventAdapter) {
            eb.setFooter(String.format("Type '1' ~ '%s', 'all' to play all, or 'c' to cancel.", max));
        }

        s.editMessage(eb.build(), botResp -> {
            if (botResp instanceof SentMessageAdapter) {
                Response handler = new Response(event.getChannel().getIdLong(), event.getAuthor().getIdLong(),
                        response -> {
                            boolean next = chooseTrack(event, state, tracks, botResp, response);
                            if (next) {
                                // Delete response message if possible
                                try {
                                    response.getMessage().delete().queue();
                                } catch (Exception ignored) {
                                }
                            }
                            return next;
                        }
                );
                this.responseManager.addEventListener(handler);
            } else if (botResp instanceof InteractionHookAdapter hook) {
                MusicSelectButtonHandler handler = new MusicSelectButtonHandler(hook.hook(), tracks, event, state);
                this.buttonClickManager.addEventListener(handler);
            }
        });
    }

    private class MusicSelectButtonHandler extends ButtonClickHandler {
        private final InteractionHook hook;
        private final List<AudioTrack> tracks;
        private final CommandEvent cEvent;
        private final MusicState state;

        public MusicSelectButtonHandler(InteractionHook hook, List<AudioTrack> tracks, CommandEvent cEvent, MusicState state) {
            super(hook.getInteraction().getIdLong(), (event) -> false, () -> {
            });

            this.hook = hook;
            this.tracks = tracks;
            this.cEvent = cEvent;
            this.state = state;

            int max = Math.min(10, tracks.size());
            hook.editOriginalComponents(getLayout(max)).queue();
        }

        private static List<ComponentLayout> getLayout(int maxTracks) {
            List<ComponentLayout> layouts = new ArrayList<>();
            layouts.add(ActionRow.of(IntStream
                    .range(0, Math.min(maxTracks, 5))
                    .mapToObj(i -> Button.primary("track-" + i, String.valueOf(i + 1)))
                    .collect(Collectors.toCollection(ArrayList::new))));
            if (maxTracks > 5) {
                layouts.add(ActionRow.of(IntStream
                        .range(5, Math.min(maxTracks, 10))
                        .mapToObj(i -> Button.primary("track-" + i, String.valueOf(i + 1)))
                        .collect(Collectors.toCollection(ArrayList::new))));
            }
            layouts.add(ActionRow.of(
                    Button.secondary("all", "Play all"),
                    Button.danger("cancel", "Cancel")
            ));
            return layouts;
        }

        @Override
        public boolean handle(ButtonClickEvent event) {
            super.handle(event);

            if (event.getButton() == null || event.getButton().getId() == null) return false;

            String buttonId = event.getButton().getId();
            switch (buttonId) {
                case "all" -> MusicPlayHandler.this.enqueueMultipleSongs(this.cEvent, new InteractionHookAdapter(this.hook), this.state, this.tracks);
                case "cancel" -> this.cEvent.reply("Cancelled.", m -> m.deleteAfter(3, TimeUnit.SECONDS));
                default -> { // "track-i"
                    if (!buttonId.startsWith("track-")) {
                        return false;
                    }
                    int trackId = Integer.parseInt(buttonId.substring("track-".length()));
                    MusicPlayHandler.this.enqueueSong(this.cEvent, new InteractionHookAdapter(this.hook), this.state, this.tracks.get(trackId));
                }
            }

            return true;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            // Delete buttons
            this.hook.editOriginalComponents().queue();
        }
    }

    private boolean chooseTrack(CommandEvent event, MusicState state, List<AudioTrack> tracks, SentMessage botResp, MessageReceivedEvent userMsg) {
        String response = userMsg.getMessage().getContentRaw();
        System.out.println("MusicPlayerHandler#L742: " + response);
        if ("all".equalsIgnoreCase(response)) {
            this.enqueueMultipleSongs(event, botResp, state, tracks);
            return true;
        }
        if ("c".equalsIgnoreCase(response)) {
            event.reply("Cancelled.", m -> m.deleteAfter(3, TimeUnit.SECONDS));
            return true;
        }

        try {
            int max = Math.min(10, tracks.size());
            int index = Integer.parseInt(response);
            if (index < 1 || max < index) {
                event.reply(String.format("Please input a number between 1 and %s!", max),
                        m -> m.deleteAfter(3, TimeUnit.SECONDS));
                return false;
            }

            this.enqueueSong(event, botResp, state, tracks.get(index - 1));
            return true;
        } catch (NumberFormatException ignored) {
            // Ignore normal messages
            return false;
        }
    }

    private void enqueueSong(CommandEvent event, SentMessage msg, MusicState state, AudioTrack audioTrack) {
        long userId = event.getAuthor().getIdLong();
        boolean toShowQueuedMsg = !state.getCurrentQueue().getQueue().isEmpty();
        int queueSize = state.getCurrentQueue().getQueue().size();
        long remainingLength = state.getRemainingLength();

        if (!toShowQueuedMsg) {
            state.setMessageToEdit(msg);
        }

        try {
            state.enqueue(new QueueEntry(audioTrack, userId));
        } catch (DuplicateTrackException | QueueFullException e) {
            event.replyException(e.getMessage());
            state.setMessageToEdit(null);
            return;
        }

        if (toShowQueuedMsg) {
            AudioTrackInfo info = audioTrack.getInfo();
            msg.editMessage(new EmbedBuilder()
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

    private void enqueueMultipleSongs(CommandEvent event, SentMessage msg, MusicState state, List<AudioTrack> tracks) {
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

        msg.editMessage(new EmbedBuilder()
                .setColor(MinecraftColor.DARK_GREEN.getColor())
                .setAuthor(String.format("✔ Queued %s song%s.", success, success == 1 ? "" : "s"),
                        null, event.getAuthor().getEffectiveAvatarUrl())
                .addField("Length", formatLength(queuedLength), true)
                .addField("Position in queue", String.valueOf(queueSize), true)
                .addField("Estimated time until playing", formatLength(remainingLength), true)
                .build());

        if (success != tracks.size()) {
            // Not using reply() because of the reply above
            event.getChannel().sendMessage(
                    String.format("Failed to queue %s song%s due to duplicated track(s) or queue being full.",
                            tracks.size() - success, (tracks.size() - success) == 1 ? "" : "s")
            ).queue();
        }
    }
}

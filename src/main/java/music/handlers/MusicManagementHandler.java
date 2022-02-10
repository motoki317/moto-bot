package music.handlers;

import app.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import commands.event.CommandEvent;
import music.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import utils.MinecraftColor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static music.MusicUtils.*;

public class MusicManagementHandler {
    private final ShardManager manager;

    public MusicManagementHandler(Bot bot) {
        this.manager = bot.getManager();
    }

    /**
     * Handles "nowPlaying" command.
     *
     * @param event Event.
     * @param state Music state of the guild.
     */
    public void handleNowPlaying(CommandEvent event, @NotNull MusicState state) {
        QueueState queueState = state.getCurrentQueue();
        List<QueueEntry> entries = queueState.getQueue();
        if (entries.isEmpty()) {
            event.reply("Nothing seems to be playing right now.");
            return;
        }

        QueueEntry first = entries.get(0);

        event.reply(MusicUtils.formatNowPlaying(
                first.getTrack(), this.manager.getUserById(first.getUserId()),
                state.getSetting(), event.getJDA().getSelfUser().getEffectiveAvatarUrl(),
                true
        ));
    }

    /**
     * Handles "queue" command.
     *
     * @param event Event.
     * @param state Music state of the guild.
     */
    public void handleQueue(CommandEvent event, @NotNull MusicState state) {
        QueueState queueState = state.getCurrentQueue();
        if (queueState.getQueue().isEmpty()) {
            event.reply("Nothing seems to be playing right now.");
            return;
        }

        String authorAvatarURL = event.getAuthor().getEffectiveAvatarUrl();
        if (maxQueuePage(state) == 0) {
            event.reply(formatQueuePage(0, state, authorAvatarURL));
            return;
        }

        Function<Integer, Message> pages = page -> new MessageBuilder(formatQueuePage(page, state, authorAvatarURL)).build();
        event.replyMultiPage(pages.apply(0), pages, () -> maxQueuePage(state));
    }

    private static final int SONGS_PER_PAGE = 10;

    private static int maxQueuePage(MusicState state) {
        int queueSize = state.getCurrentQueue().getQueue().size();
        // subtract one because the first one is the current song
        queueSize = Math.max(0, queueSize - 1);
        return (queueSize - 1) / SONGS_PER_PAGE;
    }

    private MessageEmbed formatQueuePage(int page, MusicState state, String authorAvatarURL) {
        RepeatState repeat = state.getSetting().getRepeat();

        int maxPage = maxQueuePage(state);
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(MinecraftColor.DARK_GREEN.getColor())
                .setAuthor(String.format("Current Queue%s", repeat.getMessage().isEmpty() ? "" : " : " + repeat.getMessage()),
                        null, authorAvatarURL)
                .setFooter(String.format("Page [ %s / %s ]", page + 1, maxPage + 1))
                .setTimestamp(Instant.now());

        QueueState queueState = state.getCurrentQueue();
        List<QueueEntry> entries = queueState.getQueue();

        QueueEntry nowPlaying = entries.size() > 0 ? entries.get(0) : null;

        List<String> desc = new ArrayList<>();
        desc.add("__**Now Playing**__");
        if (nowPlaying != null) {
            desc.add(formatQueueEntry(nowPlaying, true));
        } else {
            desc.add("Nothing seems to be playing right now...");
        }

        int begin = page * SONGS_PER_PAGE + 1;
        int end = Math.min((page + 1) * SONGS_PER_PAGE + 1, entries.size());
        if (begin < end) {
            desc.add("");
            desc.add("__**Next Up**__");
            for (int i = begin; i < end; i++) {
                desc.add(String.format("**%s**. %s", i, formatQueueEntry(entries.get(i), false)));
            }
        }

        desc.add("");
        desc.add("");
        if (repeat.isEndlessMode()) {
            desc.add(String.format("`[%s]` track%s | Queue Length `[%s]`",
                    entries.size(), entries.size() == 1 ? "" : "s", formatLength(state.getQueueLength())));
        } else {
            desc.add(String.format("`[%s]` track%s | Remaining Length `[%s]`",
                    entries.size(), entries.size() == 1 ? "" : "s", formatLength(state.getRemainingLength())));
        }

        eb.setDescription(String.join("\n", desc));
        return eb.build();
    }

    /**
     * Formats a single song in the queue page.
     *
     * @param entry Queue entry.
     * @return Formatted entry.
     */
    private String formatQueueEntry(QueueEntry entry, boolean showPosition) {
        User user = this.manager.getUserById(entry.getUserId());
        AudioTrack track = entry.getTrack();
        AudioTrackInfo info = track.getInfo();

        return String.format("[%s](%s) `[%s]` | Requested by %s",
                info.title, info.uri,
                showPosition ? formatLength(track.getPosition()) + "/" + formatLength(info.length) : formatLength(info.length),
                user != null ? user.getName() + "#" + user.getDiscriminator() : "Unknown User");
    }

    /**
     * Handles "pause" and "resume" commands.
     *
     * @param event  Event.
     * @param state  Music state of the guild.
     * @param toStop If {@code true}, the bot should stop the player.
     */
    public void handlePause(CommandEvent event, @NotNull MusicState state, boolean toStop) {
        AudioPlayer player = state.getPlayer();
        AudioTrack np = player.getPlayingTrack();
        if (np == null) {
            event.reply("Nothing seems to be playing right now.");
            return;
        }

        player.setPaused(toStop);

        if (toStop) {
            event.reply("Paused the player.");
        } else {
            event.reply("Resumed the player.");
        }
    }

    /**
     * Handles "skip" command.
     *
     * @param event Event.
     * @param args  Command arguments.
     * @param state Music state of the guild.
     */
    public void handleSkip(CommandEvent event, String[] args, @NotNull MusicState state) {
        QueueState queueState = state.getCurrentQueue();
        if (queueState.getQueue().isEmpty()) {
            event.reply("Nothing seems to be playing right now.");
            return;
        }

        int skipAmount;
        if (args.length <= 2) {
            skipAmount = 1;
        } else {
            try {
                skipAmount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                event.replyException("Please input a valid number for the skip amount!");
                return;
            }
            if (skipAmount < 1 || queueState.getQueue().size() < skipAmount) {
                event.replyException(String.format("Please input a number between 1 and %s for the skip amount!",
                        queueState.getQueue().size()));
                return;
            }
        }

        event.reply(String.format("Skipping %s song%s.", skipAmount, skipAmount == 1 ? "" : "s"));
        state.skip(skipAmount);
    }

    /**
     * Handles "seek" command.
     *
     * @param event Event.
     * @param args  Command arguments.
     * @param state Music state of the guild.
     */
    public void handleSeek(CommandEvent event, String[] args, @NotNull MusicState state) {
        if (args.length <= 2) {
            event.reply("Input the time you want to seek to. e.g. `m seek 1:02:03`, `m seek 4:33`");
            return;
        }

        QueueState queueState = state.getCurrentQueue();
        if (queueState.getQueue().isEmpty()) {
            event.reply("Nothing seems to be playing right now.");
            return;
        }

        QueueEntry np = queueState.getQueue().get(0);
        if (!np.getTrack().isSeekable()) {
            event.reply("This track is not seek-able.");
            return;
        }

        String positionStr = args[2];
        long position;
        try {
            position = parseLength(positionStr);
        } catch (IllegalArgumentException e) {
            event.replyException("Please input a valid time. e.g. `m seek 1:02:03`, `m seek 4:33`");
            return;
        }

        np.getTrack().setPosition(position);

        User user = this.manager.getUserById(np.getUserId());
        String botAvatarURL = event.getJDA().getSelfUser().getEffectiveAvatarUrl();
        event.getChannel().sendMessage(String.format("Seeked to %s.", positionStr))
                .delay(1, TimeUnit.SECONDS)
                .flatMap(Message::delete)
                .flatMap(v -> event.getChannel().sendMessageEmbeds(
                        formatNowPlaying(np.getTrack(), user, state.getSetting(), botAvatarURL, true)
                )).queue();
    }

    /**
     * Handles "shuffle" command.
     *
     * @param event Event.
     * @param state Music state of the guild.
     */
    public void handleShuffle(CommandEvent event, @NotNull MusicState state) {
        QueueState queueState = state.getCurrentQueue();
        if (queueState.getQueue().isEmpty()) {
            event.reply("Nothing seems to be playing right now.");
            return;
        }

        state.shuffle();
        event.reply("Shuffled the queue.");
    }

    /**
     * Handles "purge" command.
     *
     * @param event Event.
     * @param state Music state of the guild.
     */
    public void handlePurge(CommandEvent event, @NotNull MusicState state) {
        state.stopLoadingCache();
        state.purgeWaitingQueue();

        event.reply(new EmbedBuilder()
                .setColor(MinecraftColor.RED.getColor())
                .setDescription("Purged the queue.")
                .build());
    }
}

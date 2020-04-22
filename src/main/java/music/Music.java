package music;

import app.Bot;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import commands.base.GuildCommand;
import music.handlers.MusicManagementHandler;
import music.handlers.MusicPlayHandler;
import music.handlers.SearchSite;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Music extends GuildCommand {
    private static final Map<Long, MusicState> states;
    private static final AudioPlayerManager playerManager;

    static {
        states = new HashMap<>();
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    // Command handlers
    private final MusicPlayHandler playHandler;
    private final MusicManagementHandler managementHandler;

    private final ShardManager manager;

    public Music(Bot bot) {
        this.manager = bot.getManager();
        this.playHandler = new MusicPlayHandler(bot, states, playerManager);
        this.managementHandler = new MusicManagementHandler(bot, states);
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
                this.playHandler.handleJoin(event);
                return;
            case "l":
            case "leave":
            case "stop":
                this.playHandler.handleLeave(event, true);
                return;
            case "c":
            case "clear":
                this.playHandler.handleLeave(event, false);
                return;
            // Play handlers
            case "p":
            case "play":
                this.playHandler.handlePlay(event, args, false, SearchSite.YouTube);
                return;
            case "pa":
            case "playall":
                this.playHandler.handlePlay(event, args, true, SearchSite.YouTube);
                return;
            case "sc":
            case "soundcloud":
                this.playHandler.handlePlay(event, args, false, SearchSite.SoundCloud);
                return;
            // Player management handlers
            case "np":
            case "nowplaying":
                this.managementHandler.handleNowPlaying(event);
                return;
            case "q":
            case "queue":
                this.managementHandler.handleQueue(event);
                return;
            case "pause":
                this.managementHandler.handlePause(event, true);
                return;
            case "resume":
                this.managementHandler.handlePause(event, false);
                return;
            case "s":
            case "skip":
                this.managementHandler.handleSkip(event, args);
                return;
            case "seek":
                this.managementHandler.handleSeek(event, args);
                return;
            case "shuffle":
                this.managementHandler.handleShuffle(event);
                return;
            case "purge":
                this.managementHandler.handlePurge(event);
                return;
        }

        respond(event, "Unknown music command. Try `m help`!");
        return;
    }
}

package app;

import db.model.serverLog.ServerLogEntry;
import db.repository.base.ServerLogRepository;
import db.repository.base.TrackChannelRepository;
import log.Logger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import update.button.ButtonClickManager;

public class UpdaterListener extends ListenerAdapter {
    private final Bot bot;
    private final ButtonClickManager buttonClickManager;
    private final Logger logger;
    private final ShardManager manager;
    private final TrackChannelRepository trackChannelRepository;
    private final ServerLogRepository serverLogRepository;

    UpdaterListener(Bot bot) {
        this.bot = bot;
        this.buttonClickManager = bot.getButtonClickManager();
        this.logger = bot.getLogger();
        this.manager = bot.getManager();
        this.trackChannelRepository = bot.getDatabase().getTrackingChannelRepository();
        this.serverLogRepository = bot.getDatabase().getServerLogRepository();
    }

    // ----------------------------
    // JDA Events
    // ----------------------------

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        this.bot.setConnected(this.bot.getShardId(event.getJDA()), true);
    }

    @Override
    public void onResumed(@NotNull ResumedEvent event) {
        this.bot.setConnected(this.bot.getShardId(event.getJDA()), true);
    }

    @Override
    public void onReconnected(@NotNull ReconnectedEvent event) {
        this.bot.setConnected(this.bot.getShardId(event.getJDA()), true);
    }

    @Override
    public void onDisconnect(@NotNull DisconnectEvent event) {
        this.bot.setConnected(this.bot.getShardId(event.getJDA()), false);
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        this.bot.setConnected(this.bot.getShardId(event.getJDA()), false);
    }

    // ----------------------------
    // User response / reaction handlers
    // ----------------------------

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        this.buttonClickManager.handle(event);
    }

    // ----------------------------
    // Guild join / leave, channel deletion handlers
    // ----------------------------

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        long totalGuilds = this.manager.getGuildCache().size();
        Guild guild = event.getGuild();
        this.logger.log(0, String.format("[+ Guild (%s)] %s (%s Members, ID: %s)",
                totalGuilds, guild.getName(), guild.getMemberCount(), guild.getIdLong()
        ));
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        long totalGuilds = this.manager.getGuildCache().size();
        Guild guild = event.getGuild();
        this.logger.log(0, String.format("[- Guild (%s)] %s (%s Members, ID: %s)",
                totalGuilds, guild.getName(), guild.getMemberCount(), guild.getIdLong()
        ));

        // Delete tracking of guild channels
        if (!this.trackChannelRepository.deleteAllOfGuild(guild.getIdLong())) {
            this.logger.log(0, "Failed to remove tracking entries of the guild");
        }
        // Delete server log channel
        if (this.serverLogRepository.exists(() -> event.getGuild().getIdLong())) {
            boolean res = this.serverLogRepository.delete(() -> event.getGuild().getIdLong());
            if (!res) {
                this.logger.log(0, "Failed to remove server log channel of the guild");
            }
        }
    }

    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        long channelId = event.getChannel().getIdLong();

        boolean res = this.trackChannelRepository.deleteAllOfChannel(channelId);
        if (!res) {
            this.logger.log(0, "Failed to remove tracking entries of the channel");
        }
        // Delete server log channel if it was the deleted channel
        ServerLogEntry serverLog = this.serverLogRepository.findOne(() -> event.getGuild().getIdLong());
        if (serverLog != null && serverLog.getChannelId() == channelId) {
            res = this.serverLogRepository.delete(() -> event.getGuild().getIdLong());
            if (!res) {
                this.logger.log(0, "Failed to remove server log channel of the guild");
            }
        }
    }
}

package app;

import db.model.serverLog.ServerLogEntry;
import db.repository.base.ServerLogRepository;
import db.repository.base.TrackChannelRepository;
import log.Logger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import update.reaction.ReactionManager;
import update.response.ResponseManager;

import javax.annotation.Nonnull;

public class UpdaterListener extends ListenerAdapter {
    private final ResponseManager responseManager;
    private final ReactionManager reactionManager;
    private final Logger logger;
    private final ShardManager manager;
    private final TrackChannelRepository trackChannelRepository;
    private final ServerLogRepository serverLogRepository;

    UpdaterListener(Bot bot) {
        this.responseManager = bot.getResponseManager();
        this.reactionManager = bot.getReactionManager();
        this.logger = bot.getLogger();
        this.manager = bot.getManager();
        this.trackChannelRepository = bot.getDatabase().getTrackingChannelRepository();
        this.serverLogRepository = bot.getDatabase().getServerLogRepository();
    }

    // ----------------------------
    // User response / reaction handlers
    // ----------------------------

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        // Do not respond to webhook/bot messages
        if (event.isWebhookMessage() || event.getAuthor().isBot()) return;

        this.responseManager.handle(event);
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        // Do not respond to bot messages
        long authorId = event.getUserIdLong();
        User author = event.getJDA().getUserById(authorId);
        long botId = event.getJDA().getSelfUser().getIdLong();
        if (authorId == botId || (author != null && author.isBot())) return;

        this.reactionManager.handle(event);
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

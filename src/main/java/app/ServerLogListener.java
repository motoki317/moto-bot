package app;

import db.model.serverLog.ServerLogEntry;
import db.model.timezone.CustomTimeZone;
import db.repository.base.ServerLogRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.channel.text.GenericTextChannelEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.text.update.*;
import net.dv8tion.jda.api.events.channel.voice.GenericVoiceChannelEvent;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.voice.update.*;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.update.*;
import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.role.GenericRoleEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.*;
import net.dv8tion.jda.api.events.user.GenericUserEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateDiscriminatorEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import utils.BotUtils;
import utils.FormatUtils;
import utils.MinecraftColor;
import utils.cache.DataCache;
import utils.cache.HashMapDataCache;

import javax.annotation.Nonnull;
import java.awt.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ServerLogListener extends ListenerAdapter {
    private static DateFormat getDateFormat() {
        return new SimpleDateFormat("yyyy/MM/dd E',' HH:mm:ss.SSS");
    }

    private static final DataCache<Long, String> messageCache = new HashMapDataCache<>(
            5000, TimeUnit.HOURS.toMillis(3), TimeUnit.MINUTES.toMillis(10)
    );

    private final ServerLogRepository serverLogRepository;
    private final TimeZoneRepository timeZoneRepository;
    private final ShardManager shardManager;

    ServerLogListener(Bot bot) {
        this.serverLogRepository = bot.getDatabase().getServerLogRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.shardManager = bot.getManager();
    }

    private String getFormattedCurrentTime(long guildId, long channelId) {
        return getFormattedTime(guildId, channelId, System.currentTimeMillis());
    }

    private String getFormattedTime(long guildId, long channelId, long epochMillis) {
        DateFormat dateFormat = getDateFormat();
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(
                guildId, channelId
        );
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());
        return String.format("%s (%s)", dateFormat.format(new Date(epochMillis)), customTimeZone.getFormattedTime());
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        messageCache.add(event.getMessageIdLong(), event.getMessage().getContentRaw());
    }

    @Override
    public void onGenericTextChannel(GenericTextChannelEvent event) {
        long guildId = event.getGuild().getIdLong();
        ServerLogEntry log = this.serverLogRepository.findOne(() -> guildId);
        if (log == null) {
            return;
        }
        TextChannel logChannel = this.shardManager.getTextChannelById(log.getChannelId());
        if (logChannel == null) {
            return;
        }
        String formattedDate = getFormattedCurrentTime(guildId, log.getChannelId());

        // Base embed message
        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                .setFooter(String.format("Channel ID: %s | %s", event.getChannel().getIdLong(), formattedDate));

        if (event instanceof TextChannelDeleteEvent) {
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.RED.getColor())
                    .setDescription(
                            "Channel Deleted: #" + event.getChannel().getName()
                    ).build()
            ).queue();
        } else if (event instanceof TextChannelUpdateNameEvent) {
            TextChannelUpdateNameEvent e = (TextChannelUpdateNameEvent) event;
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Channel Name Updated: " + event.getChannel().getAsMention()
                    )
                    .addField("Before", e.getOldName(), false)
                    .addField("After", e.getNewName(), false)
                    .build()
            ).queue();
        } else if (event instanceof TextChannelUpdateTopicEvent) {
            TextChannelUpdateTopicEvent e = (TextChannelUpdateTopicEvent) event;
            String oldTopic = e.getOldTopic() == null ? "" : e.getOldTopic();
            String newTopic = e.getNewTopic() == null ? "" : e.getNewTopic();
            if (oldTopic.equals(newTopic)) {
                return;
            }

            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Channel Topic Updated: " + event.getChannel().getAsMention()
                    )
                    .addField("Before", oldTopic, false)
                    .addField("After", newTopic, false)
                    .build()
            ).queue();
        } else if (event instanceof TextChannelUpdatePositionEvent) {
            TextChannelUpdatePositionEvent e = (TextChannelUpdatePositionEvent) event;
            if (e.getOldPosition() == e.getNewPosition()) return;

            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Channel Position Updated: " + event.getChannel().getAsMention()
                    )
                    .addField("Before", String.valueOf(e.getOldPosition()), false)
                    .addField("After", String.valueOf(e.getNewPosition()), false)
                    .build()
            ).queue();
        } else if (event instanceof TextChannelUpdatePermissionsEvent) {
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Channel Permission Updated: " + event.getChannel().getAsMention()
                    )
                    .build()
            ).queue();
        } else if (event instanceof TextChannelUpdateNSFWEvent) {
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Channel NSFW " + (event.getChannel().isNSFW() ? "Disabled" : "Enabled") + ": " + event.getChannel().getAsMention()
                    )
                    .build()
            ).queue();
        } else if (event instanceof TextChannelUpdateParentEvent) {
            TextChannelUpdateParentEvent e = (TextChannelUpdateParentEvent) event;
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Channel Category Changed: " + event.getChannel().getAsMention()
                    )
                    .addField("Before", e.getOldParent() == null ? "" : e.getOldParent().getName(), false)
                    .addField("After", e.getNewParent() == null ? "" : e.getNewParent().getName(), false)
                    .build()
            ).queue();
        } else if (event instanceof TextChannelCreateEvent) {
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_GREEN.getColor())
                    .setDescription(
                            "Channel Created: " + event.getChannel().getAsMention()
                    )
                    .build()
            ).queue();
        }
    }

    @Override
    public void onGenericVoiceChannel(GenericVoiceChannelEvent event) {
        long guildId = event.getGuild().getIdLong();
        ServerLogEntry log = this.serverLogRepository.findOne(() -> guildId);
        if (log == null) {
            return;
        }
        TextChannel logChannel = this.shardManager.getTextChannelById(log.getChannelId());
        if (logChannel == null) {
            return;
        }
        String formattedDate = getFormattedCurrentTime(guildId, log.getChannelId());

        // Base embed message
        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                .setFooter(String.format("Channel ID: %s | %s", event.getChannel().getIdLong(), formattedDate));

        if (event instanceof VoiceChannelDeleteEvent) {
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.RED.getColor())
                    .setDescription(
                            "Voice Channel Deleted: #" + event.getChannel().getName()
                    )
                    .build()
            ).queue();
        } else if (event instanceof VoiceChannelUpdateNameEvent) {
            VoiceChannelUpdateNameEvent e = (VoiceChannelUpdateNameEvent) event;
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Voice Channel Name Updated: #" + event.getChannel().getName()
                    )
                    .addField("Before", e.getOldName(), false)
                    .addField("After", e.getNewName(), false)
                    .build()
            ).queue();
        } else if (event instanceof VoiceChannelUpdatePositionEvent) {
            VoiceChannelUpdatePositionEvent e = (VoiceChannelUpdatePositionEvent) event;
            int oldPosition = e.getOldPosition();
            int newPosition = e.getNewPosition();
            if (oldPosition == newPosition) return;

            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Voice Channel Position Updated: #" + event.getChannel().getName()
                    )
                    .addField("Before", String.valueOf(e.getOldPosition()), false)
                    .addField("After", String.valueOf(e.getNewPosition()), false)
                    .build()
            ).queue();
        } else if (event instanceof VoiceChannelUpdateUserLimitEvent) {
            VoiceChannelUpdateUserLimitEvent e = (VoiceChannelUpdateUserLimitEvent) event;
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Voice Channel User Limit Updated: #" + event.getChannel().getName()
                    )
                    .addField("Before", String.valueOf(e.getOldUserLimit()), false)
                    .addField("After", String.valueOf(e.getNewUserLimit()), false)
                    .build()
            ).queue();
        } else if (event instanceof VoiceChannelUpdateBitrateEvent) {
            VoiceChannelUpdateBitrateEvent e = (VoiceChannelUpdateBitrateEvent) event;
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Voice Channel Bitrate Updated: #" + event.getChannel().getName()
                    )
                    .addField("Before", new DecimalFormat("#,###").format(e.getOldBitrate()) + " bps", false)
                    .addField("After", new DecimalFormat("#,###").format(e.getNewBitrate()) + " bps", false)
                    .build()
            ).queue();
        } else if (event instanceof VoiceChannelUpdatePermissionsEvent) {
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Voice Channel Permission Updated: #" + event.getChannel().getName()
                    )
                    .build()
            ).queue();
        } else if (event instanceof VoiceChannelUpdateParentEvent) {
            VoiceChannelUpdateParentEvent e = (VoiceChannelUpdateParentEvent) event;
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Voice Channel Category Updated: #" + event.getChannel().getName()
                    )
                    .addField("Before", e.getOldParent() == null ? "" : e.getOldParent().getName(), false)
                    .addField("After", e.getNewParent() == null ? "" : e.getNewParent().getName(), false)
                    .build()
            ).queue();
        } else if (event instanceof VoiceChannelCreateEvent) {
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_GREEN.getColor())
                    .setDescription(
                            "Voice Channel Created: #" + event.getChannel().getName()
                    )
                    .build()
            ).queue();
        }
    }

    // User Events
    @Override
    public void onGenericUser(GenericUserEvent event) {
        // Base embed message
        String nameWithDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();
        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(nameWithDiscriminator, null, event.getUser().getEffectiveAvatarUrl());

        if (event instanceof UserUpdateNameEvent) {
            UserUpdateNameEvent e = (UserUpdateNameEvent) event;
            eb.setColor(MinecraftColor.GRAY.getColor())
                    .setDescription(
                            "Username Changed: " + nameWithDiscriminator
                    )
                    .addField("Before", e.getOldName() + "#" + event.getUser().getDiscriminator(), false)
                    .addField("After", e.getNewName() + "#" + event.getUser().getDiscriminator(), false)
                    .build();
        } else if (event instanceof UserUpdateDiscriminatorEvent) {
            UserUpdateDiscriminatorEvent e = (UserUpdateDiscriminatorEvent) event;
            eb.setColor(MinecraftColor.GRAY.getColor())
                    .setDescription(
                            "User Discriminator Changed: " + nameWithDiscriminator
                    )
                    .addField("Before", event.getUser().getName() + "#" + e.getOldDiscriminator(), false)
                    .addField("After", event.getUser().getName() + "#" + e.getNewDiscriminator(), false)
                    .build();
        } else {
            return;
        }

        // Retrieve channels to send
        long userId = event.getUser().getIdLong();
        long[] guildIDs = event.getUser().getMutualGuilds().stream().mapToLong(ISnowflake::getIdLong).toArray();
        List<ServerLogEntry> logs = this.serverLogRepository.findAllIn(guildIDs);
        if (logs == null) {
            return;
        }

        for (ServerLogEntry log : logs) {
            TextChannel logChannel = this.shardManager.getTextChannelById(log.getChannelId());
            if (logChannel == null) {
                continue;
            }
            // different timezones for each guild / log channel
            String formattedDate = getFormattedCurrentTime(log.getGuildId(), log.getChannelId());
            logChannel.sendMessage(
                    new EmbedBuilder(eb)
                    .setFooter(String.format("User ID: %s | %s", userId, formattedDate))
                    .build()
            ).queue();
        }
    }

    @Override
    public void onGenericGuild(GenericGuildEvent event) {
        long guildId = event.getGuild().getIdLong();
        ServerLogEntry log = this.serverLogRepository.findOne(() -> guildId);
        if (log == null) {
            return;
        }
        TextChannel logChannel = this.shardManager.getTextChannelById(log.getChannelId());
        if (logChannel == null) {
            return;
        }
        String formattedDate = getFormattedCurrentTime(guildId, log.getChannelId());

        if (event instanceof GuildBanEvent) {
            GuildBanEvent e = (GuildBanEvent) event;
            String banReason;
            try {
                banReason = e.getGuild().retrieveBan(e.getUser()).complete().getReason();
            } catch (RuntimeException ex) {
                banReason = "(Couldn't retrieve ban reason)";
            }

            String user = e.getUser().getName() + "#" + e.getUser().getDiscriminator();
            logChannel.sendMessage(
                    new EmbedBuilder()
                    .setColor(MinecraftColor.BLACK.getColor())
                    .setAuthor(user, null, e.getUser().getEffectiveAvatarUrl())
                    .setFooter("User ID: " + e.getUser().getIdLong() + " | " + formattedDate)
                    .setDescription(
                            "User Banned: " + user
                    )
                    .addField("Ban Reason", banReason, false)
                    .build()
            ).queue();
        } else if (event instanceof GuildUnbanEvent) {
            GuildUnbanEvent e = (GuildUnbanEvent) event;
            String user = e.getUser().getName() + "#" + e.getUser().getDiscriminator();
            logChannel.sendMessage(
                    new EmbedBuilder()
                    .setColor(MinecraftColor.DARK_GRAY.getColor())
                    .setAuthor(user, null, ((GuildUnbanEvent) event).getUser().getEffectiveAvatarUrl())
                    .setFooter("User ID: " + e.getUser().getIdLong() + " | " + formattedDate)
                    .setDescription(
                            "User Unbanned: " + user
                    )
                    .build()
            ).queue();
        } else if (event instanceof GuildMessageUpdateEvent) {
            GuildMessageUpdateEvent e = (GuildMessageUpdateEvent) event;
            if (e.getAuthor().isBot()) return;
            if (log.getChannelId() == e.getChannel().getIdLong()) return;

            long messageId = e.getMessageIdLong();
            EmbedBuilder eb = new EmbedBuilder();
            // Retrieve old message from the cache, if possible
            String oldMessage = messageCache.get(messageId);
            if (oldMessage != null) {
                eb.addField("Before",
                        oldMessage.length() > MessageEmbed.VALUE_MAX_LENGTH
                                ? oldMessage.substring(0, MessageEmbed.VALUE_MAX_LENGTH - 1) + "…"
                                : oldMessage,
                        false);
            }

            // Update message cache
            messageCache.add(messageId, e.getMessage().getContentRaw());

            String user = e.getAuthor().getName() + "#" + e.getAuthor().getDiscriminator();
            long elapsedSeconds = (System.currentTimeMillis() - BotUtils.getIdCreationTime(messageId)) / 1000L;
            String newContentRaw = e.getMessage().getContentRaw();

            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(user, null, e.getAuthor().getEffectiveAvatarUrl())
                    .setFooter("Message ID: " + messageId + " | " + formattedDate, null)
                    .setDescription(
                            String.format("Message sent by %s edited in %s\nSent time: %s (%s ago)",
                                    e.getAuthor().getAsMention(),
                                    e.getChannel().getAsMention(),
                                    getFormattedTime(guildId, log.getChannelId(), BotUtils.getIdCreationTime(messageId)),
                                    FormatUtils.formatReadableTime(elapsedSeconds, false, "s"))
                    )
                    .addField("After",
                            newContentRaw.length() > MessageEmbed.VALUE_MAX_LENGTH
                                    ? newContentRaw.substring(0, MessageEmbed.VALUE_MAX_LENGTH - 1) + "…"
                                    : newContentRaw,
                            false)
                    .build()
            ).queue();
        } else if (event instanceof GuildMessageDeleteEvent) {
            GuildMessageDeleteEvent e = (GuildMessageDeleteEvent) event;
            if (log.getChannelId() == e.getChannel().getIdLong()) return;

            long messageId = e.getMessageIdLong();
            EmbedBuilder eb = new EmbedBuilder();

            // Try to retrieve the old messsage
            String oldMessage = messageCache.get(messageId);
            User user = null;
            try {
                Message message = e.getChannel().retrieveMessageById(messageId).complete();
                if (oldMessage == null) {
                    oldMessage = message.getContentRaw();
                }
                user = message.getAuthor();
            } catch (RuntimeException ignored) {
            }
            if (oldMessage != null) {
                eb.addField("Content",
                        oldMessage.length() > MessageEmbed.VALUE_MAX_LENGTH
                                ? oldMessage.substring(0, MessageEmbed.VALUE_MAX_LENGTH - 1) + "…"
                                : oldMessage,
                        false);
            }

            // TODO: might want to delete cache

            long elapsedSeconds = (System.currentTimeMillis() - BotUtils.getIdCreationTime(messageId)) / 1000L;

            logChannel.sendMessage(
                    eb.setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                    .setColor(MinecraftColor.RED.getColor())
                    .setFooter("Message ID: " + e.getMessageIdLong() + " | " + formattedDate)
                    .setDescription(
                            String.format("%s\nSent time: %s (%s ago)",
                                    user == null
                                            ? "Message deleted in " + e.getChannel().getAsMention()
                                            : "Message sent by " + user.getAsMention() + " deleted in " + e.getChannel().getAsMention(),
                                    getFormattedTime(guildId, log.getChannelId(), BotUtils.getIdCreationTime(messageId)),
                                    FormatUtils.formatReadableTime(elapsedSeconds, false, "s"))
                    )
                    .build()
            ).queue();
        } else if (event instanceof GuildUpdateSystemChannelEvent) {
            GuildUpdateSystemChannelEvent e = (GuildUpdateSystemChannelEvent) event;

            TextChannel oldChannel = e.getOldSystemChannel();
            TextChannel newChannel = e.getNewSystemChannel();

            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                    .setFooter("New Channel ID: " +
                            (newChannel == null ? "None" : newChannel.getIdLong()) + " | " + formattedDate)
                    .setDescription(
                            "System Messages Channel Updated"
                    )
                    .addField("Before", oldChannel == null ? "None" : oldChannel.getAsMention(), false)
                    .addField("After", newChannel == null ? "None" : newChannel.getAsMention(), false)
                    .build()
            ).queue();
        } else if (event instanceof GuildUpdateAfkTimeoutEvent) {
            GuildUpdateAfkTimeoutEvent e = (GuildUpdateAfkTimeoutEvent) event;
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                    .setFooter(formattedDate)
                    .setDescription(
                            "Afk Timeout Updated"
                    )
                    .addField("Before",
                            FormatUtils.formatReadableTime(e.getOldAfkTimeout().getSeconds(), false, "s"),
                            false)
                    .addField("After",
                            FormatUtils.formatReadableTime(e.getNewAfkTimeout().getSeconds(), false, "s"),
                            false)
                    .build()
            ).queue();
        } else if (event instanceof GuildUpdateExplicitContentLevelEvent) {
            GuildUpdateExplicitContentLevelEvent e = (GuildUpdateExplicitContentLevelEvent) event;
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                    .setFooter(formattedDate)
                    .setDescription(
                            "Explicit Content Scan Level Updated"
                    )
                    .addField("Before", e.getOldLevel().getDescription()
                            + " - " + e.getOldLevel().getKey(), false)
                    .addField("After", e.getNewLevel().getDescription()
                            + " - " + e.getNewLevel().getKey(), false)
                    .build()
            ).queue();
        } else if (event instanceof GuildUpdateIconEvent) {
            GuildUpdateIconEvent e = (GuildUpdateIconEvent) event;
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                    .setFooter(formattedDate)
                    .setDescription(
                            "Guild Icon Updated"
                    )
                    .addField("Before", "Thumbnail →", false)
                    .addField("After", "Image ↓", false)
                    .setThumbnail(e.getOldIconUrl())
                    .setImage(e.getNewIconUrl())
                    .build()
            ).queue();
        } else if (event instanceof GuildUpdateMFALevelEvent) {
            GuildUpdateMFALevelEvent e = (GuildUpdateMFALevelEvent) event;
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                    .setFooter(formattedDate)
                    .setDescription(
                            "Guild MFA Level Updated"
                    )
                    .addField("Before", e.getOldMFALevel().name(), false)
                    .addField("After", e.getNewMFALevel().name(), false)
                    .build()
            ).queue();
        } else if (event instanceof GuildUpdateNameEvent) {
            GuildUpdateNameEvent e = (GuildUpdateNameEvent) event;
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                    .setFooter(formattedDate)
                    .setDescription(
                            "Guild Name Updated"
                    )
                    .addField("Before", e.getOldName(), false)
                    .addField("After", e.getNewName(), false)
                    .build()
            ).queue();
        } else if (event instanceof GuildUpdateNotificationLevelEvent) {
            GuildUpdateNotificationLevelEvent e = (GuildUpdateNotificationLevelEvent) event;
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                    .setFooter(formattedDate)
                    .setDescription(
                            "Guild Default Notification Setting Updated"
                    )
                    .addField("Before", e.getOldNotificationLevel().name(), false)
                    .addField("After", e.getNewNotificationLevel().name(), false)
                    .build()
            ).queue();
        } else if (event instanceof GuildUpdateOwnerEvent) {
            GuildUpdateOwnerEvent e = (GuildUpdateOwnerEvent) event;

            Member oldOwner = e.getOldOwner();
            Member newOwner = e.getNewOwner();

            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                    .setFooter(String.format("New Owner ID: %d | %s", e.getNewOwnerIdLong(), formattedDate))
                    .setDescription(
                            "Guild Owner Updated"
                    )
                    .addField("Old Owner", String.format("%s (%d)",
                            oldOwner != null ? oldOwner.getAsMention() : "None", e.getOldOwnerIdLong()),
                            false)
                    .addField("New Owner", String.format("%s (%d)",
                            newOwner != null ? newOwner.getAsMention() : "None", e.getNewOwnerIdLong()),
                            false)
                    .build()
            ).queue();
        } else if (event instanceof GuildUpdateRegionEvent) {
            GuildUpdateRegionEvent e = (GuildUpdateRegionEvent) event;
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                            .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                            .setFooter(formattedDate)
                            .setDescription(
                                    "Guild Region Updated"
                            )
                            .addField("Before", e.getOldRegion().getName(), false)
                            .addField("After", e.getNewRegion().getName(), false)
                            .build()
            ).queue();
        } else if (event instanceof GuildUpdateSplashEvent) {
            GuildUpdateSplashEvent e = (GuildUpdateSplashEvent) event;
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                            .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                            .setFooter(formattedDate)
                            .setDescription(
                                    "Guild Splash Updated"
                            )
                            .addField("Before", "Thumbnail →", false)
                            .addField("After", "Image ↓", false)
                            .setThumbnail(e.getOldSplashUrl())
                            .setImage(e.getNewSplashUrl())
                            .build()
            ).queue();
        } else if (event instanceof GuildUpdateVerificationLevelEvent) {
            GuildUpdateVerificationLevelEvent e = (GuildUpdateVerificationLevelEvent) event;
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                            .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                            .setFooter(formattedDate)
                            .setDescription(
                                    "Guild Verification Level Updated"
                            )
                            .addField("Before", e.getOldVerificationLevel().name(), false)
                            .addField("After", e.getNewVerificationLevel().name(), false)
                            .build()
            ).queue();
        } else if (event instanceof GuildMemberJoinEvent) {
            GuildMemberJoinEvent e = (GuildMemberJoinEvent) event;
            long accountCreation = BotUtils.getIdCreationTime(e.getUser().getIdLong());
            long elapsedMillis = System.currentTimeMillis() - accountCreation;

            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_GREEN.getColor())
                    .setAuthor(e.getUser().getName() + "#" + e.getUser().getDiscriminator(),
                            null, e.getUser().getEffectiveAvatarUrl())
                    .setFooter("User ID: " + e.getUser().getIdLong() + " | " + formattedDate)
                    .setDescription(
                            String.format("Member Joined (%d): %s#%s%s",
                                    event.getGuild().getMembers().size(),
                                    e.getUser().getName(),
                                    e.getUser().getDiscriminator(),
                                    elapsedMillis < TimeUnit.DAYS.toMillis(1)
                                        ? String.format("\n\n**Account created %d hours ago**\n\n(At %s, %s)",
                                            elapsedMillis / TimeUnit.HOURS.toMillis(1),
                                            getFormattedTime(guildId, log.getChannelId(), accountCreation),
                                            FormatUtils.formatReadableTime(elapsedMillis / 1000L, false, "s"))
                                        : "")
                    )
                    .build()
            ).queue();
        } else if (event instanceof GuildMemberLeaveEvent) {
            GuildMemberLeaveEvent e = (GuildMemberLeaveEvent) event;
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.RED.getColor())
                    .setAuthor(e.getUser().getName() + "#" + e.getUser().getDiscriminator(),
                            null, e.getUser().getEffectiveAvatarUrl())
                    .setFooter("User ID: " + e.getUser().getIdLong() + " | " + formattedDate)
                    .setDescription(
                            String.format("Member Left (%d): %s#%s",
                                    event.getGuild().getMembers().size(),
                                    e.getUser().getName(),
                                    e.getUser().getDiscriminator())
                    )
                    .build()
            ).queue();
        } else if (event instanceof GuildMemberRoleAddEvent) {
            GuildMemberRoleAddEvent e = (GuildMemberRoleAddEvent) event;
            String addedRoles = e.getRoles().stream()
                    .map(IMentionable::getAsMention).collect(Collectors.joining(" , "));

            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                            .setAuthor(e.getUser().getName() + "#" + e.getUser().getDiscriminator(),
                                    null, e.getUser().getEffectiveAvatarUrl())
                            .setFooter("User ID: " + e.getUser().getIdLong() + " | " + formattedDate)
                            .setDescription(
                                    String.format("Member Role Added: %s has been given role %s",
                                            e.getUser().getAsMention(),
                                            addedRoles)
                            )
                            .build()
            ).queue();
        } else if (event instanceof GuildMemberRoleRemoveEvent) {
            GuildMemberRoleRemoveEvent e = (GuildMemberRoleRemoveEvent) event;
            String removedRoles = e.getRoles().stream()
                    .map(IMentionable::getAsMention).collect(Collectors.joining(" , "));

            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(e.getUser().getName() + "#" + e.getUser().getDiscriminator(),
                            null, e.getUser().getEffectiveAvatarUrl())
                    .setFooter("User ID: " + e.getUser().getIdLong() + " | " + formattedDate)
                    .setDescription(
                            String.format("Member Role Removed: %s has been removed from role %s",
                                    e.getUser().getAsMention(),
                                    removedRoles)
                    )
                    .build()
            ).queue();
        } else if (event instanceof GuildMemberUpdateNicknameEvent) {
            GuildMemberUpdateNicknameEvent e = (GuildMemberUpdateNicknameEvent) event;

            String oldNick = e.getOldNickname();
            String newNick = e.getNewNickname();

            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(e.getUser().getName() + "#" + e.getUser().getDiscriminator(),
                            null, e.getUser().getEffectiveAvatarUrl())
                    .setFooter("User ID: " + e.getUser().getIdLong() + " | " + formattedDate)
                    .setDescription(
                            "Member Nickname Changed: " + e.getUser().getAsMention()
                    )
                    .addField("Before", oldNick == null ? "_None_" : oldNick, false)
                    .addField("After", newNick == null ? "_None_" : newNick, false)
                    .build()
            ).queue();
        } else if (event instanceof GuildVoiceJoinEvent) {
            GuildVoiceJoinEvent e = (GuildVoiceJoinEvent) event;
            User user = e.getMember().getUser();
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.LIGHT_PURPLE.getColor())
                    .setAuthor(user.getName() + "#" + user.getDiscriminator(),
                            null, user.getEffectiveAvatarUrl())
                    .setFooter("Voice Channel ID: " + e.getChannelJoined().getIdLong() + " | " + formattedDate)
                    .setDescription(
                            user.getAsMention()
                                    + " joined voice channel #" + e.getChannelJoined().getName()
                    )
                    .build()
            ).queue();
        } else if (event instanceof GuildVoiceMoveEvent) {
            GuildVoiceMoveEvent e = (GuildVoiceMoveEvent) event;
            User user = e.getMember().getUser();
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.LIGHT_PURPLE.getColor())
                    .setAuthor(user.getName() + "#" + user.getDiscriminator(),
                            null, user.getEffectiveAvatarUrl())
                    .setFooter("Voice Channel ID: " + e.getChannelJoined().getIdLong() + " | " + formattedDate)
                    .setDescription(
                            user.getAsMention()
                                    + " moved voice channel to #" + e.getChannelJoined().getName()
                    )
                    .build()
            ).queue();
        } else if (event instanceof GuildVoiceLeaveEvent) {
            GuildVoiceLeaveEvent e = (GuildVoiceLeaveEvent) event;
            User user = e.getMember().getUser();
            logChannel.sendMessage(
                    new EmbedBuilder().setColor(MinecraftColor.LIGHT_PURPLE.getColor())
                    .setAuthor(user.getName() + "#" + user.getDiscriminator(),
                            null, user.getEffectiveAvatarUrl())
                    .setFooter("Voice Channel ID: " + e.getChannelLeft().getIdLong() + " | " + formattedDate)
                    .setDescription(
                            user.getAsMention()
                                    + " left voice channel #" + e.getChannelLeft().getName()
                    )
                    .build()
            ).queue();
        } else if (event instanceof GuildVoiceGuildMuteEvent) {
            GuildVoiceGuildMuteEvent e = (GuildVoiceGuildMuteEvent) event;
            User user = e.getMember().getUser();
            logChannel.sendMessage(
                    new EmbedBuilder()
                    .setColor(e.isGuildMuted() ? MinecraftColor.RED.getColor() : MinecraftColor.DARK_GREEN.getColor())
                    .setAuthor(user.getName() + "#" + user.getDiscriminator(),
                            null, user.getEffectiveAvatarUrl())
                    .setFooter("User ID: " + user.getIdLong() + " | " + formattedDate)
                    .setDescription(
                            user.getAsMention()
                                    + " has been " + (e.isGuildMuted() ? "muted." : "un-muted.")
                    )
                    .build()
            ).queue();
        } else if (event instanceof GuildVoiceGuildDeafenEvent) {
            GuildVoiceGuildDeafenEvent e = (GuildVoiceGuildDeafenEvent) event;
            User user = e.getMember().getUser();
            logChannel.sendMessage(
                    new EmbedBuilder()
                    .setColor(e.isGuildDeafened() ? MinecraftColor.RED.getColor() : MinecraftColor.DARK_GREEN.getColor())
                    .setAuthor(user.getName() + "#" + user.getDiscriminator(),
                            null, user.getEffectiveAvatarUrl())
                    .setFooter("User ID: " + user.getIdLong() + " | " + formattedDate)
                    .setDescription(
                            user.getAsMention()
                                    + " has been " + (e.isGuildDeafened() ? "deafened." : "un-deafened.")
                    )
                    .build()
            ).queue();
        } else if (event instanceof GuildVoiceSuppressEvent) {
            GuildVoiceSuppressEvent e = (GuildVoiceSuppressEvent) event;
            User user = e.getMember().getUser();
            logChannel.sendMessage(
                    new EmbedBuilder()
                    .setColor(e.isSuppressed() ? MinecraftColor.RED.getColor() : MinecraftColor.DARK_GREEN.getColor())
                    .setAuthor(user.getName() + "#" + user.getDiscriminator(),
                            null, user.getEffectiveAvatarUrl())
                    .setFooter("User ID: " + user.getIdLong() + " | " + formattedDate)
                    .setDescription(
                            user.getAsMention()
                                    + " has been " + (e.isSuppressed() ? "suppressed." : "un-suppressed.")
                    )
                    .build()
            ).queue();
        }
    }

    @Override
    public void onGenericRole(GenericRoleEvent event) {
        long guildId = event.getGuild().getIdLong();
        ServerLogEntry log = this.serverLogRepository.findOne(() -> guildId);
        if (log == null) {
            return;
        }
        TextChannel logChannel = this.shardManager.getTextChannelById(log.getChannelId());
        if (logChannel == null) {
            return;
        }
        String formattedDate = getFormattedCurrentTime(guildId, log.getChannelId());

        // Base embed message
        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                .setFooter(String.format("Role ID: %s | %s", event.getRole().getIdLong(), formattedDate));

        if (event instanceof RoleCreateEvent) {
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_GREEN.getColor())
                    .setDescription(
                            "Role Created: `" + event.getRole().getName() + "`"
                    )
                    .build()
            ).queue();
        } else if (event instanceof RoleDeleteEvent) {
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.RED.getColor())
                    .setDescription(
                            "Role Deleted: `" + event.getRole().getName() + "`"
                    )
                    .build()
            ).queue();
        } else if (event instanceof RoleUpdateColorEvent) {
            RoleUpdateColorEvent e = (RoleUpdateColorEvent) event;

            Color oldColor = e.getOldColor();
            Color newColor = e.getNewColor();
            Function<Color, String> formatRGB = color -> String.format(
                    "RGB: %s, %s, %s", color.getRed(), color.getGreen(), color.getBlue());

            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Role Color Updated: `" + event.getRole().getName() + "`"
                    )
                    .addField("Before", oldColor != null ? formatRGB.apply(oldColor) : "None", false)
                    .addField("After", newColor != null ? formatRGB.apply(newColor) : "None", false)
                    .build()
            ).queue();
        } else if (event instanceof RoleUpdateHoistedEvent) {
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            String.format("Role is %s displayed on online list: `%s`",
                                    event.getRole().isHoisted() ? "now" : "no longer",
                                    event.getRole().getName())
                    )
                    .build()
            ).queue();
        } else if (event instanceof RoleUpdateMentionableEvent) {
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            String.format("Role is %s mentionable: `%s`",
                                    event.getRole().isMentionable() ? "now" : "no longer",
                                    event.getRole().getName())
                    )
                    .build()
            ).queue();
        } else if (event instanceof RoleUpdateNameEvent) {
            RoleUpdateNameEvent e = (RoleUpdateNameEvent) event;
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Role Name Updated: `" + event.getRole().getName() + "`"
                    )
                    .addField("Before", e.getOldName(), false)
                    .addField("After", e.getNewName(), false)
                    .build()
            ).queue();
        } else if (event instanceof RoleUpdatePermissionsEvent) {
            RoleUpdatePermissionsEvent e = (RoleUpdatePermissionsEvent) event;
            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Role Permission Updated: `" + event.getRole().getName() + "`"
                    )
                    .addField("Before", String.valueOf(e.getOldPermissionsRaw()), false)
                    .addField("After", String.valueOf(e.getNewPermissionsRaw()), false)
                    .build()
            ).queue();
        } else if (event instanceof RoleUpdatePositionEvent) {
            RoleUpdatePositionEvent e = (RoleUpdatePositionEvent) event;

            int oldPosition = e.getOldPosition();
            int newPosition = e.getNewPosition();
            if (oldPosition == newPosition) return;

            logChannel.sendMessage(
                    eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Role Position Updated: `" + event.getRole().getName() + "`"
                    )
                    .addField("Before", String.valueOf(e.getOldPosition()), false)
                    .addField("After", String.valueOf(e.getNewPosition()), false)
                    .build()
            ).queue();
        }
    }
}

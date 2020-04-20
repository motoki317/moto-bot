package app;

import db.model.serverLog.ServerLogEntry;
import db.model.timezone.CustomTimeZone;
import db.repository.base.ServerLogRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.Event;
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
import org.jetbrains.annotations.Nullable;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ServerLogListener extends ListenerAdapter {
    private static DateFormat getDateFormat() {
        return new SimpleDateFormat("yyyy/MM/dd E',' HH:mm:ss.SSS");
    }

    private static class MessageCache {
        private final String content;
        private final long userId;

        private MessageCache(String content, long userId) {
            this.content = content;
            this.userId = userId;
        }
    }

    private static final DataCache<Long, MessageCache> messageCache = new HashMapDataCache<>(
            5000, TimeUnit.HOURS.toMillis(3), TimeUnit.MINUTES.toMillis(10)
    );

    private interface Handler<T> {
        /**
         * Handles event and adds description to embed message.
         * @param event Event.
         * @param eb Embed builder.
         * @return Returns null if the message should not be sent.
         */
        @Nullable
        EmbedBuilder handle(T event, EmbedBuilder eb);
    }

    private interface GuildEventHandler<T> {
        /**
         * Handles guild event and adds description to embed message.
         * @param event Event.
         * @param eb Embed builder.
         * @param getFormattedTime If given a unix milliseconds time, formats the date.
         * @param getUser Given a user ID, retrieves discord user.
         * @return Returns null if the message should not be sent.
         */
        @Nullable
        EmbedBuilder handle(T event, EmbedBuilder eb, Function<Long, String> getFormattedTime, Function<Long, @Nullable User> getUser);
    }

    private static final Map<Class<?>, Handler<?>> handlers;

    private static final Map<Class<?>, GuildEventHandler<?>> guildEventHandlers;

    private static <T extends Event> void addHandler(Class<T> c, Handler<T> handler) {
        if (handlers.containsKey(c)) {
            throw new Error("Duplicate handler for " + c.toString());
        }
        handlers.put(c, handler);
    }

    private static <T extends GenericGuildEvent> void addGuildEventHandler(Class<T> c, GuildEventHandler<T> handler) {
        if (guildEventHandlers.containsKey(c)) {
            throw new Error("Duplicate guild event handler for " + c.toString());
        }
        guildEventHandlers.put(c, handler);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T extends Event> EmbedBuilder handle(Class<T> c, Event event, EmbedBuilder eb) {
        Handler<T> handler = (Handler<T>) handlers.getOrDefault(c, null);
        if (handler == null) {
            return null;
        }
        T casted;
        try {
            casted = c.cast(event);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return null;
        }
        return handler.handle(casted, eb);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T extends GenericGuildEvent> EmbedBuilder handleGuildEvent(Class<T> c, Event event,
                                                                               EmbedBuilder eb, Function<Long, String> formatTime, Function<Long, @Nullable User> getUser) {
        GuildEventHandler<T> handler = (GuildEventHandler<T>) guildEventHandlers.getOrDefault(c, null);
        if (handler == null) {
            return null;
        }
        T casted;
        try {
            casted = c.cast(event);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return null;
        }
        return handler.handle(casted, eb, formatTime, getUser);
    }

    static {
        handlers = new HashMap<>();
        guildEventHandlers = new HashMap<>();

        // Generic text channel
        addHandler(TextChannelDeleteEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.RED.getColor())
                        .setDescription(
                                "Channel Deleted: #" + event.getChannel().getName()
                        )
        );
        addHandler(TextChannelUpdateNameEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                "Channel Name Updated: " + event.getChannel().getAsMention()
                        )
                        .addField("Before", event.getOldName(), false)
                        .addField("After", event.getNewName(), false)
        );
        addHandler(TextChannelUpdateTopicEvent.class, (event, eb) -> {
            String oldTopic = event.getOldTopic() == null ? "" : event.getOldTopic();
            String newTopic = event.getNewTopic() == null ? "" : event.getNewTopic();
            if (oldTopic.equals(newTopic)) {
                return null;
            }

            return eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Channel Topic Updated: " + event.getChannel().getAsMention()
                    )
                    .addField("Before", oldTopic, false)
                    .addField("After", newTopic, false);
        });
        addHandler(TextChannelUpdatePositionEvent.class, (event, eb) -> {
            if (event.getOldPosition() == event.getNewPosition()) {
                return null;
            }

            return eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Channel Position Updated: " + event.getChannel().getAsMention()
                    )
                    .addField("Before", String.valueOf(event.getOldPosition()), false)
                    .addField("After", String.valueOf(event.getNewPosition()), false);
        });
        addHandler(TextChannelUpdatePermissionsEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                "Channel Permission Updated: " + event.getChannel().getAsMention()
                        )
        );
        addHandler(TextChannelUpdateNSFWEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                String.format("Channel NSFW %s: %s",
                                        event.getChannel().isNSFW() ? "Disabled" : "Enabled",
                                        event.getChannel().getAsMention())
                        )
        );
        addHandler(TextChannelUpdateParentEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                "Channel Category Changed: " + event.getChannel().getAsMention()
                        )
                        .addField("Before",
                                event.getOldParent() == null ? "" : event.getOldParent().getName(),
                                false)
                        .addField("After",
                                event.getNewParent() == null ? "" : event.getNewParent().getName(),
                                false)
        );
        addHandler(TextChannelCreateEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_GREEN.getColor())
                        .setDescription(
                                "Channel Created: " + event.getChannel().getAsMention()
                        )
        );

        // Generic voice channel
        addHandler(VoiceChannelDeleteEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.RED.getColor())
                        .setDescription(
                                "Voice Channel Deleted: #" + event.getChannel().getName()
                        )
        );
        addHandler(VoiceChannelUpdateNameEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                "Voice Channel Name Updated: #" + event.getChannel().getName()
                        )
                        .addField("Before", event.getOldName(), false)
                        .addField("After", event.getNewName(), false)
        );
        addHandler(VoiceChannelUpdatePositionEvent.class, (event, eb) -> {
            int oldPosition = event.getOldPosition();
            int newPosition = event.getNewPosition();
            if (oldPosition == newPosition) return null;

            return eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Voice Channel Position Updated: #" + event.getChannel().getName()
                    )
                    .addField("Before", String.valueOf(oldPosition), false)
                    .addField("After", String.valueOf(newPosition), false);
        });
        addHandler(VoiceChannelUpdateUserLimitEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                "Voice Channel User Limit Updated: #" + event.getChannel().getName()
                        )
                        .addField("Before", String.valueOf(event.getOldUserLimit()), false)
                        .addField("After", String.valueOf(event.getNewUserLimit()), false)
        );
        addHandler(VoiceChannelUpdateBitrateEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                "Voice Channel Bitrate Updated: #" + event.getChannel().getName()
                        )
                        .addField("Before", new DecimalFormat("#,###").format(event.getOldBitrate()) + " bps", false)
                        .addField("After", new DecimalFormat("#,###").format(event.getNewBitrate()) + " bps", false)
        );
        addHandler(VoiceChannelUpdatePermissionsEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                "Voice Channel Permission Updated: #" + event.getChannel().getName()
                        )
        );
        addHandler(VoiceChannelUpdateParentEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                "Voice Channel Category Updated: #" + event.getChannel().getName()
                        )
                        .addField("Before", event.getOldParent() == null ? "" : event.getOldParent().getName(), false)
                        .addField("After", event.getNewParent() == null ? "" : event.getNewParent().getName(), false)
        );
        addHandler(VoiceChannelCreateEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_GREEN.getColor())
                        .setDescription(
                                "Voice Channel Created: #" + event.getChannel().getName()
                        )
        );

        // Generic user
        addHandler(UserUpdateNameEvent.class, (event, eb) -> {
            String nameWithDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();
            return eb.setColor(MinecraftColor.GRAY.getColor())
                    .setDescription(
                            "Username Changed: " + nameWithDiscriminator
                    )
                    .addField("Before", event.getOldName() + "#" + event.getUser().getDiscriminator(), false)
                    .addField("After", event.getNewName() + "#" + event.getUser().getDiscriminator(), false);
        });
        addHandler(UserUpdateDiscriminatorEvent.class, (event, eb) -> {
            String nameWithDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();
            return eb.setColor(MinecraftColor.GRAY.getColor())
                    .setDescription(
                            "User Discriminator Changed: " + nameWithDiscriminator
                    )
                    .addField("Before", event.getUser().getName() + "#" + event.getOldDiscriminator(), false)
                    .addField("After", event.getUser().getName() + "#" + event.getNewDiscriminator(), false);
        });

        // Generic guild
        addGuildEventHandler(GuildBanEvent.class, ServerLogListener::handleGuildBan);
        addGuildEventHandler(GuildUnbanEvent.class, (event, eb, getFormattedTime, getUser) -> {
            String nameWithDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();
            return eb.setColor(MinecraftColor.DARK_GRAY.getColor())
                    .setAuthor(nameWithDiscriminator, null, event.getUser().getEffectiveAvatarUrl())
                    .setFooter("User ID: " + event.getUser().getIdLong())
                    .setDescription(
                            "User Unbanned: " + nameWithDiscriminator
                    );
        });
        addGuildEventHandler(GuildMessageUpdateEvent.class, ServerLogListener::handleMessageUpdate);
        addGuildEventHandler(GuildMessageDeleteEvent.class, ServerLogListener::handleMessageDelete);
        addGuildEventHandler(GuildUpdateSystemChannelEvent.class, (event, eb, getFormattedTime, getUser) -> {
            TextChannel oldChannel = event.getOldSystemChannel();
            TextChannel newChannel = event.getNewSystemChannel();

            return eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                    .setFooter("New Channel ID: " +
                            (newChannel == null ? "None" : newChannel.getIdLong()))
                    .setDescription(
                            "System Messages Channel Updated"
                    )
                    .addField("Before", oldChannel == null ? "None" : oldChannel.getAsMention(), false)
                    .addField("After", newChannel == null ? "None" : newChannel.getAsMention(), false);
        });
        addGuildEventHandler(GuildUpdateAfkTimeoutEvent.class, (event, eb, getFormattedTime, getUser) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                        .setDescription(
                                "Afk Timeout Updated"
                        )
                        .addField("Before",
                                FormatUtils.formatReadableTime(event.getOldAfkTimeout().getSeconds(), false, "s"),
                                false)
                        .addField("After",
                                FormatUtils.formatReadableTime(event.getNewAfkTimeout().getSeconds(), false, "s"),
                                false)
        );
        addGuildEventHandler(GuildUpdateExplicitContentLevelEvent.class, (event, eb, getFormattedTime, getUser) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                        .setDescription(
                                "Explicit Content Scan Level Updated"
                        )
                        .addField("Before", event.getOldLevel().getDescription()
                                + " - " + event.getOldLevel().getKey(), false)
                        .addField("After", event.getNewLevel().getDescription()
                                + " - " + event.getNewLevel().getKey(), false)
        );
        addGuildEventHandler(GuildUpdateIconEvent.class, (event, eb, getFormattedTime, getUser) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                        .setDescription(
                                "Guild Icon Updated"
                        )
                        .addField("Before", "Thumbnail →", false)
                        .addField("After", "Image ↓", false)
                        .setThumbnail(event.getOldIconUrl())
                        .setImage(event.getNewIconUrl())
        );
        addGuildEventHandler(GuildUpdateMFALevelEvent.class, (event, eb, getFormattedTime, getUser) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                        .setDescription(
                                "Guild MFA Level Updated"
                        )
                        .addField("Before", event.getOldMFALevel().name(), false)
                        .addField("After", event.getNewMFALevel().name(), false)
        );
        addGuildEventHandler(GuildUpdateNameEvent.class, (event, eb, getFormattedTime, getUser) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                        .setDescription(
                                "Guild Name Updated"
                        )
                        .addField("Before", event.getOldName(), false)
                        .addField("After", event.getNewName(), false)
        );
        addGuildEventHandler(GuildUpdateNotificationLevelEvent.class, (event, eb, getFormattedTime, getUser) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                        .setDescription(
                                "Guild Default Notification Setting Updated"
                        )
                        .addField("Before", event.getOldNotificationLevel().name(), false)
                        .addField("After", event.getNewNotificationLevel().name(), false)
        );
        addGuildEventHandler(GuildUpdateOwnerEvent.class, (event, eb, getFormattedTime, getUser) -> {
            Member oldOwner = event.getOldOwner();
            Member newOwner = event.getNewOwner();

            return eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                    .setFooter(String.format("New Owner ID: %d", event.getNewOwnerIdLong()))
                    .setDescription(
                            "Guild Owner Updated"
                    )
                    .addField("Old Owner", String.format("%s (%d)",
                            oldOwner != null ? oldOwner.getAsMention() : "None", event.getOldOwnerIdLong()),
                            false)
                    .addField("New Owner", String.format("%s (%d)",
                            newOwner != null ? newOwner.getAsMention() : "None", event.getNewOwnerIdLong()),
                            false);
        });
        addGuildEventHandler(GuildUpdateRegionEvent.class, (event, eb, getFormattedTime, getUser) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                        .setDescription(
                                "Guild Region Updated"
                        )
                        .addField("Before", event.getOldRegion().getName(), false)
                        .addField("After", event.getNewRegion().getName(), false)
        );
        addGuildEventHandler(GuildUpdateSplashEvent.class, (event, eb, getFormattedTime, getUser) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                        .setDescription(
                                "Guild Splash Updated"
                        )
                        .addField("Before", "Thumbnail →", false)
                        .addField("After", "Image ↓", false)
                        .setThumbnail(event.getOldSplashUrl())
                        .setImage(event.getNewSplashUrl())
        );
        addGuildEventHandler(GuildUpdateVerificationLevelEvent.class, (event, eb, getFormattedTime, getUser) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                        .setDescription(
                                "Guild Verification Level Updated"
                        )
                        .addField("Before", event.getOldVerificationLevel().name(), false)
                        .addField("After", event.getNewVerificationLevel().name(), false)
        );
        addGuildEventHandler(GuildMemberJoinEvent.class, (event, eb, getFormattedTime, getUser) -> {
            long accountCreation = BotUtils.getIdCreationTime(event.getUser().getIdLong());
            long elapsedMillis = System.currentTimeMillis() - accountCreation;
            String nameWithDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();

            return eb.setColor(MinecraftColor.DARK_GREEN.getColor())
                    .setAuthor(nameWithDiscriminator, null, event.getUser().getEffectiveAvatarUrl())
                    .setFooter("User ID: " + event.getUser().getIdLong())
                    .setDescription(
                            String.format("Member Joined (%d): %s%s",
                                    event.getGuild().getMembers().size(),
                                    nameWithDiscriminator,
                                    elapsedMillis < TimeUnit.DAYS.toMillis(1)
                                            ? String.format("\n\n**Account created %d hours ago**\n\n(At %s, %s)",
                                            elapsedMillis / TimeUnit.HOURS.toMillis(1),
                                            getFormattedTime.apply(accountCreation),
                                            FormatUtils.formatReadableTime(elapsedMillis / 1000L, false, "s"))
                                            : "")
                    );
        });
        addGuildEventHandler(GuildMemberLeaveEvent.class, (event, eb, getFormattedTime, getUser) -> {
            String nameWithDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();

            return eb.setColor(MinecraftColor.RED.getColor())
                    .setAuthor(nameWithDiscriminator, null, event.getUser().getEffectiveAvatarUrl())
                    .setFooter("User ID: " + event.getUser().getIdLong())
                    .setDescription(
                            String.format("Member Left (%d): %s",
                                    event.getGuild().getMembers().size(),
                                    nameWithDiscriminator)
                    );
        });
        addGuildEventHandler(GuildMemberRoleAddEvent.class, (event, eb, getFormattedTime, getUser) -> {
            String nameWithDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();
            String addedRoles = event.getRoles().stream()
                    .map(IMentionable::getAsMention).collect(Collectors.joining(" , "));

            return eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(nameWithDiscriminator, null, event.getUser().getEffectiveAvatarUrl())
                    .setFooter("User ID: " + event.getUser().getIdLong())
                    .setDescription(
                            String.format("Member Role Added: %s has been given role %s",
                                    event.getUser().getAsMention(),
                                    addedRoles)
                    );
        });
        addGuildEventHandler(GuildMemberRoleRemoveEvent.class, (event, eb, getFormattedTime, getUser) -> {
            String nameWithDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();
            String removedRoles = event.getRoles().stream()
                    .map(IMentionable::getAsMention).collect(Collectors.joining(" , "));

            return eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(nameWithDiscriminator, null, event.getUser().getEffectiveAvatarUrl())
                    .setFooter("User ID: " + event.getUser().getIdLong())
                    .setDescription(
                            String.format("Member Role Removed: %s has been removed from role %s",
                                    event.getUser().getAsMention(),
                                    removedRoles)
                    );
        });
        addGuildEventHandler(GuildMemberUpdateNicknameEvent.class, (event, eb, getFormattedTime, getUser) -> {
            String nameWithDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();
            String oldNick = event.getOldNickname();
            String newNick = event.getNewNickname();

            return eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setAuthor(nameWithDiscriminator, null, event.getUser().getEffectiveAvatarUrl())
                    .setFooter("User ID: " + event.getUser().getIdLong())
                    .setDescription(
                            "Member Nickname Changed: " + event.getUser().getAsMention()
                    )
                    .addField("Before", oldNick == null ? "_None_" : oldNick, false)
                    .addField("After", newNick == null ? "_None_" : newNick, false);
        });
        addGuildEventHandler(GuildVoiceJoinEvent.class, (event, eb, getFormattedTime, getUser) -> {
            User user = event.getMember().getUser();
            String nameWithDiscriminator = user.getName() + "#" + user.getDiscriminator();

            return eb.setColor(MinecraftColor.LIGHT_PURPLE.getColor())
                    .setAuthor(nameWithDiscriminator, null, user.getEffectiveAvatarUrl())
                    .setFooter("Voice Channel ID: " + event.getChannelJoined().getIdLong())
                    .setDescription(
                            user.getAsMention() + " joined voice channel #" + event.getChannelJoined().getName()
                    );
        });
        addGuildEventHandler(GuildVoiceMoveEvent.class, (event, eb, getFormattedTime, getUser) -> {
            User user = event.getMember().getUser();
            String nameWithDiscriminator = user.getName() + "#" + user.getDiscriminator();

            return eb.setColor(MinecraftColor.LIGHT_PURPLE.getColor())
                    .setAuthor(nameWithDiscriminator, null, user.getEffectiveAvatarUrl())
                    .setFooter("Voice Channel ID: " + event.getChannelJoined().getIdLong())
                    .setDescription(
                            user.getAsMention() + " moved voice channel to #" + event.getChannelJoined().getName()
                    );
        });
        addGuildEventHandler(GuildVoiceLeaveEvent.class, (event, eb, getFormattedTime, getUser) -> {
            User user = event.getMember().getUser();
            String nameWithDiscriminator = user.getName() + "#" + user.getDiscriminator();

            return eb.setColor(MinecraftColor.LIGHT_PURPLE.getColor())
                    .setAuthor(nameWithDiscriminator, null, user.getEffectiveAvatarUrl())
                    .setFooter("Voice Channel ID: " + event.getChannelLeft().getIdLong())
                    .setDescription(
                            user.getAsMention() + " left voice channel #" + event.getChannelLeft().getName()
                    );
        });
        addGuildEventHandler(GuildVoiceGuildMuteEvent.class, (event, eb, getFormattedTime, getUser) -> {
            User user = event.getMember().getUser();
            String nameWithDiscriminator = user.getName() + "#" + user.getDiscriminator();

            return eb.setColor(event.isGuildMuted() ? MinecraftColor.RED.getColor() : MinecraftColor.DARK_GREEN.getColor())
                    .setAuthor(nameWithDiscriminator, null, user.getEffectiveAvatarUrl())
                    .setFooter("User ID: " + user.getIdLong())
                    .setDescription(
                            user.getAsMention() + " has been " + (event.isGuildMuted() ? "muted." : "un-muted.")
                    );
        });
        addGuildEventHandler(GuildVoiceGuildDeafenEvent.class, (event, eb, getFormattedTime, getUser) -> {
            User user = event.getMember().getUser();
            String nameWithDiscriminator = user.getName() + "#" + user.getDiscriminator();

            return eb.setColor(event.isGuildDeafened() ? MinecraftColor.RED.getColor() : MinecraftColor.DARK_GREEN.getColor())
                    .setAuthor(nameWithDiscriminator, null, user.getEffectiveAvatarUrl())
                    .setFooter("User ID: " + user.getIdLong())
                    .setDescription(
                            user.getAsMention() + " has been " + (event.isGuildDeafened() ? "deafened." : "un-deafened.")
                    );
        });
        addGuildEventHandler(GuildVoiceSuppressEvent.class, (event, eb, getFormattedTime, getUser) -> {
            User user = event.getMember().getUser();
            String nameWithDiscriminator = user.getName() + "#" + user.getDiscriminator();

            return eb.setColor(event.isSuppressed() ? MinecraftColor.RED.getColor() : MinecraftColor.DARK_GREEN.getColor())
                    .setAuthor(nameWithDiscriminator, null, user.getEffectiveAvatarUrl())
                    .setFooter("User ID: " + user.getIdLong())
                    .setDescription(
                            user.getAsMention() + " has been " + (event.isSuppressed() ? "suppressed." : "un-suppressed.")
                    );
        });

        // Generic role
        addHandler(RoleCreateEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_GREEN.getColor())
                        .setDescription(
                                "Role Created: `" + event.getRole().getName() + "`"
                        )
        );
        addHandler(RoleDeleteEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.RED.getColor())
                        .setDescription(
                                "Role Deleted: `" + event.getRole().getName() + "`"
                        )
        );
        addHandler(RoleUpdateColorEvent.class, (event, eb) -> {
            Color oldColor = event.getOldColor();
            Color newColor = event.getNewColor();
            Function<Color, String> formatRGB = color -> String.format(
                    "RGB: %s, %s, %s", color.getRed(), color.getGreen(), color.getBlue());

            return eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Role Color Updated: `" + event.getRole().getName() + "`"
                    )
                    .addField("Before", oldColor != null ? formatRGB.apply(oldColor) : "None", false)
                    .addField("After", newColor != null ? formatRGB.apply(newColor) : "None", false);
        });
        addHandler(RoleUpdateHoistedEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                String.format("Role is %s displayed on online list: `%s`",
                                        event.getRole().isHoisted() ? "now" : "no longer",
                                        event.getRole().getName())
                        )
        );
        addHandler(RoleUpdateMentionableEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                String.format("Role is %s mentionable: `%s`",
                                        event.getRole().isMentionable() ? "now" : "no longer",
                                        event.getRole().getName())
                        )
        );
        addHandler(RoleUpdateNameEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                "Role Name Updated: `" + event.getRole().getName() + "`"
                        )
                        .addField("Before", event.getOldName(), false)
                        .addField("After", event.getNewName(), false)
        );
        addHandler(RoleUpdatePermissionsEvent.class, (event, eb) ->
                eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                        .setDescription(
                                "Role Permission Updated: `" + event.getRole().getName() + "`"
                        )
                        .addField("Before (Raw)", String.valueOf(event.getOldPermissionsRaw()), false)
                        .addField("After (Raw)", String.valueOf(event.getNewPermissionsRaw()), false)
        );
        addHandler(RoleUpdatePositionEvent.class, (event, eb) -> {
            int oldPosition = event.getOldPosition();
            int newPosition = event.getNewPosition();
            if (oldPosition == newPosition) return null;

            return eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                    .setDescription(
                            "Role Position Updated: `" + event.getRole().getName() + "`"
                    )
                    .addField("Before", String.valueOf(oldPosition), false)
                    .addField("After", String.valueOf(newPosition), false);
        });
    }

    private static EmbedBuilder handleGuildBan(GuildBanEvent event, EmbedBuilder eb, Function<Long, String> getFormattedTime, Function<Long, @Nullable User> getUser) {
        String banReason;
        try {
            banReason = event.getGuild().retrieveBan(event.getUser()).complete().getReason();
        } catch (RuntimeException ex) {
            banReason = "(Couldn't retrieve ban reason)";
        }
        String nameWithDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();

        return eb.setColor(MinecraftColor.BLACK.getColor())
                .setAuthor(nameWithDiscriminator, null, event.getUser().getEffectiveAvatarUrl())
                .setFooter("User ID: " + event.getUser().getIdLong())
                .setDescription(
                        "User Banned: " + nameWithDiscriminator
                )
                .addField("Ban Reason", banReason, false);
    }

    private static EmbedBuilder handleMessageUpdate(GuildMessageUpdateEvent event, EmbedBuilder eb, Function<Long, String> getFormattedTime, Function<Long, @Nullable User> getUser) {
        if (event.getAuthor().isBot()) return null;

        // Retrieve old message from the cache, if possible
        long messageId = event.getMessageIdLong();
        MessageCache oldMessage = messageCache.get(messageId);
        if (oldMessage != null) {
            eb.addField("Before",
                    oldMessage.content.length() > MessageEmbed.VALUE_MAX_LENGTH
                            ? oldMessage.content.substring(0, MessageEmbed.VALUE_MAX_LENGTH - 1) + "…"
                            : oldMessage.content,
                    false);
        }

        // Update message cache
        messageCache.add(messageId, new MessageCache(event.getMessage().getContentRaw(), event.getAuthor().getIdLong()));

        String nameWithDiscriminator = event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator();
        long elapsedSeconds = (System.currentTimeMillis() - BotUtils.getIdCreationTime(messageId)) / 1000L;
        String newContentRaw = event.getMessage().getContentRaw();

        return eb.setColor(MinecraftColor.DARK_AQUA.getColor())
                .setAuthor(nameWithDiscriminator, null, event.getAuthor().getEffectiveAvatarUrl())
                .setFooter("Message ID: " + messageId, null)
                .setDescription(
                        String.format("Message sent by %s edited in %s\nSent time: %s (%s ago)",
                                event.getAuthor().getAsMention(),
                                event.getChannel().getAsMention(),
                                getFormattedTime.apply(BotUtils.getIdCreationTime(messageId)),
                                FormatUtils.formatReadableTime(elapsedSeconds, false, "s"))
                )
                .addField("After",
                        newContentRaw.length() > MessageEmbed.VALUE_MAX_LENGTH
                                ? newContentRaw.substring(0, MessageEmbed.VALUE_MAX_LENGTH - 1) + "…"
                                : newContentRaw,
                        false);
    }

    private static EmbedBuilder handleMessageDelete(GuildMessageDeleteEvent event, EmbedBuilder eb, Function<Long, String> getFormattedTime, Function<Long, @Nullable User> getUser) {
        long messageId = event.getMessageIdLong();

        // Try to retrieve the old message
        MessageCache oldMessage = messageCache.get(messageId);
        User user = oldMessage != null ? getUser.apply(oldMessage.userId) : null;
        if (oldMessage != null) {
            eb.addField("Content",
                    oldMessage.content.length() > MessageEmbed.VALUE_MAX_LENGTH
                            ? oldMessage.content.substring(0, MessageEmbed.VALUE_MAX_LENGTH - 1) + "…"
                            : oldMessage.content,
                    false);
        }

        // delete cache
        messageCache.delete(messageId);

        long elapsedSeconds = (System.currentTimeMillis() - BotUtils.getIdCreationTime(messageId)) / 1000L;

        return eb.setAuthor(event.getGuild().getName(), null, event.getGuild().getIconUrl())
                .setColor(MinecraftColor.RED.getColor())
                .setFooter("Message ID: " + event.getMessageIdLong())
                .setDescription(
                        String.format("%s\nSent time: %s (%s ago)",
                                user == null
                                        ? "Message deleted in " + event.getChannel().getAsMention()
                                        : "Message sent by " + user.getAsMention() + " deleted in " + event.getChannel().getAsMention(),
                                getFormattedTime.apply(BotUtils.getIdCreationTime(messageId)),
                                FormatUtils.formatReadableTime(elapsedSeconds, false, "s"))
                );
    }

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
        // Do not keep messages of guilds that does not have a server log channel set
        if (!this.serverLogRepository.exists(() -> event.getGuild().getIdLong())) {
            return;
        }
        messageCache.add(event.getMessageIdLong(),
                new MessageCache(event.getMessage().getContentRaw(), event.getAuthor().getIdLong()));
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

        eb = handle(event.getClass(), event, eb);
        if (eb == null) {
            return;
        }
        logChannel.sendMessage(eb.build()).queue();
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

        eb = handle(event.getClass(), event, eb);
        if (eb == null) {
            return;
        }
        logChannel.sendMessage(eb.build()).queue();
    }

    // User Events
    @Override
    public void onGenericUser(GenericUserEvent event) {
        // Base embed message
        String nameWithDiscriminator = event.getUser().getName() + "#" + event.getUser().getDiscriminator();
        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(nameWithDiscriminator, null, event.getUser().getEffectiveAvatarUrl());

        eb = handle(event.getClass(), event, eb);
        if (eb == null) {
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

        EmbedBuilder eb = handleGuildEvent(
                event.getClass(), event, new EmbedBuilder(),
                time -> getFormattedTime(guildId, log.getChannelId(), time),
                this.shardManager::getUserById
        );
        if (eb == null) {
            return;
        }
        // Append formatted date to footer
        MessageEmbed me = eb.build();
        MessageEmbed.Footer footer = me.getFooter();
        String footerText;
        if (footer != null && footer.getText() != null) {
            footerText = footer.getText() + " | " + formattedDate;
        } else {
            footerText = formattedDate;
        }
        eb.setFooter(footerText);

        logChannel.sendMessage(eb.build()).queue();
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

        eb = handle(event.getClass(), event, eb);
        if (eb == null) {
            return;
        }

        logChannel.sendMessage(eb.build()).queue();
    }
}

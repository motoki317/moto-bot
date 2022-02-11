package commands.guild;

import commands.event.CommandEvent;
import commands.event.message.ButtonClickEventAdapter;
import commands.event.message.SentMessage;
import db.model.guild.Guild;
import db.repository.base.GuildRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import update.button.ButtonClickHandler;
import update.button.ButtonClickManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class GuildNameResolver {
    private final GuildRepository guildRepository;
    private final ButtonClickManager buttonClickManager;

    public GuildNameResolver(GuildRepository guildRepository, ButtonClickManager buttonClickManager) {
        this.guildRepository = guildRepository;
        this.buttonClickManager = buttonClickManager;
    }

    public interface GuildNameResolvedHandler {
        void onResolve(SentMessage next, @NotNull String guildName, @Nullable String prefix);
    }

    /**
     * Resolves guild name from user input.
     *
     * @param guildName Guild name, or possibly guild prefix.
     * @param event     Command event.
     * @param onResolve On resolve, guild name and guild prefix are given.
     */
    public void resolve(@NotNull String guildName,
                        CommandEvent event,
                        GuildNameResolvedHandler onResolve) {
        event.reply(new EmbedBuilder().setDescription("Processing...").build(), s ->
                resolveProcessCallback(event, s, guildName, onResolve));
    }

    private void resolveProcessCallback(CommandEvent event, SentMessage s, String guildName, GuildNameResolvedHandler onResolve) {
        List<Guild> guilds = findGuilds(guildName);
        if (guilds == null) {
            s.editError(event.getAuthor(), "Something went wrong while retrieving guilds data...");
            return;
        }

        String resolvedName;
        String guildPrefix;
        if (guilds.size() == 0) {
            // Unknown guild, but pass it on as prefix unknown in case the bot haven't loaded the guild data yet
            resolvedName = guildName;
            guildPrefix = null;
        } else if (guilds.size() == 1) {
            resolvedName = guilds.get(0).getName();
            guildPrefix = guilds.get(0).getPrefix();
        } else {
            // Choose a guild
            s.getMessage(m -> {
                GuildNameSelectionHandler handler = new GuildNameSelectionHandler(
                        m.getIdLong(), event, guilds, onResolve);
                s.editMessage(handler.getPage(0), next ->
                        buttonClickManager.addEventListener(handler));
            });
            return;
        }

        onResolve.onResolve(s, resolvedName, guildPrefix);
    }

    private static class GuildNameSelectionHandler extends ButtonClickHandler {
        private static final String BUTTON_ID_LEFT_PAGE = "left_page";
        private static final String BUTTON_ID_RIGHT_PAGE = "right_page";
        private static final String BUTTON_ID_CANCEL = "cancel";

        private static final int GUILDS_PER_PAGE = 10;

        private static int getSelectUIMaxPage(int trackCount) {
            return (trackCount - 1) / GUILDS_PER_PAGE;
        }

        private static MessageEmbed getSelectUI(CommandEvent event, List<Guild> guilds, int page) {
            String description = IntStream.range(page * GUILDS_PER_PAGE, Math.min((page + 1) * GUILDS_PER_PAGE, guilds.size()))
                    .mapToObj(i -> {
                        Guild g = guilds.get(i);
                        return String.format("%s. %s [%s]",
                                i + 1,
                                g.getName(),
                                g.getPrefix() != null ? g.getPrefix() : "???");
                    })
                    .collect(Collectors.joining("\n"));

            return new EmbedBuilder()
                    .setAuthor(String.format("%s, Select a guild.", event.getAuthor().getName()),
                            null, event.getAuthor().getEffectiveAvatarUrl())
                    .setDescription(description)
                    .setFooter(String.format("Page [ %d / %d ]", page + 1, getSelectUIMaxPage(guilds.size()) + 1))
                    .build();
        }

        private static List<ActionRow> getLayout(int guildCount, int page) {
            List<ActionRow> layouts = new ArrayList<>();
            layouts.add(ActionRow.of(IntStream
                    .range(page * GUILDS_PER_PAGE, Math.min(guildCount, page * GUILDS_PER_PAGE + 5))
                    .mapToObj(i -> Button.primary("guild-" + i, String.valueOf(i + 1)))
                    .collect(Collectors.toCollection(ArrayList::new))));
            if (guildCount > page * GUILDS_PER_PAGE + 5) {
                layouts.add(ActionRow.of(IntStream
                        .range(page * GUILDS_PER_PAGE + 5, Math.min(guildCount, (page + 1) * GUILDS_PER_PAGE))
                        .mapToObj(i -> Button.primary("guild-" + i, String.valueOf(i + 1)))
                        .collect(Collectors.toCollection(ArrayList::new))));
            }
            if (getSelectUIMaxPage(guildCount) == 0) {
                layouts.add(ActionRow.of(
                        Button.danger(BUTTON_ID_CANCEL, "Cancel")
                ));
            } else {
                layouts.add(ActionRow.of(
                        Button.primary(BUTTON_ID_LEFT_PAGE, Emoji.fromUnicode("\u2B05")),
                        Button.primary(BUTTON_ID_RIGHT_PAGE, Emoji.fromUnicode("\u27A1")),
                        Button.danger(BUTTON_ID_CANCEL, "Cancel")
                ));
            }
            return layouts;
        }

        private final List<Guild> guilds;
        private final CommandEvent cEvent;
        private final GuildNameResolvedHandler onResolve;
        private int page;

        public GuildNameSelectionHandler(long messageId, CommandEvent cEvent, List<Guild> guilds, GuildNameResolvedHandler onResolve) {
            super(messageId, (event) -> false, () -> {
            });

            this.cEvent = cEvent;
            this.guilds = guilds;
            this.onResolve = onResolve;
        }

        public Message getPage(int page) {
            return new MessageBuilder()
                    .setEmbeds(getSelectUI(this.cEvent, this.guilds, page))
                    .setActionRows(getLayout(this.guilds.size(), page))
                    .build();
        }

        @Override
        public boolean handle(ButtonClickEvent event) {
            super.handle(event);

            if (event.getButton() == null || event.getButton().getId() == null) return false;

            String buttonId = event.getButton().getId();
            switch (buttonId) {
                case BUTTON_ID_LEFT_PAGE -> {
                    int mod = getSelectUIMaxPage(this.guilds.size()) + 1;
                    int nextPage = (this.page - 1 + mod) % mod;
                    event.editMessage(this.getPage(nextPage)).queue();
                    this.page = nextPage;
                    return false;
                }
                case BUTTON_ID_RIGHT_PAGE -> {
                    int mod = getSelectUIMaxPage(this.guilds.size()) + 1;
                    int nextPage = (this.page + 1) % mod;
                    event.editMessage(this.getPage(nextPage)).queue();
                    this.page = nextPage;
                    return false;
                }
                case BUTTON_ID_CANCEL -> event.editMessage(new MessageBuilder().setEmbeds(new EmbedBuilder().setDescription("Cancelled.").build()).build())
                        .queue(m -> m.deleteOriginal().queueAfter(3, TimeUnit.SECONDS));
                default -> { // "guild-i"
                    if (!buttonId.startsWith("guild-")) {
                        return false;
                    }
                    int guildId = Integer.parseInt(buttonId.substring("guild-".length()));
                    Guild guild = this.guilds.get(guildId);
                    this.onResolve.onResolve(new ButtonClickEventAdapter(event), guild.getName(), guild.getPrefix());
                }
            }

            return true;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            // We don't have to destroy the buttons here; we'll update it in the next action
        }
    }

    /**
     * Finds guild with specified name (both full name or prefix was possibly specified).
     *
     * @param specified Specified name.
     * @return List of found guilds. null if something went wrong.
     */
    @Nullable
    List<Guild> findGuilds(@NotNull String specified) {
        // Case-sensitive full name search
        List<Guild> ret;
        Guild guild = this.guildRepository.findOne(() -> specified);
        if (guild != null) {
            ret = new ArrayList<>();
            ret.add(guild);
            return ret;
        }

        // Case-insensitive full name search
        ret = this.guildRepository.findAllCaseInsensitive(specified);
        if (ret == null) {
            return null;
        }
        if (ret.size() > 0) {
            return ret;
        }

        // Prefix search
        if (specified.length() == 3 || specified.length() == 4) {
            // Case-sensitive prefix search
            ret = this.guildRepository.findAllByPrefix(specified);
            if (ret == null) {
                return null;
            }
            if (ret.size() > 0) {
                return ret;
            }

            // Case-insensitive prefix search
            ret = this.guildRepository.findAllByPrefixCaseInsensitive(specified);
            if (ret == null) {
                return null;
            }
            if (ret.size() > 0) {
                return ret;
            }
        }

        return ret;
    }
}

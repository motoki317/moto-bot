package commands;

import api.wynn.WynnApi;
import api.wynn.structs.ForumId;
import api.wynn.structs.Player;
import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import db.model.dateFormat.CustomDateFormat;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.FormatUtils;
import utils.InputChecker;
import utils.MinecraftColor;
import utils.UUID;
import utils.rateLimit.RateLimitException;

import java.awt.*;
import java.text.DateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlayerStats extends GenericCommand {
    private final WynnApi wynnApi;

    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;

    public PlayerStats(Bot bot) {
        this.wynnApi = new WynnApi(bot.getLogger());
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"stats", "playerStats", "pStats"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"stats"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "name-or-uuid", "Name or UUID of player", true)
        };
    }

    @Override
    public @NotNull String syntax() {
        return "stats <player name|uuid>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows a player's general information.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Player Stats Command Help")
                        .setDescription(this.shortHelp())
                        .addField(
                                "Syntax",
                                "`" + this.syntax() + "`",
                                false
                        )
                        .build()
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(3);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            event.reply(this.longHelp());
            return;
        }

        String specified = args[1];
        if (!UUID.isUUID(specified) && !InputChecker.isValidMinecraftUsername(specified)) {
            event.reply(String.format("Given name `%s` doesn't seem to be a valid Minecraft username or a UUID...",
                    specified));
            return;
        }

        if (UUID.isUUID(specified)) {
            // Wynncraft API only accepts UUIDs with hyphens
            specified = new UUID(specified).toStringWithHyphens();
        }

        Player player;
        try {
            player = this.wynnApi.getPlayerStats(specified, false);
        } catch (RateLimitException e) {
            event.replyException(e.getMessage());
            return;
        }
        if (player == null) {
            event.replyException(String.format("Failed to retrieve player statistics for `%s`.", specified));
            return;
        }

        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        event.reply(format(player, customDateFormat, customTimeZone));
    }

    @NotNull
    private Message format(@NotNull Player player,
                           @NotNull CustomDateFormat customDateFormat,
                           @NotNull CustomTimeZone customTimeZone) {
        DateFormat dateFormat = customDateFormat.getDateFormat().getMinuteFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());
        String formattedTimeZone = customTimeZone.getFormattedTime();

        List<String> ret = new ArrayList<>();

        ret.add("```ml");
        ret.add("---- Player Stats ----");
        ret.add("");
        ret.add("Name: " + player.getUsername());
        ret.add(String.format(
                "Joined: %s (%s)",
                dateFormat.format(new Date(player.getMetaInfo().getFirstJoin())),
                formattedTimeZone
        ));
        ret.add("Rank: " + getFormattedRank(player));
        if (player.getGuildInfo().getName() != null) {
            ret.add(String.format(
                    "Guild: %s (%s)",
                    player.getGuildInfo().getName(),
                    player.getGuildInfo().getRank()
            ));
        }

        ret.add("");

        String world = this.wynnApi.findPlayer(player.getUsername());
        if (world == null) {
            ret.add(String.format(
                    "Last Seen: %s (%s)",
                    dateFormat.format(new Date(player.getMetaInfo().getLastJoin())),
                    formattedTimeZone
            ));
        } else {
            ret.add("Current World: " + world);
        }

        ret.add("");

        ret.add("---- Statistics ----");

        ret.add("");

        class Display {
            private final String left;
            private final String right;

            private Display(String left, String right) {
                this.left = left;
                this.right = right;
            }

            private Display(String left, int right) {
                this.left = left;
                this.right = String.valueOf(right);
            }
        }

        List<Display> displays = new ArrayList<>();
        displays.add(
                new Display("Total Level", player.getGlobalInfo().getTotalLevelCombined())
        );
        displays.add(
                new Display("Combat Total", player.getGlobalInfo().getTotalLevelCombat())
        );
        displays.add(
                new Display("Professions Total", player.getGlobalInfo().getTotalLevelProfession())
        );
        displays.add(
                new Display("Playtime", FormatUtils.formatReadableTime(
                        player.getMetaInfo().getPlaytime() * 60L, false, "m"
                ))
        );
        displays.add(
                new Display("Mobs Killed", String.format("%,d", player.getGlobalInfo().getMobsKilled()))
        );
        displays.add(
                new Display("Chests Looted", String.format("%,d", player.getGlobalInfo().getChestsFound()))
        );
        displays.add(
                new Display("Deaths", String.format("%,d", player.getGlobalInfo().getDeaths()))
        );
        displays.add(
                new Display("Logins", String.format("%,d", player.getGlobalInfo().getLogins()))
        );
        int pvpKills = player.getGlobalInfo().getPvpKills();
        int pvpDeaths = player.getGlobalInfo().getPvpDeaths();
        displays.add(
                new Display("PvP", String.format(
                        "%,d kills / %,d deaths (%.2f)",
                        pvpKills,
                        pvpDeaths,
                        (double) pvpKills / (double) pvpDeaths
                ))
        );

        int justifyLength = displays.stream().mapToInt(d -> d.left.length() + 2 + d.right.length())
                .max().orElse(2);
        for (Display display : displays) {
            ret.add(String.format(
                    "%s: %s%s",
                    display.left,
                    nSpaces(justifyLength - display.left.length() - display.right.length() - 2),
                    display.right
            ));
        }

        ret.add("```");

        // Start building embed (at the bottom of response)
        EmbedBuilder eb = new EmbedBuilder();

        List<String> desc = new ArrayList<>();
        String headAvatarUrl = "https://minotar.net/helm/" + player.getUsername();
        eb.setAuthor(player.getUsername(), null, headAvatarUrl);

        // Add official stats page in description
        String officialStatsUrl = "https://wynncraft.com/stats/player/" + player.getUsername();
        desc.add("[Official Stats Page](" + officialStatsUrl + ")");

        // Retrieve forum id if possible
        ForumId forumId = null;
        try {
            forumId = this.wynnApi.getForumId(player.getUsername());
        } catch (RateLimitException ignored) {
        }
        if (forumId != null) {
            String username = forumId.getUsername();
            String forumPersonalPageUrl = "https://forums.wynncraft.com/members/" + forumId.getId() + "/";
            desc.add("");
            desc.add(String.format("[Forum Page](%s) (%s)", forumPersonalPageUrl, username));
        }

        // Tag (purchase-able rank) color
        String tag = player.getMetaInfo().getTag();
        if (tag != null) {
            Color tagColor = getTagColor(tag);
            if (tagColor != null) {
                eb.setColor(tagColor);
            }
        }
        // Override tag (purchase-able) color if player has special ranks
        String rank = player.getRank();
        if (!rank.equals("Player")) {
            Color rankColor = getRankColor(rank);
            if (rankColor != null) {
                eb.setColor(rankColor);
            }
        }

        String wynnIconUrl = "https://cdn.wynncraft.com/img/ico/favicon-96x96.png";
        eb.setFooter("Player Statistics Last Updated", wynnIconUrl);
        eb.setTimestamp(Instant.ofEpochMilli(player.getRequest().getTimestamp()));

        eb.setDescription(String.join("\n", desc));
        return new MessageBuilder()
                .setContent(String.join("\n", ret))
                .setEmbeds(eb.build())
                .build();
    }

    private static String getFormattedRank(Player player) {
        @Nullable
        String donorRank = player.getMetaInfo().getTag();
        // Default = "Player"
        // Ranks such as Builder, Moderator, Hybrid etc.
        String specialRank = player.getRank();
        if (donorRank == null) {
            return specialRank;
        }
        if ("Player".equals(specialRank)) {
            return donorRank;
        }
        return donorRank + ", " + specialRank;
    }

    @Nullable
    private static Color getTagColor(@NotNull String tag) {
        return switch (tag) {
            case "VIP" -> MinecraftColor.DARK_GREEN.getColor();
            case "VIP+" -> MinecraftColor.AQUA.getColor();
            case "HERO" -> MinecraftColor.DARK_PURPLE.getColor();
            case "CHAMPION" -> MinecraftColor.YELLOW.getColor();
            default -> null;
        };
    }

    @Nullable
    private static Color getRankColor(@NotNull String rank) {
        return switch (rank) {
            case "Moderator" -> MinecraftColor.GOLD.getColor();
            case "Administrator", "Developer", "WebDev" -> MinecraftColor.DARK_RED.getColor();
            case "Game Master", "Item", "Music", "Builder", "CMD", "Script", "Modeler" -> MinecraftColor.DARK_AQUA.getColor();
            default -> null;
        };
    }

    private static String nSpaces(int n) {
        return String.join("", Collections.nCopies(n, " "));
    }
}

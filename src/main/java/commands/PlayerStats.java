package commands;

import api.wynn.WynnApi;
import api.wynn.structs.ForumId;
import api.wynn.structs.Player;
import app.Bot;
import commands.base.GenericCommand;
import db.model.dateFormat.CustomDateFormat;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.FormatUtils;
import utils.InputChecker;
import utils.MinecraftColor;
import utils.rateLimit.RateLimitException;

import java.awt.*;
import java.text.DateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PlayerStats extends GenericCommand {
    private final WynnApi wynnApi;

    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;

    public PlayerStats(Bot bot) {
        this.wynnApi = new WynnApi(bot.getLogger(), bot.getProperties().wynnTimeZone);
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"stats", "playerStats", "pStats"}};
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
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(event, this.longHelp());
            return;
        }

        String specified = args[1];
        if (!InputChecker.isValidMinecraftUsername(specified)) {
            respond(event, String.format("Given player name `%s` doesn't seem to be a valid minecraft username...",
                            specified));
            return;
        }

        Player player;
        try {
            player = this.wynnApi.getPlayerStats(specified, false);
        } catch (RateLimitException e) {
            respondException(event, e.getMessage());
            return;
        }
        if (player == null) {
            respondException(event, String.format("Failed to retrieve player statistics for `%s`.", specified));
            return;
        }

        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        respond(event, format(player, customDateFormat, customTimeZone));
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
        ret.add(
                String.format("Joined: %s (%s)",
                        dateFormat.format(new Date(player.getMetaInfo().getFirstJoin())),
                        formattedTimeZone
                )
        );
        ret.add("Rank: " + player.getRank());
        if (player.getGuildInfo().getName() != null) {
            ret.add(
                    String.format("Guild: %s (%s)",
                            player.getGuildInfo().getName(),
                            player.getGuildInfo().getRank()
                    )
            );
        }

        ret.add("");

        String world = this.wynnApi.mustFindPlayer(player.getUsername());
        if (world == null) {
            ret.add(
                    String.format(
                        "Last Seen: %s (%s)",
                        dateFormat.format(new Date(player.getMetaInfo().getLastJoin())),
                        formattedTimeZone
                    )
            );
        } else {
            ret.add("Current World: " + world);
        }

        ret.add("");

        ret.add("---- Statistics ----");

        ret.add("");

        class Display {
            private String left;
            private String right;

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
                        player.getMetaInfo().getPlaytime() * 60, false, "m"
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
                new Display(
                        "PvP",
                        String.format("%,d kills / %,d deaths (%.2f)",
                                pvpKills,
                                pvpDeaths,
                                (double) pvpKills / (double) pvpDeaths
                        )
                )
        );

        int justifyLength = displays.stream().mapToInt(d -> d.left.length() + 2 + d.right.length())
                .max().orElse(2);
        for (Display display : displays) {
            ret.add(
                    String.format("%s: %s%s",
                            display.left,
                            nSpaces(justifyLength - display.left.length() - display.right.length() - 2),
                            display.right
                    )
            );
        }

        ret.add("```");

        EmbedBuilder eb = new EmbedBuilder();

        String officialStatsUrl = "https://wynncraft.com/stats/player/";
        String headAvatarUrl = "https://minotar.net/avatar/";
        eb.setAuthor("Official Stats Page",
                officialStatsUrl + player.getUsername(),
                headAvatarUrl + player.getUsername());

        // Retrieve forum id if possible
        ForumId forumId = null;
        try {
            forumId = this.wynnApi.getForumId(player.getUsername());
        } catch (RateLimitException ignored) {
        }

        if (forumId != null) {
            String username = forumId.getUsername();
            int id = forumId.getId();
            String forumPersonalPageUrl = "https://forums.wynncraft.com/members/";
            eb.setTitle("Forum Page", forumPersonalPageUrl + id + "/");
            eb.setDescription("Forum Name: " + username);
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

        return new MessageBuilder()
                .setContent(String.join("\n", ret))
                .setEmbed(eb.build())
                .build();
    }

    @Nullable
    private static Color getTagColor(@NotNull String tag) {
        switch (tag) {
            case "VIP":
                return new Color(0, 170, 0);
            case "VIP+":
                return new Color(85, 225, 225);
            case "HERO":
                return new Color(170, 0, 170);
        }
        return null;
    }

    @Nullable
    private static Color getRankColor(@NotNull String rank) {
        switch (rank) {
            case "Moderator":
                return new Color(255, 170, 0);
            case "Administrator":
            case "Developer":
            case "WebDev":
                return new Color(170, 0, 0);
            case "Game Master":
            case "Item":
            case "Music":
            case "Builder":
            case "CMD":
            case "Script":
            case "Modeler":
                return new Color(0, 170, 170);
        }
        return null;
    }

    private static String nSpaces(int n) {
        return String.join("", Collections.nCopies(n, " "));
    }
}

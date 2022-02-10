package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import db.model.dateFormat.CustomDateFormat;
import db.model.guildLeaderboard.GuildLeaderboard;
import db.model.guildList.GuildListEntry;
import db.model.guildXpLeaderboard.GuildXpLeaderboard;
import db.model.timezone.CustomTimeZone;
import db.repository.base.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.ArgumentParser;
import utils.FormatUtils;
import utils.TableFormatter;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static utils.TableFormatter.Justify.Left;
import static utils.TableFormatter.Justify.Right;

public class GuildLevelRank extends GenericCommand {
    private final GuildLeaderboardRepository guildLeaderboardRepository;
    private final GuildXpLeaderboardRepository guildXpLeaderboardRepository;
    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;
    private final GuildListRepository guildListRepository;

    public GuildLevelRank(Bot bot) {
        this.guildLeaderboardRepository = bot.getDatabase().getGuildLeaderboardRepository();
        this.guildXpLeaderboardRepository = bot.getDatabase().getGuildXpLeaderboardRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.guildListRepository = bot.getDatabase().getGuildListRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"levelRank", "lRank", "lr"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"g", "levelrank"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{};
    }

    @Override
    public @NotNull String syntax() {
        return "g levelRank [-cl <list name>]";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Guilds ranking by level and XP.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Guild Level Rank command help")
                        .setDescription("Shows guilds ranking by their level and current XP.\n" +
                                "Append your custom guild list name to arguments to view the rank of guilds in the list.")
                        .addField("Syntax", this.syntax(), false)
                        .addField("Why is it that only about 90 guilds are displayed?",
                                "The bot is using leaderboard retrieved from the Wynncraft API to make level rank " +
                                        "leaderboard. Since leaderboard retrieved from the API is sorted by territory " +
                                        "number first and then levels, discarding all entries below the lowest level guild " +
                                        "with 0 territories will make an accurate level rank leaderboard. This is why " +
                                        "a few guilds from the original leaderboard (100 top guilds) are not displayed. " +
                                        "Those discarded guilds are guilds with much lower levels and have a few " +
                                        "territories to be displayed on the original leaderboard.",
                                false)
                        .build()
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    /**
     * Parses the command args and returns non-null list name if custom guild list was specified.
     *
     * @param args Command args.
     * @return List name if specified.
     */
    @Nullable
    private static String parseListName(String[] args) {
        ArgumentParser parser = new ArgumentParser(args);
        return parser.getArgumentMap().getOrDefault("cl", null);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        List<GuildLeaderboard> lb = this.guildLeaderboardRepository.getLatestLeaderboard();
        List<GuildXpLeaderboard> xpGained = this.guildXpLeaderboardRepository.findAll();
        if (lb == null || xpGained == null) {
            event.replyError("Something went wrong while retrieving data...");
            return;
        }

        Map<String, GuildXpLeaderboard> xpGainedMap = xpGained.stream()
                .collect(Collectors.toMap(GuildXpLeaderboard::getName, g -> g));

        String listName = parseListName(args);
        List<GuildListEntry> list = listName != null
                ? this.guildListRepository.getList(event.getAuthor().getIdLong(), listName)
                : null;
        trimAndSortLB(lb, list);

        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        Date from = this.guildLeaderboardRepository.getOldestDate();
        Date to = this.guildLeaderboardRepository.getNewestDate();
        if (from == null || to == null) {
            event.replyError("Something went wrong while retrieving data...");
            return;
        }

        String lbDuration = FormatUtils.formatReadableTime(
                (to.getTime() - from.getTime()) / 1000L, false, "s"
        );
        String totalXPGained = FormatUtils.truncateNumber(BigDecimal.valueOf(
                xpGainedMap.values().stream().mapToLong(GuildXpLeaderboard::getXpDiff).sum()
        ));
        int totalTerritories = lb.stream().mapToInt(GuildLeaderboard::getTerritories).sum();

        LBDisplay lbDisplay = new LBDisplay(
                maxPage(lb), lbDuration, totalXPGained,
                String.valueOf(totalTerritories), to
        );

        Function<Integer, Message> pages = page -> {
            List<Display> displays = getDisplays(page, lb, xpGainedMap);
            return new MessageBuilder(formatDisplays(page, displays, customDateFormat, customTimeZone, lbDisplay)).build();
        };

        if (maxPage(lb) == 0) {
            event.reply(pages.apply(0));
            return;
        }

        event.replyMultiPage(pages.apply(0), pages, () -> maxPage(lb));
    }

    private static final int GUILDS_PER_PAGE = 20;

    private static int maxPage(List<GuildLeaderboard> lb) {
        return (lb.size() - 1) / GUILDS_PER_PAGE;
    }

    /**
     * Trims and sorts leaderboard to make level rank leaderboard.
     * Since leaderboard retrieved from the API is sorted by territory number first and then levels,
     * discarding all entries below the lowest level guild with 0 territories will make an accurate level rank leaderboard.
     *
     * @param lb   Original leaderboard retrieved from the Wynncraft API.
     * @param list If specified, custom guild list of the user.
     */
    private static void trimAndSortLB(List<GuildLeaderboard> lb, @Nullable List<GuildListEntry> list) {
        GuildLeaderboard threshold = lb.stream().filter(g -> g.getTerritories() == 0)
                .min(GuildLeaderboard::compareLevelAndXP)
                .orElse(null);

        if (list != null) {
            Set<String> guildNames = list.stream().map(GuildListEntry::getGuildName).collect(Collectors.toSet());
            lb.removeIf(g -> !guildNames.contains(g.getName()));
        }

        if (threshold == null) {
            return;
        }
        lb.removeIf(g -> g.compareLevelAndXP(threshold) < 0);
        lb.sort((g1, g2) -> g2.compareLevelAndXP(g1));
    }

    private record Display(String num, String guildName, String lv, String xp,
                           String gainedXp, String territory) {
    }

    private static List<Display> getDisplays(int page, List<GuildLeaderboard> lb, Map<String, GuildXpLeaderboard> xpGainedMap) {
        int start = page * GUILDS_PER_PAGE;
        int end = Math.min((page + 1) * GUILDS_PER_PAGE, lb.size());

        List<Display> displays = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            GuildLeaderboard g = lb.get(i);
            displays.add(new Display(
                    (i + 1) + ".",
                    String.format("[%s] %s", g.getPrefix(), g.getName()),
                    String.valueOf(g.getLevel()),
                    FormatUtils.truncateNumber(BigDecimal.valueOf(g.getXp())),
                    FormatUtils.truncateNumber(
                            BigDecimal.valueOf(xpGainedMap.containsKey(g.getName())
                                    ? xpGainedMap.get(g.getName()).getXpDiff() : 0L)
                    ),
                    String.valueOf(g.getTerritories())
            ));
        }

        return displays;
    }

    private record LBDisplay(int maxPage, String lbDuration, String totalXPGained,
                             String totalTerritories, Date lastUpdate) {
    }

    private static String formatDisplays(int page, List<Display> displays,
                                         CustomDateFormat customDateFormat, CustomTimeZone customTimeZone,
                                         LBDisplay lbDisplay) {
        StringBuilder sb = new StringBuilder();
        sb.append("```ml\n");
        sb.append("---- Guild Level Rank ----\n");
        sb.append("\n");

        TableFormatter tf = new TableFormatter(false);
        tf.addColumn("", Left, Left)
                .addColumn("Name", Left, Left)
                .addColumn("Lv", Left, Right)
                .addColumn("XP", Left, Right)
                .addColumn("Gained", lbDisplay.totalXPGained, Left, Right)
                .addColumn("Territory", lbDisplay.totalTerritories, Left, Left);
        for (Display d : displays) {
            tf.addRow(d.num, d.guildName, d.lv, d.xp, d.gainedXp, d.territory);
        }
        tf.toString(sb);
        tf.addSeparator(sb);

        String pageView = String.format("< page %s / %s >", page + 1, lbDisplay.maxPage + 1);
        sb.append(String.format("%s%sTotal | %s | %s\n",
                pageView,
                nSpaces(tf.widthAt(0) + 3 + tf.widthAt(1) + 3 + tf.widthAt(2) + 3 + tf.widthAt(3) - pageView.length() - "Total".length()),
                lbDisplay.totalXPGained,
                lbDisplay.totalTerritories));
        sb.append("\n");

        DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());
        sb.append(String.format("  xp gained: %s\n", lbDisplay.lbDuration));
        // in seconds
        long lastUpdateDiff = (new Date().getTime() - lbDisplay.lastUpdate.getTime()) / 1000L;
        sb.append(String.format("last update: %s (%s), %s ago\n", dateFormat.format(lbDisplay.lastUpdate),
                customTimeZone.getFormattedTime(), FormatUtils.formatReadableTime(lastUpdateDiff, false, "s")));

        sb.append("```");

        return sb.toString();
    }

    private static String nSpaces(int n) {
        return String.join("", Collections.nCopies(n, " "));
    }
}

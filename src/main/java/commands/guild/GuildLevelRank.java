package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import db.model.dateFormat.CustomDateFormat;
import db.model.guildLeaderboard.GuildLeaderboard;
import db.model.guildXpLeaderboard.GuildXpLeaderboard;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
import db.repository.base.GuildLeaderboardRepository;
import db.repository.base.GuildXpLeaderboardRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import update.multipage.MultipageHandler;
import update.reaction.ReactionManager;
import utils.FormatUtils;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class GuildLevelRank extends GenericCommand {
    private final ReactionManager reactionManager;
    private final GuildLeaderboardRepository guildLeaderboardRepository;
    private final GuildXpLeaderboardRepository guildXpLeaderboardRepository;
    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;

    public GuildLevelRank(Bot bot) {
        this.reactionManager = bot.getReactionManager();
        this.guildLeaderboardRepository = bot.getDatabase().getGuildLeaderboardRepository();
        this.guildXpLeaderboardRepository = bot.getDatabase().getGuildXpLeaderboardRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"levelRank", "lRank", "lr"}};
    }

    @Override
    public @NotNull String syntax() {
        return "g levelrank";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Guilds ranking by level and XP.";
        // TODO: Append your custom guild list name to arguments to view the rank of guilds in the list.
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Guild Level Rank command help")
                        .setDescription("Shows guilds ranking by their level and current XP.")
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
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        List<GuildLeaderboard> lb = this.guildLeaderboardRepository.getLatestLeaderboard();
        List<GuildXpLeaderboard> xpGained = this.guildXpLeaderboardRepository.findAll();
        if (lb == null || xpGained == null) {
            respondError(event, "Something went wrong while retrieving data...");
            return;
        }

        Map<String, GuildXpLeaderboard> xpGainedMap = xpGained.stream()
                .collect(Collectors.toMap(GuildXpLeaderboard::getName, g -> g));

        trimAndSortLB(lb);

        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        if (maxPage(lb) == 0) {
            respond(event, format(0, lb, xpGainedMap, customDateFormat, customTimeZone));
        }

        respond(event, format(0, lb, xpGainedMap, customDateFormat, customTimeZone), message -> {
            MultipageHandler handler = new MultipageHandler(
                    message,
                    event.getAuthor().getIdLong(),
                    page -> new MessageBuilder(format(page, lb, xpGainedMap, customDateFormat, customTimeZone)).build(),
                    () -> maxPage(lb)
            );
            this.reactionManager.addEventListener(handler);
        });
    }

    private static final int GUILDS_PER_PAGE = 20;

    private static int maxPage(List<GuildLeaderboard> lb) {
        return (lb.size() - 1) / GUILDS_PER_PAGE;
    }

    /**
     * Trims and sorts leaderboard to make level rank leaderboard.
     * Since leaderboard retrieved from the API is sorted by territory number first and then levels,
     * discarding all entries below the lowest level guild with 0 territories will make an accurate level rank leaderboard.
     * @param lb Original leaderboard retrieved from the Wynncraft API.
     */
    private static void trimAndSortLB(List<GuildLeaderboard> lb) {
        GuildLeaderboard threshold = lb.stream().filter(g -> g.getTerritories() == 0)
                .min(GuildLeaderboard::compareLevelAndXP)
                .orElse(null);
        if (threshold == null) {
            return;
        }
        lb.removeIf(g -> g.compareLevelAndXP(threshold) < 0);
        lb.sort((g1, g2) -> g2.compareLevelAndXP(g1));
    }

    private static class Display {
        private String num;
        private String guildName;
        private String lv;
        private String xp;
        private String gainedXp;
        private String territory;

        private Display(String num, String guildName, String lv, String xp, String gainedXp, String territory) {
            this.num = num;
            this.guildName = guildName;
            this.lv = lv;
            this.xp = xp;
            this.gainedXp = gainedXp;
            this.territory = territory;
        }
    }

    private static String format(int page, List<GuildLeaderboard> lb, Map<String, GuildXpLeaderboard> xpGainedMap,
                          CustomDateFormat customDateFormat, CustomTimeZone customTimeZone) {
        int start = page * GUILDS_PER_PAGE;
        int end = Math.min((page + 1) * GUILDS_PER_PAGE, lb.size());

        List<Display> displays = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            GuildLeaderboard g = lb.get(i);
            displays.add(new Display(
                    String.valueOf(i + 1),
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

        GuildXpLeaderboard xp = xpGainedMap.values().stream().findAny().orElse(null);
        String lbDuration = FormatUtils.formatReadableTime(
                xp != null ? (xp.getTo().getTime() - xp.getFrom().getTime()) / 1000L : 0L,
                false, "s"
        );
        String totalXPGained = FormatUtils.truncateNumber(BigDecimal.valueOf(
                xpGainedMap.values().stream().mapToLong(GuildXpLeaderboard::getXpDiff).sum()
        ));
        int totalTerritories = lb.stream().mapToInt(GuildLeaderboard::getTerritories).sum();

        LBDisplay lbDisplay = new LBDisplay(
                page, maxPage(lb), lbDuration, totalXPGained,
                String.valueOf(totalTerritories),
                xp != null ? xp.getTo() : new Date()
        );

        return formatDisplays(displays, customDateFormat, customTimeZone, lbDisplay);
    }

    private static class LBDisplay {
        private int page;
        private int maxPage;
        private String lbDuration;
        private String totalXPGained;
        private String totalTerritories;
        private Date lastUpdate;

        private LBDisplay(int page, int maxPage, String lbDuration, String totalXPGained, String totalTerritories, Date lastUpdate) {
            this.page = page;
            this.maxPage = maxPage;
            this.lbDuration = lbDuration;
            this.totalXPGained = totalXPGained;
            this.totalTerritories = totalTerritories;
            this.lastUpdate = lastUpdate;
        }
    }

    private static String formatDisplays(List<Display> displays,
                                         CustomDateFormat customDateFormat, CustomTimeZone customTimeZone,
                                         LBDisplay lbDisplay) {
        int numJustify = displays.stream().mapToInt(d -> d.num.length()).max().orElse(1);
        int nameJustify = displays.stream().mapToInt(d -> d.guildName.length()).max().orElse(6);
        int lvJustify = displays.stream().mapToInt(d -> d.lv.length()).max().orElse(2);
        int xpJustify = displays.stream().mapToInt(d -> d.xp.length()).max().orElse(6);
        int gainedJustify = "Gained".length();
        int territoryJustify = displays.stream().mapToInt(d -> d.territory.length()).max().orElse(1);

        List<String> ret = new ArrayList<>();

        ret.add("```ml");
        ret.add("---- Guild Level Rank ----");
        ret.add("");
        ret.add(String.format("%s  Name%s | Lv%s | XP%s | Gained | Territory",
                nCopies(" ", numJustify), nCopies(" ", nameJustify - 4),
                nCopies(" ", lvJustify - 2), nCopies(" ", xpJustify - 2)));
        ret.add(String.format("%s--%s-+-%s-+-%s-+-%s-+-%s-",
                nCopies("-", numJustify), nCopies("-", nameJustify),
                nCopies("-", lvJustify), nCopies("-", xpJustify),
                nCopies("-", gainedJustify), nCopies("-", "Territory".length())));

        for (Display d : displays) {
            ret.add(String.format("%s.%s %s%s | %s%s | %s%s | %s%s | %s%s",
                    d.num, nCopies(" ", numJustify - d.num.length()),
                    d.guildName, nCopies(" ", nameJustify - d.guildName.length()),
                    nCopies(" ", lvJustify - d.lv.length()), d.lv,
                    nCopies(" ", xpJustify - d.xp.length()), d.xp,
                    nCopies(" ", gainedJustify - d.gainedXp.length()), d.gainedXp,
                    nCopies(" ", territoryJustify - d.territory.length()), d.territory));
        }

        ret.add(String.format("%s--%s-+-%s-+-%s-+-%s-+-%s-",
                nCopies("-", numJustify), nCopies("-", nameJustify),
                nCopies("-", lvJustify), nCopies("-", xpJustify),
                nCopies("-", gainedJustify), nCopies("-", "Territory".length())));

        int leftJustify = numJustify + 2 + nameJustify + 3 + lvJustify + 3 + xpJustify;
        String pageView = String.format("< page %s / %s >", lbDisplay.page + 1, lbDisplay.maxPage + 1);
        ret.add(String.format("%s%sTotal | %s | %s",
                pageView, nCopies(" ", leftJustify - 5 - pageView.length()),
                lbDisplay.totalXPGained, lbDisplay.totalTerritories));

        ret.add("");

        DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

        ret.add(String.format("  xp gained: %s", lbDisplay.lbDuration));
        // in seconds
        long lastUpdateDiff = (new Date().getTime() - lbDisplay.lastUpdate.getTime()) / 1000L;
        ret.add(String.format("last update: %s (%s), %s ago", dateFormat.format(lbDisplay.lastUpdate),
                customTimeZone.getFormattedTime(), FormatUtils.formatReadableTime(lastUpdateDiff, false, "s")));

        ret.add("```");

        return String.join("\n", ret);
    }

    private static String nCopies(String s, int n) {
        return String.join("", Collections.nCopies(n, s));
    }
}

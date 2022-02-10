package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import db.model.dateFormat.CustomDateFormat;
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

public class GainedXpRank extends GenericCommand {
    private final GuildXpLeaderboardRepository guildXpLeaderboardRepository;
    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;
    private final TerritoryRepository territoryRepository;
    private final GuildListRepository guildListRepository;

    public GainedXpRank(Bot bot) {
        this.guildXpLeaderboardRepository = bot.getDatabase().getGuildXpLeaderboardRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.territoryRepository = bot.getDatabase().getTerritoryRepository();
        this.guildListRepository = bot.getDatabase().getGuildListRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"xp", "gainedXp"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"g", "xp"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{};
    }

    @Override
    public @NotNull String syntax() {
        return "g xp [-cl <list name>]";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Top guilds in order of amount of XP gained in last 24 hours.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Gained XP Command Help")
                        .setDescription(this.shortHelp() + "\n" +
                                "Append your custom guild list name to arguments to view the rank of guilds in the list.")
                        .addField("Syntax", this.syntax(), false)
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

    @Nullable
    private List<GuildXpLeaderboard> getLeaderboard(@Nullable List<GuildListEntry> list) {
        List<GuildXpLeaderboard> lb = this.guildXpLeaderboardRepository.findAll();
        if (lb != null && list != null) {
            // If custom guild list is specified, filter only to the guilds inside the leaderboard
            Set<String> guildNames = list.stream().map(GuildListEntry::getGuildName).collect(Collectors.toSet());
            lb.removeIf(g -> !guildNames.contains(g.getName()));
        }
        return lb;
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        String listName = parseListName(args);
        List<GuildListEntry> list = null;
        if (listName != null) {
            list = this.guildListRepository.getList(event.getAuthor().getIdLong(), listName);
            if (list == null) {
                event.replyError("Something went wrong while retrieving data...");
                return;
            }
            if (list.isEmpty()) {
                event.reply(String.format("You don't seem to have a custom guild list called `%s`.", listName));
                return;
            }
        }

        List<GuildXpLeaderboard> xpLeaderboard = getLeaderboard(list);
        if (xpLeaderboard == null) {
            event.replyError("Something went wrong while getting XP leaderboard...");
            return;
        }
        if (xpLeaderboard.size() == 0) {
            event.reply("Somehow, there were no guilds to display on the leaderboard.");
            return;
        }

        xpLeaderboard.sort(Comparator.comparingLong(GuildXpLeaderboard::getXpDiff).reversed());

        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        Function<Integer, Message> pageSupplier = page -> getPage(page, xpLeaderboard, customDateFormat, customTimeZone);
        if (maxPage(xpLeaderboard) == 0) {
            event.reply(pageSupplier.apply(0));
            return;
        }

        event.replyMultiPage(pageSupplier.apply(0), pageSupplier, () -> maxPage(xpLeaderboard));
    }

    private static final int GUILDS_PER_PAGE = 20;

    private static int maxPage(@NotNull List<GuildXpLeaderboard> leaderboard) {
        return (leaderboard.size() - 1) / GUILDS_PER_PAGE;
    }

    private record Display(String num, String name, int lv, String xp,
                           String gained, int territory) {
    }

    private List<Display> createDisplays(List<GuildXpLeaderboard> leaderboard) {
        List<Display> displays = new ArrayList<>();
        for (int i = 0; i < leaderboard.size(); i++) {
            GuildXpLeaderboard g = leaderboard.get(i);
            displays.add(new Display(
                    (i + 1) + ".",
                    String.format("[%s] %s", g.getPrefix(), g.getName()),
                    g.getLevel(),
                    FormatUtils.truncateNumber(new BigDecimal(g.getXp())),
                    FormatUtils.truncateNumber(new BigDecimal(g.getXpDiff())),
                    this.territoryRepository.countGuildTerritories(g.getName())
            ));
        }
        return displays;
    }

    private Message getPage(int page,
                            @NotNull List<GuildXpLeaderboard> leaderboard,
                            @NotNull CustomDateFormat customDateFormat,
                            @NotNull CustomTimeZone customTimeZone) {
        List<Display> displays = createDisplays(leaderboard);

        long total = leaderboard.stream().mapToLong(GuildXpLeaderboard::getXpDiff).sum();
        String totalGained = FormatUtils.truncateNumber(new BigDecimal(total));
        String totalTerritories = "" + displays.stream().mapToInt(d -> d.territory).sum();

        StringBuilder sb = new StringBuilder();

        sb.append("```ml\n");
        sb.append("---- Guild XP Transitions ----\n");
        sb.append("\n");

        TableFormatter tf = new TableFormatter(false);
        tf.addColumn("", Left, Left)
                .addColumn("Name", Left, Left)
                .addColumn("Lv", Left, Right)
                .addColumn("XP", Left, Right)
                .addColumn("Gained", totalGained, Left, Right)
                .addColumn("Territory", totalTerritories, Left, Left);
        int min = page * GUILDS_PER_PAGE;
        int max = Math.min((page + 1) * GUILDS_PER_PAGE, displays.size());
        for (int i = min; i < max; i++) {
            Display d = displays.get(i);
            tf.addRow(d.num, d.name, "" + d.lv, d.xp, d.gained, "" + d.territory);
        }
        tf.toString(sb);
        tf.addSeparator(sb);

        String pageView = String.format(
                "< page %s / %s >",
                page + 1, maxPage(leaderboard) + 1
        );
        sb.append(String.format(
                "%s%sTotal | %s | %s\n",
                pageView,
                nSpaces(tf.widthAt(0) + 3 + tf.widthAt(1) + 3 + tf.widthAt(2) + 3 + tf.widthAt(3) - pageView.length() - "Total".length()),
                totalGained,
                totalTerritories
        ));
        sb.append("\n");

        sb.append(makeFooter(leaderboard, customDateFormat, customTimeZone));
        sb.append("\n");

        sb.append("```");

        return new MessageBuilder(sb.toString()).build();
    }

    private static String makeFooter(@NotNull List<GuildXpLeaderboard> leaderboard,
                                     @NotNull CustomDateFormat customDateFormat,
                                     @NotNull CustomTimeZone customTimeZone) {
        List<String> ret = new ArrayList<>();

        // oldest and newest date could differ by guilds
        // i.e. guilds that were not in the leaderboard for all time
        // (this depends on implementation of update of `guild_xp_leaderboard` table)
        Date oldest = leaderboard.stream().map(GuildXpLeaderboard::getFrom)
                .min(Comparator.comparingLong(Date::getTime)).orElse(new Date());
        Date newest = leaderboard.stream().map(GuildXpLeaderboard::getTo)
                .max(Comparator.comparingLong(Date::getTime)).orElse(new Date());

        DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

        long duration = (newest.getTime() - oldest.getTime()) / 1000L;
        ret.add(String.format(
                "   duration: %s",
                FormatUtils.formatReadableTime(duration, false, "s")
        ));
        long sinceLastUpdate = (new Date().getTime() - newest.getTime()) / 1000L;
        ret.add(String.format(
                "last update: %s (%s), %s ago",
                dateFormat.format(newest), customTimeZone.getFormattedTime(),
                FormatUtils.formatReadableTime(sinceLastUpdate, false, "s")
        ));

        return String.join("\n", ret);
    }

    private static String nSpaces(int n) {
        return String.join("", Collections.nCopies(n, " "));
    }
}

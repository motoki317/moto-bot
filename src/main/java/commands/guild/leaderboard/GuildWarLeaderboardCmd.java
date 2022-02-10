package commands.guild.leaderboard;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import commands.guild.GuildPrefixesResolver;
import db.model.dateFormat.CustomDateFormat;
import db.model.guildWarLeaderboard.GuildWarLeaderboard;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
import db.repository.base.GuildWarLeaderboardRepository;
import db.repository.base.GuildWarLogRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.ArgumentParser;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utils.RangeParser.Range;
import static utils.RangeParser.parseRange;

public class GuildWarLeaderboardCmd extends GenericCommand {
    private final GuildWarLogRepository guildWarLogRepository;
    private final GuildWarLeaderboardRepository guildWarLeaderboardRepository;

    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;

    private final GuildPrefixesResolver guildPrefixesResolver;

    public GuildWarLeaderboardCmd(Bot bot) {
        this.guildWarLogRepository = bot.getDatabase().getGuildWarLogRepository();
        this.guildWarLeaderboardRepository = bot.getDatabase().getGuildWarLeaderboardRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();

        this.guildPrefixesResolver = new GuildPrefixesResolver(bot.getDatabase().getGuildRepository());
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"lb", "leaderboard", "wlb", "warLeaderboard", "warLB"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"g", "lb"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{};
    }

    @Override
    public @NotNull String syntax() {
        return "g lb [-t|--total] [-sc|--success] [-d|--days <num>] [--since|-S <date>] [--until|-U <date>]";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Guild war leaderboard. `g lb help` for more.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Guild War Leaderboard Help")
                        .setDescription(
                                """
                                        This command displays war leaderboard for guilds.
                                        They are ordered by their # of success wars by default.
                                        Note: Wars are logged and stored since around the beginning of April 2018."""
                        )
                        .addField("Syntax",
                                this.syntax(),
                                false
                        )
                        .addField("Optional Arguments",
                                String.join("\n",
                                        "**-t --total** : Sorts in order of # of total wars.",
                                        "**-sc --success** : Sorts in order of # of success wars.",
                                        "**-d|--days <days>** : Shows leaderboard of last given days, up to last 30 days.",
                                        "**--since|-S <date>**, **--until|-U <date>** : Directly specifies time range of the leaderboard.",
                                        "If --until is omitted, current time is specified.",
                                        "Acceptable formats: \"2020/01/01\", \"2020-01-01 15:00:00\", \"15 days ago\", \"8 hours ago\", \"30 minutes ago\""
                                ),
                                false
                        )
                        .addField("Examples",
                                String.join("\n",
                                        ">g lb : Displays leaderboard of all guilds ordered by # of success wars.",
                                        ">g lb -t : Displays leaderboard of guilds ordered by # of total wars.",
                                        ">g lb -sc -d 7 : Displays leaderboard of all guilds in last 7 days in order of # of success rate.",
                                        ">g lb --since 3 days ago --until 1 day ago : Displays leaderboard from 3 days ago to 1 day ago."
                                ),
                                false
                        ).build()
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    private enum SortType {
        Total,
        Success;

        private static SortType getDefault() {
            return Success;
        }
    }

    private static final int GUILDS_PER_PAGE = 10;

    @NotNull
    private static SortType parseSortType(Map<String, String> parsedArgs) {
        if (parsedArgs.containsKey("t") || parsedArgs.containsKey("-total")) {
            return SortType.Total;
        } else if (parsedArgs.containsKey("sc") || parsedArgs.containsKey("-success")) {
            return SortType.Success;
        } else {
            return SortType.getDefault();
        }
    }

    private static final long MAX_RANGE = TimeUnit.DAYS.toMillis(32);

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        Map<String, String> parsedArgs = new ArgumentParser(args).getArgumentMap();

        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        SortType sortType = parseSortType(parsedArgs);
        Range range;
        try {
            range = parseRange(parsedArgs, customTimeZone.getTimeZoneInstance(), MAX_RANGE);
        } catch (IllegalArgumentException e) {
            event.replyException(e.getMessage());
            return;
        }

        Supplier<Integer> maxPageSupplier;
        if (range == null) {
            maxPageSupplier = this::getMaxPageAllTime;
        } else {
            // max page should not change for ranged leaderboard
            int maxPage = this.getMaxPageRange(range);
            maxPageSupplier = () -> maxPage;
        }

        Function<Integer, Message> pageSupplier = page -> getPage(page, sortType, range, customDateFormat, customTimeZone, maxPageSupplier);
        if (maxPageSupplier.get() == 0) {
            event.reply(pageSupplier.apply(0));
            return;
        }

        event.replyMultiPage(pageSupplier.apply(0), pageSupplier, maxPageSupplier);
    }

    private record Display(String rank, String guildName, String successWarNum,
                           String totalWarNum, String successRate) {
    }

    private record Justify(int rank, int guildName, int successWarNum, int totalWarNum, int successRate) {
    }

    // Get max page for all time leaderboard
    private int getMaxPageAllTime() {
        return ((int) this.guildWarLeaderboardRepository.count() - 1) / GUILDS_PER_PAGE;
    }

    // Get max page for ranged leaderboard
    private int getMaxPageRange(@NotNull Range range) {
        return (this.guildWarLeaderboardRepository.getGuildsInRange(range.start, range.end) - 1) / GUILDS_PER_PAGE;
    }

    // Retrieves sorted leaderboard of the given context in arguments
    @Nullable
    private List<GuildWarLeaderboard> getPartialLeaderboard(@NotNull SortType sortType,
                                                            @Nullable Range range,
                                                            int offset) {
        if (range == null) {
            return switch (sortType) {
                case Total -> this.guildWarLeaderboardRepository.getByTotalWarDescending(GUILDS_PER_PAGE, offset);
                case Success -> this.guildWarLeaderboardRepository.getBySuccessWarDescending(GUILDS_PER_PAGE, offset);
            };
        } else {
            return switch (sortType) {
                case Total -> this.guildWarLeaderboardRepository.getByTotalWarDescending(GUILDS_PER_PAGE, offset, range.start, range.end);
                case Success -> this.guildWarLeaderboardRepository.getBySuccessWarDescending(GUILDS_PER_PAGE, offset, range.start, range.end);
            };
        }
    }

    // Get description of the leaderboard of the given context in arguments
    @NotNull
    private static String getTypeDescription(@NotNull SortType sortType,
                                             @Nullable Range range,
                                             @NotNull CustomTimeZone customTimeZone,
                                             @NotNull CustomDateFormat customDateFormat) {
        if (range == null) {
            return switch (sortType) {
                case Total -> "All Time: by # of total wars";
                case Success -> "All Time: by # of success wars";
            };
        } else {
            DateFormat dateFormat = customDateFormat.getDateFormat().getMinuteFormat();
            dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

            String startAndEnd = String.format(
                    "Start: %s (%s)\n" +
                            "  End: %s (%s)",
                    dateFormat.format(range.start), customTimeZone.getFormattedTime(),
                    dateFormat.format(range.end), customTimeZone.getFormattedTime());

            return switch (sortType) {
                case Total -> String.format("Ranged: by # of total wars\n%s", startAndEnd);
                case Success -> String.format("Ranged: by # of success wars\n%s", startAndEnd);
            };
        }
    }

    private int getSuccessWarSum(@Nullable Range range) {
        return range == null
                ? this.guildWarLogRepository.countSuccessWarsSum()
                : this.guildWarLogRepository.countSuccessWarsSum(range.start, range.end);
    }

    private int getTotalWarSum(@Nullable Range range) {
        return range == null
                ? this.guildWarLogRepository.countTotalWarsSum()
                : this.guildWarLogRepository.countTotalWarsSum(range.start, range.end);
    }

    // Get single formatted page for given sort type and range
    private Message getPage(int page,
                            @NotNull SortType sortType,
                            @Nullable Range range,
                            @NotNull CustomDateFormat customDateFormat,
                            @NotNull CustomTimeZone customTimeZone,
                            @NotNull Supplier<Integer> maxPageSupplier) {
        // Retrieve leaderboard
        int offset = page * GUILDS_PER_PAGE;

        // retrieved leaderboard is already sorted
        List<GuildWarLeaderboard> leaderboard = getPartialLeaderboard(sortType, range, offset);
        if (leaderboard == null) {
            return new MessageBuilder("Something went wrong while retrieving data...").build();
        }

        Map<String, String> prefixMap = this.guildPrefixesResolver.resolveGuildPrefixes(
                leaderboard.stream().map(GuildWarLeaderboard::getGuildName).collect(Collectors.toList())
        );

        List<Display> displays = new ArrayList<>();
        for (int i = 0; i < leaderboard.size(); i++) {
            GuildWarLeaderboard l = leaderboard.get(i);
            displays.add(new Display(
                    (offset + i + 1) + ".",
                    String.format("[%s] %s",
                            prefixMap.getOrDefault(l.getGuildName(), "???"),
                            l.getGuildName()
                    ),
                    String.valueOf(l.getSuccessWar()),
                    String.valueOf(l.getTotalWar()),
                    String.format("%.2f%%", l.getSuccessRate() * 100d)
            ));
        }

        int successWarSum = getSuccessWarSum(range);
        int totalWarSum = getTotalWarSum(range);
        String totalRate = String.format("%.2f%%", (double) successWarSum / (double) totalWarSum * 100d);

        Justify justify = new Justify(
                displays.stream().mapToInt(d -> d.rank.length()).max().orElse(2),
                IntStream.concat(
                        displays.stream().mapToInt(d -> d.guildName.length()),
                        IntStream.of("Guild".length())
                ).max().orElse("Guild".length()),
                IntStream.concat(
                        displays.stream().mapToInt(d -> d.successWarNum.length()),
                        IntStream.of(String.valueOf(successWarSum).length())
                ).max().orElse(String.valueOf(successWarSum).length()),
                IntStream.concat(
                        displays.stream().mapToInt(d -> d.totalWarNum.length()),
                        IntStream.of(String.valueOf(totalWarSum).length())
                ).max().orElse(String.valueOf(totalWarSum).length()),
                IntStream.concat(
                        displays.stream().mapToInt(d -> d.successRate.length()),
                        IntStream.of("0.00%".length(), totalRate.length())
                ).max().orElse("0.00%".length())
        );

        String warNumbers = "Wars: Success / Total";
        String rateMessage = "Success Rate";

        List<String> ret = new ArrayList<>();
        ret.add("```ml");
        ret.add("---- Guild War Leaderboard ----");
        ret.add("");
        String typeDescription = getTypeDescription(sortType, range, customTimeZone, customDateFormat);
        ret.add(typeDescription);
        ret.add(warNumbers);
        ret.add("");

        ret.add(formatTableDisplays(displays, justify, rateMessage, successWarSum, totalWarSum, totalRate));

        ret.add("");
        ret.add(String.format(
                "< page %s / %s >",
                page + 1, maxPageSupplier.get() + 1
        ));
        ret.add("");

        Date now = new Date();
        DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());
        ret.add(String.format(
                "Updated At: %s (%s)",
                dateFormat.format(now),
                customTimeZone.getFormattedTime()
        ));

        ret.add("```");

        return new MessageBuilder(String.join("\n", ret)).build();
    }

    // Formats displays according the given justify info.
    // Asserts that justify info should make sense for the given displays.
    private static String formatTableDisplays(List<Display> displays, Justify justify,
                                              String rateMessage,
                                              int successWarSum, int totalWarSum, String totalRate) {
        List<String> ret = new ArrayList<>();
        ret.add(String.format(
                "%s | Guild%s | Wars%s | %s",
                nCopies(" ", justify.rank),
                nCopies(" ", justify.guildName - "Guild".length()),
                nCopies(" ", justify.successWarNum + justify.totalWarNum - 1),
                rateMessage
        ));
        ret.add(String.format(
                "%s-+-%s-+-%s---%s-+-%s-",
                nCopies("-", justify.rank),
                nCopies("-", justify.guildName),
                nCopies("-", justify.successWarNum), nCopies("-", justify.totalWarNum),
                nCopies("-", rateMessage.length())
        ));

        for (Display d : displays) {
            ret.add(String.format(
                    "%s%s | %s%s | %s%s / %s%s | %s%s",
                    d.rank, nCopies(" ", justify.rank - d.rank.length()),
                    d.guildName, nCopies(" ", justify.guildName - d.guildName.length()),
                    nCopies(" ", justify.successWarNum - d.successWarNum.length()), d.successWarNum,
                    nCopies(" ", justify.totalWarNum - d.totalWarNum.length()), d.totalWarNum,
                    nCopies(" ", justify.successRate - d.successRate.length()), d.successRate
            ));
        }

        ret.add(String.format(
                "%s-+-%s-+-%s---%s-+-%s-",
                nCopies("-", justify.rank),
                nCopies("-", justify.guildName),
                nCopies("-", justify.successWarNum), nCopies("-", justify.totalWarNum),
                nCopies("-", rateMessage.length())
        ));
        ret.add(String.format(
                "%s   %sTotal | %s / %s | %s%s",
                nCopies(" ", justify.rank),
                nCopies(" ", justify.guildName - "Total".length()),
                successWarSum, totalWarSum,
                nCopies(" ", justify.successRate - totalRate.length()), totalRate
        ));

        return String.join("\n", ret);
    }

    private static String nCopies(String s, int n) {
        return String.join("", Collections.nCopies(Math.max(n, 0), s));
    }
}

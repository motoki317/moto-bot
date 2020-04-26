package commands.guild.leaderboard;

import api.wynn.WynnApi;
import api.wynn.structs.WynnGuild;
import app.Bot;
import commands.base.GenericCommand;
import commands.guild.GuildNameResolver;
import db.model.dateFormat.CustomDateFormat;
import db.model.playerWarLeaderboard.PlayerWarLeaderboard;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
import db.repository.base.GuildWarLogRepository;
import db.repository.base.PlayerWarLeaderboardRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import update.multipage.MultipageHandler;
import update.reaction.ReactionManager;
import utils.ArgumentParser;
import utils.FormatUtils;
import utils.UUID;
import utils.rateLimit.RateLimitException;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utils.RangeParser.Range;
import static utils.RangeParser.parseRange;

public class PlayerWarLeaderboardCmd extends GenericCommand {
    private final GuildWarLogRepository guildWarLogRepository;
    private final PlayerWarLeaderboardRepository playerWarLeaderboardRepository;

    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;

    private final ReactionManager reactionManager;

    private final WynnApi wynnApi;
    private final GuildNameResolver guildNameResolver;

    public PlayerWarLeaderboardCmd(Bot bot) {
        this.guildWarLogRepository = bot.getDatabase().getGuildWarLogRepository();
        this.playerWarLeaderboardRepository = bot.getDatabase().getPlayerWarLeaderboardRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();

        this.reactionManager = bot.getReactionManager();

        this.wynnApi = new WynnApi(bot.getLogger(), bot.getProperties().wynnTimeZone);
        this.guildNameResolver = new GuildNameResolver(bot.getResponseManager(), bot.getDatabase().getGuildRepository());
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"playerWarLeaderboard", "PWLb", "plb", "playerLeaderboard",
        }};
    }

    @Override
    public @NotNull String syntax() {
        return "g plb [-g|--guild <guild name>] [-t|--total] [-sc|--success] [-sr|--survived] [-d|--days <num>] [--since|-S <date>] [--until|-U <date>]";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Player war leaderboard. `g plb help` for more.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("Player War Leaderboard Help")
                .setDescription(
                        "This command displays war leaderboard for players.\n" +
                        "They are ordered by their # of success wars by default.\n" +
                        "Note: Wars are logged and stored since around the beginning of April 2018."
                )
                .addField("Syntax",
                        this.syntax(),
                        false
                )
                .addField("Optional Arguments",
                        String.join("\n",
                                "**-g|--guild <guild name>** : Specifies a guild, and display leaderboard for that guild.",
                                "**-t --total** : Sorts in order of # of total wars.",
                                "**-sc --success** : Sorts in order of # of success wars.",
                                "**-sr --survived** : Sorts in order of # of survived wars.",
                                "**-d|--days <days>** : Specifies the range of leaderboard, day-specifying is up to 30 days.",
                                "**--since|-S <date>**, **--until|-U <date>** : Directly specifies time range of the leaderboard.",
                                "If --until is omitted, current time is specified.",
                                "Acceptable formats: \"2020/01/01\", \"2020-01-01 15:00:00\", \"15 days ago\", \"8 hours ago\", \"30 minutes ago\""
                        ),
                        false
                )
                .addField("Examples",
                        String.join("\n",
                                ">g plb : Displays leaderboard of all players ordered by # of success wars.",
                                ">g plb -g UXs -t : Displays leaderboard of players in the guild UXs ordered by # of total wars.",
                                ">g plb -sc -d 7 : Displays leaderboard of all players, in last 7 days, and in order of # of success rate.",
                                ">g plb --since 3 days ago --until 1 day ago : Displays leaderboard from 3 days ago to 1 day ago."
                        ),
                        false
                ).build()
        ).build();
    }

    private enum SortType {
        Total,
        Success,
        Survived;

        private static SortType getDefault() {
            return Success;
        }

        private String getWarNumbersMessage() {
            if (this == Survived) {
                return "Survived";
            } else {
                return "Success";
            }
        }

        private int getWarNumber(PlayerWarLeaderboard l) {
            if (this == Survived) {
                return l.getSurvivedWar();
            } else {
                return l.getSuccessWar();
            }
        }

        private String getWarRate(PlayerWarLeaderboard l) {
            if (this == Survived) {
                return String.format("%.2f%%", l.getSurvivedRate() * 100d);
            } else {
                return String.format("%.2f%%", l.getSuccessRate() * 100d);
            }
        }

        private void sort(List<PlayerWarLeaderboard> leaderboard) {
            switch (this) {
                case Total:
                    leaderboard.sort((l1, l2) -> l2.getTotalWar() - l1.getTotalWar());
                    break;
                case Success:
                    leaderboard.sort((l1, l2) -> l2.getSuccessWar() - l1.getSuccessWar());
                    break;
                case Survived:
                    leaderboard.sort((l1, l2) -> l2.getSurvivedWar() - l1.getSurvivedWar());
                    break;
            }
        }
    }

    private int getSuccessWars(@Nullable Range range, @NotNull String guildName) {
        return range == null
                ? this.guildWarLogRepository.countSuccessWars(guildName)
                : this.guildWarLogRepository.countSuccessWars(guildName, range.start, range.end);
    }

    private int getTotalWars(@Nullable Range range, @NotNull String guildName) {
        return range == null
                ? this.guildWarLogRepository.countTotalWars(guildName)
                : this.guildWarLogRepository.countTotalWars(guildName, range.start, range.end);
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

    // Get description of the leaderboard of the given context in arguments
    @NotNull
    private static String getTypeDescription(@NotNull SortType sortType,
                                             @Nullable Range range,
                                             @NotNull CustomTimeZone customTimeZone,
                                             @NotNull CustomDateFormat customDateFormat) {
        if (range == null) {
            switch (sortType) {
                case Total:
                    return "All Time: by # of total wars";
                case Success:
                    return "All Time: by # of success wars";
                case Survived:
                    return "All Time: by # of survived wars";
            }
        } else {
            DateFormat dateFormat = customDateFormat.getDateFormat().getMinuteFormat();
            dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

            String startAndEnd = String.format(
                    "Start: %s (%s)\n" +
                            "  End: %s (%s)",
                    dateFormat.format(range.start), customTimeZone.getFormattedTime(),
                    dateFormat.format(range.end), customTimeZone.getFormattedTime());

            switch (sortType) {
                case Total:
                    return String.format("Ranged: by # of total wars\n%s", startAndEnd);
                case Success:
                    return String.format("Ranged: by # of success wars\n%s", startAndEnd);
                case Survived:
                    return String.format("Ranged: by # of survived wars\n%s", startAndEnd);
            }
        }

        return "error: unknown sorting type";
    }

    private static final int PLAYERS_PER_PAGE = 10;

    @NotNull
    private static SortType parseSortType(Map<String, String> parsedArgs) {
        if (parsedArgs.containsKey("t") || parsedArgs.containsKey("-total")) {
            return SortType.Total;
        } else if (parsedArgs.containsKey("sc") || parsedArgs.containsKey("-success")) {
            return SortType.Success;
        } else if (parsedArgs.containsKey("sr") || parsedArgs.containsKey("-survived")) {
            return SortType.Survived;
        } else {
            return SortType.getDefault();
        }
    }

    private static final long MAX_RANGE = TimeUnit.DAYS.toMillis(32);

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        Map<String, String> parsedArgs = new ArgumentParser(args).getArgumentMap();

        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        SortType sortType = parseSortType(parsedArgs);
        Range range;
        try {
            range = parseRange(parsedArgs, customTimeZone.getTimeZoneInstance(), MAX_RANGE);
        } catch (IllegalArgumentException e) {
            respondException(event, e.getMessage());
            return;
        }

        // Guild leaderboard
        if (parsedArgs.containsKey("g") || parsedArgs.containsKey("-guild")) {
            String specifiedName = parsedArgs.containsKey("g")
                    ? parsedArgs.get("g")
                    : parsedArgs.get("-guild");

            this.guildNameResolver.resolve(specifiedName, event.getTextChannel(), event.getAuthor(),
                    (guildName, prefix) -> processGuildPlayerLeaderboard(event, sortType, range, customDateFormat, customTimeZone, guildName, prefix),
                    error -> respondError(event, error)
            );
            return;
        }

        // Else, all players leaderboard
        Supplier<Integer> maxPage = () -> this.getMaxPageAllPlayers(range);
        Function<Integer, Message> pageSupplier = page -> allPlayersPageSupplier(page, sortType, range, customDateFormat, customTimeZone);
        respondLeaderboard(event, maxPage, pageSupplier);
    }

    private void respondLeaderboard(MessageReceivedEvent event,
                                    Supplier<Integer> maxPage,
                                    Function<Integer, Message> pageSupplier) {
        if (maxPage.get() == 0) {
            respond(event, pageSupplier.apply(0));
            return;
        }

        respond(event, pageSupplier.apply(0), message -> {
            MultipageHandler handler = new MultipageHandler(message, event.getAuthor().getIdLong(), pageSupplier, maxPage);
            this.reactionManager.addEventListener(handler);
        });
    }

    private void processGuildPlayerLeaderboard(@NotNull MessageReceivedEvent event,
                                               SortType sortType,
                                               @Nullable Range range,
                                               CustomDateFormat customDateFormat,
                                               CustomTimeZone customTimeZone,
                                               @NotNull String guildName,
                                               @Nullable String prefix) {
        WynnGuild wynnGuild;
        try {
            wynnGuild = this.wynnApi.getGuildStats(guildName);
        } catch (RateLimitException e) {
            respondException(event, e.getMessage());
            return;
        }
        if (wynnGuild == null) {
            respondException(event, "Something went wrong while requesting Wynncraft API.");
            return;
        }
        List<UUID> guildMemberUUIDs = wynnGuild.getMembers().stream()
                .map(m -> new UUID(m.getUuid()))
                .collect(Collectors.toList());

        List<PlayerWarLeaderboard> guildLeaderboard;
        if (range == null) {
            guildLeaderboard = this.playerWarLeaderboardRepository.getRecordsOf(guildMemberUUIDs);
        } else {
            guildLeaderboard = this.playerWarLeaderboardRepository.getRecordsOf(guildMemberUUIDs, range.start, range.end);
        }

        if (guildLeaderboard == null) {
            respondError(event, "Something went wrong while retrieving leaderboard.");
            return;
        }
        if (guildLeaderboard.isEmpty()) {
            respond(event, String.format("No war logs found for members of guild `%s` `[%s]` (within the provided time frame). (%s members)",
                    guildName, prefix, wynnGuild.getMembers().size()));
            return;
        }

        Date updatedAt = new Date();
        Function<Integer, Message> pageSupplier = guildPageSupplier(
                guildName,
                prefix,
                guildLeaderboard,
                sortType,
                range,
                customDateFormat,
                customTimeZone,
                updatedAt
        );
        Supplier<Integer> maxPage = () -> (guildLeaderboard.size() - 1) / PLAYERS_PER_PAGE;
        respondLeaderboard(event, maxPage, pageSupplier);
    }

    private static class Display {
        private String rank;
        private String playerName;
        private String firstWarNum;
        private String totalWarNum;
        private String rate;

        private Display(String rank, String playerName, String firstWarNum, String totalWarNum, String rate) {
            this.rank = rank;
            this.playerName = playerName;
            this.firstWarNum = firstWarNum;
            this.totalWarNum = totalWarNum;
            this.rate = rate;
        }
    }

    private static class Justify {
        private int rank;
        private int playerName;
        private int firstWarNum;
        private int totalWarNum;
        private int rate;

        private Justify(int rank, int playerName, int firstWarNum, int totalWarNum, int rate) {
            this.rank = rank;
            this.playerName = playerName;
            this.firstWarNum = firstWarNum;
            this.totalWarNum = totalWarNum;
            this.rate = rate;
        }
    }

    // Page supplier for guild leaderboard
    private Function<Integer, Message> guildPageSupplier(String guildName,
                                                         String prefix,
                                                         List<PlayerWarLeaderboard> guildLeaderboard,
                                                         SortType sortType,
                                                         @Nullable Range range,
                                                         CustomDateFormat customDateFormat,
                                                         CustomTimeZone customTimeZone,
                                                         Date updatedAt) {
        int maxPage = (guildLeaderboard.size() - 1) / PLAYERS_PER_PAGE;

        sortType.sort(guildLeaderboard);

        List<Display> displays = new ArrayList<>();
        for (int i = 0; i < guildLeaderboard.size(); i++) {
            PlayerWarLeaderboard l = guildLeaderboard.get(i);
            displays.add(new Display(
                    (i + 1) + ".",
                    l.getLastName(),
                    String.valueOf(sortType.getWarNumber(l)),
                    String.valueOf(l.getTotalWar()),
                    sortType.getWarRate(l)
            ));
        }

        int successWars = this.getSuccessWars(range, guildName);
        int totalWars = this.getTotalWars(range, guildName);
        String totalRate = String.format("%.2f%%", (double) successWars / (double) totalWars * 100d);

        Justify justify = getSpaceJustify(displays, successWars, totalWars, totalRate);

        String rateMessage = sortType.getWarNumbersMessage() + " Rate";
        String typeDescription = getTypeDescription(sortType, range, customTimeZone, customDateFormat);
        String warNumbers = "Wars: " + sortType.getWarNumbersMessage() + " / Total";

        return page -> {
            List<String> ret = new ArrayList<>();
            ret.add("```ml");
            ret.add("---- Player War Leaderboard ----");
            ret.add("");
            ret.add(String.format("%s [%s]", guildName, prefix));
            ret.add("");
            ret.add(typeDescription);
            ret.add(warNumbers);
            ret.add("");

            int begin = page * PLAYERS_PER_PAGE;
            int end = Math.min((page + 1) * PLAYERS_PER_PAGE, guildLeaderboard.size());
            ret.add(formatTableDisplays(displays, begin, end, justify, rateMessage, successWars, totalWars, totalRate));

            ret.add("");
            ret.add(String.format(
                    "< page %s / %s >",
                    page + 1, maxPage + 1
            ));
            ret.add("");

            DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
            dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());
            long elapsedSeconds = (new Date().getTime() - updatedAt.getTime()) / 1000L;
            ret.add(String.format(
                    "Updated At: %s (%s), %s ago",
                    dateFormat.format(updatedAt),
                    customTimeZone.getFormattedTime(),
                    FormatUtils.formatReadableTime(elapsedSeconds, false, "s")
            ));

            ret.add("```");

            return new MessageBuilder(String.join("\n", ret)).build();
        };
    }

    // Get max page for normal leaderboard
    private int getMaxPageAllPlayers(@Nullable Range range) {
        int count;
        if (range == null) {
            count = (int) this.playerWarLeaderboardRepository.count();
        } else {
            count = this.playerWarLeaderboardRepository.getPlayersInRange(range.start, range.end);
        }
        return (count - 1) / PLAYERS_PER_PAGE;
    }

    @Nullable
    private List<PlayerWarLeaderboard> allPlayersGetPartialLeaderboard(@NotNull SortType sortType,
                                                                       @Nullable Range range,
                                                                       int offset) {
        if (range == null) {
            switch (sortType) {
                case Total:
                    return this.playerWarLeaderboardRepository.getByTotalWarDescending(PLAYERS_PER_PAGE, offset);
                case Success:
                    return this.playerWarLeaderboardRepository.getBySuccessWarDescending(PLAYERS_PER_PAGE, offset);
                case Survived:
                    return this.playerWarLeaderboardRepository.getBySurvivedWarDescending(PLAYERS_PER_PAGE, offset);
            }
        } else {
            switch (sortType) {
                case Total:
                    return this.playerWarLeaderboardRepository.getByTotalWarDescending(PLAYERS_PER_PAGE, offset, range.start, range.end);
                case Success:
                    return this.playerWarLeaderboardRepository.getBySuccessWarDescending(PLAYERS_PER_PAGE, offset, range.start, range.end);
                case Survived:
                    return this.playerWarLeaderboardRepository.getBySurvivedWarDescending(PLAYERS_PER_PAGE, offset, range.start, range.end);
            }
        }
        return null;
    }

    // Page supplier for normal leaderboard
    private Message allPlayersPageSupplier(int page,
                                           SortType sortType,
                                           @Nullable Range range,
                                           CustomDateFormat customDateFormat,
                                           CustomTimeZone customTimeZone) {
        // Retrieve leaderboard
        int offset = page * PLAYERS_PER_PAGE;

        // retrieved partial leaderboard is already sorted
        List<PlayerWarLeaderboard> leaderboard = allPlayersGetPartialLeaderboard(sortType, range, offset);
        if (leaderboard == null) {
            return new MessageBuilder("Something went wrong while retrieving data...").build();
        }

        List<Display> displays = new ArrayList<>();
        for (int i = 0; i < leaderboard.size(); i++) {
            PlayerWarLeaderboard l = leaderboard.get(i);
            displays.add(new Display(
                    (offset + i + 1) + ".",
                    l.getLastName(),
                    String.valueOf(sortType.getWarNumber(l)),
                    String.valueOf(l.getTotalWar()),
                    sortType.getWarRate(l)
            ));
        }

        int successWarSum = this.getSuccessWarSum(range);
        int totalWarSum = this.getTotalWarSum(range);
        String totalRate = String.format("%.2f%%", (double) successWarSum / (double) totalWarSum * 100d);

        Justify justify = getSpaceJustify(displays, successWarSum, totalWarSum, totalRate);

        String typeDescription = getTypeDescription(sortType, range, customTimeZone, customDateFormat);
        String warNumbers = "Wars: " + sortType.getWarNumbersMessage() + " / Total";
        String rateMessage = sortType.getWarNumbersMessage() + " Rate";

        List<String> ret = new ArrayList<>();
        ret.add("```ml");
        ret.add("---- Player War Leaderboard ----");
        ret.add("");
        ret.add(typeDescription);
        ret.add(warNumbers);
        ret.add("");

        ret.add(formatTableDisplays(displays, 0, displays.size(), justify, rateMessage, successWarSum, totalWarSum, totalRate));

        ret.add("");
        ret.add(String.format(
                "< page %s / %s >",
                page + 1, getMaxPageAllPlayers(range) + 1
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

    @NotNull
    private static Justify getSpaceJustify(List<Display> displays, int successWarSum, int totalWarSum, String totalRate) {
        return new Justify(
                displays.stream().mapToInt(d -> d.rank.length()).max().orElse(2),
                IntStream.concat(
                        displays.stream().mapToInt(d -> d.playerName.length()),
                        IntStream.of("Player".length())
                ).max().orElse("Player".length()),
                IntStream.concat(
                        displays.stream().mapToInt(d -> d.firstWarNum.length()),
                        IntStream.of(String.valueOf(successWarSum).length())
                ).max().orElse(String.valueOf(successWarSum).length()),
                IntStream.concat(
                        displays.stream().mapToInt(d -> d.totalWarNum.length()),
                        IntStream.of(String.valueOf(totalWarSum).length())
                ).max().orElse(String.valueOf(totalWarSum).length()),
                IntStream.concat(
                        displays.stream().mapToInt(d -> d.rate.length()),
                        IntStream.of("0.00%".length(), totalRate.length())
                ).max().orElse("0.00%".length())
        );
    }

    // Formats displays according the given justify info.
    // Asserts that justify info should make sense for the given displays.
    private static String formatTableDisplays(List<Display> displays, int begin, int end, Justify justify,
                                              String rateMessage,
                                              int firstWarSum, int totalWarSum, String totalRate) {
        List<String> ret = new ArrayList<>();
        ret.add(String.format(
                "%s | Player%s | Wars%s | %s",
                nCopies(" ", justify.rank),
                nCopies(" ", justify.playerName - "Player".length()),
                nCopies(" ", justify.firstWarNum + justify.totalWarNum - 1),
                rateMessage
        ));
        ret.add(String.format(
                "%s-+-%s-+-%s---%s-+-%s-",
                nCopies("-", justify.rank),
                nCopies("-", justify.playerName),
                nCopies("-", justify.firstWarNum), nCopies("-", justify.totalWarNum),
                nCopies("-", rateMessage.length())
        ));

        for (int i = begin; i < end; i++) {
            Display d = displays.get(i);
            ret.add(String.format(
                    "%s%s | %s%s | %s%s / %s%s | %s%s",
                    d.rank, nCopies(" ", justify.rank - d.rank.length()),
                    d.playerName, nCopies(" ", justify.playerName - d.playerName.length()),
                    nCopies(" ", justify.firstWarNum - d.firstWarNum.length()), d.firstWarNum,
                    nCopies(" ", justify.totalWarNum - d.totalWarNum.length()), d.totalWarNum,
                    nCopies(" ", justify.rate - d.rate.length()), d.rate
            ));
        }

        ret.add(String.format(
                "%s-+-%s-+-%s---%s-+-%s-",
                nCopies("-", justify.rank),
                nCopies("-", justify.playerName),
                nCopies("-", justify.firstWarNum), nCopies("-", justify.totalWarNum),
                nCopies("-", rateMessage.length())
        ));
        ret.add(String.format(
                "%s   %sTotal | %s / %s | %s%s",
                nCopies(" ", justify.rank),
                nCopies(" ", justify.playerName - "Total".length()),
                firstWarSum, totalWarSum,
                nCopies(" ", justify.rate - totalRate.length()), totalRate
        ));

        return String.join("\n", ret);
    }

    private static String nCopies(String s, int n) {
        return String.join("", Collections.nCopies(Math.max(n, 0), s));
    }
}

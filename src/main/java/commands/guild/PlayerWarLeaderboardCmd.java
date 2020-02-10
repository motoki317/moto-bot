package commands.guild;

import api.wynn.WynnApi;
import api.wynn.structs.WynnGuild;
import app.Bot;
import commands.base.GenericCommand;
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
import update.multipage.MultipageHandler;
import update.reaction.ReactionManager;
import utils.ArgumentParser;
import utils.FormatUtils;
import utils.UUID;

import java.text.DateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        return new String[][]{{"g", "guild"}, {"playerWarLeaderboard", "plb"}};
    }

    @Override
    public @NotNull String syntax() {
        return "g plb [-g|--guild guild name] [-t|--total] [-sc|--success] [-sr|--survived]";
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
                                "**-sr --survived** : Sorts in order of # of survived wars."
                                // TODO: implement range specify
                                // "**-d <days>** : Specifies the range of leaderboard, day-specifying is up to 30 days."
                        ),
                        false
                )
                .addField("Examples",
                        String.join("\n",
                                ".g plb : Displays leaderboard of all players, and in order of # of success wars. (No arguments)",
                                ".g plb -g UXs -t : Displays leaderboard of players in the guild UXs, and in order of # of total wars."
                                // ".g plb -sc -d 7 : Displays leaderboard of all players, in last 7 days, and in order of # of success rate."
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

        private String getMessage() {
            // TODO implement range specify
            switch (this) {
                case Total:
                    return "All Time: by # of total wars";
                case Success:
                    return "All Time: by # of success wars";
                case Survived:
                    return "All Time: by # of survived wars";
                default:
                    return "error: unknown sorting type";
            }
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

    private static final int PLAYERS_PER_PAGE = 10;

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        Map<String, String> parsedArgs = new ArgumentParser(args).getArgumentMap();

        final SortType sortType;
        if (parsedArgs.containsKey("t") || parsedArgs.containsKey("-total")) {
            sortType = SortType.Total;
        } else if (parsedArgs.containsKey("sc") || parsedArgs.containsKey("-success")) {
            sortType = SortType.Success;
        } else if (parsedArgs.containsKey("sr") || parsedArgs.containsKey("-survived")) {
            sortType = SortType.Survived;
        } else {
            sortType = SortType.getDefault();
        }

        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        // Guild leaderboard
        if (parsedArgs.containsKey("g") || parsedArgs.containsKey("-guild")) {
            String specifiedName = parsedArgs.containsKey("g")
                    ? parsedArgs.get("g")
                    : parsedArgs.get("-guild");

            this.guildNameResolver.resolve(specifiedName, event.getTextChannel(), event.getAuthor(),
                    (guildName, prefix) -> {
                        WynnGuild wynnGuild = this.wynnApi.getGuildStats(guildName);
                        if (wynnGuild == null) {
                            respondError(event, "Something went wrong while requesting Wynncraft API.");
                            return;
                        }
                        List<UUID> guildMemberUUIDs = wynnGuild.getMembers().stream()
                                .map(m -> new UUID(m.getUuid()))
                                .collect(Collectors.toList());
                        // TODO implement range specify
                        List<PlayerWarLeaderboard> guildLeaderboard = this.playerWarLeaderboardRepository.getRecordsOf(guildMemberUUIDs);
                        if (guildLeaderboard == null) {
                            respondError(event, "Something went wrong while retrieving leaderboard.");
                            return;
                        }
                        if (guildLeaderboard.isEmpty()) {
                            respond(event, String.format("No war logs found for members of guild `%s` `[%s]`. (%s members)",
                                    guildName, prefix, wynnGuild.getMembers().size()));
                            return;
                        }

                        Date updatedAt = new Date();
                        Function<Integer, Message> pageSupplier = guildPageSupplier(
                                guildName,
                                prefix,
                                guildLeaderboard,
                                sortType,
                                customDateFormat,
                                customTimeZone,
                                updatedAt
                        );
                        Supplier<Integer> maxPage = () -> (guildLeaderboard.size() - 1) / PLAYERS_PER_PAGE;

                        if (maxPage.get() == 0) {
                            respond(event, pageSupplier.apply(0));
                            return;
                        }

                        respond(event, pageSupplier.apply(0), message -> {
                            MultipageHandler handler = new MultipageHandler(message, pageSupplier, maxPage);
                            this.reactionManager.addEventListener(handler);
                        });
                    },
                    error -> respondError(event, error)
            );
            return;
        }

        // Else, all players leaderboard
        // TODO implement range specify
        Function<Integer, Message> pageSupplier = allPlayersPageSupplier(sortType, customDateFormat, customTimeZone);
        if (getMaxPageAllPlayers() == 0) {
            respond(event, pageSupplier.apply(0));
            return;
        }

        respond(event, pageSupplier.apply(0), message -> {
            MultipageHandler handler = new MultipageHandler(message, pageSupplier, this::getMaxPageAllPlayers);
            this.reactionManager.addEventListener(handler);
        });
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
    private static Function<Integer, Message> guildPageSupplier(String guildName,
                                                                String prefix,
                                                                List<PlayerWarLeaderboard> guildLeaderboard,
                                                                SortType sortType,
                                                                CustomDateFormat customDateFormat,
                                                                CustomTimeZone customTimeZone,
                                                                Date updatedAt) {
        int maxPage = (guildLeaderboard.size() - 1) / PLAYERS_PER_PAGE;

        sortType.sort(guildLeaderboard);

        String warNumbers = "Wars: " + sortType.getWarNumbersMessage() + " / Total";

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

        // TODO: get a precise number of wars for a guild
        // (in this way, if multiple players joined the same war then it duplicates the count)
        int firstWarSum = guildLeaderboard.stream().mapToInt(sortType::getWarNumber).sum();
        int totalWarSum = guildLeaderboard.stream().mapToInt(PlayerWarLeaderboard::getTotalWar).sum();
        String totalRate = String.format("%.2f%%", (double) firstWarSum / (double) totalWarSum * 100d);

        Justify justify = new Justify(
                displays.stream().mapToInt(d -> d.rank.length()).max().orElse(2),
                IntStream.concat(
                        displays.stream().mapToInt(d -> d.playerName.length()),
                        IntStream.of("Player".length())
                ).max().orElse("Player".length()),
                IntStream.concat(
                        displays.stream().mapToInt(d -> d.firstWarNum.length()),
                        IntStream.of(String.valueOf(firstWarSum).length())
                ).max().orElse(String.valueOf(firstWarSum).length()),
                IntStream.concat(
                        displays.stream().mapToInt(d -> d.totalWarNum.length()),
                        IntStream.of(String.valueOf(totalWarSum).length())
                ).max().orElse(String.valueOf(totalWarSum).length()),
                IntStream.concat(
                        displays.stream().mapToInt(d -> d.rate.length()),
                        IntStream.of("0.00%".length(), totalRate.length())
                ).max().orElse("0.00%".length())
        );

        String rateMessage = sortType.getWarNumbersMessage() + " Rate";

        return page -> {
            List<String> ret = new ArrayList<>();
            ret.add("```ml");
            ret.add("---- Player War Leaderboard ----");
            ret.add("");
            ret.add(String.format("%s [%s]", guildName, prefix));
            ret.add("");
            ret.add(sortType.getMessage());
            ret.add(warNumbers);
            ret.add("");

            int begin = page * PLAYERS_PER_PAGE;
            int end = Math.min((page + 1) * PLAYERS_PER_PAGE, guildLeaderboard.size());
            ret.add(formatTableDisplays(displays, begin, end, justify, rateMessage, firstWarSum, totalWarSum, totalRate));

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
    private int getMaxPageAllPlayers() {
        int count = (int) this.playerWarLeaderboardRepository.count();
        return (count - 1) / PLAYERS_PER_PAGE;
    }

    // Page supplier for normal leaderboard
    private Function<Integer, Message> allPlayersPageSupplier(SortType sortType,
                                                              CustomDateFormat customDateFormat,
                                                              CustomTimeZone customTimeZone) {
        return page -> {
            // Retrieve leaderboard
            int offset = page * PLAYERS_PER_PAGE;

            // retrieved leaderboard is already sorted
            List<PlayerWarLeaderboard> leaderboard;
            switch (sortType) {
                case Total:
                    leaderboard = this.playerWarLeaderboardRepository.getByTotalWarDescending(PLAYERS_PER_PAGE, offset);
                    break;
                case Success:
                    leaderboard = this.playerWarLeaderboardRepository.getBySuccessWarDescending(PLAYERS_PER_PAGE, offset);
                    break;
                case Survived:
                    leaderboard = this.playerWarLeaderboardRepository.getBySurvivedWarDescending(PLAYERS_PER_PAGE, offset);
                    break;
                default:
                    return new MessageBuilder("Something went wrong while retrieving data...").build();
            }
            if (leaderboard == null) {
                return new MessageBuilder("Something went wrong while retrieving data...").build();
            }

            String warNumbers = "Wars: " + sortType.getWarNumbersMessage() + " / Total";

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

            int firstWarSum = this.guildWarLogRepository.countSuccessWarsSum();
            int totalWarSum = this.guildWarLogRepository.countTotalWarsSum();
            String totalRate = String.format("%.2f%%", (double) firstWarSum / (double) totalWarSum * 100d);

            Justify justify = new Justify(
                    displays.stream().mapToInt(d -> d.rank.length()).max().orElse(2),
                    IntStream.concat(
                            displays.stream().mapToInt(d -> d.playerName.length()),
                            IntStream.of("Player".length())
                    ).max().orElse("Player".length()),
                    IntStream.concat(
                            displays.stream().mapToInt(d -> d.firstWarNum.length()),
                            IntStream.of(String.valueOf(firstWarSum).length())
                    ).max().orElse(String.valueOf(firstWarSum).length()),
                    IntStream.concat(
                            displays.stream().mapToInt(d -> d.totalWarNum.length()),
                            IntStream.of(String.valueOf(totalWarSum).length())
                    ).max().orElse(String.valueOf(totalWarSum).length()),
                    IntStream.concat(
                            displays.stream().mapToInt(d -> d.rate.length()),
                            IntStream.of("0.00%".length(), totalRate.length())
                    ).max().orElse("0.00%".length())
            );

            String rateMessage = sortType.getWarNumbersMessage() + " Rate";

            List<String> ret = new ArrayList<>();
            ret.add("```ml");
            ret.add("---- Player War Leaderboard ----");
            ret.add("");
            ret.add(sortType.getMessage());
            ret.add(warNumbers);
            ret.add("");

            ret.add(formatTableDisplays(displays, 0, displays.size(), justify, rateMessage, firstWarSum, totalWarSum, totalRate));

            ret.add("");
            ret.add(String.format(
                    "< page %s / %s >",
                    page + 1, getMaxPageAllPlayers() + 1
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
        };
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

package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import db.model.dateFormat.CustomDateFormat;
import db.model.guild.Guild;
import db.model.guildWarLeaderboard.GuildWarLeaderboard;
import db.model.timezone.CustomTimeZone;
import db.repository.base.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import update.multipage.MultipageHandler;
import update.reaction.ReactionManager;
import utils.ArgumentParser;

import java.text.DateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GuildWarLeaderboardCmd extends GenericCommand {
    private final GuildRepository guildRepository;
    private final GuildWarLogRepository guildWarLogRepository;
    private final GuildWarLeaderboardRepository guildWarLeaderboardRepository;

    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;

    private final ReactionManager reactionManager;

    public GuildWarLeaderboardCmd(Bot bot) {
        this.guildRepository = bot.getDatabase().getGuildRepository();
        this.guildWarLogRepository = bot.getDatabase().getGuildWarLogRepository();
        this.guildWarLeaderboardRepository = bot.getDatabase().getGuildWarLeaderboardRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();

        this.reactionManager = bot.getReactionManager();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"lb", "leaderboard", "glb", "guildLeaderboard"}};
    }

    @Override
    public @NotNull String syntax() {
        return "g lb [-t|--total] [-sc|--success]";
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
                        "This command displays war leaderboard for guilds.\n" +
                        "They are ordered by their # of success wars by default.\n" +
                        "Note: Wars are logged and stored since around the beginning of April 2018."
                )
                .addField("Syntax",
                        this.syntax(),
                        false
                )
                .addField("Optional Arguments",
                        String.join("\n",
                                "**-t --total** : Sorts in order of # of total wars.",
                                "**-sc --success** : Sorts in order of # of success wars."
                                // TODO: implement range specify
                                // "**-d <days>** : Specifies the range of leaderboard, day-specifying is up to 30 days."
                        ),
                        false
                )
                .addField("Examples",
                        String.join("\n",
                                ".g lb : Displays leaderboard of all guilds ordered by # of success wars.",
                                ".g lb -t : Displays leaderboard of guilds ordered by # of total wars."
                                // ".g plb -sc -d 7 : Displays leaderboard of all players, in last 7 days, and in order of # of success rate."
                        ),
                        false
                ).build()
        ).build();
    }

    /**
     * Retrieves a map containing guild name -> prefix data.
     * @param guildNames List of guild names.
     * @return Map from guild names to prefixes.
     */
    @NotNull
    private Map<String, String> resolveGuildNames(List<String> guildNames) {
        Map<String, String> ret = new HashMap<>();
        List<Guild> guilds = this.guildRepository.findAllIn(guildNames.toArray(new String[]{}));
        if (guilds == null) {
            return ret;
        }

        for (Guild guild : guilds) {
            ret.put(guild.getName(), guild.getPrefix());
        }
        return ret;
    }

    private enum SortType {
        Total,
        Success;

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
                default:
                    return "error: unknown sorting type";
            }
        }
    }

    private static final int GUILDS_PER_PAGE = 10;

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        Map<String, String> parsedArgs = new ArgumentParser(args).getArgumentMap();

        final SortType sortType;
        if (parsedArgs.containsKey("t") || parsedArgs.containsKey("-total")) {
            sortType = SortType.Total;
        } else if (parsedArgs.containsKey("sc") || parsedArgs.containsKey("-success")) {
            sortType = SortType.Success;
        } else {
            sortType = SortType.getDefault();
        }

        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        Function<Integer, Message> pageSupplier = allTimePageSupplier(sortType, customDateFormat, customTimeZone);
        if (getMaxPageAllTime() == 0) {
            respond(event, pageSupplier.apply(0));
            return;
        }

        respond(event, pageSupplier.apply(0), message -> {
            MultipageHandler handler = new MultipageHandler(message, pageSupplier, this::getMaxPageAllTime);
            this.reactionManager.addEventListener(handler);
        });
    }

    private static class Display {
        private String rank;
        private String guildName;
        private String successWarNum;
        private String totalWarNum;
        private String successRate;

        private Display(String rank, String guildName, String successWarNum, String totalWarNum, String successRate) {
            this.rank = rank;
            this.guildName = guildName;
            this.successWarNum = successWarNum;
            this.totalWarNum = totalWarNum;
            this.successRate = successRate;
        }
    }

    private static class Justify {
        private int rank;
        private int guildName;
        private int successWarNum;
        private int totalWarNum;
        private int successRate;

        private Justify(int rank, int guildName, int successWarNum, int totalWarNum, int successRate) {
            this.rank = rank;
            this.guildName = guildName;
            this.successWarNum = successWarNum;
            this.totalWarNum = totalWarNum;
            this.successRate = successRate;
        }
    }

    // Get max page for all time
    private int getMaxPageAllTime() {
        return ((int) this.guildWarLeaderboardRepository.count() - 1) / GUILDS_PER_PAGE;
    }

    // Page supplier for normal leaderboard
    private Function<Integer, Message> allTimePageSupplier(SortType sortType,
                                                           CustomDateFormat customDateFormat,
                                                           CustomTimeZone customTimeZone) {
        return page -> {
            // Retrieve leaderboard
            int offset = page * GUILDS_PER_PAGE;

            // retrieved leaderboard is already sorted
            List<GuildWarLeaderboard> leaderboard = null;
            switch (sortType) {
                case Total:
                    leaderboard = this.guildWarLeaderboardRepository.getByTotalWarDescending(GUILDS_PER_PAGE, offset);
                    break;
                case Success:
                    leaderboard = this.guildWarLeaderboardRepository.getBySuccessWarDescending(GUILDS_PER_PAGE, offset);
                    break;
            }
            if (leaderboard == null) {
                return new MessageBuilder("Something went wrong while retrieving data...").build();
            }

            Map<String, String> prefixMap = this.resolveGuildNames(leaderboard.stream()
                    .map(GuildWarLeaderboard::getGuildName).collect(Collectors.toList()));

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

            int successWarSum = this.guildWarLogRepository.countSuccessWarsSum();
            int totalWarSum = this.guildWarLogRepository.countTotalWarsSum();
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
            ret.add(sortType.getMessage());
            ret.add(warNumbers);
            ret.add("");

            ret.add(formatTableDisplays(displays, 0, displays.size(), justify, rateMessage, successWarSum, totalWarSum, totalRate));

            ret.add("");
            ret.add(String.format(
                    "< page %s / %s >",
                    page + 1, getMaxPageAllTime() + 1
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

        for (int i = begin; i < end; i++) {
            Display d = displays.get(i);
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
                firstWarSum, totalWarSum,
                nCopies(" ", justify.successRate - totalRate.length()), totalRate
        ));

        return String.join("\n", ret);
    }

    private static String nCopies(String s, int n) {
        return String.join("", Collections.nCopies(Math.max(n, 0), s));
    }
}

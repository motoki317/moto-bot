package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import db.model.dateFormat.CustomDateFormat;
import db.model.territory.Territory;
import db.model.territoryLog.TerritoryLog;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
import db.repository.base.TerritoryLogRepository;
import db.repository.base.TerritoryRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.FormatUtils;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TerritoryLogsCmd extends GenericCommand {
    private final TerritoryRepository territoryRepository;
    private final TerritoryLogRepository territoryLogRepository;
    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;

    public TerritoryLogsCmd(Bot bot) {
        this.territoryRepository = bot.getDatabase().getTerritoryRepository();
        this.territoryLogRepository = bot.getDatabase().getTerritoryLogRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"terr", "territory"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"g", "territory"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "territory", "Name of a territory", true)
        };
    }

    @Override
    public @NotNull String syntax() {
        return "g territory <territory name>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows transition of owners of a specific territory.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Territory command help")
                        .setDescription(String.join("\n", new String[]{
                                this.shortHelp(),
                                "Give a full, or prefix of territory name that can identify unique territory to the first argument."
                        }))
                        .build()
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length < 3) {
            event.reply(this.longHelp());
            return;
        }

        String prefix = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        List<String> candidates = this.territoryRepository.territoryNamesBeginsWith(prefix);
        if (candidates == null) {
            event.replyError("Something went wrong while retrieving data...");
            return;
        }

        if (candidates.isEmpty()) {
            event.reply(String.format("No territories matched given the name or prefix \"%s\".", prefix));
            return;
        }

        // Check exact match (like "Detlas")
        String exactMatch = null;
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(prefix)) {
                exactMatch = candidate;
                break;
            }
        }

        if (candidates.size() > 1 && exactMatch == null) {
            event.reply(String.format("Multiple territories (%s) matched given the name or prefix.\n%s",
                    candidates.size(),
                    candidates.stream().limit(50).map(s -> "`" + s + "`").collect(Collectors.joining(", "))));
            return;
        }

        if (candidates.size() == 1) {
            exactMatch = candidates.get(0);
        }

        // process the match
        String territoryName = exactMatch;
        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        if (maxPage(territoryName) == 0) {
            event.reply(format(territoryName, 0, customDateFormat, customTimeZone));
            return;
        }

        Function<Integer, Message> pages = page -> new MessageBuilder(format(territoryName, page, customDateFormat, customTimeZone)).build();
        event.replyMultiPage(pages.apply(0), pages, () -> this.maxPage(territoryName));
    }

    private static final int LOGS_PER_PAGE = 5;

    private int maxPage(String territoryName) {
        int count = this.territoryLogRepository.territoryLogCount(territoryName);
        return (count - 1) / LOGS_PER_PAGE;
    }

    @Nullable
    private List<TerritoryLog> retrieveLogs(String territoryName, int page) {
        int offset = page * LOGS_PER_PAGE;
        if (page == 0) {
            return this.territoryLogRepository.territoryLogs(territoryName, LOGS_PER_PAGE, offset);
        } else {
            // retrieve one log after the page to make display
            return this.territoryLogRepository.territoryLogs(territoryName, LOGS_PER_PAGE + 1, offset - 1);
        }
    }

    private record Display(String num, String guildName, String dateFrom,
                           String dateTo, String heldTime) {
    }

    private String format(String territoryName, int page, CustomDateFormat customDateFormat, CustomTimeZone customTimeZone) {
        List<TerritoryLog> logs = retrieveLogs(territoryName, page);
        Territory territory = this.territoryRepository.findOne(() -> territoryName);
        int maxPage = this.maxPage(territoryName);
        if (logs == null || territory == null || maxPage == -1) {
            return "Something went wrong while retrieving data...";
        }

        DateFormat dateFormat = customDateFormat.getDateFormat().getMinuteFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

        int offset = page * LOGS_PER_PAGE;

        List<Display> displays = new ArrayList<>();

        int min = page == 0 ? 0 : 1;
        for (int i = min; i < logs.size(); i++) {
            TerritoryLog log = logs.get(i);
            TerritoryLog next = i == 0 ? null : logs.get(i - 1);
            int num = page == 0 ? offset + i + 1 : offset + i;

            Date from = log.getAcquired();
            Date to = i == 0 ? new Date() : next.getAcquired();
            displays.add(new Display(
                    String.valueOf(num),
                    log.getNewGuildName(),
                    dateFormat.format(from),
                    dateFormat.format(to) + (num == 1 ? " (Now)" : ""),
                    FormatUtils.formatReadableTime((to.getTime() - from.getTime()) / 1000L, false, "s")
            ));
        }

        return formatDisplays(territory, displays, page, maxPage, customTimeZone);
    }

    private static String formatDisplays(Territory territory, List<Display> displays, int page, int maxPage,
                                         CustomTimeZone customTimeZone) {
        List<String> ret = new ArrayList<>();

        ret.add("```ml");
        ret.add("---- Territory History ----");
        ret.add("");
        ret.add(territory.getName());
        ret.add("");
        Territory.Location location = territory.getLocation();
        ret.add(String.format("(x, z) = (%s, %s)",
                (location.getStartX() + location.getEndX()) / 2,
                (location.getStartZ() + location.getEndZ()) / 2));
        ret.add("");

        int numJustify = displays.stream().mapToInt(d -> d.num.length()).max().orElse(1);

        for (Display display : displays) {
            ret.add(String.format("%s.%s %s",
                    display.num, nSpaces(numJustify - display.num.length()),
                    display.guildName));
            ret.add(String.format("%s  %s ~ %s", nSpaces(numJustify), display.dateFrom, display.dateTo));
            ret.add(String.format("%s  Held for %s", nSpaces(numJustify), display.heldTime));
            ret.add("");
        }

        ret.add(String.format("< page %s / %s >", page + 1, maxPage + 1));
        ret.add("");
        ret.add(String.format("timezone offset: GMT%s", customTimeZone.getFormattedTime()));
        ret.add("```");

        return String.join("\n", ret);
    }

    private static String nSpaces(int n) {
        return String.join("", Collections.nCopies(n, " "));
    }
}

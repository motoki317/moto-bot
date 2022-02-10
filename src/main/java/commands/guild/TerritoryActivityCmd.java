package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import db.model.dateFormat.CustomDateFormat;
import db.model.territoryLog.TerritoryActivity;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
import db.repository.base.TerritoryLogRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.ArgumentParser;
import utils.FormatUtils;
import utils.TableFormatter;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static utils.RangeParser.Range;
import static utils.RangeParser.parseRange;
import static utils.TableFormatter.Justify.Left;
import static utils.TableFormatter.Justify.Right;

public class TerritoryActivityCmd extends GenericCommand {
    private final TerritoryLogRepository territoryLogRepository;
    private final TimeZoneRepository timeZoneRepository;
    private final DateFormatRepository dateFormatRepository;

    public TerritoryActivityCmd(Bot bot) {
        this.territoryLogRepository = bot.getDatabase().getTerritoryLogRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"territoryActivity", "terrActivity", "ta"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"g", "territoryactivity"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{};
    }

    @Override
    public @NotNull String syntax() {
        return "g territoryActivity [-d|--days <num>] [--since|-S <date>] [--until|-U <date>]";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows leaderboard of territories ordered by number of times each territory has been taken in specified time range. " +
                "If no time range is specified, displays data of all territory logs.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Territory Activity Command Help")
                        .setDescription(this.shortHelp())
                        .addField("Syntax",
                                this.syntax(),
                                false
                        )
                        .addField("Optional Arguments",
                                String.join("\n",
                                        "**-d|--days <days>** : Shows leaderboard of last given days.",
                                        "**--since|-S <date>**, **--until|-U <date>** : Directly specifies time range of the leaderboard.",
                                        "If --until is omitted, current time is specified.",
                                        "Acceptable formats: \"2020/01/01\", \"2020-01-01 15:00:00\", \"15 days ago\", \"8 hours ago\", \"30 minutes ago\""
                                ),
                                false
                        )
                        .addField("Examples",
                                String.join("\n",
                                        ">g ta : Displays leaderboard of all territories in last 7 days.",
                                        ">g ta -d 30 : Displays leaderboard in last 30 days.",
                                        ">g ta --since 3 days ago --until 1 day ago : Displays leaderboard from 3 days ago to 1 day ago."
                                ),
                                false
                        ).build()
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    private static final int ACTIVITIES_PER_PAGE = 10;

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        Map<String, String> parsedArgs = new ArgumentParser(args).getArgumentMap();

        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        Range range;
        try {
            range = parseRange(parsedArgs, customTimeZone.getTimeZoneInstance(), null);
        } catch (IllegalArgumentException e) {
            event.replyException(e.getMessage());
            return;
        }

        List<TerritoryActivity> activities = range == null
                ? this.territoryLogRepository.territoryActivity()
                : this.territoryLogRepository.territoryActivity(range.start, range.end);
        if (activities == null) {
            event.replyError("Something went wrong while retrieving data...");
            return;
        }
        if (activities.isEmpty()) {
            event.reply("There doesn't seem to be any territory activities in the given time.");
            return;
        }

        activities.sort((t1, t2) -> Integer.compare(t2.getCount(), t1.getCount()));

        int maxPage = (activities.size() - 1) / ACTIVITIES_PER_PAGE;
        ActivityDisplay display = new ActivityDisplay(activities, range, customDateFormat, customTimeZone, new Date());

        if (maxPage == 0) {
            event.reply(formatPage(0, display));
            return;
        }

        event.replyMultiPage(
                formatPage(0, display),
                page -> new MessageBuilder(formatPage(page, display)).build(),
                () -> maxPage);
    }

    private record ActivityDisplay(List<TerritoryActivity> activities,
                                   @Nullable utils.RangeParser.Range range,
                                   CustomDateFormat customDateFormat,
                                   CustomTimeZone customTimeZone, Date lastUpdate) {
    }

    private record Display(String num, String territoryName, String count) {
    }

    private static String formatPage(int page, ActivityDisplay display) {
        DateFormat dateFormat = display.customDateFormat.getDateFormat().getSecondFormat();
        dateFormat.setTimeZone(display.customTimeZone.getTimeZoneInstance());

        List<String> ret = new ArrayList<>();

        ret.add("```ml");
        ret.add("---- Territory Activity ----");
        ret.add("");
        if (display.range == null) {
            ret.add("All Time");
        } else {
            String timeZoneStr = display.customTimeZone.getFormattedTime();
            ret.add("Ranged Activity");
            ret.add(String.format("From: %s (%s)",
                    dateFormat.format(display.range.start), timeZoneStr));
            ret.add(String.format("  To: %s (%s)",
                    dateFormat.format(display.range.end), timeZoneStr));
        }
        ret.add("");

        int begin = page * ACTIVITIES_PER_PAGE;
        int end = Math.min((page + 1) * ACTIVITIES_PER_PAGE, display.activities.size());
        List<Display> displays = new ArrayList<>(end - begin);
        for (int i = begin; i < end; i++) {
            TerritoryActivity ta = display.activities.get(i);
            displays.add(new Display(
                    (i + 1) + ".",
                    ta.getTerritoryName(),
                    String.valueOf(ta.getCount())
            ));
        }

        int totalCount = display.activities.stream().mapToInt(TerritoryActivity::getCount).sum();
        ret.add(formatDisplays(displays, totalCount));

        int maxPage = (display.activities.size() - 1) / ACTIVITIES_PER_PAGE;
        ret.add(String.format("< page %s / %s >", page + 1, maxPage + 1));
        ret.add("");

        long elapsedSeconds = (System.currentTimeMillis() - display.lastUpdate.getTime()) / 1000L;
        ret.add(String.format("last update: %s (%s), %s ago",
                dateFormat.format(display.lastUpdate),
                display.customTimeZone.getFormattedTime(),
                FormatUtils.formatReadableTime(elapsedSeconds, false, "s")));
        ret.add("```");

        return String.join("\n", ret);
    }

    private static String formatDisplays(List<Display> displays, int totalCount) {
        StringBuilder sb = new StringBuilder();
        TableFormatter tf = new TableFormatter(false);
        tf.addColumn("", Left, Left)
                .addColumn("Territory", Left, Left)
                .addColumn("Count", "" + totalCount, Left, Right);
        for (Display d : displays) {
            tf.addRow(d.num, d.territoryName, d.count);
        }
        tf.toString(sb);
        tf.addSeparator(sb);
        sb.append(String.format("%sTotal | %s%s",
                nSpaces(tf.widthAt(0) + 3 + tf.widthAt(1) - "Total".length()),
                nSpaces(tf.widthAt(2) - String.valueOf(totalCount).length()),
                totalCount));
        return sb.toString();
    }

    private static String nSpaces(int n) {
        return String.join("", Collections.nCopies(n, " "));
    }
}

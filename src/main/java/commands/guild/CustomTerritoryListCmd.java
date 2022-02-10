package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import db.model.dateFormat.CustomDateFormat;
import db.model.territory.Territory;
import db.model.territoryList.TerritoryListEntry;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
import db.repository.base.TerritoryListRepository;
import db.repository.base.TerritoryRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import utils.FormatUtils;
import utils.TableFormatter;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static utils.TableFormatter.Justify.Left;
import static utils.TableFormatter.Justify.Right;

public class CustomTerritoryListCmd extends GenericCommand {
    private final TerritoryRepository territoryRepository;
    private final TerritoryListRepository territoryListRepository;
    private final TimeZoneRepository timeZoneRepository;
    private final DateFormatRepository dateFormatRepository;

    public CustomTerritoryListCmd(Bot bot) {
        this.territoryRepository = bot.getDatabase().getTerritoryRepository();
        this.territoryListRepository = bot.getDatabase().getTerritoryListRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {
                "customTerritoryList", "custom-territory-list",
                "myList", "ml"
        }};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"g", "custom-territory-list"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{};
    }

    @Override
    public @NotNull String syntax() {
        return "g customTerritoryList";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Customize your own territory list to view a set of territory stats at one time.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                String.join("\n", new String[]{
                        "This command allows you to make a custom list of guild territories, and display their owners " +
                                "& held time in a list, so that you can check your favorite territories at one time!",
                        "Note that every custom list is saved per user.",
                        "Lists will be created or removed automatically when you add or remove territories to / from the list.",
                        "Usage:",
                        "`>g myList` : Shows your custom lists.",
                        "`>g myList <list name>` : View the list.",
                        "`>g myList add <list name> <territory1>, <territory2>, ...` : Adds territories to the list.",
                        "`>g myList remove <list name> <territory1>, <territory2>, ...` : Removes territories from the list.",
                        "`>g myList remove <list name>` : Removes the list."
                })
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length <= 2) {
            viewUserLists(event);
            return;
        }

        switch (args[2].toLowerCase()) {
            case "add" -> addToList(event, args);
            case "remove" -> removeFromList(event, args);
            default -> viewList(event, args[2]);
        }
    }

    private void viewUserLists(CommandEvent event) {
        Map<String, Integer> lists = this.territoryListRepository.getUserLists(event.getAuthor().getIdLong());
        if (lists == null) {
            event.replyError("Something went wrong while retrieving data...");
            return;
        }
        if (lists.isEmpty()) {
            event.reply("You do not have any custom territory lists configured. `>g myList help` for more.");
            return;
        }

        List<String> ret = new ArrayList<>();
        ret.add(String.format("Your have `%s` custom territory list%s.", lists.size(), lists.size() == 1 ? "" : "s"));
        ret.add("");
        for (Map.Entry<String, Integer> e : lists.entrySet()) {
            ret.add(String.format("`%s` : `%s` %s",
                    e.getKey(), e.getValue(), e.getValue() == 1 ? "territory" : "territories"));
        }

        event.reply(String.join("\n", ret));
    }

    private static final int TERRITORIES_PER_PAGE = 10;

    private void viewList(CommandEvent event, String listName) {
        List<TerritoryListEntry> list = this.territoryListRepository.getList(event.getAuthor().getIdLong(), listName);
        if (list == null) {
            event.replyError("Something went wrong while retrieving data...");
            return;
        }
        if (list.isEmpty()) {
            event.reply(String.format("You don't seem to have any territories in a list named `%s`. " +
                    "Check your spelling or try adding territories first.", listName));
            return;
        }

        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);
        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);

        List<String> territoryNames = list.stream().map(TerritoryListEntry::getTerritoryName).collect(Collectors.toList());
        List<Territory> territories = this.territoryRepository.findAllIn(territoryNames);
        if (territories == null) {
            event.replyError("Something went wrong while retrieving data...");
            return;
        }
        if (territories.isEmpty()) {
            event.replyException(String.format("Somehow, there were no territories to display on this list `%s`...", listName));
            return;
        }

        int maxPage = (territories.size() - 1) / TERRITORIES_PER_PAGE;
        if (maxPage == 0) {
            event.reply(formatPage(0, listName, territoryNames, customTimeZone, customDateFormat));
            return;
        }

        Function<Integer, Message> pages = page -> new MessageBuilder(formatPage(page, listName, territoryNames, customTimeZone, customDateFormat)).build();
        event.replyMultiPage(pages.apply(0), pages, () -> maxPage);
    }

    private record Display(String num, String territory, String owner,
                           String heldTime) {
    }

    private String formatPage(int page, String listName, List<String> territoryNames,
                              CustomTimeZone customTimeZone, CustomDateFormat customDateFormat) {
        List<Territory> territories = this.territoryRepository.findAllIn(territoryNames);
        if (territories == null || territories.isEmpty()) {
            return "Something went wrong while retrieving data...";
        }

        territories.sort((t1, t2) -> Long.compare(t2.getAcquired().getTime(), t1.getAcquired().getTime()));

        long now = System.currentTimeMillis();

        int begin = page * TERRITORIES_PER_PAGE;
        int end = Math.min((page + 1) * TERRITORIES_PER_PAGE, territories.size());
        List<Display> displays = new ArrayList<>();
        for (int i = begin; i < end; i++) {
            Territory t = territories.get(i);
            displays.add(new Display(
                    (i + 1) + ".",
                    t.getName(),
                    t.getGuild(),
                    FormatUtils.formatReadableTime((now - t.getAcquired().getTime()) / 1000L, true, "m")
            ));
        }

        List<String> ret = new ArrayList<>();
        ret.add("```ml");
        ret.add("---- Custom Territory List ----");
        ret.add("");
        ret.add(String.format("List: %s", listName));
        ret.add("");

        TableFormatter tf = new TableFormatter(false);
        tf.addColumn("", Left, Left)
                .addColumn("Territory", Left, Left)
                .addColumn("Owner", Left, Left)
                .addColumn("Held Time", Left, Right);
        for (Display d : displays) {
            tf.addRow(d.num, d.territory, d.owner, d.heldTime);
        }
        ret.add(tf.toString());

        int maxPage = (territories.size() - 1) / TERRITORIES_PER_PAGE;
        ret.add(String.format("< page %s / %s >", page + 1, maxPage + 1));
        ret.add("");

        DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());
        ret.add(String.format("last update: %s (%s)",
                dateFormat.format(new Date()), customTimeZone.getFormattedTime()));
        ret.add("```");

        return String.join("\n", ret);
    }

    private void addToList(CommandEvent event, String[] args) {
        if (args.length <= 4) {
            event.reply("Please specify a list name and at least one territory name to add.");
            return;
        }

        String listName = args[3];
        List<String> territoryNames = Arrays.stream(String.join(" ", Arrays.copyOfRange(args, 4, args.length))
                .split(",")).map(String::trim).collect(Collectors.toList());
        List<Territory> territories = this.territoryRepository.findAllIn(territoryNames);
        if (territories == null) {
            event.replyError("Something went wrong while retrieving data...");
            return;
        }
        if (territories.isEmpty()) {
            event.reply("No territories found with the given names. Make sure the territory exists and give the exact name(s).");
            return;
        }

        boolean success = territories.stream()
                .map(territory -> new TerritoryListEntry(
                        event.getAuthor().getIdLong(),
                        listName,
                        territory.getName()
                ))
                .filter(e -> !this.territoryListRepository.exists(e))
                .map(this.territoryListRepository::create)
                .reduce(true, (acc, elt) -> acc && elt);

        if (!success) {
            event.replyError("Something went wrong while saving data...");
            return;
        }

        event.reply(String.format("Successfully added %s %s to list `%s`!\n%s",
                territories.size(), territories.size() == 1 ? "territory" : "territories", listName,
                territories.stream().map(t -> "`" + t.getName() + "`").collect(Collectors.joining(", "))));
    }

    private void removeFromList(CommandEvent event, String[] args) {
        if (args.length <= 3) {
            event.reply("Please specify a list name (and list of territories) to remove from the list.");
            return;
        }
        String listName = args[3];
        List<String> territoryNames;
        if (args.length == 4) {
            // no territories specified
            List<TerritoryListEntry> list = this.territoryListRepository.getList(event.getAuthor().getIdLong(), listName);
            if (list == null) {
                event.replyError("Something went wrong while retrieving data...");
                return;
            }
            if (list.isEmpty()) {
                event.reply(String.format("No territories to remove for list `%s`.", listName));
                return;
            }

            territoryNames = list.stream().map(TerritoryListEntry::getTerritoryName).collect(Collectors.toList());
        } else {
            // territories to remove specified
            List<String> specifiedNames = Arrays.stream(String.join(" ", Arrays.copyOfRange(args, 4, args.length))
                    .split(",")).map(String::trim).collect(Collectors.toList());
            List<Territory> territories = this.territoryRepository.findAllIn(specifiedNames);
            if (territories == null) {
                event.replyError("Something went wrong while retrieving data...");
                return;
            }
            if (territories.isEmpty()) {
                event.reply("No territories found with the given names. Make sure the territory exists and give the exact name(s).");
                return;
            }

            territoryNames = territories.stream().map(Territory::getName).collect(Collectors.toList());
        }

        boolean success = territoryNames.stream()
                .map(territoryName -> new TerritoryListEntry(
                        event.getAuthor().getIdLong(),
                        listName,
                        territoryName
                ))
                .filter(this.territoryListRepository::exists)
                .map(this.territoryListRepository::delete)
                .reduce(true, (acc, elt) -> acc && elt);

        if (!success) {
            event.replyError("Something went wrong while saving data...");
            return;
        }

        event.reply(String.format("Successfully removed %s %s from list `%s`!\n%s",
                territoryNames.size(), territoryNames.size() == 1 ? "territory" : "territories", listName,
                territoryNames.stream().map(t -> "`" + t + "`").collect(Collectors.joining(", "))));
    }
}

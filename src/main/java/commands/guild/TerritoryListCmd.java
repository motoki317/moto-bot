package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import commands.event.message.SentMessage;
import db.model.dateFormat.CustomDateFormat;
import db.model.territory.Territory;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
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

public class TerritoryListCmd extends GenericCommand {
    private final TerritoryRepository territoryRepository;

    private final TimeZoneRepository timeZoneRepository;
    private final DateFormatRepository dateFormatRepository;

    private final GuildNameResolver guildNameResolver;
    private final GuildPrefixesResolver guildPrefixesResolver;

    public TerritoryListCmd(Bot bot) {
        this.territoryRepository = bot.getDatabase().getTerritoryRepository();

        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();

        this.guildNameResolver = new GuildNameResolver(
                bot.getDatabase().getGuildRepository(),
                bot.getButtonClickManager()
        );
        this.guildPrefixesResolver = new GuildPrefixesResolver(bot.getDatabase().getGuildRepository());
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"territoryList", "tList", "tl"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"g", "territorylist"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "guild", "Name or prefix of a guild")
        };
    }

    @Override
    public @NotNull String syntax() {
        return "g territoryList [guild name]";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows a list of all/guild's territories.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Territory List Command Help")
                        .setDescription(this.shortHelp())
                        .build()
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length <= 2) {
            // No guild specified
            showAllTerritories(event);
            return;
        }

        String specified = String.join("", Arrays.copyOfRange(args, 2, args.length));
        this.guildNameResolver.resolve(
                specified,
                event,
                (next, guildName, prefix) -> showGuildTerritories(event, next, new Guild(guildName, prefix))
        );
    }

    private void showAllTerritories(CommandEvent event) {
        List<Territory> territories = this.territoryRepository.findAll();
        Date lastUpdate = new Date();
        if (territories == null || territories.isEmpty()) {
            event.replyError("Something went wrong while retrieving a list of territories...");
            return;
        }

        event.reply(new EmbedBuilder().setDescription("Processing...").build(), next ->
                sendMessage(event, next, territories, lastUpdate, null));
    }

    private record Guild(@NotNull String name, @Nullable String prefix) {
        private Guild(@NotNull String name, @Nullable String prefix) {
            this.name = name;
            this.prefix = prefix;
        }
    }

    private void showGuildTerritories(CommandEvent event, SentMessage next, @NotNull Guild guild) {
        List<Territory> territories = this.territoryRepository.getGuildTerritories(guild.name);
        Date lastUpdate = new Date();
        if (territories == null) {
            next.editError(event.getAuthor(), String.format(
                    "Something went wrong while retrieving a list of territories for guild `%s`...", guild.name
            ));
            return;
        }
        if (territories.isEmpty()) {
            next.editMessage(String.format("Guild `%s` `[%s]` doesn't seem to own any territories.",
                    guild.name, guild.prefix != null ? guild.prefix : "???"));
            return;
        }

        sendMessage(event, next, territories, lastUpdate, guild);
    }

    private static final int TERRITORIES_PER_PAGE = 5;

    private void sendMessage(CommandEvent event,
                             SentMessage next,
                             List<Territory> territories,
                             Date lastUpdate,
                             @Nullable Guild guild) {
        // Sort by descending order of acquired time
        territories.sort((t1, t2) -> Long.compare(t2.getAcquired().getTime(), t1.getAcquired().getTime()));

        int maxPage = (territories.size() - 1) / TERRITORIES_PER_PAGE;

        if (maxPage == 0) {
            next.editMessage(formatPage(0, territories, event, lastUpdate, guild));
            return;
        }

        Function<Integer, Message> pages = page -> new MessageBuilder(formatPage(page, territories, event, lastUpdate, guild)).build();
        next.editMultiPage(event.getBot(), pages, () -> maxPage);
    }

    private record Display(String num, String territoryName,
                           Guild guild, String acquired,
                           String heldTime) {
    }

    private String formatPage(int page, List<Territory> territories, CommandEvent event, Date lastUpdate,
                              @Nullable Guild guild) {
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);
        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

        List<String> ret = new ArrayList<>();

        ret.add("```ml");
        ret.add("---- Territory List ----");
        ret.add("");

        if (guild != null) {
            ret.add(String.format("%s [%s]", guild.name, guild.prefix != null ? guild.prefix : "???"));
            ret.add("");
        }

        int begin = page * TERRITORIES_PER_PAGE;
        int end = Math.min((page + 1) * TERRITORIES_PER_PAGE, territories.size());
        Date now = new Date();
        List<Display> displays = new ArrayList<>();

        Map<String, String> guildPrefixes = this.guildPrefixesResolver.resolveGuildPrefixes(
                territories.subList(begin, end).stream().map(Territory::getGuild).collect(Collectors.toList())
        );

        for (int i = begin; i < end; i++) {
            Territory t = territories.get(i);
            displays.add(new Display(
                    String.valueOf(i + 1),
                    t.getName(),
                    new Guild(t.getGuild(), guildPrefixes.getOrDefault(t.getGuild(), null)),
                    dateFormat.format(t.getAcquired()),
                    FormatUtils.formatReadableTime(
                            (now.getTime() - t.getAcquired().getTime()) / 1000L, false, "s"
                    )
            ));
        }

        ret.add(formatDisplays(displays));

        int maxPage = (territories.size() - 1) / TERRITORIES_PER_PAGE;
        ret.add(String.format("< page %s / %s >", page + 1, maxPage + 1));
        ret.add("");

        long elapsedSeconds = (now.getTime() - lastUpdate.getTime()) / 1000L;
        ret.add(String.format("last update: %s (%s), %s ago",
                dateFormat.format(lastUpdate),
                customTimeZone.getFormattedTime(),
                FormatUtils.formatReadableTime(elapsedSeconds, false, "s")
        ));
        ret.add("```");

        return String.join("\n", ret);
    }

    private static String formatDisplays(List<Display> displays) {
        int numJustify = displays.stream().mapToInt(d -> d.num.length()).max().orElse(1);

        List<String> ret = new ArrayList<>();
        for (Display d : displays) {
            ret.add(String.format("%s.%s %s : %s [%s]",
                    d.num, nSpaces(numJustify - d.num.length()),
                    d.territoryName,
                    d.guild.name, d.guild.prefix != null ? d.guild.prefix : "???"
            ));
            ret.add(String.format("%s  Acquired: %s",
                    nSpaces(numJustify), d.acquired
            ));
            ret.add(String.format("%s  Held for %s",
                    nSpaces(numJustify), d.heldTime
            ));
            ret.add("");
        }

        return String.join("\n", ret);
    }

    private static String nSpaces(int n) {
        return String.join("", Collections.nCopies(n, " "));
    }
}

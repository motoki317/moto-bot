package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import db.model.dateFormat.CustomDateFormat;
import db.model.territory.Territory;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
import db.repository.base.TerritoryRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import update.multipage.MultipageHandler;
import update.reaction.ReactionManager;
import utils.FormatUtils;

import java.text.DateFormat;
import java.util.*;

public class TerritoryListCmd extends GenericCommand {
    private final TerritoryRepository territoryRepository;
    private final ReactionManager reactionManager;
    private final GuildNameResolver guildNameResolver;
    private final TimeZoneRepository timeZoneRepository;
    private final DateFormatRepository dateFormatRepository;

    public TerritoryListCmd(Bot bot) {
        this.territoryRepository = bot.getDatabase().getTerritoryRepository();
        this.reactionManager = bot.getReactionManager();
        this.guildNameResolver = new GuildNameResolver(bot.getResponseManager(), bot.getDatabase().getGuildRepository());
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"territoryList", "tList", "tl"}};
    }

    @Override
    public @NotNull String syntax() {
        return "g territoryList [guild name]";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows a list of all territories, or if a guild name is given, shows a list of territories the guild possesses.";
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
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 2) {
            // No guild specified
            showAllTerritories(event);
            return;
        }

        String specified = String.join("", Arrays.copyOfRange(args, 2, args.length));
        this.guildNameResolver.resolve(
                specified, event.getTextChannel(), event.getAuthor(),
                (guildName, prefix) -> showGuildTerritories(event, new Guild(guildName, prefix)),
                reason -> respondError(event, reason)
        );
    }

    private void showAllTerritories(MessageReceivedEvent event) {
        List<Territory> territories = this.territoryRepository.findAll();
        Date lastUpdate = new Date();
        if (territories == null || territories.isEmpty()) {
            respondError(event, "Something went wrong while retrieving a list of territories...");
            return;
        }

        sendMessage(event, territories, lastUpdate, null);
    }

    private static class Guild {
        @NotNull
        private String name;
        @Nullable
        private String prefix;

        private Guild(@NotNull String name, @Nullable String prefix) {
            this.name = name;
            this.prefix = prefix;
        }
    }

    private void showGuildTerritories(MessageReceivedEvent event, @NotNull Guild guild) {
        List<Territory> territories = this.territoryRepository.getGuildTerritories(guild.name);
        Date lastUpdate = new Date();
        if (territories == null) {
            respondError(event, String.format(
                    "Something went wrong while retrieving a list of territories for guild `%s`...", guild.name
            ));
            return;
        }
        if (territories.isEmpty()) {
            respond(event, String.format("Guild `%s` `[%s]` doesn't seem to own any territories.",
                    guild.name, guild.prefix != null ? guild.prefix : "???"));
            return;
        }

        sendMessage(event, territories, lastUpdate, guild);
    }

    private static final int TERRITORIES_PER_PAGE = 5;

    private void sendMessage(MessageReceivedEvent event, List<Territory> territories, Date lastUpdate,
                             @Nullable Guild guild) {
        // Sort by descending order of acquired time
        territories.sort((t1, t2) -> Long.compare(t2.getAcquired().getTime(), t1.getAcquired().getTime()));

        int maxPage = (territories.size() - 1) / TERRITORIES_PER_PAGE;

        if (maxPage == 0) {
            respond(event, formatPage(0, territories, event, lastUpdate, guild));
            return;
        }

        respond(event, formatPage(0, territories, event, lastUpdate, guild), message -> {
            MultipageHandler handler = new MultipageHandler(
                    message, event.getAuthor().getIdLong(),
                    page -> new MessageBuilder(formatPage(page, territories, event, lastUpdate, guild)).build(),
                    () -> maxPage
            );
            this.reactionManager.addEventListener(handler);
        });
    }

    private static class Display {
        private String num;
        private String territoryName;
        private String acquired;
        private String heldTime;

        private Display(String num, String territoryName, String acquired, String heldTime) {
            this.num = num;
            this.territoryName = territoryName;
            this.acquired = acquired;
            this.heldTime = heldTime;
        }
    }

    private String formatPage(int page, List<Territory> territories, MessageReceivedEvent event, Date lastUpdate,
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
        for (int i = begin; i < end; i++) {
            Territory t = territories.get(i);
            displays.add(new Display(
                    String.valueOf(i + 1),
                    t.getName(),
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
            ret.add(String.format("%s.%s %s",
                    d.num, nSpaces(numJustify - d.num.length()),
                    d.territoryName
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

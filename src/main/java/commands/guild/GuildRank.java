package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import db.model.dateFormat.CustomDateFormat;
import db.model.territory.TerritoryRank;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
import db.repository.base.TerritoryRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import update.multipage.MultipageHandler;
import update.reaction.ReactionManager;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class GuildRank extends GenericCommand {
    private final TerritoryRepository territoryRepository;
    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;
    private final ReactionManager reactionManager;

    public GuildRank(Bot bot) {
        this.territoryRepository = bot.getDatabase().getTerritoryRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.reactionManager = bot.getReactionManager();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"rank", "r"}};
    }

    @Override
    public @NotNull String syntax() {
        return "g rank";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Guilds ranking by territory numbers.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("Guild Rank command help")
                .setDescription("Shows guilds ranking by number of territories each guild possesses.")
                .build()
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        Function<Integer, Message> pageSupplier = page -> getPage(page, customDateFormat, customTimeZone);
        if (maxPage() == 0) {
            respond(event, pageSupplier.apply(0));
            return;
        }

        respond(event, pageSupplier.apply(0), msg -> {
            MultipageHandler handler = new MultipageHandler(
                    msg, event.getAuthor().getIdLong(), pageSupplier, this::maxPage
            );
            this.reactionManager.addEventListener(handler);
        });
    }

    private static final int GUILDS_PER_PAGE = 10;

    private Message getPage(int page,
                            @NotNull CustomDateFormat customDateFormat,
                            @NotNull CustomTimeZone customTimeZone) {
        List<TerritoryRank> ranking = this.territoryRepository.getGuildTerritoryNumbers();
        Date lastAcquired = this.territoryRepository.getLatestAcquiredTime();
        if (ranking == null || lastAcquired == null) {
            return new MessageBuilder("Something went wrong while retrieving data...").build();
        }
        // should not probably happen
        if (ranking.isEmpty()) {
            return new MessageBuilder("No one seems to own any territories...?").build();
        }

        int justifyRank = ranking.stream().mapToInt(g -> String.valueOf(g.getRank()).length()).max().getAsInt();
        int justifyGuildName = ranking.stream().mapToInt(g -> g.getGuildName().length()).max().orElse(5);

        int totalTerritories = ranking.stream().mapToInt(TerritoryRank::getCount).sum();

        int min = page * GUILDS_PER_PAGE;
        int max = Math.min((page + 1) * GUILDS_PER_PAGE, ranking.size());

        List<String> ret = new ArrayList<>();
        ret.add("```ml");
        ret.add("---- Territory Rank ----");
        ret.add("");
        ret.add(String.format("%s Territories / %s Guilds", totalTerritories, ranking.size()));
        ret.add("");

        for (int i = min; i < max; i++) {
            TerritoryRank rank = ranking.get(i);
            ret.add(String.format(
                    "%s.%s %s%s - %s",
                    rank.getRank(), nSpaces(justifyRank - String.valueOf(rank.getRank()).length()),
                    rank.getGuildName(), nSpaces(justifyGuildName - rank.getGuildName().length()),
                    rank.getCount()
            ));
        }

        ret.add("");
        int maxPage = (ranking.size() - 1) / GUILDS_PER_PAGE;
        ret.add(String.format("< page %s / %s >", page + 1, maxPage + 1));
        ret.add("");

        DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());
        String formattedTime = String.format("%s (%s)", dateFormat.format(lastAcquired), customTimeZone.getFormattedTime());
        ret.add("territory last acquired at: " + formattedTime);

        ret.add("```");
        return new MessageBuilder(String.join("\n", ret)).build();
    }

    private static String nSpaces(int n) {
        return String.join("", Collections.nCopies(n, " "));
    }

    private int maxPage() {
        List<TerritoryRank> ranking = this.territoryRepository.getGuildTerritoryNumbers();
        return ranking != null ? (ranking.size() - 1) / GUILDS_PER_PAGE : 0;
    }
}

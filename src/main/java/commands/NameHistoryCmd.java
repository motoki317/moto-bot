package commands;

import api.mojang.MojangApi;
import api.mojang.structs.NameHistory;
import app.Bot;
import commands.base.GenericCommand;
import db.model.dateFormat.CustomDateFormat;
import db.model.timezone.CustomTimeZone;
import db.repository.base.DateFormatRepository;
import db.repository.base.TimeZoneRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import update.multipage.MultipageHandler;
import update.reaction.ReactionManager;
import utils.FormatUtils;
import utils.UUID;

import java.text.DateFormat;
import java.util.Date;
import java.util.stream.Collectors;

public class NameHistoryCmd extends GenericCommand {
    private final MojangApi mojangApi;
    private final TimeZoneRepository timeZoneRepository;
    private final DateFormatRepository dateFormatRepository;
    private final ReactionManager reactionManager;

    public NameHistoryCmd(Bot bot) {
        this.mojangApi = new MojangApi(bot.getLogger());
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.reactionManager = bot.getReactionManager();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"nameHistory", "names", "name"}};
    }

    @Override
    public @NotNull String syntax() {
        return "names <player name>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows a minecraft player's name history.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("Name History Command Help")
                .setDescription(this.shortHelp())
                .addField("Syntax", this.syntax(), false)
                .build()
        ).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(event, this.longHelp());
            return;
        }

        String playerName = args[1];
        UUID uuid;
        if (UUID.isUUID(playerName)) {
            uuid = new UUID(playerName);
        } else {
            uuid = this.mojangApi.mustGetUUIDAtTime(playerName, System.currentTimeMillis());
            if (uuid == null) {
                respond(event, String.format("Failed to retrieve player UUID for `%s`. " +
                        "Make sure the player exists and check your spelling.", playerName));
                return;
            }
        }

        NameHistory history = this.mojangApi.mustGetNameHistory(uuid);
        if (history == null) {
            respondError(event, String.format("Something went wrong while retrieving name history for player `%s`... " +
                    "(UUID: `%s`)", playerName, uuid.toStringWithHyphens()));
            return;
        }

        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);

        int maxPage = maxPage(history);

        if (maxPage == 0) {
            respond(event, formatPage(0, history, customDateFormat, customTimeZone));
            return;
        }

        respond(event, formatPage(0, history, customDateFormat, customTimeZone), message -> {
            MultipageHandler handler = new MultipageHandler(
                    message, event.getAuthor().getIdLong(),
                    page -> formatPage(page, history, customDateFormat, customTimeZone),
                    () -> maxPage
            );
            this.reactionManager.addEventListener(handler);
        });
    }

    private static final int HISTORIES_PER_PAGE = 5;

    private static int maxPage(NameHistory history) {
        // 1st page displaying all name, and other page displaying each history
        return (history.getHistory().size() - 1) / HISTORIES_PER_PAGE + 1;
    }

    private static Message formatPage(int page, NameHistory history,
                                      CustomDateFormat customDateFormat, CustomTimeZone customTimeZone) {
        if (page == 0) {
            return formatFirstPage(history);
        }

        // details page
        return formatDetailsPage(page - 1, history, customDateFormat, customTimeZone);
    }

    // Minecraft head avatar
    // this url + name or UUID without hyphens
    private static final String AVATAR_URL = "https://minotar.net/avatar/";

    private static Message formatFirstPage(NameHistory history) {
        String currentName = history.getHistory().get(
                history.getHistory().size() - 1
        ).getUsername();
        int maxPage = maxPage(history);

        String description = String.format("*%s had these name(s) in the past:* %s",
                currentName,
                history.getHistory().stream().map(h -> "**" + escapeUsername(h.getUsername()) + "**")
                .distinct().collect(Collectors.joining(", "))
        );

        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor(String.format("%s's Name History : Page [%s/%s]", currentName, 1, maxPage + 1),
                        null, AVATAR_URL + history.getUuid().toString())
                .setDescription(description)
                .setFooter("UUID: " + history.getUuid().toStringWithHyphens())
                .build()
        ).build();
    }

    // Escapes underscores in minecraft username for discord markup
    private static String escapeUsername(String username) {
        return username.replace("_", "\\_");
    }

    private static Message formatDetailsPage(int page, NameHistory history,
                                             CustomDateFormat customDateFormat, CustomTimeZone customTimeZone) {
        int begin = page * HISTORIES_PER_PAGE;
        int end = Math.min((page + 1) * HISTORIES_PER_PAGE, history.getHistory().size());

        String currentName = history.getHistory().get(
                history.getHistory().size() - 1
        ).getUsername();
        int maxPage = maxPage(history);

        EmbedBuilder eb = new EmbedBuilder();
        // details page num = given page num + 2
        eb.setAuthor(String.format("%s's Name History : Page [%s/%s]", currentName, page + 2, maxPage + 1),
                null, AVATAR_URL + history.getUuid().toString());
        eb.setDescription(String.format("Timezone: %s", customTimeZone.getFormattedTime()));

        DateFormat dateFormat = customDateFormat.getDateFormat().getMinuteFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());
        for (int i = begin; i < end; i++) {
            NameHistory.NameHistoryEntry e = history.getHistory().get(i);
            String startStr, endStr, rangeStr;
            if (e.getChangedToAt() == 0L) {
                startStr = "(Beginning)";
            } else {
                startStr = dateFormat.format(new Date(e.getChangedToAt()));
            }
            if (i == (history.getHistory().size() - 1)) {
                endStr = "(Now)";
            } else {
                NameHistory.NameHistoryEntry next = history.getHistory().get(i + 1);
                endStr = dateFormat.format(new Date(next.getChangedToAt()));
            }
            if (e.getChangedToAt() > 0L) {
                long changedToAt = i < (history.getHistory().size() - 1)
                        ? history.getHistory().get(i + 1).getChangedToAt()
                        : System.currentTimeMillis();
                long seconds = (changedToAt - e.getChangedToAt()) / 1000L;
                rangeStr = FormatUtils.formatReadableTime(seconds, false, "m");
            } else {
                rangeStr = "";
            }

            eb.addField(String.format("%s. %s", i + 1, e.getUsername()),
                    startStr + " ~ " + endStr + (rangeStr.isEmpty() ? "" : "\n" + rangeStr),
                    false
            );
        }

        eb.setFooter("UUID: " + history.getUuid().toStringWithHyphens());

        return new MessageBuilder(eb.build()).build();
    }
}

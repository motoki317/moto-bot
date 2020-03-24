package migrate;

import app.Bot;
import commands.base.GenericCommand;
import db.model.territoryLog.TerritoryLog;
import db.model.warLog.WarLog;
import db.model.warPlayer.WarPlayer;
import db.repository.base.TerritoryLogRepository;
import db.repository.base.WarLogRepository;
import log.Logger;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import utils.FormatUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WarsMigration extends GenericCommand {
    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"migrate"}, {"wars"}};
    }

    @Override
    public @NotNull String syntax() {
        return "migrate wars <bot user id> <channel id> <start message id> <end message id>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Migrates war tracking. Start and end message IDs are exclusive.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(this.shortHelp()).build();
    }

    private final Logger logger;
    private final ShardManager manager;
    private final WarLogRepository warLogRepository;
    private final TerritoryLogRepository territoryLogRepository;

    public WarsMigration(Bot bot) {
        this.logger = bot.getLogger();
        this.manager = bot.getManager();
        this.warLogRepository = bot.getDatabase().getWarLogRepository();
        this.territoryLogRepository = bot.getDatabase().getTerritoryLogRepository();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length < 6) {
            respond(event, "Invalid syntax: " + this.syntax());
            return;
        }

        long botUserId, channelId, startMessageId, endMessageId;
        try {
            botUserId = Long.parseLong(args[2]);
            channelId = Long.parseLong(args[3]);
            startMessageId = Long.parseLong(args[4]);
            endMessageId = Long.parseLong(args[5]);
        } catch (Exception e) {
            respondException(event, "Invalid syntax: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        TextChannel channel = this.manager.getTextChannelById(channelId);
        if (channel == null) {
            respond(event, "Channel with ID " + channelId + " not found");
            return;
        }

        int processed = 0;
        long begin = startMessageId;
        messageRetrieval:
        while (true) {
            MessageHistory history = channel.getHistoryAfter(begin, 100).complete();
            this.logger.debug(String.format("Retrieved %s message(s) after marker ID %s (exclusive).", history.size(), begin));
            if (history.isEmpty()) {
                respond(event, "Retrieved history at point " + begin + " was empty! Breaking the loop.");
                break;
            }
            // retrieve messages are from newest to oldest
            List<Message> messages = history.getRetrievedHistory();

            // process from oldest to newest
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message message = messages.get(i);
                if (endMessageId <= message.getIdLong()) {
                    respond(event, "Reached high point " + message.getIdLong() + ". Breaking the loop.");
                    break messageRetrieval;
                }
                if (message.getAuthor().getIdLong() != botUserId) {
                    this.logger.debug(String.format("Ignoring message ID %s, sent by %s, not sent by bot ID %s.",
                            message.getIdLong(), message.getAuthor().getIdLong(), botUserId));
                    continue;
                }
                processed++;
                try {
                    handleSingleLog(message);
                } catch (Exception e) {
                    respondError(event, "an exception occurred while handling single log: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }

            begin = messages.get(0).getIdLong();
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        respond(event, String.format("Processed %s message(s), finished migrating war / territory logs.", processed));
    }

    private static final DateFormat logFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private static final Pattern warLogPattern = Pattern.compile(
            "\\*\\*(WAR\\d{1,3})\\*\\* \\*(.{1,30})\\* → (.+)\\R" +
                    "\\s*Time: (\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}) ~ (\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2})"
    );

    private static final Pattern territoryLogPattern = Pattern.compile(
            "(.{1,100}): \\*(.{1,30})\\* \\((\\d+)\\) → \\*\\*(.{1,30})\\*\\* \\((\\d+)\\)\\R" +
                    "\\s*Territory held for\\s{1,2}((((\\d+ d\\s{1,2})?\\d+ h\\s{1,2})?\\d+ m\\s{1,2})?\\d+ s)\\R" +
                    "\\s*Acquired: (\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}) \\(([+\\-]\\d+)\\)"
    );

    // Handles single war or territory log message.
    private void handleSingleLog(Message message) throws Exception {
        String content = message.getContentRaw();
        if (warLogPattern.matcher(content).matches()) {
            handleWarLog(message);
            return;
        }
        if (territoryLogPattern.matcher(content).matches()) {
            handleTerritoryLog(message);
            return;
        }
        this.logger.debug(String.format("Ignoring message ID %s, matched neither war log or territory log pattern.\nContent: %s", message.getIdLong(), content));
    }

    private void handleWarLog(Message message) throws Exception {
        Matcher m = warLogPattern.matcher(message.getContentRaw());
        if (!m.matches()) {
            throw new Exception("No match");
        }

        // Retrieve data from regex group matches
        String serverName = m.group(1);
        String guildName = m.group(2);
        if (guildName.equals("(Unknown Guild)")) {
            guildName = null;
        }
        List<WarPlayer> players = Arrays.stream(m.group(3).split(", "))
                .map(s -> s.replace("\\", ""))
                .map(s -> {
                    boolean exited = s.startsWith("~~") && s.endsWith("~~");
                    String playerName = s.replace("~~", "");
                    return new WarPlayer(playerName, null, exited);
                }).collect(Collectors.toList());
        Date start = logFormat.parse(m.group(4));
        Date end = logFormat.parse(m.group(5));

        WarLog warLog = new WarLog(serverName, guildName, start, end, true, true, players);
        int id = this.warLogRepository.createAndGetLastInsertId(warLog);
        if (id == 0) {
            throw new Exception("Failed to insert war log to DB");
        }
        this.logger.debug(String.format("Created war log %s", id));
    }

    private void handleTerritoryLog(Message message) throws Exception {
        Matcher m = territoryLogPattern.matcher(message.getContentRaw());
        if (!m.matches()) {
            throw new Exception("No match");
        }

        // Retrieve data from regex group matches
        String territoryName = m.group(1);

        String oldGuild = m.group(2);
        int oldGuildTerrAmt = Integer.parseInt(m.group(3));

        String newGuild = m.group(4);
        int newGuildTerrAmt = Integer.parseInt(m.group(5));

        String heldTime = m.group(6);
        long timeDiff = FormatUtils.parseReadableTime(heldTime) * 1000L;
        String[] tzNames = TimeZone.getAvailableIDs(
                (int) (Integer.parseInt(m.group(11)) * TimeUnit.HOURS.toMillis(1))
        );
        if (tzNames.length == 0) {
            throw new Exception("Failed to retrieve timezone for " + m.group(11));
        }
        logFormat.setTimeZone(TimeZone.getTimeZone(tzNames[0]));
        Date acquired = logFormat.parse(m.group(10));

        TerritoryLog territoryLog = new TerritoryLog(territoryName, oldGuild, newGuild, oldGuildTerrAmt, newGuildTerrAmt, acquired, timeDiff);
        if (this.territoryLogRepository.create(territoryLog)) {
            this.logger.debug("Created territory log");
        } else {
            throw new Exception("Failed to insert territory log to DB");
        }
    }
}

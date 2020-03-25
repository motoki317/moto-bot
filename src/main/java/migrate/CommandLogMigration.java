package migrate;

import app.Bot;
import commands.base.GenericCommand;
import db.model.commandLog.CommandLog;
import db.repository.base.CommandLogRepository;
import log.Logger;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLogMigration extends GenericCommand {
    private final Logger logger;
    private final ShardManager manager;
    private final CommandLogRepository commandLogRepository;

    public CommandLogMigration(Bot bot) {
        this.logger = bot.getLogger();
        this.manager = bot.getManager();
        this.commandLogRepository = bot.getDatabase().getCommandLogRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"migrate"}, {"commandLog"}};
    }

    @Override
    public @NotNull String syntax() {
        return "migrate commandLog <bot user id> <channel id> <start message id> <end message id>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Migrates command logs";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(this.shortHelp()).build();
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
                Thread.sleep(TimeUnit.MILLISECONDS.toMillis(250));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        respond(event, String.format("Processed %s message(s), finished migrating command logs.", processed));
    }

    private static final Pattern commandLogPatternOld = Pattern.compile(
            "(\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3}) \\((.+)\\)\\[(.+)]<(.+)>: (.+)"
    );

    private static final Pattern commandLogPattern = Pattern.compile(
            "(\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\((.+)\\)\\[(.+)]<(.+)>: `(.+)`"
    );

    private void handleSingleLog(Message message) throws Exception {
        String content = message.getContentRaw();
        Matcher m;
        m = commandLogPattern.matcher(content);
        if (m.matches()) {
            saveCommandLog(m);
            return;
        }
        m = commandLogPatternOld.matcher(content);
        if (m.matches()) {
            saveCommandLog(m);
            return;
        }

        this.logger.debug(String.format("Skipping message ID %s, did not match command log pattern.\n" +
                "Content: %s", message.getIdLong(), content));
    }

    private final static DateFormat logFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    static {
        logFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
    }

    private void saveCommandLog(Matcher m) throws Exception {
        Date createdAt = logFormat.parse(m.group(1));
        // group 2 -> guild name, 3 -> channel name, 4 -> user name
        // but logging as all "0" here because the actual discord IDs are unknown
        String full = m.group(5);
        String kind = getKind(full);
        if (kind == null) {
            return;
        }

        CommandLog log = new CommandLog(kind, full, 0L, 0L, 0L, createdAt);
        if (!this.commandLogRepository.create(log)) {
            throw new Exception("Failed to save to DB");
        }
    }

    private static final int MAX_PREFIX_LENGTH = 3;
    private static final int MAX_COMMAND_LENGTH = 2;
    private static final Set<String> commandKinds = new HashSet<>();

    static {
        commandKinds.addAll(Arrays.asList(
                "help", "h",
                "info",
                "ping",
                "wynnhorse", "horse",
                "i", "item", "ib", "itembase",
                "id", "identify", "idb", "identifybase",
                "dps", "dpsb",
                "idlist",
                "servertrack",
                "g", "guild",
                // Guild commands
                "g terrtrack", "guild terrtrack",
                "g territorytrack", "guild territorytrack",
                "g wartrack", "guild wartrack",
                "g playerwartrack", "guild playerwartrack",
                "g pwt", "guild pwt",
                "g terr", "guild terr",
                "g territory", "guild territory",
                "g tlist", "guild territorylist",
                "g warstats", "guild warstats",
                "g wstats", "guild wstats",
                "g ws", "guild ws",
                "g pwstats", "guild pwstats",
                "g pwarstats", "guild pwarstats",
                "g playerwstats", "guild playerwstats",
                "g playerwarstats", "guild playerwarstats",
                "g pws", "guild pws",
                "g stats", "guild stats",
                "g s", "guild s",
                "g info", "guild info",
                "g i", "guild i",
                "g rank", "guild rank",
                "g r", "guild r",
                "g levelrank", "guild levelrank",
                "g lrank", "guild lrank",
                "g lr", "guild lr",
                "g territoryactivity", "guild territoryactivity",
                "g terractivity", "guild terractivity",
                "g ta", "guild ta",
                "g war", "guild war",
                "g gainedxp", "guild gainedxp",
                "g gainedexp", "guild gainedexp",
                "g gainedxprank", "guild gainedxprank",
                "g gainedexprank", "guild gainedexprank",
                "g xp", "guild xp",
                "g ml", "guild ml",
                "g customterritorylist", "guild customterritorylist",
                "g mylist", "guild mylist",
                "g cl", "guild cl",
                "g customguildlist", "guild customguildlist",
                "g customlist", "guild customlist",
                "g custom", "guild custom",
                "g warleaderboard", "guild warleaderboard",
                "g wlb", "guild wlb",
                "g warlb", "guild warlb",
                "g leaderboard", "guild leaderboard",
                "g lb", "guild lb",
                "g playerwarleaderboard", "guild playerwarleaderboard",
                "g playerleaderboard", "guild playerleaderboard",
                "g pwlb", "guild pwlb",
                "g plb", "guild plb",
                "g help", "guilg help",
                "g h", "guild h",
                // End guild commands
                "gstats", "gs",
                "save",
                "prefix", "timezone", "timeformat",
                "up", "upserver", "serverlist", "servers",
                "idsearch",
                "debug", "d",
                "find",
                "stats", "pstats",
                "friend",
                "purge",
                "notify",
                "onlinemods", "onlinemod", "om", "mod", "mods",
                "perms", "perm",
                "itemsearch", "is",
                "idinfo",
                "serverlog",
                "m", "music",
                // Music commands
                "m help", "music help",
                "m h", "music h",
                "m join", "music join",
                "m play", "music play",
                "m p", "music p",
                "m playall", "music playall",
                "m pa", "music pa",
                "m soundcloud", "music soundcloud",
                "m sc", "music sc",
                "m inputstream", "music inputstream",
                "m is", "music is",
                "m direct", "music direct",
                "m animeradio", "music animeradio",
                "m anime", "music anime",
                "m ar", "music ar",
                "m skip", "music skip",
                "m s", "music s",
                "m clear", "music clear",
                "m c", "music c",
                "m stop", "music stop",
                "m st", "music st",
                "m leave", "music leave",
                "m shuffle", "music shuffle",
                "m sh", "music sh",
                "m sf", "music sf",
                "m nowplaying", "music nowplaying",
                "m np", "music np",
                "m now", "music now",
                "m n", "music n",
                "m purgequeue", "music purgequeue",
                "m purge", "music purge",
                "m queue", "music queue",
                "m q", "music q",
                "m list", "music list",
                "m repeat", "music repeat",
                "m rp", "music rp",
                "m r", "music r",
                "m volume", "music volume",
                "m v", "music v",
                "m pause", "music pause",
                "m resume", "music resume",
                "m seek", "music seek",
                "m settings", "music settings",
                "m debug", "music debug",
                "m d", "music d",
                // End music commands
                "cat",
                "page",
                "online", "count",
                "metric", "metrics",
                "leaderboard", "playerleaderboard", "lb", "plb",
                "name", "names", "namehistory"
        ));
    }

    @Nullable
    private String getKind(String full) {
        for (int i = 1; i <= MAX_PREFIX_LENGTH; i++) {
            if (full.length() < i) {
                return full;
            }

            String content = full.substring(i);
            String[] args = content.split(" ");
            for (int j = MAX_COMMAND_LENGTH; j >= 1; j--) {
                if (args.length < j) {
                    continue;
                }

                String kindCandidate = String.join(" ", Arrays.copyOfRange(args, 0, j)).toLowerCase();
                if (commandKinds.contains(kindCandidate)) {
                    return kindCandidate;
                }
            }
        }

        // unknown prefix / command type
        this.logger.debug(String.format("Failed to check prefix / command type for message: %s", full));
        return null;
    }
}

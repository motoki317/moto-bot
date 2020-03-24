package heartbeat.tasks;

import api.mojang.MojangApi;
import api.mojang.structs.NameHistory;
import app.Bot;
import db.model.warLog.WarLog;
import db.model.warPlayer.WarPlayer;
import db.repository.base.WarLogRepository;
import db.repository.base.WarPlayerRepository;
import heartbeat.base.TaskBase;
import log.Logger;
import utils.UUID;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Retrieves null player UUIDs in war player table, and fills them by requesting Mojang API.
 */
public class PlayerUUIDRetriever implements TaskBase {
    private final MojangApi mojangApi;
    private final Logger logger;
    private final WarPlayerRepository warPlayerRepository;
    private final WarLogRepository warLogRepository;

    public PlayerUUIDRetriever(Bot bot) {
        this.mojangApi = new MojangApi(bot.getLogger());
        this.logger = bot.getLogger();
        this.warPlayerRepository = bot.getDatabase().getWarPlayerRepository();
        this.warLogRepository = bot.getDatabase().getWarLogRepository();
    }

    @Override
    public void run() {
        while (true) {
            // Pick one player whose logged UUID is null
            WarPlayer warPlayer = this.warPlayerRepository.getUUIDNullPlayer();
            if (warPlayer == null) {
                break;
            }
            WarLog warLog = this.warLogRepository.findOne(warPlayer::getWarLogId);
            if (warLog == null) {
                break;
            }

            UUID uuid = this.mojangApi.mustGetUUIDAtTime(warPlayer.getPlayerName(), warLog.getCreatedAt().getTime());
            if (uuid == null) {
                this.logger.log(0, String.format(
                        "Player UUID Retriever: Failed to get UUID for %s at %s, skipping.",
                        warPlayer.getPlayerName(), warLog.getCreatedAt().getTime()));
                continue;
            }

            // Retrieve name history for them, and fill player uuid fields
            NameHistory history = this.mojangApi.mustGetNameHistory(uuid);
            if (history == null) {
                this.logger.log(0, String.format(
                        "Player UUID Retriever: Failed to get name history for %s, skipping.",
                        uuid.toStringWithHyphens()));
                continue;
            }

            this.fillPlayerUUIDs(history);

            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(2));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void fillPlayerUUIDs(NameHistory nameHistory) {
        List<NameHistory.NameHistoryEntry> history = nameHistory.getHistory();
        UUID uuid = nameHistory.getUuid();
        for (int i = 0; i < history.size(); i++) {
            long start, end;
            start = history.get(i).getChangedToAt();
            if (i == history.size() - 1) {
                end = System.currentTimeMillis();
            } else {
                end = history.get(i + 1).getChangedToAt();
            }
            String username = history.get(i).getUsername();

            // update corresponding entries
            if (!this.warPlayerRepository.getPlayerNameOfBetween(username, uuid, new Date(start), new Date(end))) {
                this.logger.log(0, "Player UUID Retriever: Failed to update DB");
            }
        }
    }

    @Override
    public long getFirstDelay() {
        return TimeUnit.MINUTES.toMillis(3);
    }

    @Override
    public long getInterval() {
        return TimeUnit.HOURS.toMillis(1);
    }
}

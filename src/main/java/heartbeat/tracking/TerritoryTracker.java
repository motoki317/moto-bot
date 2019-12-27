package heartbeat.tracking;

import api.WynnApi;
import api.structs.TerritoryList;
import app.Bot;
import db.model.territory.Territory;
import db.repository.TerritoryLogRepository;
import db.repository.TerritoryRepository;
import log.Logger;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TerritoryTracker {
    private final Logger logger;
    private final Object dbLock;
    private final WynnApi wynnApi;
    private final TerritoryRepository territoryRepository;
    private final TerritoryLogRepository territoryLogRepository;

    public TerritoryTracker(Bot bot, Object dbLock) {
        this.logger = bot.getLogger();
        this.dbLock = dbLock;
        this.wynnApi = new WynnApi(this.logger, bot.getProperties().wynnTimeZone);
        this.territoryRepository = bot.getDatabase().getTerritoryRepository();
        this.territoryLogRepository = bot.getDatabase().getTerritoryLogRepository();
    }

    public void run() {
        TerritoryList territoryList = this.wynnApi.getTerritoryList();
        if (territoryList == null) return;

        List<Territory> territories = new ArrayList<>();
        for (Map.Entry<String, api.structs.Territory> e : territoryList.getTerritories().entrySet()) {
            try {
                territories.add(e.getValue().convert());
            } catch (ParseException ex) {
                this.logger.logException("an error occurred in territory tracker", ex);
                return;
            }
        }

        int oldLastId;
        int newLastId;
        synchronized (this.dbLock) {
            oldLastId = this.territoryLogRepository.lastInsertId();

            // Update DB
            if (!this.territoryRepository.updateAll(territories)) {
                this.logger.log(0, "Territory tracker: failed to update db");
                return;
            }

            newLastId = this.territoryLogRepository.lastInsertId();
        }

        this.handleTracking(oldLastId, newLastId);
    }

    /**
     * Do territory tracking. Sends all territory_log from oldLastId (exclusive) to newLastId (inclusive).
     * @param oldLastId Last max id in territory_log before db update.
     * @param newLastId Current max id in territory_log table after db update.
     */
    private void handleTracking(int oldLastId, int newLastId) {
        // TODO: territory tracking
    }
}

package heartbeat.tracking;

import api.WynnApi;
import api.structs.TerritoryList;
import app.Bot;
import db.model.territory.Territory;
import db.repository.TerritoryRepository;
import log.Logger;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TerritoryTracker {
    private final Logger logger;
    private final WynnApi wynnApi;
    private final TerritoryRepository territoryRepository;

    public TerritoryTracker(Bot bot) {
        this.logger = bot.getLogger();
        this.wynnApi = new WynnApi(this.logger, bot.getProperties().wynnTimeZone);
        this.territoryRepository = bot.getDatabase().getTerritoryRepository();
    }

    public void run() {
        TerritoryList territoryList = this.wynnApi.getTerritoryList();
        if (territoryList == null) {
            this.logger.log(0, "an error occurred in territory tracker (couldn't retrieve territory list)");
            return;
        }
        List<Territory> territories = new ArrayList<>();
        for (Map.Entry<String, api.structs.Territory> e : territoryList.getTerritories().entrySet()) {
            try {
                territories.add(e.getValue().convert());
            } catch (ParseException ex) {
                this.logger.logException("an error occurred in territory tracker", ex);
                return;
            }
        }

        boolean res = this.territoryRepository.updateAll(territories);

        if (!res) {
            this.logger.log(0, "an error occurred in territory tracker (updating db)");
        }
    }
}

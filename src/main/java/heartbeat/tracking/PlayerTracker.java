package heartbeat.tracking;

import app.Bot;

public class PlayerTracker {
    private final Bot bot;

    public PlayerTracker(Bot bot) {
        this.bot = bot;
    }

    public void run() {
        bot.getLogger().log(-1, "Player tracker running");
        try {
            Thread.sleep(1000 + (int) (Math.random() * 3000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

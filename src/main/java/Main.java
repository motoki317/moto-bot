import app.App;

public class Main {
    public static void main(String[] args) {
        Runnable app;
        try {
            app = new App();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Failed to initialize app");
        }
        Thread appThread = new Thread(app);
        appThread.setName("moto-bot app");
        appThread.start();
    }
}

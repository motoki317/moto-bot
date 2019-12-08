import app.App;

public class Main {
    public static void main(String[] args) {
        Runnable app = null;
        try {
            app = new App();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        Thread appThread = new Thread(app);
        appThread.setName("moto-bot app");
        appThread.start();
    }
}

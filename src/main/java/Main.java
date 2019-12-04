import app.App;

public class Main {
    public static void main(String[] args) {
        App app = new App();
        Thread appThread = new Thread(app);
        appThread.start();
    }
}

import app.App;
import app.Properties;
import update.UpdaterFactoryImpl;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, LoginException {
        App app = new App(new Properties(), new UpdaterFactoryImpl());
        Thread appThread = new Thread(app);
        appThread.setName("moto-bot app");
        appThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(app::onShutDown));
    }
}

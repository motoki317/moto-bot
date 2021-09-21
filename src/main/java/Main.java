import app.App;
import app.Properties;
import io.prometheus.client.exporter.HTTPServer;
import update.UpdaterFactoryImpl;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, LoginException {
        Properties props = new Properties();
        App app = new App(props, new UpdaterFactoryImpl());
        Thread appThread = new Thread(app);
        appThread.setName("moto-bot app");
        appThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(app::onShutDown));

        HTTPServer server = new HTTPServer.Builder().withPort(props.port).build();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    }
}

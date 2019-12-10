package app;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.BotData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataLoader {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String FILE_PATH;

    static {
        Path root = FileSystems.getDefault().getPath("").toAbsolutePath();
        Path filePath = Paths.get(root.toString(), "src", "main", "resources", "data.json");
        FILE_PATH = filePath.toString();
    }

    private final BotData data;

    DataLoader() throws IOException {
        File dataFile = new File(FILE_PATH);
        if (dataFile.exists()) {
            this.data = mapper.readValue(dataFile, BotData.class);
            this.data.ensureNonNull();
        } else {
            this.data = new BotData();
        }
    }

    public BotData getData() {
        return data;
    }

    void saveData() throws IOException {
        File dataFile = new File(FILE_PATH);
        if (!dataFile.exists()) {
            boolean success = dataFile.createNewFile();
            if (!success) {
                throw new IOException("failed to create file " + FILE_PATH);
            }
        }

        String dataString = mapper.writeValueAsString(this.data);
        FileOutputStream writer = new FileOutputStream(dataFile, false);
        writer.write(dataString.getBytes());
        writer.close();
    }
}

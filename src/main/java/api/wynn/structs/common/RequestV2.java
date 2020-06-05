package api.wynn.structs.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.Map;

public class RequestV2 {
    private static final ObjectMapper mapper = new ObjectMapper();

    private String kind;
    private int code;
    private long timestamp;
    private String version;

    public RequestV2(String body) throws JsonProcessingException {
        JsonNode json = mapper.readTree(body);

        for (Iterator<Map.Entry<String, JsonNode>> i = json.fields(); i.hasNext(); ) {
            Map.Entry<String, JsonNode> e = i.next();

            switch (e.getKey()) {
                case "kind":
                    this.kind = e.getValue().asText();
                    break;
                case "code":
                    this.code = e.getValue().asInt();
                    break;
                case "timestamp":
                    this.timestamp = e.getValue().asLong();
                    break;
                case "version":
                    this.version = e.getValue().asText();
                    break;
            }
        }

        if (this.kind == null || this.code == 0 || this.timestamp == 0L || this.version == null) {
            throw new RuntimeException("Some fields are missing in response");
        }
    }

    public String getKind() {
        return kind;
    }

    public int getCode() {
        return code;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getVersion() {
        return version;
    }
}

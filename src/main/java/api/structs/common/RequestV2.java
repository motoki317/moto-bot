package api.structs.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.Map;

public class RequestV2 {
    private static final ObjectMapper mapper = new ObjectMapper();

    private String kind;
    private int code;
    private String message;
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
                case "message":
                    this.message = e.getValue().asText();
                    break;
                case "timestamp":
                    this.timestamp = e.getValue().asLong();
                case "version":
                    this.version = e.getValue().asText();
                    break;
            }
        }
    }

    public String getKind() {
        return kind;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getVersion() {
        return version;
    }
}

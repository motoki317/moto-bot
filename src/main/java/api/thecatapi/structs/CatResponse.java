package api.thecatapi.structs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(value = {"breeds", "id", "width", "height"}, ignoreUnknown = true)
public class CatResponse {
    private String url;

    public String getUrl() {
        return url;
    }
}

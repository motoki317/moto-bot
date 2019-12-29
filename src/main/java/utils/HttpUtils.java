package utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nullable;
import java.io.IOException;

public class HttpUtils {
    /**
     * Sends GET request to specified URL.
     * @param url URL string.
     * @return Response body. Null if something went wrong.
     * @throws IOException On connection issues & status code other than 2xx was returned.
     */
    @Nullable
    public static String get(String url) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            return client.execute(request, defaultResponseHandler());
        }
    }

    /**
     * Sends POST request to specified URL.
     * @param url URL string.
     * @param body Post body. Used to post with header "Content-Type: application/json".
     * @return Response body. Null if something went wrong.
     * @throws IOException On connection issues & status code other than 2xx was returned.
     */
    @Nullable
    public static String postJson(String url, String body) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
            return client.execute(request, defaultResponseHandler());
        }
    }

    private static ResponseHandler<String> defaultResponseHandler() {
        return response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };
    }
}

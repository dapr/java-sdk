package io.dapr.examples.state.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.System.out;

/**
 * OrderManager web app.
 * 
 * Based on the helloworld Node.js example in https://github.com/dapr/samples/blob/master/1.hello-world/app.js
 *
 * To install jars into your local maven repo:
 *   mvn clean install
 *
 * To run (after step above):
 *   dapr run --app-id orderapp --app-port 3000 --port 3500 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.state.http.OrderManager
 *
 * If this class changes, run this before running it again:
 *   mvn compile
 */
public class OrderManager {

    static HttpClient httpClient;

    public static void main(String[] args) throws IOException {
        int httpPort = 3000;
        String daprPort = Optional.ofNullable(System.getenv("DAPR_HTTP_PORT")).orElse("3500");
        String stateUrl = String.format("http://localhost:%s/v1.0/state", daprPort);
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);

        httpClient = HttpClient.newBuilder().version(Version.HTTP_1_1).followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(2)).build();

        httpServer.createContext("/order").setHandler(e -> {
            out.println("Fetching order!");
            fetch(stateUrl + "/order").thenAccept(response -> {
                int resCode = response.statusCode() == 200 ? 200 : 500;
                String body = response.statusCode() == 200 ? response.body() : "Could not get state.";

                try {
                    e.sendResponseHeaders(resCode, body.getBytes().length);
                    OutputStream os = e.getResponseBody();
                    try {
                        os.write(body.getBytes());
                    } finally {
                        os.close();
                    }
                } catch (IOException ioerror) {
                    out.println(ioerror);
                }
            });
        });

        httpServer.createContext("/neworder").setHandler(e -> {
            try {
                out.println("Received new order ...");
                String json = readBody(e);
                JSONObject jsonObject = new JSONObject(json);
                JSONObject data = jsonObject.getJSONObject("data");
                String orderId = data.getString("orderId");
                out.printf("Got a new order! Order ID: %s\n", orderId);

                JSONObject item = new JSONObject();
                item.put("key", "order");
                item.put("value", data);
                JSONArray state = new JSONArray();
                state.put(item);
                out.printf("Writing to state: %s\n", state.toString());

                post(stateUrl, state.toString()).thenAccept(response -> {
                    int resCode = response.statusCode() == 200 ? 200 : 500;
                    String body = response.body();
                    try {
                        e.sendResponseHeaders(resCode, body.getBytes().length);
                        OutputStream os = e.getResponseBody();
                        try {
                            os.write(body.getBytes());
                        } finally {
                            os.close();
                        }
                    } catch (IOException ioerror) {
                        out.println(ioerror);
                    }
                });
            } catch (IOException ioerror) {
                out.println(ioerror);
            }
        });

        httpServer.start();
        out.printf("Java App listening on port %s.", httpPort);
    }

    private static CompletableFuture<HttpResponse<String>> fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return httpClient.sendAsync(request, BodyHandlers.ofString());
    }

    private static CompletableFuture<HttpResponse<String>> post(String url, String body) {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "application/json; charset=UTF-8").POST(BodyPublishers.ofString(body)).build();

        return httpClient.sendAsync(request, BodyHandlers.ofString());
    }

    private static String readBody(HttpExchange t) throws IOException {
        // retrieve the request json data
        InputStream is = t.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = is.read(buffer)) > 0)
                bos.write(buffer, 0, len);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bos.close();
        }
        return new String(bos.toByteArray(), Charset.forName("UTF-8"));
    }
}

package io.dapr.examples;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import static java.lang.System.out;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.json.JSONObject;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

/**
 * OrderManager web app.
 * 
 * Based on the helloworld Node.js example in https://github.com/dapr/samples/blob/master/1.hello-world/app.js
 */
public class OrderManager {

    static HttpClient httpClient;

    public static void main(String[] args) throws IOException {
        var httpPort = 8080;
        var daprPort = Optional.ofNullable(System.getenv("DAPR_HTTP_PORT")).orElse("3500");
        var stateUrl = String.format("http://localhost:%s/v1.0/state", daprPort);
        var httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);

        httpClient = HttpClient.newBuilder().version(Version.HTTP_1_1).followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(2)).build();

        httpServer.createContext("/order").setHandler(e -> {
            fetch(stateUrl + "/order").thenAccept(response -> {
                var resCode = response.statusCode() == 200 ? 200 : 500;
                var body = response.statusCode() == 200 ? response.body() : "Could not get state.";

                try {
                    e.sendResponseHeaders(resCode, body.getBytes().length);
                    try (var os = e.getResponseBody()) {
                        os.write(body.getBytes());
                    }
                } catch (IOException ioerror) {
                    out.println(ioerror);
                }
            });
        });

        httpServer.createContext("/neworder").setHandler(e -> {
            var json = readBody(e);
            var jsonObject = new JSONObject(json);
            var orderId = jsonObject.getString("orderId");
            out.printf("Got a new order! Order ID: %s", orderId);

            var state = new JSONObject();
            state.put("key", "order");
            state.put("value", jsonObject);

            post(stateUrl + "/neworder", state.toString()).thenAccept(response -> {
                var resCode = response.statusCode() == 200 ? 200 : 500;
                var body = response.statusCode() == 200 ? "" : "Failed to persist state.";
                try {
                    e.sendResponseHeaders(resCode, body.getBytes().length);
                    try (var os = e.getResponseBody()) {
                        os.write(body.getBytes());
                    }
                } catch (IOException ioerror) {
                    out.println(ioerror);
                }
            });
        });

        httpServer.start();
        out.printf("Java App listening on port %s.", httpPort);
    }

    private static CompletableFuture<HttpResponse<String>> fetch(String url) {
        var request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return httpClient.sendAsync(request, BodyHandlers.ofString());
    }

    private static CompletableFuture<HttpResponse<String>> post(String url, String body) {
        var request = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Content-Type", "application/json; charset=UTF-8").POST(BodyPublishers.ofString(body)).build();

        return httpClient.sendAsync(request, BodyHandlers.ofString());
    }

    private static String readBody(HttpExchange t) {
        // retrieve the request json data
        var is = t.getRequestBody();
        var bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        try (bos) {
            while ((len = is.read(buffer)) > 0)
                bos.write(buffer, 0, len);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(bos.toByteArray(), Charset.forName("UTF-8"));
    }
}

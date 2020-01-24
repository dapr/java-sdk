package io.dapr.examples.state.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.serializer.DefaultObjectSerializer;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.System.out;

/**
 * OrderManager web app.
 * <p>
 * Based on the helloworld Node.js example in https://github.com/dapr/samples/blob/master/1.hello-world/app.js
 * <p>
 * To install jars into your local maven repo:
 * mvn clean install
 * <p>
 * To run (after step above):
 * dapr run --app-id orderapp --app-port 3000 --port 3500 -- mvn exec:java -pl=examples -Dexec.mainClass=io.dapr.examples.state.http.OrderManager
 * <p>
 * If this class changes, run this before running it again:
 * mvn compile
 */
public class OrderManager {

  public static void main(String[] args) throws IOException {
    int httpPort = 3001;
    HttpServer httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);

    DaprClient daprClient =
      (new DaprClientBuilder(new DefaultObjectSerializer(), new DefaultObjectSerializer())).build();

    httpServer.createContext("/order").setHandler(e -> {
      out.println("Fetching order!");
        try {
          byte[] data = daprClient.getState("order", String.class).block().getValue().getBytes();
          e.getResponseHeaders().set("content-type", "application/json");
          e.sendResponseHeaders(200, data.length);
          e.getResponseBody().write(data);
          e.getResponseBody().close();
        } catch (IOException ioerror) {
          out.println(ioerror);
          e.sendResponseHeaders(500, ioerror.getMessage().getBytes().length);
          e.getResponseBody().write(ioerror.getMessage().getBytes());
          e.getResponseBody().close();
        }
    });

    httpServer.createContext("/neworder").setHandler(e -> {
      try {
        out.println("Received new order ...");
        String json = readBody(e);
        JSONObject jsonObject = new JSONObject(json);
        JSONObject data = jsonObject.getJSONObject("data");
        String orderId = data.getString("orderId");
        out.printf("Got a new order! Order ID: %s\n", orderId);

        daprClient.saveState("order", data.toString()).block();

        out.printf("Saved state: %s\n", data.toString());
        e.sendResponseHeaders(200, 0);
        e.getResponseBody().write(new byte[0]);
        e.getResponseBody().close();
      } catch (IOException ioerror) {
        out.println(ioerror);
        e.sendResponseHeaders(500, ioerror.getMessage().getBytes().length);
        e.getResponseBody().write(ioerror.getMessage().getBytes());
        e.getResponseBody().close();
      }
    });

    httpServer.start();
    out.printf("Java App listening on port %s.", httpPort);
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

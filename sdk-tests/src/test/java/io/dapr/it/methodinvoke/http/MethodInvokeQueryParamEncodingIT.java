package io.dapr.it.methodinvoke.http;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprHttp;
import io.dapr.client.domain.HttpExtension;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MethodInvokeQueryParamEncodingIT extends BaseIT {

  private DaprRun daprRun = null;

  @BeforeEach
  public void init() throws Exception {
    daprRun = startDaprApp(
        MethodInvokeQueryParamEncodingIT.class.getSimpleName() + "http",
        MethodInvokeService.SUCCESS_MESSAGE,
        MethodInvokeService.class,
        true,
        30000);
    daprRun.waitForAppHealth(20000);
  }

  @Test
  public void testInvokeQueryParams() throws Exception {
    try (DaprClient client = daprRun.newDaprClientBuilder().build()) {
      client.waitForSidecar(10000).block();

      String uri = "abc/pqr";
      Map<String, List<String>> queryParams = Map.of("uri", List.of(uri));
      HttpExtension httpExtension = new HttpExtension(DaprHttp.HttpMethods.GET, queryParams, Map.of());
      String result = client.invokeMethod(daprRun.getAppName(), "query", null,
          httpExtension, String.class).block();

      assertEquals(uri, result);
    }
  }

}

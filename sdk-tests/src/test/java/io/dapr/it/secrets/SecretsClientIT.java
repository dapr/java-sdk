/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.it.secrets;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.DaprClientGrpc;
import io.dapr.client.DaprClientHttp;
import io.dapr.it.BaseIT;
import io.dapr.it.DaprRun;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Test Secrets Store APIs using Harshicorp's vault.
 *
 * 1. Start harshicorp vault locally:
 *  docker run --cap-add=IPC_LOCK -e 'VAULT_DEV_ROOT_TOKEN_ID=myroot' --name=dev-vault -p 8200:8200 -d vault
 * 2. Create token file (path defined in integration test's vault.yaml):
 *   echo myroot > /tmp/.hashicorp_vault_token
 */
@RunWith(Parameterized.class)
public class SecretsClientIT extends BaseIT {

  private static final String LOCAL_VAULT_ADDRESS = "http://127.0.0.1:8200";

  private static final String LOCAL_VAULT_TOKEN = "myroot";

  private static final String PREFIX = "dapr";

  private static final String SECRETS_STORE_NAME = "vault";

  private static DaprRun daprRun;

  private static Vault vault;

  /**
   * Parameters for this test.
   * Param #1: useGrpc.
   * @return Collection of parameter tuples.
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { { false }, { true } });
  }

  @Parameterized.Parameter
  public boolean useGrpc;

  private DaprClient daprClient;

  @BeforeClass
  public static void init() throws Exception {
    daprRun = startDaprApp(SecretsClientIT.class.getSimpleName(), 5000);

    VaultConfig vaultConfig = new VaultConfig()
      .address(LOCAL_VAULT_ADDRESS)
      .token(LOCAL_VAULT_TOKEN)
      .prefixPath(PREFIX)
      .build();
    vault = new Vault(vaultConfig);
  }

  @Before
  public void setup() throws Exception {
    if (this.useGrpc) {
      daprRun.switchToGRPC();
    } else {
      daprRun.switchToHTTP();
    }

    this.daprClient = new DaprClientBuilder().build();
    if (this.useGrpc) {
      assertEquals(DaprClientGrpc.class, this.daprClient.getClass());
    } else {
      assertEquals(DaprClientHttp.class, this.daprClient.getClass());
    }
  }

  @After
  public void tearDown() throws IOException {
    daprClient.close();
  }

  @Test
  public void getSecret() throws Exception {
    String key = UUID.randomUUID().toString();
    String attributeKey = "title";
    String attributeValue = "The Metrics IV";
    writeSecret(key, attributeKey, attributeValue);

    Map<String, String> data = daprClient.getSecret(SECRETS_STORE_NAME, key).block();
    assertEquals(1, data.size());
    assertEquals("The Metrics IV", data.get("title"));
  }

  @Test(expected = RuntimeException.class)
  public void getSecretKeyNotFound() {
    daprClient.getSecret(SECRETS_STORE_NAME, "unknownKey").block();
  }

  @Test(expected = RuntimeException.class)
  public void getSecretStoreNotFound() throws Exception {
    daprClient.getSecret("unknownStore", "unknownKey").block();
  }

  private static void writeSecret(String secretName, String key, String value) throws Exception {
    Map<String, Object> secrets = Collections.singletonMap(key, value);
    vault.logical().write("secret/" + PREFIX + "/" + secretName, secrets);
  }

}

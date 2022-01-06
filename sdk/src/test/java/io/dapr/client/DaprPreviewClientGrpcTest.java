/*
 * Copyright (c) Microsoft Corporation and Dapr Contributors.
 * Licensed under the MIT License.
 */

package io.dapr.client;


import io.dapr.client.domain.ConfigurationItem;
import io.dapr.config.Properties;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static io.dapr.utils.TestUtils.findFreePort;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DaprPreviewClientGrpcTest {

	private static final String CONFIG_STORE_NAME = "MyConfigStore";

	private Closeable closeable;
	private DaprGrpc.DaprStub daprStub;
	private DaprPreviewClient previewClient;
	private ObjectSerializer serializer;

	@Before
	public void setup() throws IOException {
		closeable = mock(Closeable.class);
		daprStub = mock(DaprGrpc.DaprStub.class);
		when(daprStub.withInterceptors(any())).thenReturn(daprStub);
		previewClient = new DaprClientGrpc(
				closeable, daprStub, new DefaultObjectSerializer(), new DefaultObjectSerializer());
		serializer = new ObjectSerializer();
		doNothing().when(closeable).close();
	}

	@After
	public void tearDown() throws Exception {
		previewClient.close();
		verify(closeable).close();
		verifyNoMoreInteractions(closeable);
	}

	@Test
	public void waitForSidecarTimeout() throws Exception {
		int port = findFreePort();
		System.setProperty(Properties.GRPC_PORT.getName(), Integer.toString(port));
		assertThrows(RuntimeException.class, () -> previewClient.waitForSidecar(1).block());
	}

	@Test
	public void waitForSidecarTimeoutOK() throws Exception {
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			final int port = serverSocket.getLocalPort();
			System.setProperty(Properties.GRPC_PORT.getName(), Integer.toString(port));
			Thread t = new Thread(() -> {
				try {
					try (Socket socket = serverSocket.accept()) {
					}
				} catch (IOException e) {
				}
			});
			t.start();
			previewClient.waitForSidecar(10000).block();
		}
	}

	@Test
	public void getConfigurationIllegalArgumentExceptionTest() {
		// Empty Store name
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.getConfiguration("", "key1").block();
		});
		//Empty store name
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.getConfigurations("", "key1").block();
		});
		//Empty store name
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.getAllConfigurations("").block();
		});
		// Empty key
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.getConfiguration(CONFIG_STORE_NAME, "").block();
		});
		// Empty key
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.getConfigurations(CONFIG_STORE_NAME, "").block();
		});
	}

	@Test
	public void getSingleConfigurationTest() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("meta1", "value1");
		DaprProtos.GetConfigurationResponse responseEnvelope = DaprProtos.GetConfigurationResponse.newBuilder()
				.addItems(CommonProtos.ConfigurationItem.newBuilder()
						.setKey("configkey1")
						.setValue("configvalue1")
						.setVersion("1")
						.putAllMetadata(metadata)
						.build()
				).build();
		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.GetConfigurationResponse> observer = (StreamObserver<DaprProtos.GetConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(responseEnvelope);
			observer.onCompleted();
			return null;
		}).when(daprStub).getConfigurationAlpha1(any(DaprProtos.GetConfigurationRequest.class), any());

		ConfigurationItem ci = previewClient.getConfiguration(CONFIG_STORE_NAME, "configkey1").block();
		assertEquals("configkey1", ci.getKey());
		assertEquals("configvalue1", ci.getValue());
		assertEquals(metadata, ci.getMetadata());
		assertEquals("1", ci.getVersion());
	}

	@Test
	public void getMultipleConfigurationTest() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("meta1", "value1");
		DaprProtos.GetConfigurationResponse responseEnvelope = DaprProtos.GetConfigurationResponse.newBuilder()
				.addItems(CommonProtos.ConfigurationItem.newBuilder()
						.setKey("configkey1")
						.setValue("configvalue1")
						.setVersion("1")
						.putAllMetadata(metadata)
						.build())
				.addItems(CommonProtos.ConfigurationItem.newBuilder()
						.setKey("configkey2")
						.setValue("configvalue2")
						.setVersion("1")
						.putAllMetadata(metadata)
						.build())
				.build();

		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.GetConfigurationResponse> observer = (StreamObserver<DaprProtos.GetConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(responseEnvelope);
			observer.onCompleted();
			return null;
		}).when(daprStub).getConfigurationAlpha1(any(DaprProtos.GetConfigurationRequest.class), any());

		List<ConfigurationItem> cis = previewClient.getConfigurations(CONFIG_STORE_NAME, "configkey1","configkey2").block();
		assertEquals(2, cis.size());
		assertEquals("configkey1", cis.stream().findFirst().get().getKey());
		assertEquals("configvalue1", cis.stream().findFirst().get().getValue());
		assertEquals(metadata, cis.stream().findFirst().get().getMetadata());
		assertEquals("1", cis.stream().findFirst().get().getVersion());

		assertEquals("configkey2", cis.stream().skip(1).findFirst().get().getKey());
		assertEquals("configvalue2", cis.stream().skip(1).findFirst().get().getValue());
		assertEquals(metadata, cis.stream().skip(1).findFirst().get().getMetadata());
		assertEquals("1", cis.stream().skip(1).findFirst().get().getVersion());
	}

	@Test
	public void getAllConfigurationTest() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("meta1", "value1");
		DaprProtos.GetConfigurationResponse responseEnvelope = DaprProtos.GetConfigurationResponse.newBuilder()
				.addItems(CommonProtos.ConfigurationItem.newBuilder()
						.setKey("configkey1")
						.setValue("configvalue1")
						.setVersion("1")
						.putAllMetadata(metadata)
						.build())
				.addItems(CommonProtos.ConfigurationItem.newBuilder()
						.setKey("configkey2")
						.setValue("configvalue2")
						.setVersion("1")
						.putAllMetadata(metadata)
						.build())
				.build();

		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.GetConfigurationResponse> observer = (StreamObserver<DaprProtos.GetConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(responseEnvelope);
			observer.onCompleted();
			return null;
		}).when(daprStub).getConfigurationAlpha1(any(DaprProtos.GetConfigurationRequest.class), any());

		List<ConfigurationItem> cis = previewClient.getAllConfigurations(CONFIG_STORE_NAME).block();
		assertEquals(2, cis.size());
	}

	@Test
	public void subscribeConfigurationTest() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("meta1", "value1");
		DaprProtos.SubscribeConfigurationResponse responseEnvelope = DaprProtos.SubscribeConfigurationResponse.newBuilder()
				.addItems(CommonProtos.ConfigurationItem.newBuilder()
						.setKey("configkey1")
						.setValue("configvalue1")
						.setVersion("1")
						.putAllMetadata(metadata)
						.build())
				.build();

		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.SubscribeConfigurationResponse> observer = (StreamObserver<DaprProtos.SubscribeConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(responseEnvelope);
			observer.onCompleted();
			return null;
		}).when(daprStub).subscribeConfigurationAlpha1(any(DaprProtos.SubscribeConfigurationRequest.class), any());

		Iterator<List<ConfigurationItem>> itr = previewClient.subscribeToConfigurations(CONFIG_STORE_NAME, "configkey1").toIterable().iterator();
		assertTrue(itr.hasNext());
		assertEquals("configkey1", itr.next().get(0).getKey());
		assertFalse(itr.hasNext());
	}

	@Test
	public void subscribeConfigurationTestWithMetadata() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("meta1", "value1");
		DaprProtos.SubscribeConfigurationResponse responseEnvelope = DaprProtos.SubscribeConfigurationResponse.newBuilder()
				.addItems(CommonProtos.ConfigurationItem.newBuilder()
						.setKey("configkey1")
						.setValue("configvalue1")
						.setVersion("1")
						.putAllMetadata(metadata)
						.build())
				.build();

		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.SubscribeConfigurationResponse> observer = (StreamObserver<DaprProtos.SubscribeConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(responseEnvelope);
			observer.onCompleted();
			return null;
		}).when(daprStub).subscribeConfigurationAlpha1(any(DaprProtos.SubscribeConfigurationRequest.class), any());

		Map<String, String> reqMetadata = new HashMap<>();
		List<String> keys = Arrays.asList("configkey1");

		Iterator<List<ConfigurationItem>> itr = previewClient.subscribeToConfigurations(CONFIG_STORE_NAME, keys, reqMetadata).toIterable().iterator();
		assertTrue(itr.hasNext());
		assertEquals("configkey1", itr.next().get(0).getKey());
		assertFalse(itr.hasNext());
	}
}

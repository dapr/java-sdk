/*
 * Copyright 2021 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.client;


import io.dapr.client.domain.ConfigurationItem;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DaprPreviewClientGrpcTest {

	private static final String CONFIG_STORE_NAME = "MyConfigStore";

	private Closeable closeable;
	private DaprGrpc.DaprStub daprStub;
	private DaprPreviewClient previewClient;

	@Before
	public void setup() throws IOException {
		closeable = mock(Closeable.class);
		daprStub = mock(DaprGrpc.DaprStub.class);
		when(daprStub.withInterceptors(any())).thenReturn(daprStub);
		previewClient = new DaprClientGrpc(
				closeable, daprStub, new DefaultObjectSerializer(), new DefaultObjectSerializer());
		doNothing().when(closeable).close();
	}

	@After
	public void tearDown() throws Exception {
		previewClient.close();
		verify(closeable).close();
		verifyNoMoreInteractions(closeable);
	}

	@Test
	public void getConfigurationTestErrorScenario() {
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.getConfiguration(CONFIG_STORE_NAME, "").block();
		});
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.getConfiguration("", "key").block();
		});
	}

	@Test
	public void getSingleConfigurationTest() {
		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.GetConfigurationResponse> observer =
					(StreamObserver<DaprProtos.GetConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(getSingleMockResponse());
			observer.onCompleted();
			return null;
		}).when(daprStub).getConfigurationAlpha1(any(DaprProtos.GetConfigurationRequest.class), any());

		ConfigurationItem ci = previewClient.getConfiguration(CONFIG_STORE_NAME, "configkey1").block();
		assertEquals("configkey1", ci.getKey());
		assertEquals("configvalue1", ci.getValue());
		assertEquals("1", ci.getVersion());
	}

	@Test
	public void getSingleConfigurationWithMetadataTest() {
		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.GetConfigurationResponse> observer =
					(StreamObserver<DaprProtos.GetConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(getSingleMockResponse());
			observer.onCompleted();
			return null;
		}).when(daprStub).getConfigurationAlpha1(any(DaprProtos.GetConfigurationRequest.class), any());

		Map<String, String> reqMetadata = new HashMap<>();
		reqMetadata.put("meta1", "value1");
		ConfigurationItem ci = previewClient.getConfiguration(CONFIG_STORE_NAME, "configkey1", reqMetadata).block();
		assertEquals("configkey1", ci.getKey());
		assertEquals("configvalue1", ci.getValue());
		assertEquals("1", ci.getVersion());
	}

	@Test
	public void getMultipleConfigurationTest() {
		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.GetConfigurationResponse> observer =
					(StreamObserver<DaprProtos.GetConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(getMultipleMockResponse());
			observer.onCompleted();
			return null;
		}).when(daprStub).getConfigurationAlpha1(any(DaprProtos.GetConfigurationRequest.class), any());

		List<ConfigurationItem> cis = previewClient.getConfiguration(CONFIG_STORE_NAME, "configkey1","configkey2").block();
		assertEquals(2, cis.size());
		assertEquals("configkey1", cis.stream().findFirst().get().getKey());
		assertEquals("configvalue1", cis.stream().findFirst().get().getValue());
		assertEquals("1", cis.stream().findFirst().get().getVersion());

		assertEquals("configkey2", cis.stream().skip(1).findFirst().get().getKey());
		assertEquals("configvalue2", cis.stream().skip(1).findFirst().get().getValue());
		assertEquals("1", cis.stream().skip(1).findFirst().get().getVersion());
	}

	@Test
	public void getMultipleConfigurationWithMetadataTest() {
		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.GetConfigurationResponse> observer =
					(StreamObserver<DaprProtos.GetConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(getMultipleMockResponse());
			observer.onCompleted();
			return null;
		}).when(daprStub).getConfigurationAlpha1(any(DaprProtos.GetConfigurationRequest.class), any());

		Map<String, String> reqMetadata = new HashMap<>();
		reqMetadata.put("meta1", "value1");
		List<String> keys = Arrays.asList("configkey1","configkey2");
		List<ConfigurationItem> cis = previewClient.getConfiguration(CONFIG_STORE_NAME, keys, reqMetadata).block();
		assertEquals(2, cis.size());
		assertEquals("configkey1", cis.stream().findFirst().get().getKey());
		assertEquals("configvalue1", cis.stream().findFirst().get().getValue());
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
			StreamObserver<DaprProtos.SubscribeConfigurationResponse> observer =
					(StreamObserver<DaprProtos.SubscribeConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(responseEnvelope);
			observer.onCompleted();
			return null;
		}).when(daprStub).subscribeConfigurationAlpha1(any(DaprProtos.SubscribeConfigurationRequest.class), any());

		Iterator<List<ConfigurationItem>> itr = previewClient.subscribeToConfiguration(CONFIG_STORE_NAME, "configkey1").toIterable().iterator();
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
			StreamObserver<DaprProtos.SubscribeConfigurationResponse> observer =
					(StreamObserver<DaprProtos.SubscribeConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(responseEnvelope);
			observer.onCompleted();
			return null;
		}).when(daprStub).subscribeConfigurationAlpha1(any(DaprProtos.SubscribeConfigurationRequest.class), any());

		Map<String, String> reqMetadata = new HashMap<>();
		List<String> keys = Arrays.asList("configkey1");

		Iterator<List<ConfigurationItem>> itr = previewClient.subscribeToConfiguration(CONFIG_STORE_NAME, keys, reqMetadata).toIterable().iterator();
		assertTrue(itr.hasNext());
		assertEquals("configkey1", itr.next().get(0).getKey());
		assertFalse(itr.hasNext());
	}

	@Test
	public void subscribeConfigurationWithErrorTest() {
		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.SubscribeConfigurationResponse> observer =
					(StreamObserver<DaprProtos.SubscribeConfigurationResponse>) invocation.getArguments()[1];
			observer.onError(new RuntimeException());
			observer.onCompleted();
			return null;
		}).when(daprStub).subscribeConfigurationAlpha1(any(DaprProtos.SubscribeConfigurationRequest.class), any());

		assertThrowsDaprException(ExecutionException.class, () -> {
			previewClient.subscribeToConfiguration(CONFIG_STORE_NAME, "key").blockFirst();
		});

		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.subscribeToConfiguration("", "key").blockFirst();
		});
	}

	private DaprProtos.GetConfigurationResponse getSingleMockResponse() {
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
		return responseEnvelope;
	}

	private DaprProtos.GetConfigurationResponse getMultipleMockResponse() {
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
		return responseEnvelope;
	}
}

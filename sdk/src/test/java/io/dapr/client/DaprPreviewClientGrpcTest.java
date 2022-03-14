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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.GetConfigurationRequest;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.SubscribeConfigurationRequest;
import io.dapr.client.domain.query.Query;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DaprPreviewClientGrpcTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final String CONFIG_STORE_NAME = "MyConfigStore";
	private static final String QUERY_STORE_NAME = "testQueryStore";

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
		GetConfigurationRequest req = new GetConfigurationRequest(CONFIG_STORE_NAME, null);
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.getConfiguration(req).block();
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

		SubscribeConfigurationRequest req = new SubscribeConfigurationRequest(CONFIG_STORE_NAME, null);
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.subscribeToConfiguration(req).blockFirst();
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

	@Test
	public void queryStateExceptionsTest() {
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.queryState("", "query", String.class).block();
		});
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.queryState("storeName", "", String.class).block();
		});
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.queryState("storeName", (Query) null, String.class).block();
		});
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.queryState("storeName", (String) null, String.class).block();
		});
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.queryState(new QueryStateRequest("storeName"), String.class).block();
		});
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.queryState(null, String.class).block();
		});
	}

	@Test
	public void queryState() throws JsonProcessingException {
		List<QueryStateItem<?>> resp = new ArrayList<>();
		resp.add(new QueryStateItem<Object>("1", (Object)"testData", "6f54ad94-dfb9-46f0-a371-e42d550adb7d"));
		DaprProtos.QueryStateResponse responseEnvelope = buildQueryStateResponse(resp, "");
		doAnswer((Answer<Void>) invocation -> {
			DaprProtos.QueryStateRequest req = invocation.getArgument(0);
			assertEquals(QUERY_STORE_NAME, req.getStoreName());
			assertEquals("query", req.getQuery());
			assertEquals(0, req.getMetadataCount());

			StreamObserver<DaprProtos.QueryStateResponse> observer = (StreamObserver<DaprProtos.QueryStateResponse>)
					invocation.getArguments()[1];
			observer.onNext(responseEnvelope);
			observer.onCompleted();
			return null;
		}).when(daprStub).queryStateAlpha1(any(DaprProtos.QueryStateRequest.class), any());

		QueryStateResponse<String> response = previewClient.queryState(QUERY_STORE_NAME, "query", String.class).block();
		assertNotNull(response);
		assertEquals("result size must be 1", 1, response.getResults().size());
		assertEquals("result must be same", "1", response.getResults().get(0).getKey());
		assertEquals("result must be same", "testData", response.getResults().get(0).getValue());
		assertEquals("result must be same", "6f54ad94-dfb9-46f0-a371-e42d550adb7d", response.getResults().get(0).getEtag());
	}

	@Test
	public void queryStateMetadataError() throws JsonProcessingException {
		List<QueryStateItem<?>> resp = new ArrayList<>();
		resp.add(new QueryStateItem<Object>("1", null, "error data"));
		DaprProtos.QueryStateResponse responseEnvelope = buildQueryStateResponse(resp, "");
		doAnswer((Answer<Void>) invocation -> {
			DaprProtos.QueryStateRequest req = invocation.getArgument(0);
			assertEquals(QUERY_STORE_NAME, req.getStoreName());
			assertEquals("query", req.getQuery());
			assertEquals(1, req.getMetadataCount());
			assertEquals(1, req.getMetadataCount());

			StreamObserver<DaprProtos.QueryStateResponse> observer = (StreamObserver<DaprProtos.QueryStateResponse>)
					invocation.getArguments()[1];
			observer.onNext(responseEnvelope);
			observer.onCompleted();
			return null;
		}).when(daprStub).queryStateAlpha1(any(DaprProtos.QueryStateRequest.class), any());

		QueryStateResponse<String> response = previewClient.queryState(QUERY_STORE_NAME, "query",
				new HashMap<String, String>(){{ put("key", "error"); }}, String.class).block();
		assertNotNull(response);
		assertEquals("result size must be 1", 1, response.getResults().size());
		assertEquals("result must be same", "1", response.getResults().get(0).getKey());
		assertEquals("result must be same", "error data", response.getResults().get(0).getError());
	}

	private DaprProtos.QueryStateResponse buildQueryStateResponse(List<QueryStateItem<?>> resp,String token)
			throws JsonProcessingException {
		List<DaprProtos.QueryStateItem> items = new ArrayList<>();
		for (QueryStateItem<?> item: resp) {
			items.add(buildQueryStateItem(item));
		}
		return DaprProtos.QueryStateResponse.newBuilder()
				.addAllResults(items)
				.setToken(token)
				.build();
	}

	private DaprProtos.QueryStateItem buildQueryStateItem(QueryStateItem<?> item) throws JsonProcessingException {
		DaprProtos.QueryStateItem.Builder it = DaprProtos.QueryStateItem.newBuilder().setKey(item.getKey());
		if (item.getValue() != null) {
			it.setData(ByteString.copyFrom(MAPPER.writeValueAsBytes(item.getValue())));
		}
		if (item.getEtag() != null) {
			it.setEtag(item.getEtag());
		}
		if (item.getError() != null) {
			it.setError(item.getError());
		}
		return it.build();
	}
}

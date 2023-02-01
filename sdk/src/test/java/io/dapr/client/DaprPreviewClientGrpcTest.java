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
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishEntry;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.ConfigurationItem;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.SubscribeConfigurationResponse;
import io.dapr.client.domain.UnsubscribeConfigurationRequest;
import io.dapr.client.domain.UnsubscribeConfigurationResponse;
import io.dapr.client.domain.query.Query;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.v1.CommonProtos;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

	private static final String PUBSUB_NAME = "testPubsub";

	private static final String TOPIC_NAME = "testTopic";

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
	public void publishEventsExceptionThrownTest() {
		doAnswer((Answer<Void>) invocation -> {
			throw newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument");
		}).when(daprStub).bulkPublishEventAlpha1(any(DaprProtos.BulkPublishRequest.class), any());

		assertThrowsDaprException(
				StatusRuntimeException.class,
				"INVALID_ARGUMENT",
				"INVALID_ARGUMENT: bad bad argument",
				() -> previewClient.publishEvents(new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
						Collections.EMPTY_LIST)).block());
	}

	@Test
	public void publishEventsCallbackExceptionThrownTest() {
		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.BulkPublishResponse> observer =
					(StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
			observer.onError(newStatusRuntimeException("INVALID_ARGUMENT", "bad bad argument"));
			return null;
		}).when(daprStub).bulkPublishEventAlpha1(any(DaprProtos.BulkPublishRequest.class), any());

		assertThrowsDaprException(
				ExecutionException.class,
				"INVALID_ARGUMENT",
				"INVALID_ARGUMENT: bad bad argument",
				() -> previewClient.publishEvents(new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
						Collections.EMPTY_LIST)).block());
	}

	@Test(expected = IllegalArgumentException.class)
	public void publishEventsContentTypeMismatchException() throws IOException {
		DaprObjectSerializer mockSerializer = mock(DaprObjectSerializer.class);
		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.BulkPublishResponse> observer =
					(StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
			observer.onNext(DaprProtos.BulkPublishResponse.getDefaultInstance());
			observer.onCompleted();
			return null;
		}).when(daprStub).bulkPublishEventAlpha1(any(DaprProtos.BulkPublishRequest.class), any());


		BulkPublishEntry<String> entry = new BulkPublishEntry<>("1", "testEntry"
				, "application/octet-stream", null);
		BulkPublishRequest<String> wrongReq = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
				Collections.singletonList(entry));
		previewClient.publishEvents(wrongReq).block();
	}

	@Test
	public void publishEventsSerializeException() throws IOException {
		DaprObjectSerializer mockSerializer = mock(DaprObjectSerializer.class);
		previewClient = new DaprClientGrpc(closeable, daprStub, mockSerializer, new DefaultObjectSerializer());
		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.BulkPublishResponse> observer =
					(StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
			observer.onNext(DaprProtos.BulkPublishResponse.getDefaultInstance());
			observer.onCompleted();
			return null;
		}).when(daprStub).publishEvent(any(DaprProtos.PublishEventRequest.class), any());
		BulkPublishEntry<Map<String, String>> entry = new BulkPublishEntry<>("1", new HashMap<>(),
				"application/json", null);
		BulkPublishRequest<Map<String, String>> req = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
				Collections.singletonList(entry));
		when(mockSerializer.serialize(any())).thenThrow(IOException.class);
		Mono<BulkPublishResponse<Map<String, String>>> result = previewClient.publishEvents(req);

		assertThrowsDaprException(
				IOException.class,
				"UNKNOWN",
				"UNKNOWN: ",
				() -> result.block());
	}

	@Test
	public void publishEventsTest() {
		doAnswer((Answer<BulkPublishResponse>) invocation -> {
			StreamObserver<DaprProtos.BulkPublishResponse> observer =
					(StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
			DaprProtos.BulkPublishResponse.Builder builder = DaprProtos.BulkPublishResponse.newBuilder();
			observer.onNext(builder.build());
			observer.onCompleted();
			return null;
		}).when(daprStub).bulkPublishEventAlpha1(any(DaprProtos.BulkPublishRequest.class), any());

		BulkPublishEntry<String> entry = new BulkPublishEntry<>("1", "test",
				"text/plain", null);
		BulkPublishRequest<String> req = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
				Collections.singletonList(entry));
		Mono<BulkPublishResponse<String>> result = previewClient.publishEvents(req);
		BulkPublishResponse res = result.block();
		Assert.assertNotNull(res);
		assertEquals("expected no entry in failed entries list", 0, res.getFailedEntries().size());
	}

	@Test
	public void publishEventsWithoutMetaTest() {
		doAnswer((Answer<BulkPublishResponse>) invocation -> {
			StreamObserver<DaprProtos.BulkPublishResponse> observer =
					(StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
			DaprProtos.BulkPublishResponse.Builder builder = DaprProtos.BulkPublishResponse.newBuilder();
			observer.onNext(builder.build());
			observer.onCompleted();
			return null;
		}).when(daprStub).bulkPublishEventAlpha1(any(DaprProtos.BulkPublishRequest.class), any());

		Mono<BulkPublishResponse<String>> result = previewClient.publishEvents(PUBSUB_NAME, TOPIC_NAME,
				"text/plain", Collections.singletonList("test"));
		BulkPublishResponse<String> res = result.block();
		Assert.assertNotNull(res);
		assertEquals("expected no entries in failed entries list", 0, res.getFailedEntries().size());
	}

	@Test
	public void publishEventsWithRequestMetaTest() {
		doAnswer((Answer<BulkPublishResponse>) invocation -> {
			StreamObserver<DaprProtos.BulkPublishResponse> observer =
					(StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
			DaprProtos.BulkPublishResponse.Builder builder = DaprProtos.BulkPublishResponse.newBuilder();
			observer.onNext(builder.build());
			observer.onCompleted();
			return null;
		}).when(daprStub).bulkPublishEventAlpha1(any(DaprProtos.BulkPublishRequest.class), any());

		Mono<BulkPublishResponse<String>> result = previewClient.publishEvents(PUBSUB_NAME, TOPIC_NAME,
				 "text/plain", new HashMap<String, String>(){{
					put("ttlInSeconds", "123");
				}}, Collections.singletonList("test"));
		BulkPublishResponse<String> res = result.block();
		Assert.assertNotNull(res);
		assertEquals("expected no entry in failed entries list", 0, res.getFailedEntries().size());
	}

	@Test
	public void publishEventsObjectTest() {
		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.BulkPublishResponse> observer =
					(StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
			observer.onNext(DaprProtos.BulkPublishResponse.getDefaultInstance());
			observer.onCompleted();
			return null;
		}).when(daprStub).bulkPublishEventAlpha1(ArgumentMatchers.argThat(bulkPublishRequest -> {
			DaprProtos.BulkPublishRequestEntry entry = bulkPublishRequest.getEntries(0);
			if (!"application/json".equals(bulkPublishRequest.getEntries(0).getContentType())) {
				return false;
			}

			if (!"{\"id\":1,\"value\":\"Event\"}".equals(new String(entry.getEvent().toByteArray())) &&
					!"{\"value\":\"Event\",\"id\":1}".equals(new String(entry.getEvent().toByteArray()))) {
				return false;
			}
			return true;
		}), any());


		DaprClientGrpcTest.MyObject event = new DaprClientGrpcTest.MyObject(1, "Event");
		BulkPublishEntry<DaprClientGrpcTest.MyObject> entry = new BulkPublishEntry<>("1", event,
				"application/json", null);
		BulkPublishRequest<DaprClientGrpcTest.MyObject> req = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
				Collections.singletonList(entry));
		BulkPublishResponse<DaprClientGrpcTest.MyObject> result = previewClient.publishEvents(req).block();
		Assert.assertNotNull(result);
		Assert.assertEquals("expected no entries to be failed", 0, result.getFailedEntries().size());
	}

	@Test
	public void publishEventsContentTypeOverrideTest() {
		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.BulkPublishResponse> observer =
					(StreamObserver<DaprProtos.BulkPublishResponse>) invocation.getArguments()[1];
			observer.onNext(DaprProtos.BulkPublishResponse.getDefaultInstance());
			observer.onCompleted();
			return null;
		}).when(daprStub).bulkPublishEventAlpha1(ArgumentMatchers.argThat(bulkPublishRequest -> {
			DaprProtos.BulkPublishRequestEntry entry = bulkPublishRequest.getEntries(0);
			if (!"application/json".equals(entry.getContentType())) {
				return false;
			}

			if (!"\"hello\"".equals(new String(entry.getEvent().toByteArray()))) {
				return false;
			}
			return true;
		}), any());

		BulkPublishEntry<String> entry = new BulkPublishEntry<>("1", "hello",
				"", null);
		BulkPublishRequest<String> req = new BulkPublishRequest<>(PUBSUB_NAME, TOPIC_NAME,
				Collections.singletonList(entry));
		BulkPublishResponse<String> result = previewClient.publishEvents(req).block();
		Assert.assertNotNull(result);
		Assert.assertEquals("expected no entries to be failed", 0, result.getFailedEntries().size());
	}

	@Test
	public void getConfigurationTestErrorScenario() {
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

		Map<String, ConfigurationItem> cis = previewClient.getConfiguration(CONFIG_STORE_NAME, "configkey1","configkey2").block();
		assertEquals(2, cis.size());
		assertTrue("configkey1", cis.containsKey("configkey1"));
		assertEquals("configvalue1", cis.get("configkey1").getValue());
		assertEquals("1", cis.get("configkey1").getVersion());
		assertTrue("configkey2", cis.containsKey("configkey2"));
		assertEquals("configvalue2", cis.get("configkey2").getValue());
		assertEquals("1", cis.get("configkey2").getVersion());
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
		Map<String, ConfigurationItem> cis = previewClient.getConfiguration(CONFIG_STORE_NAME, keys, reqMetadata).block();
		assertEquals(2, cis.size());
		assertTrue("configkey1", cis.containsKey("configkey1"));
		assertEquals("configvalue1", cis.get("configkey1").getValue());
	}

	@Test
	public void subscribeConfigurationTest() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("meta1", "value1");
		Map<String, CommonProtos.ConfigurationItem> configs = new HashMap<>();
		configs.put("configkey1", CommonProtos.ConfigurationItem.newBuilder()
		.setValue("configvalue1")
		.setVersion("1")
		.putAllMetadata(metadata)
		.build());
		DaprProtos.SubscribeConfigurationResponse responseEnvelope = DaprProtos.SubscribeConfigurationResponse.newBuilder()
				.putAllItems(configs)
				.setId("subscription_id")
				.build();

		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.SubscribeConfigurationResponse> observer =
					(StreamObserver<DaprProtos.SubscribeConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(responseEnvelope);
			observer.onCompleted();
			return null;
		}).when(daprStub).subscribeConfigurationAlpha1(any(DaprProtos.SubscribeConfigurationRequest.class), any());

		Iterator<SubscribeConfigurationResponse> itr = previewClient.subscribeConfiguration(CONFIG_STORE_NAME, "configkey1").toIterable().iterator();
		assertTrue(itr.hasNext());
		SubscribeConfigurationResponse res = itr.next();
		assertTrue(res.getItems().containsKey("configkey1"));
		assertEquals("subscription_id", res.getSubscriptionId());
		assertFalse(itr.hasNext());
	}

	@Test
	public void subscribeConfigurationTestWithMetadata() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("meta1", "value1");
		Map<String, CommonProtos.ConfigurationItem> configs = new HashMap<>();
		configs.put("configkey1", CommonProtos.ConfigurationItem.newBuilder()
		.setValue("configvalue1")
		.setVersion("1")
		.putAllMetadata(metadata)
		.build());
		DaprProtos.SubscribeConfigurationResponse responseEnvelope = DaprProtos.SubscribeConfigurationResponse.newBuilder()
				.putAllItems(configs)
				.setId("subscription_id")
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

		Iterator<SubscribeConfigurationResponse> itr = previewClient.subscribeConfiguration(CONFIG_STORE_NAME, keys, reqMetadata).toIterable().iterator();
		assertTrue(itr.hasNext());
		SubscribeConfigurationResponse res = itr.next();
		assertTrue(res.getItems().containsKey("configkey1"));
		assertEquals("subscription_id", res.getSubscriptionId());
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
			previewClient.subscribeConfiguration(CONFIG_STORE_NAME, "key").blockFirst();
		});

		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.subscribeConfiguration("", "key").blockFirst();
		});
	}

	@Test
	public void unsubscribeConfigurationTest() {
		DaprProtos.UnsubscribeConfigurationResponse responseEnvelope = DaprProtos.UnsubscribeConfigurationResponse.newBuilder()
				.setOk(true)
				.setMessage("unsubscribed_message")
				.build();

		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.UnsubscribeConfigurationResponse> observer =
					(StreamObserver<DaprProtos.UnsubscribeConfigurationResponse>) invocation.getArguments()[1];
			observer.onNext(responseEnvelope);
			observer.onCompleted();
			return null;
		}).when(daprStub).unsubscribeConfigurationAlpha1(any(DaprProtos.UnsubscribeConfigurationRequest.class), any());

		UnsubscribeConfigurationResponse
				response = previewClient.unsubscribeConfiguration("subscription_id", CONFIG_STORE_NAME).block();
		assertTrue(response.getIsUnsubscribed());
		assertEquals("unsubscribed_message", response.getMessage());
	}

	@Test
	public void unsubscribeConfigurationTestWithError() {
		doAnswer((Answer<Void>) invocation -> {
			StreamObserver<DaprProtos.UnsubscribeConfigurationResponse> observer =
					(StreamObserver<DaprProtos.UnsubscribeConfigurationResponse>) invocation.getArguments()[1];
			observer.onError(new RuntimeException());
			observer.onCompleted();
			return null;
		}).when(daprStub).unsubscribeConfigurationAlpha1(any(DaprProtos.UnsubscribeConfigurationRequest.class), any());

		assertThrowsDaprException(ExecutionException.class, () -> {
			previewClient.unsubscribeConfiguration("subscription_id", CONFIG_STORE_NAME).block();
		});

		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.unsubscribeConfiguration("", CONFIG_STORE_NAME).block();
		});

		UnsubscribeConfigurationRequest req = new UnsubscribeConfigurationRequest("subscription_id", "");
		assertThrows(IllegalArgumentException.class, () -> {
			previewClient.unsubscribeConfiguration(req).block();
		});
	}

	private DaprProtos.GetConfigurationResponse getSingleMockResponse() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("meta1", "value1");
		Map<String, CommonProtos.ConfigurationItem> configs = new HashMap<>();
		configs.put("configkey1", CommonProtos.ConfigurationItem.newBuilder()
		.setValue("configvalue1")
		.setVersion("1")
		.putAllMetadata(metadata)
		.build());
		DaprProtos.GetConfigurationResponse responseEnvelope = DaprProtos.GetConfigurationResponse.newBuilder()
				.putAllItems(configs)
				.build();
		return responseEnvelope;
	}

	private DaprProtos.GetConfigurationResponse getMultipleMockResponse() {
		Map<String, String> metadata = new HashMap<>();
		metadata.put("meta1", "value1");
		Map<String, CommonProtos.ConfigurationItem> configs = new HashMap<>();
		configs.put("configkey1", CommonProtos.ConfigurationItem.newBuilder()
		.setValue("configvalue1")
		.setVersion("1")
		.putAllMetadata(metadata)
		.build());
		configs.put("configkey2", CommonProtos.ConfigurationItem.newBuilder()
		.setValue("configvalue2")
		.setVersion("1")
		.putAllMetadata(metadata)
		.build());
		DaprProtos.GetConfigurationResponse responseEnvelope = DaprProtos.GetConfigurationResponse.newBuilder()
				.putAllItems(configs)
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

	private static StatusRuntimeException newStatusRuntimeException(String status, String message) {
		return new StatusRuntimeException(Status.fromCode(Status.Code.valueOf(status)).withDescription(message));
	}
}

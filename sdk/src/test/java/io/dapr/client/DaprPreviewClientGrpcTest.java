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
import io.dapr.client.domain.BulkPublishEntry;
import io.dapr.client.domain.BulkPublishRequest;
import io.dapr.client.domain.BulkPublishResponse;
import io.dapr.client.domain.QueryStateItem;
import io.dapr.client.domain.QueryStateRequest;
import io.dapr.client.domain.QueryStateResponse;
import io.dapr.client.domain.UnlockResponseStatus;
import io.dapr.client.domain.query.Query;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.serializer.DefaultObjectSerializer;
import io.dapr.v1.DaprGrpc;
import io.dapr.v1.DaprProtos;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.dapr.utils.TestUtils.assertThrowsDaprException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

	private static final String QUERY_STORE_NAME = "testQueryStore";

	private static final String PUBSUB_NAME = "testPubsub";

	private static final String TOPIC_NAME = "testTopic";

	private static final String LOCK_STORE_NAME = "MyLockStore";

	private GrpcChannelFacade channel;
	private DaprGrpc.DaprStub daprStub;
	private DaprHttp daprHttp;
	private DaprPreviewClient previewClient;

	@BeforeEach
	public void setup() throws IOException {
		channel = mock(GrpcChannelFacade.class);
		daprStub = mock(DaprGrpc.DaprStub.class);
		daprHttp = mock(DaprHttp.class);
		when(daprStub.withInterceptors(any())).thenReturn(daprStub);
		previewClient = new DaprClientImpl(
				channel, daprStub, daprHttp, new DefaultObjectSerializer(), new DefaultObjectSerializer());
		doNothing().when(channel).close();
	}

	@AfterEach
	public void tearDown() throws Exception {
		previewClient.close();
		verify(channel).close();
		verifyNoMoreInteractions(channel);
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

	@Test
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

    assertThrows(IllegalArgumentException.class, () -> previewClient.publishEvents(wrongReq).block());
	}

	@Test
	public void publishEventsSerializeException() throws IOException {
		DaprObjectSerializer mockSerializer = mock(DaprObjectSerializer.class);
		previewClient = new DaprClientImpl(channel, daprStub, daprHttp, mockSerializer, new DefaultObjectSerializer());
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
		Assertions.assertNotNull(res);
		assertEquals( 0, res.getFailedEntries().size(), "expected no entry in failed entries list");
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
		Assertions.assertNotNull(res);
		assertEquals( 0, res.getFailedEntries().size(), "expected no entries in failed entries list");
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
		Assertions.assertNotNull(res);
		assertEquals( 0, res.getFailedEntries().size(), "expected no entry in failed entries list");
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
		Assertions.assertNotNull(result);
		Assertions.assertEquals(0, result.getFailedEntries().size(), "expected no entries to be failed");
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
		Assertions.assertNotNull(result);
		Assertions.assertEquals( 0, result.getFailedEntries().size(), "expected no entries to be failed");
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
		doAnswer(invocation -> {
			DaprProtos.QueryStateRequest req = (DaprProtos.QueryStateRequest) invocation.getArgument(0);
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
		assertEquals(1, response.getResults().size(), "result size must be 1");
		assertEquals("1", response.getResults().get(0).getKey(), "result must be same");
		assertEquals("testData", response.getResults().get(0).getValue(), "result must be same");
		assertEquals("6f54ad94-dfb9-46f0-a371-e42d550adb7d", response.getResults().get(0).getEtag(), "result must be same");
	}

	@Test
	public void queryStateMetadataError() throws JsonProcessingException {
		List<QueryStateItem<?>> resp = new ArrayList<>();
		resp.add(new QueryStateItem<Object>("1", null, "error data"));
		DaprProtos.QueryStateResponse responseEnvelope = buildQueryStateResponse(resp, "");
		doAnswer(invocation -> {
			DaprProtos.QueryStateRequest req = (DaprProtos.QueryStateRequest) invocation.getArgument(0);
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
		assertEquals(1, response.getResults().size(), "result size must be 1");
		assertEquals( "1", response.getResults().get(0).getKey(), "result must be same");
		assertEquals( "error data", response.getResults().get(0).getError(), "result must be same");
	}

	@Test
	public void tryLock() {

		DaprProtos.TryLockResponse.Builder builder = DaprProtos.TryLockResponse.newBuilder()
				.setSuccess(true);

		DaprProtos.TryLockResponse response = builder.build();

		doAnswer((Answer<Void>) invocation -> {
			DaprProtos.TryLockRequest req = invocation.getArgument(0);
			assertEquals(LOCK_STORE_NAME, req.getStoreName());
			assertEquals("1", req.getResourceId());
			assertEquals("owner", req.getLockOwner());
			assertEquals(10, req.getExpiryInSeconds());

			StreamObserver<DaprProtos.TryLockResponse> observer =
					(StreamObserver<DaprProtos.TryLockResponse>) invocation.getArguments()[1];
			observer.onNext(response);
			observer.onCompleted();
			return null;
		}).when(daprStub).tryLockAlpha1(any(DaprProtos.TryLockRequest.class), any());

		Boolean result = previewClient.tryLock("MyLockStore", "1", "owner", 10).block();
		assertEquals(Boolean.TRUE, result);
	}

	@Test
	public void unLock() {

		DaprProtos.UnlockResponse.Builder builder = DaprProtos.UnlockResponse.newBuilder()
				.setStatus(DaprProtos.UnlockResponse.Status.SUCCESS);

		DaprProtos.UnlockResponse response = builder.build();

		doAnswer((Answer<Void>) invocation -> {
			DaprProtos.UnlockRequest req = invocation.getArgument(0);
			assertEquals(LOCK_STORE_NAME, req.getStoreName());
			assertEquals("1", req.getResourceId());
			assertEquals("owner", req.getLockOwner());

			StreamObserver<DaprProtos.UnlockResponse> observer =
					(StreamObserver<DaprProtos.UnlockResponse>) invocation.getArguments()[1];
			observer.onNext(response);
			observer.onCompleted();
			return null;
		}).when(daprStub).unlockAlpha1(any(DaprProtos.UnlockRequest.class), any());

		UnlockResponseStatus result = previewClient.unlock("MyLockStore", "1", "owner").block();
		assertEquals(UnlockResponseStatus.SUCCESS, result);
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

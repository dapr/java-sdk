/*
 * Copyright 2026 The Dapr Authors
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

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.dapr.client.domain.DeleteSearchDocumentsRequestAlpha1;
import io.dapr.client.domain.GetSearchDocumentsRequestAlpha1;
import io.dapr.client.domain.GetSearchIndexResponseAlpha1;
import io.dapr.client.domain.IndexAck;
import io.dapr.client.domain.IndexSearchDocumentsRequestAlpha1;
import io.dapr.client.domain.IndexSearchDocumentsResponseAlpha1;
import io.dapr.client.domain.SearchDocument;
import io.dapr.client.domain.SearchHit;
import io.dapr.client.domain.SearchQuery;
import io.dapr.client.domain.SearchRequestAlpha1;
import io.dapr.client.domain.SearchResponseAlpha1;
import io.dapr.client.domain.SortClause;
import io.dapr.client.domain.SortOrder;
import io.dapr.client.domain.TotalHitsRelation;
import io.dapr.exceptions.DaprException;
import io.dapr.v1.DaprSearchProtos;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements the search building block (alpha) of {@link DaprPreviewClient} over gRPC.
 */
final class DaprSearchClient {

  private final DaprGrpcCalls grpcCalls;

  DaprSearchClient(DaprGrpcCalls grpcCalls) {
    this.grpcCalls = grpcCalls;
  }

  Mono<Void> createSearchIndex(String storeName, String index, Map<String, String> metadata) {
    try {
      validateIndex(storeName, index);

      DaprSearchProtos.CreateIndexRequestAlpha1.Builder builder =
          DaprSearchProtos.CreateIndexRequestAlpha1.newBuilder()
              .setStoreName(storeName)
              .setIndex(index);
      SearchVectorHelper.putMetadata(builder::putAllMetadata, metadata);
      DaprSearchProtos.CreateIndexRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<Empty>createMono(
              it -> grpcCalls.intercept(context).createIndexAlpha1(protoRequest, it)
          )
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<GetSearchIndexResponseAlpha1> getSearchIndex(String storeName, String index, Map<String, String> metadata) {
    try {
      validateIndex(storeName, index);

      DaprSearchProtos.GetIndexRequestAlpha1.Builder builder =
          DaprSearchProtos.GetIndexRequestAlpha1.newBuilder()
              .setStoreName(storeName)
              .setIndex(index);
      SearchVectorHelper.putMetadata(builder::putAllMetadata, metadata);
      DaprSearchProtos.GetIndexRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprSearchProtos.GetIndexResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).getIndexAlpha1(protoRequest, it)
          )
      ).map(response -> new GetSearchIndexResponseAlpha1(
          response.getIndex(),
          response.getDocumentCount(),
          response.getPropertiesMap()));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<List<String>> listSearchIndexes(String storeName, Map<String, String> metadata) {
    try {
      SearchVectorHelper.validateStoreName(storeName);

      DaprSearchProtos.ListIndexesRequestAlpha1.Builder builder =
          DaprSearchProtos.ListIndexesRequestAlpha1.newBuilder().setStoreName(storeName);
      SearchVectorHelper.putMetadata(builder::putAllMetadata, metadata);
      DaprSearchProtos.ListIndexesRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprSearchProtos.ListIndexesResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).listIndexesAlpha1(protoRequest, it)
          )
      ).map(response -> List.copyOf(response.getIndexesList()));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<Void> deleteSearchIndex(String storeName, String index, Map<String, String> metadata) {
    try {
      validateIndex(storeName, index);

      DaprSearchProtos.DeleteIndexRequestAlpha1.Builder builder =
          DaprSearchProtos.DeleteIndexRequestAlpha1.newBuilder()
              .setStoreName(storeName)
              .setIndex(index);
      SearchVectorHelper.putMetadata(builder::putAllMetadata, metadata);
      DaprSearchProtos.DeleteIndexRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<Empty>createMono(
              it -> grpcCalls.intercept(context).deleteIndexAlpha1(protoRequest, it)
          )
      ).then();
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<IndexSearchDocumentsResponseAlpha1> indexSearchDocuments(IndexSearchDocumentsRequestAlpha1 request) {
    try {
      SearchVectorHelper.requireRequest(request);
      validateIndex(request.getStoreName(), request.getIndex());
      if (request.getDocuments() == null) {
        throw new IllegalArgumentException("Documents cannot be null.");
      }

      DaprSearchProtos.IndexDocumentsRequestAlpha1.Builder builder =
          DaprSearchProtos.IndexDocumentsRequestAlpha1.newBuilder()
              .setStoreName(request.getStoreName())
              .setIndex(request.getIndex());
      for (SearchDocument document : request.getDocuments()) {
        builder.addDocuments(toProto(document));
      }
      SearchVectorHelper.putMetadata(builder::putAllMetadata, request.getMetadata());
      if (request.getOptions() != null) {
        builder.setOptions(SearchVectorHelper.toIndexingOptions(request.getOptions()));
      }
      DaprSearchProtos.IndexDocumentsRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprSearchProtos.IndexDocumentsResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).indexDocumentsAlpha1(protoRequest, it)
          )
      ).map(response -> new IndexSearchDocumentsResponseAlpha1(
          SearchVectorHelper.toIndexAck(response.getAck()),
          SearchVectorHelper.toFailedItems(response.getFailedItemsList())));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<List<SearchDocument>> getSearchDocuments(GetSearchDocumentsRequestAlpha1 request) {
    try {
      SearchVectorHelper.requireRequest(request);
      validateIndex(request.getStoreName(), request.getIndex());
      SearchVectorHelper.validateIds(request.getIds());

      DaprSearchProtos.GetDocumentsRequestAlpha1.Builder builder =
          DaprSearchProtos.GetDocumentsRequestAlpha1.newBuilder()
              .setStoreName(request.getStoreName())
              .setIndex(request.getIndex())
              .addAllIds(request.getIds());
      if (request.getIncludeContent() != null) {
        builder.setIncludeContent(request.getIncludeContent());
      }
      SearchVectorHelper.putMetadata(builder::putAllMetadata, request.getMetadata());
      DaprSearchProtos.GetDocumentsRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprSearchProtos.GetDocumentsResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).getDocumentsAlpha1(protoRequest, it)
          )
      ).map(response -> {
        List<SearchDocument> documents = new ArrayList<>();
        for (DaprSearchProtos.SearchDocument document : response.getDocumentsList()) {
          documents.add(fromProto(document));
        }
        return List.copyOf(documents);
      });
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<IndexAck> deleteSearchDocuments(DeleteSearchDocumentsRequestAlpha1 request) {
    try {
      SearchVectorHelper.requireRequest(request);
      validateIndex(request.getStoreName(), request.getIndex());
      SearchVectorHelper.validateIds(request.getIds());

      DaprSearchProtos.DeleteDocumentsRequestAlpha1.Builder builder =
          DaprSearchProtos.DeleteDocumentsRequestAlpha1.newBuilder()
              .setStoreName(request.getStoreName())
              .setIndex(request.getIndex())
              .addAllIds(request.getIds());
      SearchVectorHelper.putMetadata(builder::putAllMetadata, request.getMetadata());
      if (request.getOptions() != null) {
        builder.setOptions(SearchVectorHelper.toIndexingOptions(request.getOptions()));
      }
      DaprSearchProtos.DeleteDocumentsRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprSearchProtos.DeleteDocumentsResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).deleteDocumentsAlpha1(protoRequest, it)
          )
      ).map(response -> SearchVectorHelper.toIndexAck(response.getAck()));
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  Mono<SearchResponseAlpha1> search(SearchRequestAlpha1 request) {
    try {
      SearchVectorHelper.requireRequest(request);
      validateIndex(request.getStoreName(), request.getIndex());

      DaprSearchProtos.SearchRequestAlpha1.Builder builder =
          DaprSearchProtos.SearchRequestAlpha1.newBuilder()
              .setStoreName(request.getStoreName())
              .setIndex(request.getIndex());
      if (request.getQuery() != null) {
        builder.setQuery(toProto(request.getQuery()));
      }
      SearchVectorHelper.putMetadata(builder::putAllMetadata, request.getMetadata());
      DaprSearchProtos.SearchRequestAlpha1 protoRequest = builder.build();

      return Mono.deferContextual(
          context -> grpcCalls.<DaprSearchProtos.SearchResponseAlpha1>createMono(
              it -> grpcCalls.intercept(context).searchAlpha1(protoRequest, it)
          )
      ).map(DaprSearchClient::fromProto);
    } catch (Exception ex) {
      return DaprException.wrapMono(ex);
    }
  }

  private static void validateIndex(String storeName, String index) {
    SearchVectorHelper.validateStoreName(storeName);
    if (SearchVectorHelper.isBlank(index)) {
      throw new IllegalArgumentException("Index name cannot be null or empty.");
    }
  }

  private static void validateFields(List<String> fields, String description) {
    for (String field : fields) {
      if (SearchVectorHelper.isBlank(field)) {
        throw new IllegalArgumentException(description + " cannot contain a null or empty field.");
      }
    }
  }

  private static DaprSearchProtos.SearchDocument toProto(SearchDocument document) {
    if (document == null) {
      throw new IllegalArgumentException("Documents cannot contain a null document.");
    }
    if (SearchVectorHelper.isBlank(document.getId())) {
      throw new IllegalArgumentException("Document ID cannot be null or empty.");
    }
    if (document.getContent() == null) {
      throw new IllegalArgumentException("Document content cannot be null.");
    }

    DaprSearchProtos.SearchDocument.Builder builder = DaprSearchProtos.SearchDocument.newBuilder()
        .setId(document.getId())
        .setContent(ByteString.copyFrom(document.getContent()));
    SearchVectorHelper.putMetadata(builder::putAllMetadata, document.getMetadata());
    return builder.build();
  }

  private static DaprSearchProtos.SearchQuery toProto(SearchQuery query) {
    boolean hasText = !SearchVectorHelper.isBlank(query.getText());
    boolean hasNative = query.getNative() != null && !query.getNative().isEmpty();
    // The sidecar treats an empty query as one that matches every document, so that has to be asked for explicitly.
    if (!query.isMatchAll() && !hasText && !hasNative) {
      throw new IllegalArgumentException(
          "A text or native query cannot be empty. Use SearchQuery.matchAll() to match every document.");
    }
    if (query.getTopK() != null && query.getTopK() < 0) {
      throw new IllegalArgumentException("Top K cannot be negative.");
    }

    DaprSearchProtos.SearchQuery.Builder builder = DaprSearchProtos.SearchQuery.newBuilder();
    if (hasText) {
      builder.setText(query.getText());
    } else if (hasNative) {
      builder.setNative(SearchVectorHelper.toStruct(query.getNative()));
    }
    if (query.getFilter() != null) {
      builder.setFilter(SearchVectorHelper.toStruct(query.getFilter()));
    }
    if (query.getTopK() != null) {
      builder.setTopK(query.getTopK());
    }
    if (query.getContinuationToken() != null) {
      builder.setContinuationToken(query.getContinuationToken());
    }
    if (query.getReturnFields() != null) {
      validateFields(query.getReturnFields(), "Return fields");
      builder.addAllReturnFields(query.getReturnFields());
    }
    if (query.getIncludeContent() != null) {
      builder.setIncludeContent(query.getIncludeContent());
    }
    if (query.getSearchFields() != null) {
      validateFields(query.getSearchFields(), "Search fields");
      builder.addAllSearchFields(query.getSearchFields());
    }
    if (query.getSort() != null) {
      for (SortClause clause : query.getSort()) {
        builder.addSort(toProto(clause));
      }
    }
    if (query.getHighlightFields() != null) {
      validateFields(query.getHighlightFields(), "Highlight fields");
      builder.addAllHighlightFields(query.getHighlightFields());
    }
    return builder.build();
  }

  private static DaprSearchProtos.SortClause toProto(SortClause clause) {
    if (clause == null) {
      throw new IllegalArgumentException("Sort cannot contain a null clause.");
    }
    if (SearchVectorHelper.isBlank(clause.getField())) {
      throw new IllegalArgumentException("Sort field cannot be null or empty.");
    }
    return DaprSearchProtos.SortClause.newBuilder()
        .setField(clause.getField())
        .setOrder(clause.getOrder() == SortOrder.DESC
            ? DaprSearchProtos.SortOrder.SORT_ORDER_DESC
            : DaprSearchProtos.SortOrder.SORT_ORDER_ASC)
        .build();
  }

  private static SearchDocument fromProto(DaprSearchProtos.SearchDocument document) {
    // A stored document is a JSON object, so empty content means the read did not ask for it.
    byte[] content = document.getContent().isEmpty() ? null : document.getContent().toByteArray();
    SearchDocument searchDocument = new SearchDocument(document.getId(), content);
    if (document.getMetadataCount() > 0) {
      searchDocument.setMetadata(Map.copyOf(document.getMetadataMap()));
    }
    return searchDocument;
  }

  private static SearchResponseAlpha1 fromProto(DaprSearchProtos.SearchResponseAlpha1 response) {
    List<SearchHit> hits = new ArrayList<>();
    for (DaprSearchProtos.SearchHit hit : response.getHitsList()) {
      if (!hit.hasDocument()) {
        throw SearchVectorHelper.invalidResponse("Search store returned a hit without a document.");
      }
      hits.add(new SearchHit(fromProto(hit.getDocument()), hit.getScore(), hit.getHighlightsMap()));
    }
    return new SearchResponseAlpha1(
        hits,
        response.hasTotalHits() ? response.getTotalHits() : null,
        response.hasContinuationToken() ? response.getContinuationToken() : null,
        fromProto(response.getTotalHitsRelation()));
  }

  private static TotalHitsRelation fromProto(DaprSearchProtos.TotalHitsRelation relation) {
    switch (relation) {
      case TOTAL_HITS_RELATION_EXACT:
        return TotalHitsRelation.EXACT;
      case TOTAL_HITS_RELATION_LOWER_BOUND:
        return TotalHitsRelation.LOWER_BOUND;
      case TOTAL_HITS_RELATION_ESTIMATE:
        return TotalHitsRelation.ESTIMATE;
      default:
        // The relation only qualifies the total, so a value this SDK doesn't know shouldn't cost the caller the hits.
        return TotalHitsRelation.UNSPECIFIED;
    }
  }
}

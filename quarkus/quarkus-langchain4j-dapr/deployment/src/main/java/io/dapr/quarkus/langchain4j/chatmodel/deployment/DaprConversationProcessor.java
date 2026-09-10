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

package io.dapr.quarkus.langchain4j.chatmodel.deployment;

import dev.langchain4j.model.chat.ChatModel;
import io.dapr.quarkus.langchain4j.chatmodel.DaprConversationConfig;
import io.dapr.quarkus.langchain4j.chatmodel.DaprConversationRecorder;
import io.quarkiverse.langchain4j.deployment.items.ChatModelProviderCandidateBuildItem;
import io.quarkiverse.langchain4j.deployment.items.SelectedChatModelProviderBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Quarkus deployment processor for the Dapr Conversation ChatModel provider.
 */
public class DaprConversationProcessor {

  private static final String FEATURE = "langchain4j-dapr-conversation";
  private static final String PROVIDER = "dapr-conversation";

  @BuildStep
  FeatureBuildItem feature() {
    return new FeatureBuildItem(FEATURE);
  }

  @BuildStep
  void providerCandidates(BuildProducer<ChatModelProviderCandidateBuildItem> candidates) {
    candidates.produce(new ChatModelProviderCandidateBuildItem(PROVIDER));
  }

  @BuildStep
  @Record(ExecutionTime.RUNTIME_INIT)
  void generateBeans(DaprConversationRecorder recorder,
      List<SelectedChatModelProviderBuildItem> selectedProviders,
      BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

    for (SelectedChatModelProviderBuildItem selected : selectedProviders) {
      if (!PROVIDER.equals(selected.getProvider())) {
        continue;
      }

      SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator =
          SyntheticBeanBuildItem.configure(ChatModel.class)
              .scope(ApplicationScoped.class)
              .setRuntimeInit()
              .defaultBean()
              .unremovable()
              .createWith(recorder.chatModel());

      syntheticBeans.produce(configurator.done());
    }
  }
}

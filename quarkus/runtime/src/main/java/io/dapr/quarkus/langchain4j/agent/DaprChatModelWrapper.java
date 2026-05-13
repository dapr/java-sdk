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

package io.dapr.quarkus.langchain4j.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkus.arc.Arc;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Thin {@code @Alternative} wrapper that enables {@link DaprChatModelDecorator}
 * to fire. The decorator cannot decorate the quarkus-langchain4j synthetic bean
 * directly, but it CAN decorate this regular CDI bean.
 *
 * <p>This bean does NO Dapr routing — all routing logic is in
 * {@link DaprChatModelDecorator}. This class only exists to provide a
 * non-synthetic {@link ChatModel} bean for the decorator to wrap.
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class DaprChatModelWrapper implements ChatModel {

  private static final Logger LOG = Logger.getLogger(DaprChatModelWrapper.class);

  private volatile ChatModel delegate;
  private volatile String delegateClassName = "unknown";

  /**
   * Returns the simple class name of the underlying ChatModel provider.
   *
   * @return the provider class name (e.g., "DaprConversationChatModel")
   */
  public String getDelegateClassName() {
    return delegateClassName;
  }

  @Inject
  @SuppressWarnings("unchecked")
  void initDelegate() {
    var beanManager = Arc.container().beanManager();
    for (var bean : beanManager.getBeans(ChatModel.class, Any.Literal.INSTANCE)) {
      if (!bean.getBeanClass().getName().contains("DaprChatModel")) {
        var ctx = beanManager.createCreationalContext(
            (jakarta.enterprise.inject.spi.Bean<ChatModel>) bean);
        this.delegate = (ChatModel) beanManager.getReference(
            bean, ChatModel.class, ctx);
        // Read the configured provider name from application config
        try {
          this.delegateClassName = org.eclipse.microprofile.config.ConfigProvider
              .getConfig().getOptionalValue(
                  "quarkus.langchain4j.chat-model.provider", String.class)
              .orElse("unknown");
        } catch (Exception e) {
          this.delegateClassName = "unknown";
        }
        ChatModelProviderName.set(this.delegateClassName);
        LOG.infof("DaprChatModelWrapper initialized — delegate: %s",
            delegateClassName);
        return;
      }
    }
    LOG.warn("DaprChatModelWrapper: no delegate ChatModel found!");
  }

  @Override
  public ChatResponse doChat(ChatRequest request) {
    return delegate.doChat(request);
  }

  @Override
  public ChatResponse chat(ChatRequest request) {
    return delegate.chat(request);
  }
}

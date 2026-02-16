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

package io.dapr.examples.conversation;

import io.dapr.client.domain.ConversationResultAlpha2;
import io.dapr.client.domain.ConversationResultCompletionUsage;
import org.springframework.util.StringUtils;

public class UsageUtils {
  static void printUsage(ConversationResultAlpha2 result) {
    if (!StringUtils.hasText(result.getModel())){
      return;
    }

  System.out.printf("Conversation model : %s\n", result.getModel());
    var usage = result.getUsage();
    printUsage(usage);

    printCompletionDetails(usage);
    printPromptDetails(usage);

  }

  private static void printUsage(ConversationResultCompletionUsage usage) {
    System.out.println("Token Usage Details:");
    System.out.printf("  Completion tokens: %d\n", usage.getCompletionTokens());
    System.out.printf("  Prompt tokens: %d\n", usage.getPromptTokens());
    System.out.printf("  Total tokens: %d\n", usage.getTotalTokens());
    System.out.println();
  }

  private static void printPromptDetails(ConversationResultCompletionUsage usage) {
    var completionDetails = usage.getCompletionTokenDetails();

    // Display completion token breakdown if available
    System.out.println("Prompt Token Details:");
    if (completionDetails.getReasoningTokens() > 0) {
      System.out.printf("  Reasoning tokens: %d\n", completionDetails.getReasoningTokens());
    }
    if (completionDetails.getAudioTokens() > 0) {
      System.out.printf("  Audio tokens: %d\n", completionDetails.getAudioTokens());
    }
    System.out.println();
  }

  private static void printCompletionDetails(ConversationResultCompletionUsage usage) {
    // Print detailed token usage information
    var completionDetails = usage.getCompletionTokenDetails();

    System.out.println("Completion Token Details:");
    // If audio tokens are available, display them
    if ( completionDetails.getAudioTokens() > 0) {
      System.out.printf("  Audio tokens: %d\n", completionDetails.getAudioTokens());
    }

    // Display completion token breakdown if available
    if (completionDetails.getReasoningTokens() > 0) {
      System.out.printf("  Reasoning tokens: %d\n", completionDetails.getReasoningTokens());
    }

    // Display completion token breakdown if available
    if (completionDetails.getAcceptedPredictionTokens() > 0) {
      System.out.printf("  Accepted prediction tokens: %d\n", completionDetails.getAcceptedPredictionTokens());
    }

    // Display completion token breakdown if available
    if (completionDetails.getRejectedPredictionTokens() > 0) {
      System.out.printf("  Rejected prediction tokens: %d\n", completionDetails.getRejectedPredictionTokens());
    }
    System.out.println();
  }
}

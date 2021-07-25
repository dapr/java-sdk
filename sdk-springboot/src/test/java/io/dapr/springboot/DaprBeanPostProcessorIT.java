package io.dapr.springboot;

import io.dapr.Topic;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@ContextConfiguration(
        classes = { DaprAutoConfiguration.class }
)
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
public class DaprBeanPostProcessorIT implements WithAssertions {
    private final DaprRuntime daprRuntime = DaprRuntime.getInstance();

    @Test
    void shouldBindTopicWithPostMappingByPath() {
        // Arrange

        // Act
        DaprTopicSubscription[] subscribedTopics = daprRuntime.listSubscribedTopics();

        // Assert
        assertThat(subscribedTopics).anySatisfy(subscribedTopic -> {
            assertThat(subscribedTopic.getRoute()).isEqualTo("/path-1");
            assertThat(subscribedTopic.getPubsubName()).isEqualTo("pubsubName");
            assertThat(subscribedTopic.getTopic()).isEqualTo("topic-1");
            assertThat(subscribedTopic.getMetadata()).isEmpty();
        });
    }

    @Test
    void shouldBindTopicWithMetadata() {
        // Arrange

        // Act
        DaprTopicSubscription[] subscribedTopics = daprRuntime.listSubscribedTopics();

        // Assert
        assertThat(subscribedTopics).anySatisfy(subscribedTopic -> {
            assertThat(subscribedTopic.getRoute()).isEqualTo("/path-2");
            assertThat(subscribedTopic.getMetadata()).containsEntry("priority", "high");
        });
    }
}

@RestController
class PubsubController {
    @PostMapping(path = "/path-1")
    @Topic(pubsubName = "pubsubName", name = "topic-1")
    public ResponseEntity<Void> method1() {
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/path-2")
    @Topic(pubsubName = "pubsubName", name = "topic-2", metadata = "{ \"priority\": \"high\" }")
    public ResponseEntity<Void> method2() {
        return ResponseEntity.ok().build();
    }
}

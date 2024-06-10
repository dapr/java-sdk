package io.dapr.client.domain;

import java.util.List;

public class SubscriptionMetadata {
    private String topic;
    private String pubsubname;
    private String deadLetterTopic;
    private List<RuleMetadata> rules;

    public SubscriptionMetadata() {
    }

    public SubscriptionMetadata(String topic, String pubsubname, String deadLetterTopic, List<RuleMetadata> rules) {
        this.topic = topic;
        this.pubsubname = pubsubname;
        this.deadLetterTopic = deadLetterTopic;
        this.rules = rules;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPubsubname() {
        return pubsubname;
    }

    public void setPubsubname(String pubsubname) {
        this.pubsubname = pubsubname;
    }

    public String getDeadLetterTopic() {
        return deadLetterTopic;
    }

    public void setDeadLetterTopic(String deadLetterTopic) {
        this.deadLetterTopic = deadLetterTopic;
    }

    public List<RuleMetadata> getRules() {
        return rules;
    }

    public void setRules(List<RuleMetadata> rules) {
        this.rules = rules;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((topic == null) ? 0 : topic.hashCode());
        result = prime * result + ((pubsubname == null) ? 0 : pubsubname.hashCode());
        result = prime * result + ((deadLetterTopic == null) ? 0 : deadLetterTopic.hashCode());
        result = prime * result + ((rules == null) ? 0 : rules.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SubscriptionMetadata other = (SubscriptionMetadata) obj;
        if (topic == null) {
            if (other.topic != null)
                return false;
        } else if (!topic.equals(other.topic))
            return false;
        if (pubsubname == null) {
            if (other.pubsubname != null)
                return false;
        } else if (!pubsubname.equals(other.pubsubname))
            return false;
        if (deadLetterTopic == null) {
            if (other.deadLetterTopic != null)
                return false;
        } else if (!deadLetterTopic.equals(other.deadLetterTopic))
            return false;
        if (rules == null) {
            if (other.rules != null)
                return false;
        } else if (!rules.equals(other.rules))
            return false;
        return true;
    }

    

}

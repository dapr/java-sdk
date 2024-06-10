package io.dapr.client.domain;

import java.util.List;

public final class DaprMetadata {
    private String id;
    private String runtimeVersion;
    private List<ComponentMetadata> components;
    private List<SubscriptionMetadata> subscriptions;

    public DaprMetadata() {
    }

    public DaprMetadata(String id, String runtimeVersion, List<ComponentMetadata> components,
            List<SubscriptionMetadata> subscriptions) {
        this.id = id;
        this.runtimeVersion = runtimeVersion;
        this.components = components;
        this.subscriptions = subscriptions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public List<ComponentMetadata> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentMetadata> components) {
        this.components = components;
    }

    public List<SubscriptionMetadata> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<SubscriptionMetadata> subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((runtimeVersion == null) ? 0 : runtimeVersion.hashCode());
        result = prime * result + ((components == null) ? 0 : components.hashCode());
        result = prime * result + ((subscriptions == null) ? 0 : subscriptions.hashCode());
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
        DaprMetadata other = (DaprMetadata) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (runtimeVersion == null) {
            if (other.runtimeVersion != null)
                return false;
        } else if (!runtimeVersion.equals(other.runtimeVersion))
            return false;
        if (components == null) {
            if (other.components != null)
                return false;
        } else if (!components.equals(other.components))
            return false;
        if (subscriptions == null) {
            if (other.subscriptions != null)
                return false;
        } else if (!subscriptions.equals(other.subscriptions))
            return false;
        return true;
    }

}

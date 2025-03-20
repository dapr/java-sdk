package io.dapr.testcontainers;

import java.util.Collections;
import java.util.List;

public class AppHttpPipeline implements ConfigurationSettings {
    private List<ListEntry> handlers;
    
    public AppHttpPipeline(){}

    public AppHttpPipeline(List<ListEntry> handlers){
        this.handlers = Collections.unmodifiableList(handlers);
    }

    public List<ListEntry> getHandlers(){
        return handlers;
    }

}

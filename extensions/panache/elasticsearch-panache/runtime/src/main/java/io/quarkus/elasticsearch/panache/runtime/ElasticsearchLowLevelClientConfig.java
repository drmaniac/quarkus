package io.quarkus.elasticsearch.panache.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ElasticsearchLowLevelClientConfig {
    /**
     * 
     */
    @ConfigItem(defaultValue = "localhost:9200")
    String host;
    // public String[] hosts;

    /**
     * 
     */
    @ConfigItem(defaultValue = "10")
    Integer numThreads;

    public String getHost() {
        if (host == null) {
            host = "localhost:9200";
        }
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getNumThreads() {
        if (numThreads == null) {
            numThreads = 10;
        }
        return numThreads;
    }

    public void setNumThreads(Integer numThreads) {
        this.numThreads = numThreads;
    }
}

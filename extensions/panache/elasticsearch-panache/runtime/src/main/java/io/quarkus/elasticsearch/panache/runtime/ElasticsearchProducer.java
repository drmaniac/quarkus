package io.quarkus.elasticsearch.panache.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class ElasticsearchProducer {

    private ElasticsearchConfig elasticsearchConfig;

    private ElasticsearchRestClientManager restClientManager;

    @Produces
    @DefaultBean
    public ElasticsearchRestClientManager elasticsearchRestClientManager() {
        return restClientManager == null
                ? restClientManager = createDefaultElastisearchRestClientManager()
                : restClientManager;
    }

    public void setElasticsearchConfig(ElasticsearchConfig elasticsearchConfig) {
        this.elasticsearchConfig = elasticsearchConfig;
    }

    private ElasticsearchRestClientManager createDefaultElastisearchRestClientManager() {
        ElasticsearchRestClientManager elasticsearchRestClientManager = new ElasticsearchRestClientManager();
        elasticsearchRestClientManager.setElasticsearchConfig(elasticsearchConfig);
        return elasticsearchRestClientManager;
    }
}

package io.quarkus.elasticsearch.panache.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ElasticsearchRecorder {

    public BeanContainerListener setupProducer() {
        return beanContainer -> {
            beanContainer.instance(ElasticsearchProducer.class);
        };
    }

    public void setElasticsearchConfig(ElasticsearchConfig elasticsearchConfig,
            BeanContainer container) {
        container.instance(ElasticsearchProducer.class).setElasticsearchConfig(elasticsearchConfig);
    }

    public void doStartActions(ElasticsearchConfig elasticsearchConfig, BeanContainer container) {
        ElasticsearchRestClientManager manager = container.instance(ElasticsearchRestClientManager.class);
        manager.startUp();
    }
}

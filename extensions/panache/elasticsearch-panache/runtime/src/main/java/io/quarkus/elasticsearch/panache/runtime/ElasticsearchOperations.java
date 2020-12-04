package io.quarkus.elasticsearch.panache.runtime;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.elasticsearch.panache.ElasticsearchEntity;
import io.quarkus.elasticsearch.panache.PanacheElasticsearchEntityBase;

public class ElasticsearchOperations {
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchOperations.class);
    public static final String ID = "_id";

    public static void persist(final PanacheElasticsearchEntityBase entity) {
        LOGGER.info("persist: " + entity.toString());
        final ElasticsearchIndex elasticsearchIndex = elasticsearchIndex(entity);
        elasticsearchIndex.persist(entity);
    }

    public static Object findById(Class<?> entityClass, String id) {
        final ElasticsearchIndex elasticsearchIndex = elasticsearchIndex(entityClass);
        return elasticsearchIndex.findById(entityClass, id);
    }

    private static ElasticsearchIndex elasticsearchIndex(final Object entity) {
        final Class<?> entityClass = entity.getClass();
        return elasticsearchIndex(entityClass);
    }

    private static ElasticsearchIndex elasticsearchIndex(final Class<?> entityClass) {
        final ElasticsearchEntity elasticsearchEntity = entityClass.getAnnotation(ElasticsearchEntity.class);
        final ElasticsearchRestClientManager restClient = Arc.container().instance(ElasticsearchRestClientManager.class)
                .get();
        if (elasticsearchEntity != null && !elasticsearchEntity.index().isEmpty()) {
            return restClient.getIndex(elasticsearchEntity.index().toLowerCase());
        }
        return restClient.getIndex(entityClass.getSimpleName().toLowerCase());
    }

    public static List<?> listAll(PanacheElasticsearchEntityBase panacheElasticsearchEntityBase) {
        // TODO Auto-generated method stub
        return new ArrayList<>();
    }

    public static IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses");
    }

}

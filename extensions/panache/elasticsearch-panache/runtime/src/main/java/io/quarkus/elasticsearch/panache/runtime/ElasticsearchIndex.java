package io.quarkus.elasticsearch.panache.runtime;

import java.io.IOException;
import java.io.InputStream;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkus.elasticsearch.panache.PanacheElasticsearchEntityBase;

public class ElasticsearchIndex {

    private static final Logger log = Logger.getLogger(ElasticsearchIndex.class);

    final String indexName;
    final RestClient restClient;

    public ElasticsearchIndex(final String indexName, final RestClient restClient) {
        this.indexName = indexName;
        this.restClient = restClient;
    }

    /**
     * Put document to index
     * 
     * @param document to put
     */
    public void persist(final PanacheElasticsearchEntityBase entity) {

        log.info("persist entity to index (" + indexName + ")");
        try {
            final String entityString = getObjectMapper().writeValueAsString(entity);
            log.info("entity: " + entityString);
            final Request request = new Request("POST", "/" + indexName + "/_doc/");
            request.setJsonEntity(entityString);
            final Response response = restClient.performRequest(request);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                log.info("entity persited sucessfully");
            } else {
                // TODO: throw custom exception
                log.error("persist entity failed: " + response.getStatusLine().getReasonPhrase());
            }
        } catch (final IOException e) {
            // TODO: better exception handling
            log.error("exception occurred", e);
        }
    }

    public Object update(final PanacheElasticsearchEntityBase entity) {
        log.info("update entity in index (" + indexName + ")");
        log.error("Not Implemented for now");
        throw new UnsupportedOperationException("Not implemented, yes");
    }

    public Object findById(final Class<?> entityClass, final String id) {

        try {
            Request request = new Request("GET", indexName + "/_doc/" + id);
            Response response = restClient.performRequest(request);
            InputStream content = response.getEntity().getContent();
            //            objectMapper.setSerializationInclusion(Include.NON_NULL);
            ElasticsearchEntity entity = getObjectMapper().readValue(content, ElasticsearchEntity.class);

            if (entity.found) {
                return getObjectMapper().convertValue(entity._source, entityClass);
            }
        } catch (IOException e) {
            // TODO: better exception handling
            log.error("exception occurred", e);
        }

        return null;
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        return objectMapper;
    }
}

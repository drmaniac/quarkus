package io.quarkus.elasticsearch.panache.runtime;

import java.io.IOException;
import java.io.InputStream;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        log.info("persist entity to index (" + indexName + ")");
        try {
            final String entityString = objectMapper.writeValueAsString(entity);
            log.info("entity: " + entityString);
            final Request request = new Request("POST", "/" + indexName + "/_doc/");
            request.setJsonEntity(entityString);
            // log.info("transmit request to elasticsearch: " + request.toString());
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

    public Object findById(final Class<?> entityClass, final String id) {

        try {
            Request request = new Request("GET", indexName + "/_doc/" + id);
            Response response = restClient.performRequest(request);
            InputStream content = response.getEntity().getContent();
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(Include.NON_NULL);
            ElasticsearchEntity entity = objectMapper.readValue(content, ElasticsearchEntity.class);

            if (entity.found) {
                return objectMapper.convertValue(entity._source, entityClass);
            }
        } catch (IOException e) {
            // TODO: better exception handling
            log.error("exception occurred", e);
        }

        return null;
    }
}

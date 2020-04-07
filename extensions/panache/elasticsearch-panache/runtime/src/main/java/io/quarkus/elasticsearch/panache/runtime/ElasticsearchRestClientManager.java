package io.quarkus.elasticsearch.panache.runtime;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpHost;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.sniff.Sniffer;
import org.jboss.logging.Logger;

public class ElasticsearchRestClientManager {

    private static final Logger log = Logger.getLogger(ElasticsearchRestClientManager.class);

    private RestClient restClient;
    private Sniffer sniffer;

    private ElasticsearchConfig elasticsearchConfig;

    // Key: indexName, Value helper for Index
    private ConcurrentHashMap<String, ElasticsearchIndex> managedIndices = new ConcurrentHashMap<>();

    public ElasticsearchRestClientManager() {
    }

    public void setElasticsearchConfig(ElasticsearchConfig elasticsearchConfig) {
        this.elasticsearchConfig = elasticsearchConfig;
    }

    public void startUp() {
        log.info("setup elasticsearch client started");
        // create the low level rest client with custom ReactorConfig

        RestClientBuilder builder = RestClient.builder(getHttpHosts(elasticsearchConfig.lowLevelClient.host));
        builder.setHttpClientConfigCallback(cb -> cb.setDefaultIOReactorConfig(
                IOReactorConfig.custom().setIoThreadCount(elasticsearchConfig.lowLevelClient.numThreads)
                        .build()))
                .setRequestConfigCallback(rcb -> rcb.setConnectTimeout(0));

        //        SniffOnFailureListener sniffOnFailureListener = new SniffOnFailureListener();
        //        builder.setFailureListener(sniffOnFailureListener);

        log.info("try to connect to elasticsearch hosts: {}" + elasticsearchConfig.lowLevelClient.host);

        this.setRestClient(builder.build());

        //        this.setSniffer(Sniffer.builder(restClient).build());
        //        sniffOnFailureListener.setSniffer(sniffer);

        log.info("rest client started with " + elasticsearchConfig.lowLevelClient.numThreads
                + " threads");
        log.info("setup elasticsearch client completed");
    }

    public HttpHost[] getHttpHosts(String host) {
        String[] hostParts = host.split(":");
        HttpHost hosts[] = new HttpHost[1];
        hosts[0] = new HttpHost(hostParts[0], Integer.parseInt(hostParts[1]));
        return hosts;
    }

    public ElasticsearchIndex getIndex(final String indexName) {
        log.info("search managed index for " + indexName);
        managedIndices.putIfAbsent(indexName, getElasticsearchIndex(indexName));
        return managedIndices.get(indexName);
    }

    private ElasticsearchIndex getElasticsearchIndex(final String indexName) {
        ElasticsearchIndex managedElasticsearchIndex = null;
        // lookup if index exists
        // if (indexExists(indexName)) {
        // log.info("index found");
        // managedElasticsearchIndex = new ElasticsearchIndex(indexName, restClient);
        // } else { // create index if it not exists
        // log.info("index not found create it");
        // // TODO: custom index settings/mappings are not supported
        // createIndex(indexName);
        // managedElasticsearchIndex = new ElasticsearchIndex(indexName, restClient);
        // }
        managedElasticsearchIndex = new ElasticsearchIndex(indexName, restClient);
        return managedElasticsearchIndex;
    }

    private boolean indexExists(final String indexName) {
        try {
            final Response response = restClient.performRequest(new Request("HEAD", "/" + indexName));
            return 404 == response.getStatusLine().getStatusCode() ? Boolean.FALSE : Boolean.TRUE;
        } catch (IOException e) {
            // TODO: better exception handling
            log.error("exception occurred", e);
        }
        return false;
    }

    private boolean createIndex(final String indexName) {
        try {
            final Response response = restClient.performRequest(new Request("PUT", "/" + indexName));
            return 200 == response.getStatusLine().getStatusCode() ? Boolean.TRUE : Boolean.FALSE;
        } catch (IOException e) {
            // TODO: better exception handling
            log.error("exception occurred", e);
        }
        return false;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public Sniffer getSniffer() {
        return sniffer;
    }

    public void setSniffer(Sniffer sniffer) {
        this.sniffer = sniffer;
    }

    public void shutdown() {
        log.info("shutdown elasticsearch client started");

        // close sniffer
        closeSniffer();
        // close rest client
        closeRestClietn();

        log.info("shutdown elasticsearch client completed");
    }

    private void closeRestClietn() {
        if (Objects.isNull(restClient)) {
            log.warn("no rest client available to close");
        } else {
            try {
                restClient.close();
                log.info("low level rest client closed");
            } catch (IOException e) {
                log.error("rest client failed to close", e);
            }

        }
    }

    private void closeSniffer() {
        if (Objects.isNull(sniffer)) {
            if (log.isDebugEnabled()) {
                log.debug("sniffer is null");
            }
        } else {
            sniffer.close();
            log.info("sniffer closed");
        }
    }
}

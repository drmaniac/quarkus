package io.quarkus.elasticsearch.panache.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public final class ElasticsearchConfig {

    ElasticsearchLowLevelClientConfig lowLevelClient;

    /**
     * Transforms hosts string array to httpHost array
     * 
     */
    // public HttpHost[] getHttpHosts() {
    // System.out.println(hosts);
    // return Arrays.stream(hosts)
    // .map(s -> s.split(":"))
    // .map(sp -> new HttpHost(sp[0], Integer.parseInt(sp[1])))
    // .toArray(HttpHost[]::new);
    // }

    // public HttpHost[] getHttpHosts() {
    // String[] hostParts = host.split(":");
    // HttpHost hosts[] = new HttpHost[1];
    // hosts[0] = new HttpHost(hostParts[0], Integer.parseInt(hostParts[1]));
    // return hosts;
    // }

}

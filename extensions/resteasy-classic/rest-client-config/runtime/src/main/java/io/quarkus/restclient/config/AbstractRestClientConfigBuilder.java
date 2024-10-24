package io.quarkus.restclient.config;

import static io.smallrye.config.ConfigValue.CONFIG_SOURCE_COMPARATOR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.FallbackConfigSourceInterceptor;
import io.smallrye.config.Priorities;
import io.smallrye.config.RelocateConfigSourceInterceptor;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Registers and force load REST Client configuration.
 * <p>
 * To populate a <code>Map</code>, because the names are dynamic, the Config system has to rely on the list of
 * property names provided by each source. This also applies to the REST Client, but since the names are known to
 * Quarkus, the REST Client configuration could be loaded even for sources that don't provide a list of property
 * names. To achieve such behaviour, we use {@link io.smallrye.config.WithKeys} and
 * {@link io.quarkus.restclient.config.RestClientsConfig.RestClientKeysProvider} to provide the REST Client keys.
 * <p>
 * The REST Client configuration looks up the following names in order:
 *
 * <ol>
 * <li>quarkus.rest-client."[FQN of the REST Interface]".*</li>
 * <li>quarkus.rest-client.[Simple Name of the REST Interface].*</li>
 * <li>quarkus.rest-client."[Simple Name of the REST Interface]".*</li>
 * <li>quarkus.rest-client.[Config Key from @RegisterRestClient].*</li>
 * <li>quarkus.rest-client."[Config Key from @RegisterRestClient]".*</li>
 * <li>[FQN of the REST Interface]/mp-rest/*</li>
 * <li>[Config Key from @RegisterRestClient]/mp-rest/*</li>
 * </ol>
 *
 * The order follows the same logic specified in the MicroProfile Config REST Client, where the FQN names have
 * priority over {@link RegisterRestClient#configKey()}.
 * <p>
 * The {@link RegisterRestClient#configKey()} lookups are not included in case the <code>configKey</code> value is
 * <code>null</code>. A fallback is generated for a quoted key when the <code>configKey</code> is a single segment.
 * <p>
 * The concrete implementation is bytecode generated in
 * <code>io.quarkus.restclient.config.deployment.RestClientConfigUtils#generateRestClientConfigBuilder</code>
 */
public abstract class AbstractRestClientConfigBuilder implements ConfigBuilder {
    private static final String REST_CLIENT_PREFIX = "quarkus.rest-client.";

    @Override
    public SmallRyeConfigBuilder configBuilder(final SmallRyeConfigBuilder builder) {
        RestClientsConfig.RestClientKeysProvider.KEYS.clear();
        List<RegisteredRestClient> restClients = getRestClients();

        Map<String, String> quarkusFallbacks = new HashMap<>();
        Map<String, String> microProfileFallbacks = new HashMap<>();
        // relocates [All Combinations] -> quarkus.rest-client."FQN".*
        Map<String, String> relocates = new HashMap<>();
        for (RegisteredRestClient restClient : restClients) {
            RestClientsConfig.RestClientKeysProvider.KEYS.add(restClient.getFullName());

            // FQN -> Simple Name
            String quotedFullName = "\"" + restClient.getFullName() + "\"";
            quarkusFallbacks.put(quotedFullName, restClient.getSimpleName());
            relocates.put(restClient.getSimpleName(), quotedFullName);
            // Simple Name -> Quoted Simple Name
            String quotedSimpleName = "\"" + restClient.getSimpleName() + "\"";
            quarkusFallbacks.put(restClient.getSimpleName(), quotedSimpleName);
            relocates.put(quotedSimpleName, quotedFullName);

            String configKey = restClient.getConfigKey();
            if (configKey != null && !restClient.isConfigKeyEqualsNames()) {
                String quotedConfigKey = "\"" + configKey + "\"";
                if (!quotedConfigKey.equals(quotedFullName) && !quotedConfigKey.equals(quotedSimpleName)) {
                    if (restClient.isConfigKeyComposed()) {
                        // Quoted Simple Name -> Quoted Config Key
                        quarkusFallbacks.put(quotedSimpleName, quotedConfigKey);
                        relocates.put(quotedConfigKey, quotedFullName);
                    } else {
                        // Quoted Simple Name -> Config Key
                        quarkusFallbacks.put(quotedSimpleName, configKey);
                        relocates.put(configKey, quotedFullName);
                        // Config Key -> Quoted Config Key
                        quarkusFallbacks.put(configKey, quotedConfigKey);
                        relocates.put(quotedConfigKey, quotedFullName);
                    }
                }
            }

            // FQN -> FQN/mp-rest
            String mpRestFullName = restClient.getFullName() + "/mp-rest/";
            microProfileFallbacks.put(quotedFullName, mpRestFullName);
            relocates.put(mpRestFullName, quotedFullName);
            if (configKey != null && !restClient.isConfigKeyEqualsNames()) {
                String mpConfigKey = configKey + "/mp-rest/";
                microProfileFallbacks.put(mpRestFullName, mpConfigKey);
                relocates.put(mpConfigKey, quotedFullName);
            }
        }

        // Since interceptors do not keep state, we rewrite the key from the previous lookup:
        //  - FQN rewrites Simple Name
        //  - Simple Name rewrites "Simple Name" ... and so on
        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new Fallbacks(new QuarkusFallbacks(quarkusFallbacks));
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 590);
            }
        });
        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new Fallbacks(new MicroProfileFallbacks(microProfileFallbacks));
            }

            @Override
            public OptionalInt getPriority() {
                return OptionalInt.of(Priorities.LIBRARY + 595);
            }
        });
        // To rewrite the fallback names to the main name. Required for fallbacks to work properly with Maps
        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            @Override
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return new RelocateConfigSourceInterceptor(new Relocates(relocates)) {
                    @Override
                    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                        return context.proceed(name);
                    }
                };
            }
        });
        return builder;
    }

    /**
     * The List of discovered REST Clients, generated during build-time.
     *
     * @return a {@link List} of {@link RegisteredRestClient} generated by Quarkus.
     */
    public abstract List<RegisteredRestClient> getRestClients();

    /**
     * Overrides the base {@link FallbackConfigSourceInterceptor} to use {@link ConfigSourceInterceptorContext#restart}
     * instead of {@link ConfigSourceInterceptorContext#proceed}. The plan is to move the base one to use it as well,
     * but it is a breaking change so it is better to keep it locally here for now.
     */
    private static class Fallbacks extends FallbackConfigSourceInterceptor {
        public Fallbacks(final Function<String, String> mapping) {
            super(mapping);
        }

        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            ConfigValue configValue = context.proceed(name);
            String map = getMapping().apply(name);

            if (name.equals(map)) {
                return configValue;
            }

            ConfigValue fallbackValue = context.restart(map);
            // Check which one comes from a higher ordinal source to avoid defaults from the main name
            if (configValue != null && fallbackValue != null) {
                return CONFIG_SOURCE_COMPARATOR.compare(configValue, fallbackValue) >= 0 ? configValue
                        : fallbackValue.withName(name);
            }

            if (configValue != null) {
                return configValue;
            } else if (fallbackValue != null) {
                return fallbackValue.withName(name);
            }
            return null;
        }
    }

    /**
     * Fallbacks for Quarkus related names.
     */
    private record QuarkusFallbacks(Map<String, String> names) implements Function<String, String> {
        @Override
        public String apply(final String name) {
            int indexOfRestClient = indexOfRestClient(name);
            if (indexOfRestClient != -1) {
                for (Map.Entry<String, String> entry : names.entrySet()) {
                    String original = entry.getKey();
                    String target = entry.getValue();
                    int endOfConfigKey = indexOfRestClient + original.length();
                    if (name.regionMatches(indexOfRestClient, original, 0, original.length())) {
                        if (name.length() > endOfConfigKey && name.charAt(endOfConfigKey) == '.') {
                            return REST_CLIENT_PREFIX + target + name.substring(endOfConfigKey);
                        }
                    }
                }
            }
            return name;
        }
    }

    private static final Map<String, String> MICROPROFILE_NAMES = new HashMap<>();

    static {
        MICROPROFILE_NAMES.put("url", "url");
        MICROPROFILE_NAMES.put("uri", "uri");
        MICROPROFILE_NAMES.put("scope", "scope");
        MICROPROFILE_NAMES.put("providers", "providers");
        MICROPROFILE_NAMES.put("connect-timeout", "connectTimeout");
        MICROPROFILE_NAMES.put("read-timeout", "readTimeout");
        MICROPROFILE_NAMES.put("follow-redirects", "followRedirects");
        MICROPROFILE_NAMES.put("proxy-address", "proxyAddress");
        MICROPROFILE_NAMES.put("query-param-style", "queryParamStyle");
        MICROPROFILE_NAMES.put("hostname-verifier", "hostnameVerifier");
        MICROPROFILE_NAMES.put("verify-host", "verifyHost");
        MICROPROFILE_NAMES.put("trust-store", "trustStore");
        MICROPROFILE_NAMES.put("trust-store-password", "trustStorePassword");
        MICROPROFILE_NAMES.put("trust-store-type", "trustStoreType");
        MICROPROFILE_NAMES.put("key-store", "keyStore");
        MICROPROFILE_NAMES.put("key-store-password", "keyStorePassword");
        MICROPROFILE_NAMES.put("key-store-type", "keyStoreType");
        // Inverse
        MICROPROFILE_NAMES.put("connectTimeout", "connect-timeout");
        MICROPROFILE_NAMES.put("readTimeout", "read-timeout");
        MICROPROFILE_NAMES.put("followRedirects", "follow-redirects");
        MICROPROFILE_NAMES.put("proxyAddress", "proxy-address");
        MICROPROFILE_NAMES.put("queryParamStyle", "query-param-style");
        MICROPROFILE_NAMES.put("hostnameVerifier", "hostname-verifier");
        MICROPROFILE_NAMES.put("verifyHost", "verify-host");
        MICROPROFILE_NAMES.put("trustStore", "trust-store");
        MICROPROFILE_NAMES.put("trustStorePassword", "trust-store-password");
        MICROPROFILE_NAMES.put("trustStoreType", "trust-store-type");
        MICROPROFILE_NAMES.put("keyStore", "key-store");
        MICROPROFILE_NAMES.put("keyStorePassword", "key-store-password");
        MICROPROFILE_NAMES.put("keyStoreType", "key-store-type");
    }

    /**
     * Fallbacks from Quarkus names to the MP names and then between MP names.
     */
    private record MicroProfileFallbacks(Map<String, String> names) implements Function<String, String> {
        @Override
        public String apply(final String name) {
            int indexOfRestClient = indexOfRestClient(name);
            if (indexOfRestClient != -1) {
                for (Map.Entry<String, String> entry : names.entrySet()) {
                    String original = entry.getKey();
                    String target = entry.getValue();
                    int endOfConfigKey = indexOfRestClient + original.length();
                    if (name.regionMatches(indexOfRestClient, original, 0, original.length())) {
                        if (name.length() > endOfConfigKey && name.charAt(endOfConfigKey) == '.') {
                            String property = name.substring(endOfConfigKey + 1);
                            return target + MICROPROFILE_NAMES.getOrDefault(property, property);
                        }
                    }
                }
            }
            int slash = name.indexOf("/");
            if (slash != -1) {
                if (name.regionMatches(slash + 1, "mp-rest/", 0, 8)) {
                    for (Map.Entry<String, String> entry : names.entrySet()) {
                        String original = entry.getKey();
                        String target = entry.getValue();
                        if (name.regionMatches(0, original, 0, original.length())) {
                            String property = name.substring(slash + 9);
                            if (MICROPROFILE_NAMES.containsKey(property)) {
                                return target + property;
                            }
                        }
                    }
                }
            }
            return name;
        }
    }

    /**
     * Relocates every possible name (Quarkus and MP) to
     * <code>quarkus.rest-client."[FQN of the REST Interface]".*</code>
     */
    private record Relocates(Map<String, String> names) implements Function<String, String> {
        @Override
        public String apply(final String name) {
            int indexOfRestClient = indexOfRestClient(name);
            if (indexOfRestClient != -1) {
                for (Map.Entry<String, String> entry : names.entrySet()) {
                    String original = entry.getKey();
                    String target = entry.getValue();
                    int endOfConfigKey = indexOfRestClient + original.length();
                    if (name.regionMatches(indexOfRestClient, original, 0, original.length())) {
                        if (name.length() > endOfConfigKey && name.charAt(endOfConfigKey) == '.') {
                            return REST_CLIENT_PREFIX + target + name.substring(endOfConfigKey);
                        }
                    }
                }
            }
            int slash = name.indexOf("/");
            if (slash != -1) {
                if (name.regionMatches(slash + 1, "mp-rest/", 0, 8)) {
                    String property = name.substring(slash + 9);
                    if (MICROPROFILE_NAMES.containsKey(property)) {
                        return REST_CLIENT_PREFIX + "\"" + name.substring(0, slash) + "\"."
                                + MICROPROFILE_NAMES.getOrDefault(property, property);
                    }
                }
            }
            return name;
        }
    }

    private static int indexOfRestClient(final String name) {
        if (name.startsWith(REST_CLIENT_PREFIX)) {
            return 20;
        }
        return -1;
    }
}

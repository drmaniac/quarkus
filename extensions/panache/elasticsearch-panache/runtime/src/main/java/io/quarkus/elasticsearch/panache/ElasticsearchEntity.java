package io.quarkus.elasticsearch.panache;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation can be used to specify some configuration of the mapping of an entity to
 * Elasticsearch.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ElasticsearchEntity {
    /**
     * The name of the index (if not set the name of the entity class will be used)
     */
    String index() default "";

    /**
     * List of alias name for read queries (if not set aliases will not be used)
     */
    String[] aliases() default {};

    /**
     * the name of the database (if not set the default from the property
     * <code>quarkus.elasticsearch.database</code> will be used.
     */
    String database() default "";

    /**
     * Custom index settings which will be applied on index creation (no update!). e.g. custom
     * analysis, mappings, index shard allocation etc.
     * 
     * @see <a href=
     *      "https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#_settings_in_other_index_modules">Settings
     *      in other index modules</a>
     */
    String settings() default ""; // TODO: check if this is sufficient or it is better to use specialized annotations

    /**
     * Custom mappings setting which will be applied on index creation (no update!).
     * 
     * @see <a href=
     *      "https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html">Mapping</a>
     */
    String mappings() default ""; // TODO: check if this is sufficient or it is better to use specialized annotations

}

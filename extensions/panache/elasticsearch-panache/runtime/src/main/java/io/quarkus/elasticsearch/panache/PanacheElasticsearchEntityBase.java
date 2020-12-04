package io.quarkus.elasticsearch.panache;

import java.util.stream.Stream;

import io.quarkus.elasticsearch.panache.runtime.ElasticsearchOperations;
import io.quarkus.mongodb.panache.runtime.MongoOperations;
import io.quarkus.panache.common.impl.GenerateBridge;

/**
 * Represents an entity. If your Elasticsearch entities extend this class they gain auto-generated accessors
 * to all their public fields, as well as a lot of useful
 * methods. Unless you have a custom ID strategy, you should not extend this class directly but extend
 * {@link PanacheElasticsearchEntity} instead.
 *
 * @see PanacheElasticsearchEntity
 */
public abstract class PanacheElasticsearchEntityBase {

    // Operations

    /**
     * Persist this entity in the database.
     * This will set it's ID field if not already set.
     *
     * @see #persist(Iterable)
     * @see #persist(Stream)
     * @see #persist(Object, Object...)
     */
    public void persist() {
        ElasticsearchOperations.persist(this);
    }
    
    /**
     * Update this entity in the database.
     *
     * @see #update(Iterable)
     * @see #update(Stream)
     * @see #update(Object, Object...)
     */
    public void update() {
      ElasticsearchOperations.update(this);
    }

    /**
     * Find an entity of this type by ID.
     *
     * @param id the ID of the entity to find.
     * @return the entity found, or <code>null</code> if not found.
     */
    @GenerateBridge(targetReturnTypeErased = true)
    public static <T extends PanacheElasticsearchEntityBase> T findById(String id) {
        throw ElasticsearchOperations.implementationInjectionMissing();
    }
}

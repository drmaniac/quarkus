package io.quarkus.elasticsearch.panache;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Represents an entity with a generated ID field {@link #id} of type {@link ObjectId}. If your
 * Mongo entities extend this class they gain the ID field and auto-generated accessors to all their
 * public fields, as well as all the useful methods from {@link PanacheElasticsearchEntityBase}.
 *
 * If you want a custom ID type or strategy, you can directly extend
 * {@link PanacheElasticsearchEntityBase} instead, and write your own ID field. You will still get
 * auto-generated accessors and all the useful methods.
 *
 * @see PanacheElasticsearchEntityBase
 */
public abstract class PanacheElasticsearchEntity extends PanacheElasticsearchEntityBase {

    /**
     * The auto-generated ID field. This field is set by Elasticsearch when this entity is persisted.
     *
     * @see #persist()
     */
    @JsonIgnore
    public String _id;

    @JsonIgnore
    public String _type;

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "<" + _id + "|" + _type + ">";
    }
}

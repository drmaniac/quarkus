package io.quarkus.elasticsearch.panache.runtime;

import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticsearchEntity {

    @JsonbProperty("_index")
    public String _index;
    public String _type;
    public String _id;
    public String _version;
    public String _seq_no;
    public String _primary_term;
    public Boolean found;

    public Map<String, Object> _source;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ElasticsearchEntity [_index=").append(_index)
                .append(", _type=").append(_type)
                .append(", _id=").append(_id)
                .append(", _version=").append(_version)
                .append(", _seq_no=").append(_seq_no)
                .append(", _primary_term=").append(_primary_term)
                .append(", found=").append(found)
                .append(", _source=")
                .append(_source.toString()).append("]");

        return builder.toString();
    }

}

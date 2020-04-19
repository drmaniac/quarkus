package io.quarkus.elasticsearch.panache.jsonb;

import java.time.LocalDate;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;

public class LocalDateSerializer implements JsonbSerializer<LocalDate> {

  @Override
  public void serialize(LocalDate obj, JsonGenerator generator, SerializationContext ctx) {
    if(obj != null) {
      generator.write(obj.toString());
    }
  }

}

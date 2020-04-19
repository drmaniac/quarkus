package io.quarkus.elasticsearch.panache.jsonb;

import java.lang.reflect.Type;
import java.time.LocalDate;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;

public class LocalDateDeserializer implements JsonbDeserializer<LocalDate> {

  @Override
  public LocalDate deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
    String date = parser.getString();
    if(date != null) {
      return LocalDate.parse(date);
    }
    return null;
  }


}

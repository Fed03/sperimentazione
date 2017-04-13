package it.fed03;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import it.unifi.cassandra.scheduling.model.Assertion;
import it.unifi.cassandra.scheduling.model.Person;
import it.unifi.cassandra.scheduling.util.TimeInterval;

import java.lang.reflect.Type;
import java.util.Map;

public class MapSerializer implements JsonSerializer<Map<Person, Map<Assertion, Map<TimeInterval, Integer>>>> {
    @Override
    public JsonElement serialize(Map<Person, Map<Assertion, Map<TimeInterval, Integer>>> personMap, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonArray array = new JsonArray();
        personMap.forEach((person, assertionMap) -> {
            JsonObject object = (JsonObject) jsonSerializationContext.serialize(person);
            object.add("assertions", jsonSerializationContext.serialize(assertionMap, new TypeToken<Map<Assertion, Map<TimeInterval, Integer>>>() {}.getType()));

            array.add(object);
        });
        return array;
    }
}

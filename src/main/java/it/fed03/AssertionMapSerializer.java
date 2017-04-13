package it.fed03;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import it.unifi.cassandra.scheduling.model.Assertion;
import it.unifi.cassandra.scheduling.util.TimeInterval;

import java.lang.reflect.Type;
import java.util.Map;

public class AssertionMapSerializer implements JsonSerializer<Map<Assertion, Map<TimeInterval, Integer>>> {
    @Override
    public JsonElement serialize(Map<Assertion, Map<TimeInterval, Integer>> assertionMapMap, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonArray array = new JsonArray();
        assertionMapMap.forEach((assertion, alloc) -> {
            JsonObject object = (JsonObject) jsonSerializationContext.serialize(assertion, Assertion.class);
            object.add("allocations", jsonSerializationContext.serialize(alloc, new TypeToken<Map<TimeInterval, Integer>>() {}.getType()));

            array.add(object);
        });

        return array;
    }
}

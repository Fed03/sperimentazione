package it.fed03;

import com.google.gson.*;
import it.unifi.cassandra.scheduling.util.TimeInterval;

import java.lang.reflect.Type;
import java.util.Map;

public class AllocMapSerializer implements JsonSerializer<Map<TimeInterval, Integer>> {
    @Override
    public JsonElement serialize(Map<TimeInterval, Integer> timeIntervalIntegerMap, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonArray array = new JsonArray();
        timeIntervalIntegerMap.forEach((interval, amount) -> {
            JsonObject o = new JsonObject();
            o.addProperty("startInclusive", interval.getStartInclusive().toString());
            o.addProperty("endExclusive", interval.getEndExclusive().toString());
            o.addProperty("amount", amount);

            array.add(o);
        });

        return array;
    }
}

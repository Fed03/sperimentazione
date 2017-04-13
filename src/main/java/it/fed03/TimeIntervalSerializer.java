package it.fed03;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import it.unifi.cassandra.scheduling.util.TimeInterval;

import java.lang.reflect.Type;

public class TimeIntervalSerializer implements JsonSerializer<TimeInterval> {
    @Override
    public JsonElement serialize(TimeInterval timeInterval, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("startInclusive", timeInterval.getStartInclusive().toString());
        obj.addProperty("endExclusive", timeInterval.getEndExclusive().toString());

        return obj;
    }
}

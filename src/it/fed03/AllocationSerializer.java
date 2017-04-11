package it.fed03;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import it.unifi.cassandra.scheduling.model.Allocation;

import java.lang.reflect.Type;

public class AllocationSerializer implements JsonSerializer<Allocation> {
    @Override
    public JsonElement serialize(Allocation allocation, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("day", allocation.getDay().toString());
        obj.addProperty("hours", allocation.getHoursAmount());
        return obj;
    }
}

package it.fed03;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import it.unifi.cassandra.scheduling.model.Allocation;
import it.unifi.cassandra.scheduling.model.Assertion;

import java.lang.reflect.Type;

public class AssertionSerializer implements JsonSerializer<Assertion> {
    @Override
    public JsonElement serialize(Assertion assertion, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.add("assignedPerson", jsonSerializationContext.serialize(assertion.assignedPerson()));
        obj.addProperty("releaseTime", assertion.releaseTime().toString());
        obj.addProperty("deadline", assertion.deadline().toString());
        obj.addProperty("computationTime", assertion.computationTime());
        obj.add("allocations", jsonSerializationContext.serialize(assertion.allocations()));

        return obj;
    }
}

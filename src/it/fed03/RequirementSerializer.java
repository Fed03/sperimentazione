package it.fed03;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import it.unifi.cassandra.scheduling.model.Requirement;

import java.lang.reflect.Type;

public class RequirementSerializer implements JsonSerializer<Requirement> {
    @Override
    public JsonElement serialize(Requirement requirement, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", requirement.name());
        obj.addProperty("computationTime", requirement.computationTime());
        obj.addProperty("startDate", requirement.timeInterval().getStartInclusive().toString());
        obj.addProperty("deadline", requirement.timeInterval().getEndExclusive().toString());
        obj.add("assignedPeople", jsonSerializationContext.serialize(requirement.assignedPeople()));
        obj.add("assertions", jsonSerializationContext.serialize(requirement.assertions()));
        return obj;
    }
}

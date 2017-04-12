package it.fed03;

import it.unifi.cassandra.scheduling.model.Person;

public class Researcher implements Person {
    public final String name;

    public Researcher(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getIdentifier() {
        return name;
    }
}

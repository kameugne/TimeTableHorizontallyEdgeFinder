package org.example;

import java.util.Comparator;

class EventByDate implements Comparator<Event> {
    public int compare(Event a, Event b){
        return a.date - b.date;
    }
}
package org.example;

class Event {
    int type;
    int task_index;
    int date;

    public Event (int type, int task_index, int date) {
        this.type = type;
        this.task_index = task_index;
        this.date = date;
    }

    @Override
    public String toString() {
        return "(type : "+ this.type + ", tasks index : " + this.task_index + ", date : "+ this.date +")" ;
    }
}








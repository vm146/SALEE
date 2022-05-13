package com.example.helloworld;

import java.util.ArrayList;
import java.util.HashMap;

// TODO: levels sometimes negative/sometimes positive depending on labeling-sequence..

public class appliance {
    /** class which initializes an appliance, e.g. event is sent from server, user gets asked by app
     * which appliance was turned on. User answers and response creates a new object "appliance"
     * which will be added to an appliance ArrayList.
     */
    String name;
    HashMap<String, Float> events;                   // time and consumption
    HashMap<Integer, Integer> levels;               // level of consumption
    Integer counter;                                // save state of appliance (e.g. already on)


    public appliance(String name, HashMap<String, Float> event, HashMap<Integer, Integer> level,
                     Integer counter) {
        /** initialize appliance
         *      Args:
         *          name(String): name of the appliance, given by user
         *          event(HashMap<String, Float>): a HashMap stored with all events assigned to
         *          this appliance.
         *              key(String)  : event time
         *              value(float): power consumption of event
         *          level(HashMap<Integer, Integer>): a HashMap stored with the different levels of
         *          the power consumption of the appliance (i.e. hairdryer: 1 lv 1200W, 2lv 2000W)
         *              key(Integer): level
         *              value(Integer): power consumption on this level
         *          counter(Integer): a simple counter for the appliance. Helps to set the different
         *              levels of power consumption
         */
        this.name = name;
        this.events = event;
        this.levels = level;
        this.counter = counter;
    }

    public String getName() {
        /** Get name of appliance */
        return name;
    }
    public HashMap<String, Float> getEvents() {
        /** Get events of appliance*/
        return events;
    }
    // Note: getLevels is unfortunately not reliable. AlertDialog(pop-up) pushes unlabeled
    // events sometimes in lifo/sometimes in fifo queue. Thus the levels can be negative..
    public HashMap<Integer, Integer> getLevels() {
        /** Get levels of power consumption of appliance */
        return levels;
    }
    public Integer getCounter() {
        /** Get current level of appliance */
        return counter;
    }
    public void raiseCounter() {
        /** Raise counter of appliance */
        counter++;
    }
    public void lowerCounter() {
        /** Lower counter of appliance */
        counter--;
    }
}

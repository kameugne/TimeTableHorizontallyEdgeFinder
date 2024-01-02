package org.example;

import java.util.Comparator;

public class ttTaskAttribute {
    public int ind;
    public int est;
    public int pfix;
    public int pfree;
    public int efree;
    public boolean free;

    public ttTaskAttribute(int ind, int est, int pfix, int pfree, int efree, boolean free) {
        this.ind = ind;
        this.est = est;
        this.pfix = pfix;
        this.pfree = pfree;
        this.efree = efree;
        this.free = free;
    }

    public static class ComparatorByFreeEct implements Comparator<Integer> {
        private  ttTaskAttribute[] ttTasks;
        public ComparatorByFreeEct( ttTaskAttribute[] list_of_ttTasks) {
            this.ttTasks = list_of_ttTasks;
        }
        @Override
        public int compare(Integer a, Integer b) {
            return ttTasks[a.intValue()].est + ttTasks[a.intValue()].pfree - ttTasks[b.intValue()].est - ttTasks[b.intValue()].pfree;
        }
    }

    public static class ComparatorByEstPlus implements Comparator<Integer> {
        private  ttTaskAttribute[] ttTasks;
        public ComparatorByEstPlus( ttTaskAttribute[] list_of_ttTasks) {
            this.ttTasks = list_of_ttTasks;
        }
        @Override
        public int compare(Integer a, Integer b) {
            if (ttTasks[a.intValue()].est == ttTasks[b.intValue()].est) {
                return ttTasks[b.intValue()].est + ttTasks[b.intValue()].pfree - ttTasks[a.intValue()].est - ttTasks[a.intValue()].pfree;
            }else {
                return ttTasks[a.intValue()].est  - ttTasks[b.intValue()].est;
            }

        }
    }
    public String toString() {
        return "(" +  ind + " ; " +est + " ; "+ pfix + " ; " + pfree + " ; "+ efree + " ; " + free+ ")";
    }
}

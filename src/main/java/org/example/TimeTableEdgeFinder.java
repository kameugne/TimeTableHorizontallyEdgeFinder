package org.example;

import java.util.Arrays;
import java.util.Comparator;

public class TimeTableEdgeFinder {
    private static final int EST = 0; // est
    private static final int LRT = 1; // ect
    private static final int ERT = 2; // lst
    private static final int LCT = 3; // lct
    private final Integer C;
    private final Integer n;
    private final Task[] tasks;
    Integer makespan;
    private final int[] estPrime;
    private final Integer[] tasks_indices_est;
    private final Integer[] tasks_indices_lct;
    private final Integer[] tasks_indices_ect;
    Profile tl;
    public TimeTableEdgeFinder(Task[] tasks, int C){
        this.C = C;
        this.n = tasks.length;
        this.tasks = tasks;
        estPrime = new int[tasks.length];
        this.tl = new Profile();
        tasks_indices_est = sortWithJavaLibrary(tasks, new Task.ComparatorByEst(tasks)); //Increasing EST
        tasks_indices_lct = sortWithJavaLibrary(tasks, new Task.ComparatorByLct(tasks)); //Increasing LCT
        tasks_indices_ect = sortWithJavaLibrary(tasks, new Task.ComparatorByEct(tasks)); //Increasing ECT

        makespan = Integer.MIN_VALUE;

        InitializeTimeLine();
    }

    private int MandatoryIn(int est, int lct, int i) {
        return Math.max(0, Math.min(lct, tasks[i].earliestCompletionTime()) - Math.max(est, tasks[i].latestStartingTime()));
    }
    private int MaxAddIn(int reserve, int cap) {
        return (int)Math.floor((double)reserve/(double)cap);
    }

    private int sureIn(int i, int lct) {return tasks[i].height() * Math.max(0, lct - (tasks[i].latestCompletionTime() - tasks[i].pfree()));}

    public boolean OverloadCheck()
    {
        ttTaskAttribute[] tt = new ttTaskAttribute[tasks.length];
        for (int i = tasks.length-1; i >= 0; i--) {
            int est = tasks[tasks_indices_est[i]].earliestStartingTime();
            int pfix = Math.max(0, tasks[tasks_indices_est[i]].earliestCompletionTime() - tasks[tasks_indices_est[i]].latestStartingTime());
            int pfree = tasks[tasks_indices_est[i]].processingTime() - pfix;
            boolean free = (pfree > 0);
            int efree = pfree * tasks[tasks_indices_est[i]].height();
            tt[tasks_indices_est[i]] = new ttTaskAttribute(tasks[tasks_indices_est[i]].id(), est, pfix, pfree, efree, free);
        }

        int[] ttAfterEst = new int[tasks.length];
        int[] ttAfterLct = new int[tasks.length];

        Integer[] byEstPlus = sortWithJavaLibrary(tt, new ttTaskAttribute.ComparatorByEstPlus(tt));
        Integer[] byFreeEct = sortWithJavaLibrary(tt, new ttTaskAttribute.ComparatorByFreeEct(tt));

        Event[] events = new Event[4*tasks.length+1] ;
        int n = 0;
        for (int i = 0; i < tasks.length; i++) {
            if (tasks[i].isConsistent()) {
                events[n++] = new Event(ERT, i, tasks[i].latestStartingTime());
                events[n++] = new Event(LRT, i, tasks[i].earliestCompletionTime());
            }
            else {
                events[n++] = new Event(EST, i, tasks[i].earliestStartingTime());
                events[n++] = new Event(LCT, i, tasks[i].latestCompletionTime());
                if (tasks[i].hasFixedPart()) {
                    events[n++] = new Event(ERT, i, tasks[i].latestStartingTime());
                    events[n++] = new Event(LRT, i, tasks[i].earliestCompletionTime());
                }
            }
        }

        // last event as sentinel
        events[n++] = new Event(-1, -1, Integer.MAX_VALUE);
        Event[] eventsToSort = new Event[n];
        for (int i = 0; i < n; i++)
            eventsToSort[i] = events[i];


        Arrays.sort(eventsToSort, new EventByDate());

        n -= 1;
        eventsToSort[n].date = eventsToSort[n-1].date;

        // sweep over all events in anti-chronological order
        n -= 1;
        int ccur = 0;
        int energy = 0;
        while (n >= 0) {
            energy += ccur * (eventsToSort[n+1].date - eventsToSort[n].date);
            switch (eventsToSort[n].type) {
                case LRT:
                    ccur += tasks[eventsToSort[n].task_index].height();
                    break;
                case ERT:
                    ccur -= tasks[eventsToSort[n].task_index].height();
                    break;
                case LCT:
                    ttAfterLct[eventsToSort[n].task_index] = energy;
                    break;
                case EST:
                    ttAfterEst[eventsToSort[n].task_index] = energy;
                    break;
                default:
                    break;
            }
            n--;
        }
        int maxi, eef, reserve;
        for (int i = tasks.length-1; i >=0; i--) {
            int b = tasks_indices_est[i];
            if (tt[b].free) {
                eef = 0;
                maxi = -1;
                for (int j = tasks.length-1; j >= 0; j--) { // a in T^ef by decreasing est_a
                    int a = tasks_indices_est[j];
                    if (tt[a].free) {
                        if (tasks[a].latestCompletionTime() <= tasks[b].latestCompletionTime()) {
                            eef += tt[a].efree;
                            reserve = C * (tasks[b].latestCompletionTime() - tasks[a].earliestStartingTime()) - eef - (ttAfterEst[tasks[a].id()] - ttAfterLct[tasks[b].id()]);
                            if (reserve < 0)
                                return false;
                        }
                    }
                }
            }
        }
        return true;
    }





    public int[] Filter() {
        ttTaskAttribute[] tt = new ttTaskAttribute[tasks.length];
        InitializeIncrements(tasks.length - 1);
        makespan = ScheduleTasks(tasks[tasks_indices_lct[tasks.length - 1]].latestCompletionTime());
        for (int i = tasks.length-1; i >= 0; i--) {
            estPrime[i] = tasks[i].earliestStartingTime();
            int est = tasks[tasks_indices_est[i]].earliestStartingTime();
            int pfix = Math.max(0, tasks[tasks_indices_est[i]].earliestCompletionTime() - tasks[tasks_indices_est[i]].latestStartingTime());
            int pfree = tasks[tasks_indices_est[i]].processingTime() - pfix;
            boolean free = (pfree > 0);
            int efree = pfree * tasks[tasks_indices_est[i]].height();
            tt[tasks_indices_est[i]] = new ttTaskAttribute(tasks[tasks_indices_est[i]].id(), est, pfix, pfree, efree, free);
        }
        if(makespan > tasks[tasks_indices_lct[tasks_indices_lct.length - 1]].latestCompletionTime())
            return null;

        int[] ttAfterEst = new int[tasks.length];
        int[] ttAfterLct = new int[tasks.length];

        Integer[] byEstPlus = sortWithJavaLibrary(tt, new ttTaskAttribute.ComparatorByEstPlus(tt));
        Integer[] byFreeEct = sortWithJavaLibrary(tt, new ttTaskAttribute.ComparatorByFreeEct(tt));

        Event[] events = new Event[4*tasks.length+1] ;
        int n = 0;
        for (int i = 0; i < tasks.length; i++) {
            if (tasks[i].isConsistent()) {
                events[n++] = new Event(ERT, i, tasks[i].latestStartingTime());
                events[n++] = new Event(LRT, i, tasks[i].earliestCompletionTime());
            }
            else {
                events[n++] = new Event(EST, i, tasks[i].earliestStartingTime());
                events[n++] = new Event(LCT, i, tasks[i].latestCompletionTime());
                if (tasks[i].hasFixedPart()) {
                    events[n++] = new Event(ERT, i, tasks[i].latestStartingTime());
                    events[n++] = new Event(LRT, i, tasks[i].earliestCompletionTime());
                }
            }
        }

        // last event as sentinel
        events[n++] = new Event(-1, -1, Integer.MAX_VALUE);
        Event[] eventsToSort = new Event[n];
        for (int i = 0; i < n; i++)
            eventsToSort[i] = events[i];


        Arrays.sort(eventsToSort, new EventByDate());

        n -= 1;
        eventsToSort[n].date = eventsToSort[n-1].date;

        // sweep over all events in anti-chronological order
        n -= 1;
        int ccur = 0;
        int energy = 0;
        while (n >= 0) {
            energy += ccur * (eventsToSort[n+1].date - eventsToSort[n].date);
            switch (eventsToSort[n].type) {
                case LRT:
                    ccur += tasks[eventsToSort[n].task_index].height();
                    break;
                case ERT:
                    ccur -= tasks[eventsToSort[n].task_index].height();
                    break;
                case LCT:
                    ttAfterLct[eventsToSort[n].task_index] = energy;
                    break;
                case EST:
                    ttAfterEst[eventsToSort[n].task_index] = energy;
                    break;
                default:
                    break;
            }
            n--;
        }

        int maxi, eef, reserve;
        for (int i = tasks.length-1; i >= 0; i--) {
            int b = tasks_indices_est[i];
            if (tt[b].free) {
                // "Inside" and "Right"
                eef = 0;
                maxi = -1;
                for (int j = tasks.length-1; j >= 0; j--) { // a in T^ef by decreasing est_a
                    int a = tasks_indices_est[j];
                    if (tt[a].free && tasks[a].earliestStartingTime() < tasks[b].latestCompletionTime()) {
                        if (tasks[a].latestCompletionTime() <= tasks[b].latestCompletionTime()) {
                            eef += tt[a].efree;
                        } else {
                            if (maxi == -1 || Math.min(tt[a].efree, tasks[a].height() * (tasks[b].latestCompletionTime() - tasks[a].earliestStartingTime()))  > Math.min(tt[maxi].efree, tasks[maxi].height() * (tasks[b].latestCompletionTime() - tasks[maxi].earliestStartingTime())) ) {
                                maxi = a;
                            }
                        }
                        reserve = C * (tasks[b].latestCompletionTime() - tasks[a].earliestStartingTime()) - eef - (ttAfterEst[tasks[a].id()] - ttAfterLct[tasks[b].id()]);
                        if (reserve < 0)
                            return null;//defaultEstPrime;
                        if (maxi != -1) {
                            int addEn = Math.min(tt[maxi].efree, tasks[maxi].height() * (tasks[b].latestCompletionTime() - tasks[maxi].earliestStartingTime())) ;
                            if (reserve < addEn) {
                                int est = tasks[b].latestCompletionTime() - MandatoryIn(tasks[a].earliestStartingTime(), tasks[b].latestCompletionTime(), maxi) - MaxAddIn(reserve, tasks[maxi].height());
                                estPrime[maxi] = Math.max(estPrime[maxi], est);
                            }
                        }
                    }
                }
                // "Through"
                maxi = -1;
                for (int j = 0; j < tasks.length; j++) {
                    int a = byEstPlus[j];
                    if (tt[a].free && tasks[a].earliestStartingTime() < tasks[b].latestCompletionTime()) {
                        if (tasks[a].latestCompletionTime() <= tasks[b].latestCompletionTime()) {
                            reserve = C * (tasks[b].latestCompletionTime() - tasks[a].earliestStartingTime()) - eef - (ttAfterEst[tasks[a].id()] - ttAfterLct[tasks[b].id()]);
                            if (reserve < 0)
                                return null;//defaultEstPrime;
                            if(maxi != -1 && reserve < tasks[maxi].height() * (tasks[b].latestCompletionTime() - tasks[a].earliestStartingTime())) {
                                int est = tasks[b].latestCompletionTime() - MandatoryIn(tasks[a].earliestStartingTime(),tasks[b].latestCompletionTime(),maxi) - MaxAddIn(reserve, tasks[maxi].height());
                                estPrime[maxi] = Math.max(estPrime[maxi], est);
                            }
                            eef -= tt[a].efree;
                        }
                        if (tasks[a].earliestStartingTime() + tt[a].pfree >= tasks[b].latestCompletionTime() &&
                                (maxi == -1 || tasks[a].height() > tasks[maxi].height())) {
                            maxi = a;
                        }
                    }
                }
            }
        }
        // "Left"
        for (int a = tasks.length-1; a >= 0; a--) {
            if (tt[a].free) {
                eef = 0;
                maxi = -1;
                int q = 0;
                for (int k = 0; k < tasks.length; k++) { // b in T^ef by increasing est_b
                    int b = tasks_indices_lct[k];
                    if (tt[b].free) {
                        if (tasks[a].earliestStartingTime() <= tasks[b].earliestStartingTime()) {
                            eef += tt[b].efree;
                            while (q < tasks.length && tt[byFreeEct[q]].est + tt[byFreeEct[q]].pfree < tasks[b].latestCompletionTime()) {
                                if (tt[byFreeEct[q]].est < tasks[a].earliestStartingTime() &&
                                        tasks[a].earliestStartingTime() < tt[byFreeEct[q]].est + tt[byFreeEct[q]].pfree &&
                                        (maxi == -1 ||
                                                tasks[byFreeEct[q]].height() * (tt[byFreeEct[q]].est + tt[byFreeEct[q]].pfree - tasks[a].earliestStartingTime()) >
                                                        tasks[maxi].height() * (tasks[maxi].earliestStartingTime() + tt[maxi].pfree - tasks[a].earliestStartingTime()))) {
                                    maxi = byFreeEct[q];
                                }
                                q++;
                            }
                            reserve = C * (tasks[b].latestCompletionTime() - tasks[a].earliestStartingTime()) - eef - (ttAfterEst[tasks[a].id()] - ttAfterLct[tasks[b].id()]);
                            if (reserve < 0)
                                return null;//defaultEstPrime;
                            if (maxi != -1 && reserve < tasks[maxi].height() * (tt[maxi].est + tt[maxi].pfree - tasks[a].earliestStartingTime())) {
                                int est = tasks[b].latestCompletionTime() - MandatoryIn(tasks[a].earliestStartingTime(), tasks[b].latestCompletionTime(),maxi) - MaxAddIn(reserve, tasks[maxi].height());
                                estPrime[maxi] = Math.max(estPrime[maxi] , est);
                            }
                        }
                    }
                }
            }
        }
        return estPrime;
    }


    private int ScheduleTasks(int maxLCT)
    {
        int hreq, hmaxInc, ov, ect;
        ect = Integer.MIN_VALUE;
        ov = hreq = hmaxInc = 0;
        Timepoint t = tl.first;

        while(t.time < maxLCT)
        {
            int l = t.next.time - t.time;

            hmaxInc += t.incrementMax;
            t.hMaxTotal = hmaxInc;
            int hmax = Math.min(hmaxInc, C);
            hreq += t.increment;

            int hcons = Math.min(hreq + ov, hmax);

            if(ov > 0 && ov < (hcons - hreq) * l)
            {
                l = Math.max(1, ov / (hcons-hreq));
                t.InsertAfter(new Timepoint(t.time + l, t.capacity));
            }
            ov += (hreq - hcons) * l;


            t.capacity = C - hcons;

            if(t.capacity < C)
                ect = t.next.time;

            t = t.next;
        }
        if(ov > 0)
            return Integer.MAX_VALUE;

        return ect;
    }



    private void InitializeIncrements(int maxIndex)
    {
        Timepoint t = tl.first;
        while(t != null)
        {
            t.increment = 0;
            t.incrementMax = 0;
            t.hMaxTotal = 0;
            t.hreal = 0;
            t.minimumOverflow = 0;
            t.overflow = 0;
            t.capacity = C;

            t = t.next;
        }
        for(int i = 0; i <= maxIndex; i++)
        {
            t = tasks[tasks_indices_lct[i]].est_to_timepoint;
            t.increment += tasks[tasks_indices_lct[i]].height();
            t.incrementMax += tasks[tasks_indices_lct[i]].height();

            t = tasks[tasks_indices_lct[i]].ect_to_timepoint;
            t.increment -= tasks[tasks_indices_lct[i]].height();

            t = tasks[tasks_indices_lct[i]].lct_to_timepoint;
            t.incrementMax -= tasks[tasks_indices_lct[i]].height();
        }
    }


    private void InitializeTimeLine()
    {
        int n = tasks.length;
        tl.Add(new Timepoint(tasks[tasks_indices_est[0]].earliestStartingTime(), C));
        Timepoint t = tl.first;

        int p,i,j,k;
        p = i = j = k = 0;

        int maxLCT = Integer.MIN_VALUE;

        while(i < n || j < n || k < n)
        {
            if(i<n && (j == n || tasks[tasks_indices_est[i]].earliestStartingTime() <= tasks[tasks_indices_ect[j]].earliestCompletionTime()) &&
                    (k == n || tasks[tasks_indices_est[i]].earliestStartingTime() <= tasks[tasks_indices_lct[k]].latestCompletionTime()))
            {
                if(tasks[tasks_indices_est[i]].earliestStartingTime() > t.time)
                {
                    t.InsertAfter(new Timepoint(tasks[tasks_indices_est[i]].earliestStartingTime(), C));
                    t = t.next;
                }
                tasks[tasks_indices_est[i]].est_to_timepoint = t;
                p += tasks[tasks_indices_est[i]].processingTime();
                maxLCT = Math.max(maxLCT, tasks[tasks_indices_est[i]].latestCompletionTime());

                tasks[tasks_indices_est[i]].inLambda = false;

                i++;
            }
            else if(j < n && (k==n || tasks[tasks_indices_ect[j]].earliestCompletionTime() <= tasks[tasks_indices_lct[k]].latestCompletionTime()))
            {
                if(tasks[tasks_indices_ect[j]].earliestCompletionTime() > t.time)
                {
                    t.InsertAfter(new Timepoint(tasks[tasks_indices_ect[j]].earliestCompletionTime(), C));
                    t = t.next;
                }
                tasks[tasks_indices_ect[j]].ect_to_timepoint = t;
                j++;
            }
            else
            {
                if(tasks[tasks_indices_lct[k]].latestCompletionTime() > t.time)
                {
                    t.InsertAfter(new Timepoint(tasks[tasks_indices_lct[k]].latestCompletionTime(), C));
                    t = t.next;
                }
                tasks[tasks_indices_lct[k]].lct_to_timepoint = t;
                k++;
            }

        }
        t.InsertAfter(new Timepoint(maxLCT + p, 0));
    }








    /* ------------ Utility Functions --------------*/
    private static Integer[] sortWithJavaLibrary(Task[] tasks, Comparator<Integer> comparator) {

        int n = tasks.length;
        Integer[] tasks_indices = new Integer[n];
        for (int q = 0; q < n; q++) {
            tasks_indices[q] = q;
        }
        Arrays.sort(tasks_indices, comparator);
        return tasks_indices;
    }

    private static Integer[] sortWithJavaLibrary(ttTaskAttribute[] tt, Comparator<Integer> comparator) {

        int n = tt.length;
        Integer[] tt_indices = new Integer[n];
        for (int q = 0; q < n; q++) {
            tt_indices[q] = q;
        }
        Arrays.sort(tt_indices, comparator);
        return tt_indices;
    }

    private static Event[] sortWithJavaLibrary(Event[] events, Comparator<Event> comparator) {

        int n = events.length;
        Event[] e = new Event[n];
        for (int q = 0; q < n; q++) {
            e[q] = events[q];
        }
        Arrays.sort(e, comparator);
        return e;
    }


}

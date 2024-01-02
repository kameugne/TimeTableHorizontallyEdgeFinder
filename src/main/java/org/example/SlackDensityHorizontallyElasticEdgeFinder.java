package org.example;

import java.util.Arrays;
import java.util.Comparator;

public class SlackDensityHorizontallyElasticEdgeFinder {
    Integer C;
    Task[] tasks;
    Profile tl;
    Timepoint tinit;
    Integer[] Prec;

    Integer[] tasks_indices_lct;
    Integer[] tasks_indices_lst;
    Integer[] tasks_indices_est;
    Integer[] tasks_indices_ect;
    Integer[] indexEst;
    Integer[] Energy;
    Integer makespan;
    private final int[] estPrime;

    public SlackDensityHorizontallyElasticEdgeFinder(Task[] tasks, int C)
    {
        this.C = C;
        this.tasks = tasks;
        this.tl = new Profile();
        estPrime = new int[tasks.length];

        Prec = new Integer[tasks.length];
        Arrays.fill(Prec, -1);
        indexEst = new Integer[tasks.length];
        Energy = new Integer[tasks.length];
        Arrays.fill(indexEst, -1);
        Arrays.fill(Energy, 0);


        tasks_indices_lct = sortWithJavaLibrary(tasks, new Task.ComparatorByLct(tasks)); //Increasing LCT
        tasks_indices_lst = sortWithJavaLibrary(tasks, new Task.ComparatorByLst(tasks)); //Increasing LST
        tasks_indices_est = sortWithJavaLibrary(tasks, new Task.ComparatorByEst(tasks)); //Increasing EST
        tasks_indices_ect = sortWithJavaLibrary(tasks, new Task.ComparatorByEct(tasks)); //Increasing ECT

        makespan = Integer.MIN_VALUE;

        InitializeTimeLine();
    }

    public int[] Filter()
    {
        int[] results;
        if(EdgeFinder_Detection()) {
            results = EdgeFinder_Prunning();
            return results;
        } else {
            return null;
        }
    }


    private boolean EdgeFinder_Detection()
    {
        InitializeIncrements(tasks_indices_lct.length - 1);
        makespan = ScheduleTasks(tasks[tasks_indices_lct[tasks_indices_lct.length - 1]].latestCompletionTime());
        if(makespan > tasks[tasks_indices_lct[tasks_indices_lct.length - 1]].latestCompletionTime())
            return false;
        int[] minSlack = new int[tasks.length];
        Arrays.fill(minSlack, Integer.MAX_VALUE);
        int[] indLctS = new int[tasks.length];
        Arrays.fill(indLctS, -1);

        double[] maxDensity = new double[tasks.length];
        Arrays.fill(maxDensity, 0.0);
        int[] indLctD = new int[tasks.length];
        Arrays.fill(indLctD, -1);

        for(int j = 0; j < tasks.length; j++) {
            while (j+1 < tasks.length && tasks[tasks_indices_lct[j]].latestCompletionTime() == tasks[tasks_indices_lct[j+1]].latestCompletionTime())
                j++;
            int E = 0;
            int[] En = new int[tasks.length]; double maxDen = 0.0;
            for (int l = tasks.length-1; l >= 0; l--) {
                if (tasks[tasks_indices_est[l]].latestCompletionTime() <= tasks[tasks_indices_lct[j]].latestCompletionTime()) {
                    E += tasks[tasks_indices_est[l]].energy();
                    double density = (double)E/(tasks[tasks_indices_lct[j]].latestCompletionTime() - tasks[tasks_indices_est[l]].earliestStartingTime());
                    if (density >= maxDen) {
                        maxDen = density;
                    }
                } else {
                    if (maxDen >= maxDensity[tasks_indices_est[l]]) {
                        maxDensity[tasks_indices_est[l]] = maxDen;
                        indLctD[tasks_indices_est[l]] = j;
                    }
                }
                En[l] = E;
            }
            int minS = Integer.MAX_VALUE;
            for (int l = 0; l < tasks.length; l++) {
                int slack = C * (tasks[tasks_indices_lct[j]].latestCompletionTime() - tasks[tasks_indices_est[l]].earliestStartingTime()) - En[l];
                if (slack < minS) {
                    minS = slack;
                }
                if (tasks[tasks_indices_est[l]].latestCompletionTime() > tasks[tasks_indices_lct[j]].latestCompletionTime()) {
                    if (minS < minSlack[tasks_indices_est[l]]) {
                        minSlack[tasks_indices_est[l]] = minS;
                        indLctS[tasks_indices_est[l]] = j;
                    }
                }
            }
        }
        for (int i = tasks.length-1; i > 0; i--) {
            if (tasks[tasks_indices_lct[i]].earliestCompletionTime() < tasks[tasks_indices_lct[i]].latestCompletionTime()) {
                if (indLctS[tasks_indices_lct[i]] != -1) {
                    InitializeIncrementsDetect(indLctS[tasks_indices_lct[i]], i);
                    int ect = ScheduleTasks(tasks[tasks_indices_lct[indLctS[tasks_indices_lct[i]]]].latestCompletionTime());
                    if (ect > tasks[tasks_indices_lct[indLctS[tasks_indices_lct[i]]]].latestCompletionTime()) {
                        Prec[tasks_indices_lct[i]] = indLctS[tasks_indices_lct[i]];
                    }
                }
                if (Prec[tasks_indices_lct[i]] == -1 && indLctD[tasks_indices_lct[i]] != -1 && indLctD[tasks_indices_lct[i]] != indLctS[tasks_indices_lct[i]]) {
                    InitializeIncrementsDetect(indLctD[tasks_indices_lct[i]], i);
                    int ect = ScheduleTasks(tasks[tasks_indices_lct[indLctD[tasks_indices_lct[i]]]].latestCompletionTime());
                    if (ect > tasks[tasks_indices_lct[indLctD[tasks_indices_lct[i]]]].latestCompletionTime()) {
                        Prec[tasks_indices_lct[i]] = indLctD[tasks_indices_lct[i]];
                    }
                }
            }
        }
        return true;
    }



    private int[] EdgeFinder_Prunning()
    {
        for (int i = 0; i < tasks.length; i++)
            estPrime[i] = tasks[i].earliestStartingTime();
        for(int i=0; i < tasks.length; i++)
        {
            if(Prec[i] != -1)
            {
                InitializeIncrementsPruning(Prec[i], i);
                int  maxOv = ComputeMinimumOverflow(i, tasks[tasks_indices_lct[Prec[i]]].latestCompletionTime());
                if(maxOv > 0)
                {
                    int est = ComputeBound(tasks[i].height(), maxOv);
                    if(est > tasks[i].earliestStartingTime())
                    {
                        estPrime[i] = Math.max(estPrime[i], est);
                    }
                }
            }
        }
        return estPrime;
    }



    private int ComputeMinimumOverflow(int i, int lct)
    {
        int ov, h, hmax, maxOverflow;
        ov = h = hmax = maxOverflow = 0;
        int c = C - tasks[i].height();


        int avail = 0; int ovPart = 0; int ov1 = 0;

        tinit = tl.first;
        while(tinit.time < lct)
        {
            int l = tinit.next.time - tinit.time;
            tinit.overflow = ov;
            h += tinit.increment;
            hmax += tinit.incrementMax;

            tinit.hreal = h;
            tinit.hMaxTotal = hmax;

            int cmax = Math.min(hmax,c);

            int hhm = Math.min(hmax, C);
            tinit.avail = avail;
            int hcons = Math.min(h + ov1, hhm);
            if(ov1 > 0 && ov1 < (hcons - h) * l)
            {
                l = Math.max(1, ov1 / (hcons-h));
                tinit.InsertAfter(new Timepoint(tinit.time + l, tinit.capacity));
            }
            ov1 += (h - hcons) * l;
            if (tinit.time >= tasks[i].earliestStartingTime() && tasks[i].earliestCompletionTime() < lct)
                avail += Math.min(C-hcons, tasks[i].height()) * l;

            int f = cmax - h;

            if(ov < f*l)
            {
                ov = 0;
            }
            else
            {
                ov = Math.max(0, ov + (Math.max(0, h-cmax) - Math.max(0,  f)) * l);
                if (tinit.time == tasks[i].earliestCompletionTime() || tinit.next.time == lct) {
                    maxOverflow = ov;
                }
                if (tinit.time >= tasks[i].earliestCompletionTime() && maxOverflow > 0) {
                    maxOverflow -= Math.max(0, Math.max(0, f) * l);
                }
                if (tasks[i].earliestCompletionTime() < lct) {
                    if (tinit.time == tasks[i].earliestCompletionTime()) {
                        ovPart = ov;
                    }
                    if (tinit.time >= tasks[i].earliestCompletionTime() && ovPart > 0) {
                        ovPart -= Math.max(0, Math.max(0, f) * l);
                    }
                }
            }
            tinit = tinit.next;
        }
        tinit.avail = avail;
        tinit.overflow = ov;
        int maxOv = tinit.overflow;
        int min = Integer.MAX_VALUE;

        while(tinit != null)
        {
            min = Math.min(min, tinit.overflow);
            tinit.minimumOverflow = min;

            if(min == 0)
                break;
            tinit = tinit.previous;
        }
        if (tasks[i].earliestCompletionTime() < lct) {
            if (avail >= tasks[i].energy())
                return ovPart;
            else
                return maxOv;
        } else {
            return maxOverflow;
        }
    }


    private int ComputeBound(int hi, int maxOv)
    {
        int est = Integer.MIN_VALUE;
        int hreal, hmax, hcons, ov, d, time;
        d = ov = 0;
        Timepoint t = tinit;
        time = t.time;

        hreal = t.hreal;
        hmax = t.hMaxTotal;

        while(t.next != null)
        {
            int l = t.next.time - time;
            int c;
            c = Math.min(hmax, C);

            hcons = Math.min(hreal + ov, c);

            boolean next = true;
            if(ov > 0 && ov < (hcons - hreal) * l)
            {
                if(ov <= hcons-hreal)
                    l = 1;
                else
                    l = ov / (hcons-hreal);
                next = false;
            }

            int dreal;
            if(hcons <= (C - hi))
                dreal = 0;
            else
                dreal = Math.min(t.next.minimumOverflow - d,(hcons - (C - hi))*l);

            if(d + dreal >= maxOv)
            {
                est = Math.min(t.next.time, time + (int)Math.ceil((double)(maxOv - d) / (double)(hcons-(C-hi))));
                return est;
            }

            d+=dreal;
            ov += (hreal - hcons)*l;

            if(next)
            {
                t = t.next;
                time = t.time;
                hreal += t.increment;
                hmax += t.incrementMax;
            }
            else
                time += l;
        }
        return est;
    }








    private int ScheduleTasks(int maxLCT)
    {
        int hreq, hmaxInc, ov, ect, slack;
        ect = Integer.MIN_VALUE;
        ov = hreq = hmaxInc = slack = 0;
        Timepoint t = tl.first;

        while(t.time < maxLCT)
        {
            int l = t.next.time - t.time;

            hmaxInc += t.incrementMax;
            t.hMaxTotal = hmaxInc;
            int hmax = Math.min(hmaxInc, C);
            hreq += t.increment;

            int hcons = Math.min(hreq + ov, hmax);
            t.slack = slack;

            if(ov > 0 && ov < (hcons - hreq) * l)
            {
                l = Math.max(1, ov / (hcons-hreq));
                t.InsertAfter(new Timepoint(t.time + l, t.capacity));
            }
            ov += (hreq - hcons) * l;

            t.capacity = C - hcons;

            if(t.capacity < C)
                ect = t.next.time;
            slack += (hmax - hcons)*l;
            t = t.next;
        }
        t.slack = slack;
        if(ov > 0)
            return Integer.MAX_VALUE;

        return ect;
    }

    private void InitializeTimeLine()
    {
        int n = tasks.length;
        tl.Add(new Timepoint(tasks[tasks_indices_est[0]].earliestStartingTime(), C));
        Timepoint t = tl.first;

        int p,i,j,k,l;
        p = i = j = k = l = 0;

        int maxLCT = Integer.MIN_VALUE;

        while(i < n || j < n || k < n || l < n)
        {
            if(i<n && (j == n || tasks[tasks_indices_est[i]].earliestStartingTime() <= tasks[tasks_indices_ect[j]].earliestCompletionTime()) &&
                    (k == n || tasks[tasks_indices_est[i]].earliestStartingTime() <= tasks[tasks_indices_lct[k]].latestCompletionTime()) &&
                    (l == n || tasks[tasks_indices_est[i]].earliestStartingTime() <= tasks[tasks_indices_lst[l]].latestStartingTime()))
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
            else if(j < n && (k==n || tasks[tasks_indices_ect[j]].earliestCompletionTime() <= tasks[tasks_indices_lct[k]].latestCompletionTime()) &&
                    (l==n || tasks[tasks_indices_ect[j]].earliestCompletionTime() <= tasks[tasks_indices_lst[l]].latestStartingTime()))
            {
                if(tasks[tasks_indices_ect[j]].earliestCompletionTime() > t.time)
                {
                    t.InsertAfter(new Timepoint(tasks[tasks_indices_ect[j]].earliestCompletionTime(), C));
                    t = t.next;
                }
                tasks[tasks_indices_ect[j]].ect_to_timepoint = t;
                j++;
            }
            else if(k < n &&  (l == n || tasks[tasks_indices_lct[k]].latestCompletionTime() <= tasks[tasks_indices_lst[l]].latestStartingTime()))
            {
                if(tasks[tasks_indices_lct[k]].latestCompletionTime() > t.time)
                {
                    t.InsertAfter(new Timepoint(tasks[tasks_indices_lct[k]].latestCompletionTime(), C));
                    t = t.next;
                }
                tasks[tasks_indices_lct[k]].lct_to_timepoint = t;
                k++;
            }
            else{
                if(tasks[tasks_indices_lst[l]].latestStartingTime() > t.time)
                {
                    t.InsertAfter(new Timepoint(tasks[tasks_indices_lst[l]].latestStartingTime(), C));
                    t = t.next;
                }
                tasks[tasks_indices_lst[l]].lst_to_timepoint = t;
                l++;
            }
        }
        t.InsertAfter(new Timepoint(maxLCT + p, 0));
    }


    private void InitializeIncrementsPruning(int maxIndex, int u)
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
        for(int i = 0; i < tasks.length; i++)
        {
            if (tasks[tasks_indices_lct[i]].latestCompletionTime() <= tasks[tasks_indices_lct[maxIndex]].latestCompletionTime()) {
                t = tasks[tasks_indices_lct[i]].est_to_timepoint;
                t.increment += tasks[tasks_indices_lct[i]].height();
                t.incrementMax += tasks[tasks_indices_lct[i]].height();

                t = tasks[tasks_indices_lct[i]].ect_to_timepoint;
                t.increment -= tasks[tasks_indices_lct[i]].height();

                t = tasks[tasks_indices_lct[i]].lct_to_timepoint;
                t.incrementMax -= tasks[tasks_indices_lct[i]].height();
            } else {
                if (tasks[tasks_indices_lct[i]].id() != tasks[u].id() && tasks[tasks_indices_lct[i]].hasFixedPart() && tasks[tasks_indices_lct[i]].latestStartingTime() < tasks[tasks_indices_lct[maxIndex]].latestCompletionTime()) {
                    t = tasks[tasks_indices_lct[i]].lst_to_timepoint;
                    t.increment += tasks[tasks_indices_lct[i]].height();
                    t.incrementMax += tasks[tasks_indices_lct[i]].height();
                    if (tasks[tasks_indices_lct[i]].earliestCompletionTime() < tasks[tasks_indices_lct[maxIndex]].latestCompletionTime()) {
                        t = tasks[tasks_indices_lct[i]].ect_to_timepoint;
                        t.increment -= tasks[tasks_indices_lct[i]].height();
                        t.incrementMax -= tasks[tasks_indices_lct[i]].height();
                    } else {
                        t = tasks[tasks_indices_lct[maxIndex]].lct_to_timepoint;
                        t.increment -= tasks[tasks_indices_lct[i]].height();
                        t.incrementMax -= tasks[tasks_indices_lct[i]].height();
                    }
                }
            }
        }
    }



    private void InitializeIncrementsDetect(int maxIndex, int u)
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
        for(int i = 0; i < tasks.length; i++)
        {
            if (tasks[tasks_indices_lct[i]].latestCompletionTime() <= tasks[tasks_indices_lct[maxIndex]].latestCompletionTime()) {
                t = tasks[tasks_indices_lct[i]].est_to_timepoint;
                t.increment += tasks[tasks_indices_lct[i]].height();
                t.incrementMax += tasks[tasks_indices_lct[i]].height();

                t = tasks[tasks_indices_lct[i]].ect_to_timepoint;
                t.increment -= tasks[tasks_indices_lct[i]].height();

                t = tasks[tasks_indices_lct[i]].lct_to_timepoint;
                t.incrementMax -= tasks[tasks_indices_lct[i]].height();
            } else {
                if (i != u && tasks[tasks_indices_lct[i]].hasFixedPart() && tasks[tasks_indices_lct[i]].latestStartingTime() < tasks[tasks_indices_lct[maxIndex]].latestCompletionTime()) {
                    t = tasks[tasks_indices_lct[i]].lst_to_timepoint;
                    t.increment += tasks[tasks_indices_lct[i]].height();
                    t.incrementMax += tasks[tasks_indices_lct[i]].height();
                    if (tasks[tasks_indices_lct[i]].earliestCompletionTime() < tasks[tasks_indices_lct[maxIndex]].latestCompletionTime()) {
                        t = tasks[tasks_indices_lct[i]].ect_to_timepoint;
                        t.increment -= tasks[tasks_indices_lct[i]].height();
                        t.incrementMax -= tasks[tasks_indices_lct[i]].height();
                    } else {
                        t = tasks[tasks_indices_lct[maxIndex]].lct_to_timepoint;
                        t.increment -= tasks[tasks_indices_lct[i]].height();
                        t.incrementMax -= tasks[tasks_indices_lct[i]].height();
                    }
                }
            }
        }
        t = tasks[tasks_indices_lct[u]].est_to_timepoint;
        t.increment += tasks[tasks_indices_lct[u]].height();
        t.incrementMax += tasks[tasks_indices_lct[u]].height();
        if (tasks[tasks_indices_lct[u]].earliestCompletionTime() < tasks[tasks_indices_lct[maxIndex]].latestCompletionTime()) {
            t = tasks[tasks_indices_lct[u]].ect_to_timepoint;
            t.increment -= tasks[tasks_indices_lct[u]].height();
            t.incrementMax -= tasks[tasks_indices_lct[u]].height();
        } else {
            t = tasks[tasks_indices_lct[maxIndex]].lct_to_timepoint;
            t.increment -= tasks[tasks_indices_lct[u]].height();
            t.incrementMax -= tasks[tasks_indices_lct[u]].height();
        }

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
        for(int i = 0; i < tasks.length; i++)
        {
            if (tasks[tasks_indices_lct[i]].latestCompletionTime() <= tasks[tasks_indices_lct[maxIndex]].latestCompletionTime()) {
                t = tasks[tasks_indices_lct[i]].est_to_timepoint;
                t.increment += tasks[tasks_indices_lct[i]].height();
                t.incrementMax += tasks[tasks_indices_lct[i]].height();

                t = tasks[tasks_indices_lct[i]].ect_to_timepoint;
                t.increment -= tasks[tasks_indices_lct[i]].height();

                t = tasks[tasks_indices_lct[i]].lct_to_timepoint;
                t.incrementMax -= tasks[tasks_indices_lct[i]].height();
            }
        }
    }

    private static Integer[] sortWithJavaLibrary(Task[] tasks, Comparator<Integer> comparator) {
        int n = tasks.length;
        Integer[] tasks_indices = new Integer[n];
        for (int q = 0; q < n; q++) {
            tasks_indices[q] = q;
        }
        Arrays.sort(tasks_indices, comparator);
        return tasks_indices;
    }
}

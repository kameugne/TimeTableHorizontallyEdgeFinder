package org.example;

import java.util.Arrays;
import java.util.Comparator;

public class HorizontallyElasticEdgeFinder {
    Integer C;
    Task[] tasks;
    Profile tl;
    Timepoint tinit;
    Integer[] Prec;

    Integer[] tasks_indices_lct;
    Integer[] tasks_indices_est;
    Integer[] tasks_indices_ect;
    Integer makespan;
    private final int[] estPrime;

    public HorizontallyElasticEdgeFinder(Task[] tasks, int C)
    {
        this.C = C;
        this.tasks = tasks;
        this.tl = new Profile();
        estPrime = new int[tasks.length];

        Prec = new Integer[tasks.length];
        Arrays.fill(Prec, -1);


        tasks_indices_lct = sortWithJavaLibrary(tasks, new Task.ComparatorByLct(tasks)); //Increasing LCT
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
        for (int i = tasks.length-1; i > 0; i--) {
            if (tasks[tasks_indices_lct[i]].earliestCompletionTime() < tasks[tasks_indices_lct[i]].latestCompletionTime()) {
                int index = InitializeIncrementsEFSet(i);
                if (index != -1) {
                    int Lct = tasks[tasks_indices_lct[index]].latestCompletionTime();
                    int ect = ScheduleTasks(Lct);
                    if (ect > Lct) {
                        Prec[tasks_indices_lct[i]] = index;
                    } else {
                        int En = 0; boolean isDetected = false;
                        int j1 = -1; int Exp1 = Integer.MAX_VALUE;
                        int j2 = -1; int Exp2 = Integer.MAX_VALUE;
                        for (int j = 0; j < tasks.length; j++) {
                            if (tasks[tasks_indices_lct[j]].latestCompletionTime() < tasks[tasks_indices_lct[i]].latestCompletionTime()) {
                                En += tasks[tasks_indices_lct[j]].energy();
                                if (tasks[tasks_indices_lct[j]].latestCompletionTime() <= tasks[tasks_indices_lct[i]].earliestCompletionTime() &&
                                        tasks[tasks_indices_lct[i]].earliestStartingTime() < tasks[tasks_indices_lct[j]].latestCompletionTime()) {
                                    int exp = (C - tasks[tasks_indices_lct[i]].height())  * tasks[tasks_indices_lct[j]].latestCompletionTime() - En;
                                    if (exp < Exp2 && tasks[tasks_indices_lct[j]].lct_to_timepoint.overflow > 0) {
                                        Exp2 = exp;
                                        j2 = j;
                                    }
                                }
                                if (tasks[tasks_indices_lct[j]].latestCompletionTime() > tasks[tasks_indices_lct[i]].earliestCompletionTime()) {
                                    int exp = C * tasks[tasks_indices_lct[j]].latestCompletionTime() - En;
                                    if (exp < Exp1 && tasks[tasks_indices_lct[j]].lct_to_timepoint.overflow > 0) {
                                        Exp1 = exp;
                                        j1 = j;
                                    }
                                }
                            }
                        }
                        if (j1 != -1) {
                            InitializeIncrementsDetect(j1, i);
                            int ect1 = ScheduleTasks(tasks[tasks_indices_lct[j1]].latestCompletionTime());
                            if (ect1 > tasks[tasks_indices_lct[j1]].latestCompletionTime()) {
                                Prec[tasks_indices_lct[i]] = j1;
                                isDetected = true;
                            }
                        }
                        if (!isDetected && j2 != -1) {
                            InitializeIncrementsDetect(j2, i);
                            int ect1 = ScheduleTasks(tasks[tasks_indices_lct[j2]].latestCompletionTime());
                            if (ect1 > tasks[tasks_indices_lct[j2]].latestCompletionTime()) {
                                Prec[tasks_indices_lct[i]] = j2;
                            }
                        }
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
                InitializeIncrements(Prec[i]);
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
            t.overflow = ov;

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
        t.overflow = ov;
        if(ov > 0)
            return Integer.MAX_VALUE;

        return ect;
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

    private int InitializeIncrementsEFSet(int u) {
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
        int Lct = Integer.MIN_VALUE;
        int index = -1;
        for(int i = 0; i < tasks.length; i++)
        {
            if (tasks[tasks_indices_lct[i]].latestCompletionTime() < tasks[tasks_indices_lct[u]].latestCompletionTime()) {
                t = tasks[tasks_indices_lct[i]].est_to_timepoint;
                t.increment += tasks[tasks_indices_lct[i]].height();
                t.incrementMax += tasks[tasks_indices_lct[i]].height();

                t = tasks[tasks_indices_lct[i]].ect_to_timepoint;
                t.increment -= tasks[tasks_indices_lct[i]].height();

                t = tasks[tasks_indices_lct[i]].lct_to_timepoint;
                t.incrementMax -= tasks[tasks_indices_lct[i]].height();
                if (tasks[tasks_indices_lct[i]].latestCompletionTime() > Lct) {
                    index = i;
                    Lct = tasks[tasks_indices_lct[index]].latestCompletionTime();
                }
            }
        }

        if (index != -1) {
            t = tasks[tasks_indices_lct[u]].est_to_timepoint;
            t.increment += tasks[tasks_indices_lct[u]].height();
            t.incrementMax += tasks[tasks_indices_lct[u]].height();
            if (tasks[tasks_indices_lct[u]].earliestCompletionTime() < Lct) {
                t = tasks[tasks_indices_lct[u]].ect_to_timepoint;
                t.increment -= tasks[tasks_indices_lct[u]].height();
                t.incrementMax -= tasks[tasks_indices_lct[u]].height();
            } else {
                t = tasks[tasks_indices_lct[index]].lct_to_timepoint;
                t.increment -= tasks[tasks_indices_lct[u]].height();
                t.incrementMax -= tasks[tasks_indices_lct[u]].height();
            }
        }

        return index;
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
            t.overlap = 0;
            t.slackOver = 0;
            t.slackUnder = 0;
            t.contact = null;
            t.avail = 0;
            t.cons = 0;
            t.hmax = 0;
            t.slack = 0;
            t.energy = 0;

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

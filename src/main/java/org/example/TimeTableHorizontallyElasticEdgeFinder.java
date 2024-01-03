package org.example;

import java.util.Arrays;
import java.util.Comparator;

public class TimeTableHorizontallyElasticEdgeFinder {
   Integer C;
   Task[] tasks;
   private final Profile tl;
   private final int[] Prec;
   private final int[] MaxOver;

   private final Integer[] tasks_indices_lct;
   private final Integer[] tasks_indices_lst;
   private final Integer[] tasks_indices_h;
   private final Integer[] tasks_indices_est;
   private final Integer[] tasks_indices_ect;
   Integer makespan;
   private final int[] estPrime;


   public int[] Filter(){
       if(TimeTable_Horizontally_EdgeFinder_Detection()){
           return  TimeTable_Horizontally_EdgeFinder_Prunning();
       } else {
           return null;
       }
   }


   public TimeTableHorizontallyElasticEdgeFinder(Task[] tasks, int C)
   {
       this.C = C;
       this.tasks = tasks;
       this.tl = new Profile();
       estPrime = new int[tasks.length];
       Prec = new int[tasks.length];
       Arrays.fill(Prec, -1);
       MaxOver = new int[tasks.length];
       Arrays.fill(MaxOver, 0);

       tasks_indices_lct = sortWithJavaLibrary(tasks, new Task.ComparatorByLct(tasks)); //Increasing LCT
       tasks_indices_lst = sortWithJavaLibrary(tasks, new Task.ComparatorByLst(tasks)); //Increasing LST
       tasks_indices_est = sortWithJavaLibrary(tasks, new Task.ComparatorByEst(tasks)); //Increasing EST
       tasks_indices_ect = sortWithJavaLibrary(tasks, new Task.ComparatorByEct(tasks)); //Increasing ECT
       tasks_indices_h = sortWithJavaLibrary(tasks, new Task.ComparatorByHeight(tasks)); //Increasing Height

       makespan = Integer.MAX_VALUE;

       InitializeTimeLine();
   }




   public boolean TimeTable_Horizontally_OverloadCheck()
   {
       InitializeIncrements(tasks_indices_lct.length - 1);
       if(ScheduleTasks(tasks[tasks_indices_lct[tasks_indices_lct.length - 1]].latestCompletionTime(), C) > tasks[tasks_indices_lct[tasks_indices_lct.length - 1]].latestCompletionTime())
           return false;

       for(int i=tasks_indices_lct.length - 1; i>0; i--)
       {
           while(i > 0 && tasks[tasks_indices_lct[i]].latestCompletionTime() == tasks[tasks_indices_lct[i-1]].latestCompletionTime())
           {
               i--;
           }

           if(i == 0)
               return true;

           InitializeIncrementsDetection(i-1);
           if(ScheduleTasks(tasks[tasks_indices_lct[i-1]].latestCompletionTime(), C) > tasks[tasks_indices_lct[i-1]].latestCompletionTime())
               return false;
       }
       return true;
   }



   private boolean TimeTable_Horizontally_EdgeFinder_Detection()
   {
       InitializeIncrements(tasks_indices_lct.length - 1);
       makespan = ScheduleTasks(tasks[tasks_indices_lct[tasks_indices_lct.length - 1]].latestCompletionTime());
       if(makespan > tasks[tasks_indices_lct[tasks_indices_lct.length - 1]].latestCompletionTime())
           return false;
       for(int i = tasks_indices_lct.length -1; i > 0; i--)
       {
           while(i > 0 && tasks[tasks_indices_lct[i]].latestCompletionTime() == tasks[tasks_indices_lct[i-1]].latestCompletionTime())
           {
               tasks[tasks_indices_lct[i]].inLambda = true;
               i--;
           }
           tasks[tasks_indices_lct[i]].inLambda = true;
           if(i == 0)
               return true;
           int min = 0; int max = min + 1;
           while(max <= tasks.length)
           {
               if(max == tasks.length || tasks[tasks_indices_h[max]].height() > tasks[tasks_indices_h[min]].height()){
                   InitializeIncrementsDetection(i-1);
                   int ect = ScheduleTasksD(tasks[tasks_indices_lct[i-1]].latestCompletionTime(), tasks[tasks_indices_h[min]].height());
                   if(ect > tasks[tasks_indices_lct[i-1]].latestCompletionTime())
                       return false;
                   DetectePrecedences(i-1, min, max-1);
                   min = max;
               }
               max++;
           }
       }
       return true;
   }

   private int[] TimeTable_Horizontally_EdgeFinder_Prunning(){
       for (int i = 0; i < tasks.length; i++)
           estPrime[i] = tasks[i].earliestStartingTime();
       for(int i = 0; i < tasks.length; i++){
           if(Prec[i] != -1){
               InitializeIncrements(Prec[i], i);
               boolean status = true;
               int ect = ScheduleTasks(tasks[tasks_indices_lct[Prec[i]]].latestCompletionTime(), tasks[i].height());
               if(MaxOver[i] > 0){
                   int est = computeBound(i, MaxOver[i], status);
                   if(est > tasks[i].earliestStartingTime())
                       estPrime[i] = Math.max(estPrime[i], est);
               }
           }
       }
       return estPrime;
   }

   private int computeBound(int i, int maxOver, boolean status){
       int est = Integer.MIN_VALUE;
       Timepoint t = tasks[i].est_to_timepoint.contact;
       while (t.next != null) {
           int overlap = t.next.overlap - t.overlap;
           if (maxOver > overlap) {
               maxOver -= overlap;
               t = t.next;
           } else {
               est = Math.min(t.next.time, t.time + (int) Math.ceil((double) maxOver / (double) (t.cons - (C - tasks[i].height()))));
               return est;
           }
       }
       return est;
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
           t.avail = 0;
           t.overflow = 0;
           t.capacity = C;
           t.overlap = 0;
           t.slackOver = 0;
           t.slackUnder = 0;
           t.contact = null;
           t.cons = 0;

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

   private void InitializeIncrements(int maxIndex, int u)
   {
       Timepoint t = tl.first;
       while(t != null)
       {
           t.increment = 0;
           t.incrementMax = 0;
           t.hMaxTotal = 0;
           t.hreal = 0;
           t.minimumOverflow = 0;
           t.avail = 0;
           t.overflow = 0;
           t.capacity = C;
           t.overlap = 0;
           t.slackOver = 0;
           t.slackUnder = 0;
           t.contact = null;
           t.cons = 0;

           t = t.next;
       }
       for(int i = 0; i < tasks.length; i++)
       {
           if (i <= maxIndex) {
               t = tasks[tasks_indices_lct[i]].est_to_timepoint;
               t.increment += tasks[tasks_indices_lct[i]].height();
               t.incrementMax += tasks[tasks_indices_lct[i]].height();

               t = tasks[tasks_indices_lct[i]].ect_to_timepoint;
               t.increment -= tasks[tasks_indices_lct[i]].height();

               t = tasks[tasks_indices_lct[i]].lct_to_timepoint;
               t.incrementMax -= tasks[tasks_indices_lct[i]].height();
           } else {
               if (tasks[tasks_indices_lct[i]].id() != tasks[u].id() && tasks[tasks_indices_lct[i]].hasFixedPart() &&
                   tasks[tasks_indices_lct[i]].latestStartingTime() < tasks[tasks_indices_lct[maxIndex]].latestCompletionTime()) {
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

   private void InitializeIncrementsDetection(int maxIndex)
   {
       Timepoint t = tl.first;
       while(t != null)
       {
           t.increment = 0;
           t.incrementMax = 0;
           t.hMaxTotal = 0;
           t.hreal = 0;
           t.minimumOverflow = 0;
           t.avail = 0;
           t.overflow = 0;
           t.capacity = C;
           t.overlap = 0;
           t.slackOver = 0;
           t.slackUnder = 0;
           t.contact = null;
           t.cons = 0;

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
               if (tasks[tasks_indices_lct[i]].hasFixedPart() &&
                       tasks[tasks_indices_lct[i]].latestStartingTime() < tasks[tasks_indices_lct[maxIndex]].latestCompletionTime()) {
                   t = tasks[tasks_indices_lct[i]].lst_to_timepoint;
                   t.increment += tasks[tasks_indices_lct[i]].height();
                   t.incrementMax += tasks[tasks_indices_lct[i]].height();
                   if (tasks[tasks_indices_lct[i]].earliestCompletionTime() <  tasks[tasks_indices_lct[maxIndex]].latestCompletionTime()) {
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


   private int ScheduleTasksD(int maxLCT, int h)
   {
       int hreq, hmaxInc, ov, ect, overlap, slackUnder, slackOver, avail;
       ect = Integer.MIN_VALUE;
       ov = hreq = hmaxInc = overlap = slackUnder = slackOver = avail = 0;
       Timepoint t = tl.first;

       while(t.time < maxLCT)
       {
           int l = t.next.time - t.time;

           hmaxInc += t.incrementMax;
           t.hMaxTotal = hmaxInc;
           int hmax = Math.min(hmaxInc, C);
           hreq += t.increment;
           t.overlap = overlap;
           t.slackUnder = slackUnder;
           t.slackOver = slackOver;
           t.avail = avail;

           int hcons = Math.min(hreq + ov, hmax);
           t.cons = hcons;

           if(ov > 0 && ov < (hcons - hreq) * l)
           {
               l = Math.max(1, ov / (hcons-hreq));
               t.InsertAfter(new Timepoint(t.time + l, t.capacity));
           }
           ov += (hreq - hcons) * l;

           if(hcons > C - h) {
               t.contact = t;
           }


           t.capacity = C - hcons;

           overlap += Math.max(hcons - (C - h), 0) * l;
           slackOver += Math.max(hmax - Math.max(C - h, hcons), 0) * l;
           slackUnder += Math.max(Math.min(C - h, hmax) - hcons, 0) * l;
           avail += Math.min(C-hcons, h) * l;

           if(t.capacity < C)
               ect = t.next.time;

           t = t.next;
       }
       t.overlap = overlap;
       t.slackUnder = slackUnder;
       t.slackOver = slackOver;
       t.avail = avail;
       Timepoint best = null;
       while (t != null) {
           if (best == null) {
               if (t.contact != null)
                   best = t.contact;
           } else {
               if (t.contact == null)
                   t.contact = best;
               else {
                   if (best.overlap - t.contact.overlap <= best.slackUnder - t.contact.slackUnder) {
                       t.contact = best;
                   } else {
                       best = t;
                   }
               }
           }
           t = t.previous;
       }
       if(ov > 0) {
           return Integer.MAX_VALUE;
       }

       return ect;
   }



   private int ScheduleTasks(int maxLCT, int h)
   {
       int hreq, hmaxInc, ov, ect, overlap, slackUnder, slackOver, avail;
       ect = Integer.MIN_VALUE;
       ov = hreq = hmaxInc = overlap = slackUnder = slackOver = avail = 0;
       Timepoint t = tl.first;

       while(t.time < maxLCT)
       {
           int l = t.next.time - t.time;

           hmaxInc += t.incrementMax;
           t.hMaxTotal = hmaxInc;
           int hmax = Math.min(hmaxInc, C);
           hreq += t.increment;
           t.overlap = overlap;
           t.slackUnder = slackUnder;
           t.slackOver = slackOver;
           t.avail = avail;

           int hcons = Math.min(hreq + ov, hmax);
           t.cons = hcons;

           if(ov > 0 && ov < (hcons - hreq) * l)
           {
               l = Math.max(1, ov / (hcons-hreq));
               t.InsertAfter(new Timepoint(t.time + l, t.capacity));
           }
           ov += (hreq - hcons) * l;
           if(hcons > C - h) {
               t.contact = t;
           }


           t.capacity = C - hcons;

           overlap += Math.max(hcons - (C - h), 0) * l;
           slackOver += Math.max(hmax - Math.max(C - h, hcons), 0) * l;
           slackUnder += Math.max(Math.min(C - h, hmax) - hcons, 0) * l;
           avail += Math.min(C-hcons, h) * l;

           if(t.capacity < C)
               ect = t.next.time;

           t = t.next;
       }
       t.overlap = overlap;
       t.slackUnder = slackUnder;
       t.slackOver = slackOver;
       t.avail = avail;
       Timepoint best = null;
       while (t != null) {
           if (best == null) {
               if (t.contact != null)
                   best = t.contact;
           } else {
               if (t.contact == null)
                   t.contact = best;
               else {
                   if (best.overlap - t.contact.overlap <= best.slackUnder - t.contact.slackUnder) {
                       t.contact = best;
                   } else {
                       best = t;
                   }
               }
           }
           t = t.previous;
       }
       if(ov > 0)
           return Integer.MAX_VALUE;

       return ect;
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
           t.cons = hcons;

           if(ov > 0 && ov < (hcons - hreq) * l)
           {
               l = Math.max(1, ov / (hcons-hreq));
               t.InsertAfter(new Timepoint(t.time + l, t.capacity));
           }
           ov += (hreq - hcons) * l;
           if(hcons > C) {
               t.contact = t;
           }


           t.capacity = C - hcons;
           if(t.capacity < C)
               ect = t.next.time;

           t = t.next;
       }
       if(ov > 0)
           return Integer.MAX_VALUE;

       return ect;
   }



   private void DetectePrecedences(int j, int min, int max){
       for(int i = min; i <= max; i++){
           if(tasks[tasks_indices_h[i]].latestCompletionTime() > tasks[tasks_indices_lct[j]].latestCompletionTime() && tasks[tasks_indices_h[i]].inLambda &&
                   tasks[tasks_indices_h[i]].est_to_timepoint.contact != null && tasks[tasks_indices_h[i]].earliestCompletionTime() < tasks[tasks_indices_h[i]].latestCompletionTime() &&
                   tasks[tasks_indices_h[i]].est_to_timepoint.contact.time < tasks[tasks_indices_h[i]].earliestCompletionTime()){
               if (tasks[tasks_indices_h[i]].hasFixedPart() && tasks[tasks_indices_h[i]].latestStartingTime() < tasks[tasks_indices_lct[j]].latestCompletionTime()) {
                   if (tasks[tasks_indices_h[i]].est_to_timepoint.contact.time < tasks[tasks_indices_h[i]].latestStartingTime()) {
                       Timepoint t = tasks[tasks_indices_h[i]].est_to_timepoint.contact;
                       int overlap1 = t.overlap;
                       int slackUnder1 = t.slackUnder;
                       t = tasks[tasks_indices_h[i]].lst_to_timepoint;
                       int overlap2 = t.overlap;
                       int slackOver1 = t.slackOver;
                       t = tasks[tasks_indices_lct[j]].lct_to_timepoint;
                       int slackUnder2 = t.slackUnder;
                       int slackOver2 = t.slackOver;
                       if(overlap2 - overlap1 > slackUnder2 - slackUnder1 + slackOver2 - slackOver1) {
                           Prec[tasks_indices_h[i]] = j;
                           tasks[tasks_indices_h[i]].inLambda = false;
                           MaxOver[tasks_indices_h[i]] = overlap2 - overlap1 - (slackUnder2 - slackUnder1);
                       }
                   }
               } else {
                   if(tasks[tasks_indices_h[i]].earliestCompletionTime() < tasks[tasks_indices_lct[j]].latestCompletionTime()){
                       Timepoint t = tasks[tasks_indices_h[i]].est_to_timepoint.contact;
                       int overlap1 = t.overlap;
                       int slackUnder1 = t.slackUnder;
                       t = tasks[tasks_indices_h[i]].ect_to_timepoint;
                       int overlap2 = t.overlap;
                       int slackOver1 = t.slackOver;
                       t = tasks[tasks_indices_lct[j]].lct_to_timepoint;
                       int slackUnder2 = t.slackUnder;
                       int slackOver2 = t.slackOver;
                       int overlap3 = t.overlap;
                       if(overlap2 - overlap1 > slackUnder2 - slackUnder1 + slackOver2 - slackOver1){
                           Prec[tasks_indices_h[i]] = j;
                           tasks[tasks_indices_h[i]].inLambda = false;
                           t = tasks[tasks_indices_h[i]].est_to_timepoint;
                           int avail1 = t.avail;
                           t = tasks[tasks_indices_lct[j]].lct_to_timepoint;
                           int avail2 = t.avail;
                           if(avail2 - avail1 < tasks[tasks_indices_h[i]].energy()) {
                               MaxOver[tasks_indices_h[i]] = overlap3 - overlap1 - (slackUnder2 - slackUnder1);
                           }else {
                               MaxOver[tasks_indices_h[i]] = overlap2 - overlap1 - (slackUnder2 - slackUnder1);
                           }
                       }
                   }else{
                       Timepoint t = tasks[tasks_indices_h[i]].est_to_timepoint.contact;
                       int overlap1 = t.overlap;
                       int slackUnder1 = t.slackUnder;
                       t = tasks[tasks_indices_lct[j]].lct_to_timepoint;
                       int slackUnder2 = t.slackUnder;
                       int overlap2 = t.overlap;
                       if(overlap2 - overlap1 > slackUnder2 - slackUnder1 ){
                           Prec[tasks_indices_h[i]] = j;
                           tasks[tasks_indices_h[i]].inLambda = false;
                           MaxOver[tasks_indices_h[i]] = overlap2 - overlap1 - (slackUnder2 - slackUnder1);
                       }
                   }
               }
           }
       }
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
           else if(k < n && (l==n || tasks[tasks_indices_lct[k]].latestCompletionTime() <= tasks[tasks_indices_lst[l]].latestStartingTime()))
           {
               if(tasks[tasks_indices_lct[k]].latestCompletionTime() > t.time)
               {
                   t.InsertAfter(new Timepoint(tasks[tasks_indices_lct[k]].latestCompletionTime(), C));
                   t = t.next;
               }
               tasks[tasks_indices_lct[k]].lct_to_timepoint = t;
               k++;
           }
           else
           {
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
}

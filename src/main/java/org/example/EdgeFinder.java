package org.example;

import java.util.Arrays;
import java.util.Comparator;
public class EdgeFinder {
	Integer C;
	Task[] tasks;
	Profile tl;
	Timepoint tinit;
	Integer[] Prec;
	Integer[] tasks_indices_lct;
	Integer[] tasks_indices_h_est;
	Integer[] tasks_indices_est;
	Integer[] tasks_indices_ect;
	Integer makespan;
	private int[] estPrime;

	public EdgeFinder(Task[] tasks, int C)
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
		tasks_indices_h_est = sortWithJavaLibrary(tasks, new Task.ComparatorByHeight_ReverseEst(tasks)); //Increasing Height, Decreasing EST

		makespan = Integer.MAX_VALUE;

		InitializeTimeLine();
	}

	/* ------------ Filtering algorihtms --------------*/

	/*
	 * Just as presented in the paper (Generalizing the Edge-Finder Rule for the Cumulative Constraint), the algorithm proceeds in two phases : the Detection phase and the Adjustment phase
	 */
	public int[] Filter()
	{
		int[] result = new int[tasks.length];
		if(EdgeFinder_Detection()) {
			result = EdgeFinder_Pruning();
			return result;
		}else {
			return null;
		}
	}

	/*
	 * Implementation of the Algorihtm 2 : OverloadCheck
	 * as presented in Generalizing the Edge-Finder Rule for the Cumulative Constraint.
	 * Contrary to the presented algorithm, here we iterate over the LCuts in non-increasing order of lct.
	 * We do so to conserve the same idea behind the Edge-Finder algorithm. Nevertheless, the resulting algorithm is the same.
	 * Also, since we are iterating over all LCuts, we process the Tasks in batch, meaning that at each iteration, instead of addind one Task at a time to Theta, all Tasks in the current LCut are added
	 * to Theta.
	 */
	public boolean OverloadCheck()
	{
		InitializeIncrements(tasks_indices_lct.length - 1);
		if(ScheduleTasks(tasks[tasks_indices_lct[tasks_indices_lct.length - 1]].latestCompletionTime()) > tasks[tasks_indices_lct[tasks_indices_lct.length - 1]].latestCompletionTime())
			return false;

		for(int i=tasks_indices_lct.length - 1; i>0; i--)
		{
			while(i > 0 && tasks[tasks_indices_lct[i]].latestCompletionTime() == tasks[tasks_indices_lct[i-1]].latestCompletionTime())
			{
				i--;
			}

			if(i == 0)
				return true;

			InitializeIncrements(i-1);
			if(ScheduleTasks(tasks[tasks_indices_lct[i-1]].latestCompletionTime()) > tasks[tasks_indices_lct[i-1]].latestCompletionTime())
				return false;
		}

		return true;
	}




	public boolean EdgeFinder_Detection()
	{
		InitializeIncrements(tasks_indices_lct.length - 1);
		makespan = ScheduleTasks(tasks[tasks_indices_lct[tasks_indices_lct.length - 1]].latestCompletionTime());
		//System.out.println(makespan);
		if(makespan > tasks[tasks_indices_lct[tasks_indices_lct.length - 1]].latestCompletionTime())
			return false;
		for(int i=tasks_indices_lct.length - 1; i>0; i--)
		{
			while(i > 0 && tasks[tasks_indices_lct[i]].latestCompletionTime() == tasks[tasks_indices_lct[i-1]].latestCompletionTime())
			{
				tasks[tasks_indices_lct[i]].inLambda = true;
				i--;
			}
			tasks[tasks_indices_lct[i]].inLambda = true;

			if(i == 0)
				return true;

			InitializeIncrements(i-1);
			if(ScheduleTasks(tasks[tasks_indices_lct[i-1]].latestCompletionTime()) > tasks[tasks_indices_lct[i-1]].latestCompletionTime())
				return false;

			int max, min;
			min = 0;
			max = min + 1;
			while(max <= tasks_indices_h_est.length)
			{
				if(max == tasks_indices_h_est.length || tasks[tasks_indices_h_est[max]].height() > tasks[tasks_indices_h_est[min]].height())
				{
					DetectPrecedences(tasks[tasks_indices_h_est[min]].height(), i-1, tasks_indices_lct[i-1], min, max-1);
					min = max;
				}
				max ++;
			}
		}
		return true;
	}




	/*
	 * Implementation of the Algorihtm 5 : Adjustment
	 * as presented in Generalizing the Edge-Finder Rule for the Cumulative Constraint.
	 */
	private int[] EdgeFinder_Pruning()
	{
		for (int i = 0; i < tasks.length; i++)
			estPrime[i] = tasks[i].earliestStartingTime();
		for(int i=0; i<tasks.length; i++)
		{
			if(Prec[i] != -1)
			{
				InitializeIncrements(Prec[i]);
				int  maxOv = ComputeMinimumOverflow(C-tasks[i].height(), tasks[tasks_indices_lct[Prec[i]]].latestCompletionTime());
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






	/* ------------ Initialization Functions --------------*/

	/*
	 * Profile Initialization, as described in chapter 5 of Generalizing the Edge-Finder Rule for the Cumulative Constraint.
	 */
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
	private void PrintTimepoint() {
		// TODO Auto-generated method stub
		//String info = " ";
		String espace = " ";
		Timepoint t = tl.first;
		while(t != null)
		{
			String info =  "Timepoint: (t = " + t.time + ", capacity= " + t.capacity + ") ";
			espace += info;
			t = t.next;
		}
		System.out.println(espace);
	}
	/*
	 * Pre-processing in linear time, in order to compute the h_req and h_max values in constant time for every Timepoint during the execution of ScheduleTasks.
	 * This function is the implementation of lines 2-6 of Algorithm 1 : ScheduleTasks
	 * as presented in Generalizing the Edge-Finder Rule for the Cumulative Constraint.
	 */
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



	/* ------------ Functions used in the Detection phase --------------*/

	/*
	 * Implementation of the Algorihtm 1 : ScheduleTasks
	 * as presented in Generalizing the Edge-Finder Rule for the Cumulative Constraint.
	 * Lines 26-34 are omitted since a stripped down version of ScheduleTasks is instead use the in Adjustment algrorithm.
	 * See ComputeMinimumOverflow function.
	 */
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

	/*
	 * Implementation of the Algorihtm 4 : DetectPrecedences
	 * as presented in Generalizing the Edge-Finder Rule for the Cumulative Constraint.
	 */
	private void DetectPrecedences(int h, int index_lct, int index_task, int min, int max)
	{
		int j = min;
		int ov = 0;
		int e = 0;
		int minest = tasks[tasks_indices_h_est[max]].earliestStartingTime();
		Timepoint t = tasks[index_task].lct_to_timepoint.previous;
		while(t != null && t.time >= minest)
		{
			int l = t.next.time - t.time;
			int hmax = t.hMaxTotal + h;
			int c= Math.min(hmax - (C - t.capacity), t.capacity);

			e += l * Math.min(c, h) + Math.max(0, Math.min(ov, (h-c)*l ));
			ov = Math.max(0, ov + l*(c-h));

			while(j <= max && tasks[tasks_indices_h_est[j]].earliestStartingTime() >= t.time)
			{
				if(tasks[tasks_indices_h_est[j]].earliestStartingTime() == t.time && tasks[tasks_indices_h_est[j]].inLambda)
				{
					int eAfter = h * Math.max(0, tasks[tasks_indices_h_est[j]].earliestCompletionTime() - tasks[index_task].latestCompletionTime());
					int over = tasks[tasks_indices_h_est[j]].energy() - eAfter - e;
					if(over > 0 && tasks[tasks_indices_h_est[j]].earliestCompletionTime() < tasks[tasks_indices_h_est[j]].latestCompletionTime()) //Detection test
					{
						Prec[tasks_indices_h_est[j]] = index_lct;
						tasks[tasks_indices_h_est[j]].inLambda = false;
					}
				}
				j++;
			}
			t = t.previous;
		}
	}

	/* ------------ Functions used in the Adjustment phase  --------------*/

	/*
	 * Stripped down version of ScheduleTasks where the unecessary instructions, such as the insertion of Timepoints on the Profile, are omitted.
	 */
	private int ComputeMinimumOverflow(int c, int lct)
	{
		int ov, h, hmax;
		ov = h = hmax = 0;

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

			int f = cmax - h;

			if(ov < f*l)
			{
				ov = 0;
			}
			else
			{
				ov = Math.max(0, ov + (Math.max(0, h-cmax) - Math.max(0,  f)) * l);
			}
			tinit = tinit.next;
		}
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
		return maxOv;
	}

	/*
	 * Implementation of the Algorihtm 6 : ComputeBound
	 * as presented in Generalizing the Edge-Finder Rule for the Cumulative Constraint.
	 */
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


	/* ------------ Utility Functions --------------*/
	private static Integer[] sortWithJavaLibrary(Task[] tasks, Comparator<Integer> comparator) {

		int n = tasks.length;
		Integer[] tasks_indices = new Integer[n];
		for (int q = 0; q < n; q++) {
			tasks_indices[q] = q; //new Integer(q);
		}
		Arrays.sort(tasks_indices, comparator);
		return tasks_indices;
	}

	private String PrintTasksDomain()
	{
		int diff = -1 * tl.first.time;
		String text ="C = " + C + "\n Tasks = [";
		for(int i=0; i<tasks.length; i++)
		{
			text += "\n [" + i + "] : {est = " + (tasks[i].earliestStartingTime() + diff) +
					" , lct = " + (tasks[i].latestCompletionTime() + diff) +
					" , p = " + tasks[i].processingTime() +
					" , h = " + tasks[i].height() +
					"} , ";
		}
		text += "] \n";
		return text;
	}

	private String PrintPrecedences()
	{
		String text = "";

		text += "Precedences = [";
		for(int i=0; i<Prec.length; i++)
		{
			text += Prec[i] == -1 ? "-1 , " : tasks_indices_lct[Prec[i]] + " , ";
		}
		text += "] \n";

		return text;
	}
}

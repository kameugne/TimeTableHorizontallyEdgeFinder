package org.example;

public class Profile {
	public Timepoint first;
	
	public Profile()
	{
		first = null;
	}
	
	public void Add(Timepoint tp)
	{
		if(first == null)
		{
			first = tp;
		}
		else
		{
			tp.next = first;
			first.previous = tp;
			first= tp;
		}
	}
	
	public void Print(int C)
	{
		Timepoint t = first;
		String out = "";
		while(t.next != null)
		{
			out += "{" + t.time + "," + t.capacity + "} --> ";
			t = t.next;
		}
		System.out.println(out);
	}
	
	public void PrintIncrements(int C)
	{
		Timepoint t = first;
		String out = "";
		int diff = -1 * this.first.time;
		while(t.next != null)
		{
			out += "{t = " + (t.time + diff) + ", h = " + t.increment + ", hmax = " + t.incrementMax + "} --> ";
			t = t.next;
		}
		System.out.println(out);
	}
}

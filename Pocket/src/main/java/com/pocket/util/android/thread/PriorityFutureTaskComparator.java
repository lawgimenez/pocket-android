package com.pocket.util.android.thread;

import java.util.Comparator;

public class PriorityFutureTaskComparator implements Comparator<Runnable>{
	
	@Override
	public int compare(final Runnable a, final Runnable b) {
		PriorityFutureTask task1 = ((PriorityFutureTask) a);
		PriorityFutureTask task2 = ((PriorityFutureTask) b);
		
		long priorityComparison = task2.getPriority() - task1.getPriority();
		
		if(priorityComparison == 0){ // Same priority
			// Order it based on FIFO
			return Long.signum(task1.getAddedOrder() - task2.getAddedOrder());
			
		} else {
			// Order it based on Priority
			return Long.signum(priorityComparison);
		}
	}
}


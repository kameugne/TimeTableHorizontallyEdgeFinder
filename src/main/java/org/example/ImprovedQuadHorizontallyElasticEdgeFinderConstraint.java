package org.example;

import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

public class ImprovedQuadHorizontallyElasticEdgeFinderConstraint extends AdjustmentsPropagator {

    private IntVar[] startingTimes;
    private IntVar[] endingTimes;
    private IntVar makespan;

    private Integer[] processingTimes;
    private Integer[] heights;
    private int capacity;

    private int nbTasks;
    private int adjustments;

    public ImprovedQuadHorizontallyElasticEdgeFinderConstraint(IntVar[] startingTimes_makespan, IntVar[] endingTimes, Integer[] heights, Integer[] processingTimes, int capacity) {
        super(startingTimes_makespan);

        this.nbTasks = startingTimes_makespan.length - 1;
        this.processingTimes = processingTimes;
        this.heights = heights;
        this.capacity = capacity;

        this.startingTimes = new IntVar[nbTasks];
        for(int i=0; i<nbTasks; i++) {
            startingTimes[i] = vars[i];
        }
        this.makespan = vars[nbTasks];
        this.endingTimes = endingTimes;
        this.adjustments = 0;

    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        int[] adjustments = new int[nbTasks];
        for (int i = 0; i < adjustments.length; i++) {
            adjustments[i] = 0;
        }

        boolean fixpoint;
        do
        {
            fixpoint = true;

            Task[] tasks = new Task[nbTasks];
            Task[] negativeTasks = new Task[nbTasks];

            int maxLct = Integer.MIN_VALUE;
            for (int i = 0; i < nbTasks; i++)
            {
                final int est = startingTimes[i].getLB();
                final int lct = startingTimes[i].getUB() + processingTimes[i];
                tasks[i] = new Task(i, est, lct, processingTimes[i], heights[i]);
                maxLct = Math.max(maxLct, lct);
            }
            for (int i = 0; i < nbTasks; i++)
            {
                final int est = startingTimes[i].getLB();
                final int lct = startingTimes[i].getUB() + processingTimes[i];
                negativeTasks[i] = new Task(i, -lct + maxLct, -est + maxLct, processingTimes[i], heights[i]);
            }

            ImprovedQuadHorizontallyElasticEdgeFinder ef = new ImprovedQuadHorizontallyElasticEdgeFinder(tasks, capacity);
            int[] estprime = ef.Filter();
            ImprovedQuadHorizontallyElasticEdgeFinder ef_negative = new ImprovedQuadHorizontallyElasticEdgeFinder(negativeTasks, capacity);
            int[] estprime_negative = ef_negative.Filter();
            if (estprime == null || estprime_negative == null)
                throw new ContradictionException();


            //The makespan is filtered
            if(ef.makespan > makespan.getLB())
                makespan.updateLowerBound(ef.makespan, this);
            for (int i = 0; i < nbTasks; i++)
            {
                if(estprime[i] > startingTimes[i].getLB())
                {
                    //We update both set of tasks so that we don't have to reinitialize them.
                    startingTimes[i].updateLowerBound(estprime[i], this);
                    negativeTasks[i].setLatestCompletionTime(-startingTimes[i].getLB() + maxLct);
                    fixpoint = false;
                    adjustments[i]++;
                }
                if(maxLct - estprime_negative[i] - processingTimes[i] < startingTimes[i].getUB())
                {
                    //We update both set of tasks so that we don't have to reinitialize them.
                    startingTimes[i].updateUpperBound(maxLct - estprime_negative[i] - processingTimes[i], this);
                    tasks[i].setLatestCompletionTime(startingTimes[i].getUB() + processingTimes[i]);
                    fixpoint = false;
                    adjustments[i]++;
                }
            }
        }
        while(!fixpoint);
        for (int i = 0; i < nbTasks; i++) {
            this.adjustments += COUNT_ADJUSTMENTS_AFTER_FIXPOINT ? Math.min(1, adjustments[i]) : adjustments[i];
        }
    }

    @Override
    public ESat isEntailed() {
        int min = startingTimes[0].getUB();
        int max = endingTimes[0].getLB();
        // check start + duration = end
        for (int i = 0; i < nbTasks; i++) {
            min = Math.min(min, startingTimes[i].getUB());
            max = Math.max(max, endingTimes[i].getLB());
            if (startingTimes[i].getLB() + processingTimes[i] > endingTimes[i].getUB()
                    || startingTimes[i].getUB() + processingTimes[i] < endingTimes[i].getLB()) {
                return ESat.FALSE;
            }
        }
        // check capacity
        int maxLoad = 0;
        if (min <= max) {
            int capamax = capacity;
            int[] consoMin = new int[max - min];
            for (int i = 0; i < nbTasks; i++) {
                for (int t = startingTimes[i].getUB(); t < endingTimes[i].getLB(); t++) {
                    consoMin[t - min] += heights[i];
                    if (consoMin[t - min] > capamax) {
                        return ESat.FALSE;
                    }
                    maxLoad = Math.max(maxLoad, consoMin[t - min]);
                }
            }
        }
        // check variables are instantiated
        for (int i = 0; i < vars.length - 1; i++) {
            if (!vars[i].isInstantiated()) {
                return ESat.UNDEFINED;
            }
        }
        assert min <= max;
        // capacity check entailed
        if (maxLoad <= capacity) {
            return ESat.TRUE;
        }
        // capacity not instantiated
        return ESat.UNDEFINED;
    }
    @Override
    public int getNbAdjustments() {
        return this.adjustments;
    }
}
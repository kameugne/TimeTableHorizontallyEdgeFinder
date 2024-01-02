package org.example;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.nary.cumulative.Cumulative;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;

import java.util.ArrayList;
import java.util.Arrays;

public class RunRCPSP {
    // propagator used
    private static final int GQHEEF = 0;
    private static final int FTHEEF = 1;
    private static final int TTEF = 2;
    private static final int SDHEEF = 3;
    private static final int QHEEF = 4;
    private static final int IQHEEF = 5;
    private static final int IPQHEEF = 6;
    // search strategy used
    private static final int staticSearch = 0;
    private static final int dynamicSearchCOSDOWS = 1;
    private static final int dynamicSearchCOSSmallest = 2;
    private static final int dynamicSearchCOSFF = 3;
    // parameters of the problem
    private final int m_numberOfTasks;
    private final int m_numberOfResources;
    // attributes of solutions provided
    private final int m_solution[];
    private final float m_elapsedTime;
    private final long m_backtracksNum;
    private final long m_visitedNodes;
    private final int m_makespan;
    private int m_adjustements;
    // parameters input for the cofiguration of solver
    private final int propagator;
    private final int search;
    private final int timeLimit;
    private final String fileName;

    public RunRCPSP(String fileName, int propagator, int search, int timeLimit) throws Exception {
        this.fileName = fileName;
        this.propagator = propagator;
        this.search = search;
        this.timeLimit = timeLimit;
        // new model creation
        Model model = new Model("RCPSP Solver");
        // read data from a file
        RCPSPInstance data = new RCPSPInstance(fileName);

        this.m_numberOfTasks = data.numberOfTasks;
        this.m_numberOfResources = data.numberOfResources;
        // variables of the problem
        IntVar[] startingTimes = new IntVar[m_numberOfTasks];
        IntVar[] processingTimes = new IntVar[m_numberOfTasks];
        IntVar[] endingTimes = new IntVar[m_numberOfTasks];

        for (int i = 0; i < m_numberOfTasks; i++) {
            startingTimes[i] = model.intVar("s[" + i + "]", 0, data.horizon(), true);
            endingTimes[i] = model.intVar("e[" + i + "]", data.processingTimes[i], data.horizon(), true);
            processingTimes[i] = model.intVar("p[" + i + "]", data.processingTimes[i]);
        }
        // the dummy task 0 starts at time 0
        model.arithm(startingTimes[0], "=", 0).post();
        // Makespan
        IntVar makespan = model.intVar("makespan", 0, data.horizon(), true);
        for (int i = 0; i < m_numberOfTasks; i++) {
            model.arithm(endingTimes[i], "<=", makespan).post();
        }
        model.arithm(makespan, "=", endingTimes[m_numberOfTasks-1]).post();
        // propagation of precedence constraints
        for(int i = 0; i< m_numberOfTasks; i++)
        {
            for(int j = i+1; j< m_numberOfTasks; j++)
            {
                if(data.precedences[i][j] == 1)
                {
                    model.arithm(startingTimes[i], "+", processingTimes[i], "<=", startingTimes[j]).post();
                }
                else if(data.precedences[i][j] == 0)
                {
                    model.arithm(startingTimes[j], "+", processingTimes[j], "<=", startingTimes[i]).post();
                }
            }
        }
        // new auxillary variable extended startingTime variable with makespan
        IntVar[] startingTimes_and_makespan = new IntVar[m_numberOfTasks+1];
        System.arraycopy(startingTimes, 0, startingTimes_and_makespan, 0, m_numberOfTasks);
        startingTimes_and_makespan[m_numberOfTasks] = makespan;

        // new class to count the number of propagations of each propagator
        AdjustmentsPropagator[] propagators = new AdjustmentsPropagator[m_numberOfResources];
        // propagate resource constraint
        for(int i = 0; i< m_numberOfResources; i++) {
            IntVar[] heights = new IntVar[m_numberOfTasks];
            for (int j = 0; j < m_numberOfTasks; j++) {
                heights[j] = model.intVar("h[" + i + "][" + j + "]", data.heights[i][j]);
            }
            // only consider tasks with positive heigth
            ArrayList<Integer> indices = new ArrayList<>();
            for (int j = 0; j < data.heights[i].length; j++) {
                if (data.heights[i][j] > 0) {
                    indices.add(j);
                }
            }
            // filtering variable by considering only those with positive heigth
            if (indices.size() != 0) {
                IntVar[] filtered_startingTimes_makespan = new IntVar[indices.size() + 1];
                IntVar[] filtered_endingTimes = new IntVar[indices.size()];
                Integer[] filtered_heights = new Integer[indices.size()];
                Integer[] filtered_processingTimes = new Integer[indices.size()];
                Task[] filtered_tasks = new Task[indices.size()];
                IntVar[] filtered_heights_var = new IntVar[indices.size()];

                for (int j = 0; j < indices.size(); j++) {
                    int index = indices.get(j);
                    // auxillary variable extraction
                    filtered_startingTimes_makespan[j] = startingTimes[index];
                    filtered_endingTimes[j] = endingTimes[index];
                    filtered_heights[j] = data.heights[i][index];
                    filtered_processingTimes[j] = data.processingTimes[index];
                    // convert variable to format requiere by choco
                    filtered_tasks[j] = new Task(startingTimes[index], processingTimes[index], endingTimes[index]);
                    filtered_heights_var[j] = heights[index];
                }
                // add makespan to the current startTime variable
                filtered_startingTimes_makespan[indices.size()] = makespan;

                // switch to differents propagators
                switch(propagator){
                    case GQHEEF:
                        Constraint gingrasQuimperEF = new Constraint("Gingras and Quimper Horizontally elastic Edge Finder",
                                propagators[i] = new EdgeFinderConstraint(
                                        filtered_startingTimes_makespan,
                                        filtered_endingTimes,
                                        filtered_heights,
                                        filtered_processingTimes,
                                        data.capacities[i]));
                        model.post(gingrasQuimperEF);
                        break;
                    case TTEF:
                        Constraint timetableEdgefinding = new Constraint("timetable edge finding algorithm",
                                propagators[i] = new TimeTableEdgeFinderConstraint(
                                        filtered_startingTimes_makespan,
                                        filtered_endingTimes,
                                        filtered_heights,
                                        filtered_processingTimes,
                                        data.capacities[i]));
                        model.post(timetableEdgefinding);
                        break;
                    case FTHEEF:
                        Constraint horizontallyEdgefindingRevisited = new Constraint("Fetgo and Tayou horizontally elastic edge finding revisted",
                                propagators[i] = new HorizontallyEdgeFinderRevisitedConstraint(
                                        filtered_startingTimes_makespan,
                                        filtered_endingTimes,
                                        filtered_heights,
                                        filtered_processingTimes,
                                        data.capacities[i]));
                        model.post(horizontallyEdgefindingRevisited);
                        break;
                    case SDHEEF:
                        Constraint horizontallyEdgefinding = new Constraint("Slack density horizontally elastic edge finding algorithm",
                                propagators[i] = new SlackDensityHorizontallyElasticEdgeFinderConstraint(
                                        filtered_startingTimes_makespan,
                                        filtered_endingTimes,
                                        filtered_heights,
                                        filtered_processingTimes,
                                        data.capacities[i]));
                        model.post(horizontallyEdgefinding);
                        break;
                    case QHEEF:
                        Constraint qHorizontallyEdgefinding = new Constraint("improvement Slack density horizontally elastic edge finding algorithm",
                                propagators[i] = new HorizontallyElasticEdgeFinderConstraint(
                                        filtered_startingTimes_makespan,
                                        filtered_endingTimes,
                                        filtered_heights,
                                        filtered_processingTimes,
                                        data.capacities[i]));
                        model.post(qHorizontallyEdgefinding);
                        break;
                    case IQHEEF:
                        Constraint improvQhorizontallyEdgefinding = new Constraint("improvement Slack density horizontally elastic edge finding algorithm",
                                propagators[i] = new ImprovedHorizontallyElasticEdgeFinderConstraint(
                                        filtered_startingTimes_makespan,
                                        filtered_endingTimes,
                                        filtered_heights,
                                        filtered_processingTimes,
                                        data.capacities[i]));
                        model.post(improvQhorizontallyEdgefinding);
                        break;
                    case IPQHEEF:
                        Constraint improvPrecompQhorizontallyEdgefinding = new Constraint("improvement Slack density horizontally elastic edge finding algorithm",
                                propagators[i] = new ImprovedPrecomputationHorizontallyElasticEdgeFinderConstraint(
                                        filtered_startingTimes_makespan,
                                        filtered_endingTimes,
                                        filtered_heights,
                                        filtered_processingTimes,
                                        data.capacities[i]));
                        model.post(improvPrecompQhorizontallyEdgefinding);
                        break;
                    default:
                        model.cumulative(filtered_tasks, filtered_heights_var, model.intVar("capacity", data.capacities[i]), false, Cumulative.Filter.TIME).post();
                }
                model.cumulative(filtered_tasks, filtered_heights_var, model.intVar("capacity", data.capacities[i]), false, Cumulative.Filter.TIME).post();
            }
        }
        model.setObjective(false, makespan);
        Solver solver = model.getSolver();

        // switch to different search
        switch(search) {
            case staticSearch:
                solver.setSearch(Search.intVarSearch(new StaticVarOrder(model), new IntDomainMin(), startingTimes_and_makespan));
                break;
            case dynamicSearchCOSDOWS:
                solver.setSearch(Search.conflictOrderingSearch(Search.domOverWDegSearch(startingTimes_and_makespan)));
                break;
            case dynamicSearchCOSSmallest:
                solver.setSearch(Search.conflictOrderingSearch(Search.intVarSearch( new SmallestVarOrder(model), new IntDomainMin(), startingTimes_and_makespan)));
                break;
            case dynamicSearchCOSFF:
                solver.setSearch(Search.conflictOrderingSearch(Search.minDomLBSearch(startingTimes_and_makespan)));
                break;
            default:
                solver.setSearch(Search.conflictOrderingSearch(Search.intVarSearch(new StaticVarOrder(model), new IntDomainMin(), startingTimes_and_makespan)));
        }
        solver.setRestartOnSolutions();
        solver.limitTime(timeLimit * 1000);
        // solution of the problem
        Solution best = solver.findOptimalSolution(makespan, false);
        m_solution = new int[m_numberOfTasks];
        if (solver.isObjectiveOptimal()) {
            m_makespan = best.getIntVal(makespan);
            for (int i = 0; i < m_numberOfTasks; i++){
                m_solution[i] = best.getIntVal(startingTimes[i]);
            }
            m_elapsedTime =  solver.getTimeCount();
            m_backtracksNum = solver.getBackTrackCount();
            m_visitedNodes = solver.getNodeCount(); //Retourne le nombre de noeuds visit�s dans l'arbre.
        } else {
            m_makespan = -1;
            m_elapsedTime =  timeLimit;
            m_backtracksNum = 1000000000;
            m_visitedNodes = 1000000000;
        }
        m_adjustements = 0;
        for (int i = 0; i < m_numberOfResources; i++) {
            //On filtre les variables qui on un height null
            ArrayList<Integer> indices = new ArrayList<>();
            for(int j=0; j<data.heights[i].length; j++) {
                if(data.heights[i][j] > 0) {
                    indices.add(j);
                }
            }
            if(indices.size() != 0)
                m_adjustements += propagators[i].getNbAdjustments();
        }
    }
    public float howMuchTime() {
        return m_elapsedTime;
    }
    public long howManyBacktracks() {
        return m_backtracksNum;
    }
    public long howManyVisitedNodes() {
        return m_visitedNodes;
    }
    public int howManyAdjustments() {
        return m_adjustements;
    }
    public int makeSpanSolution() {
        return m_makespan;
    }
    public void printResults() {
        System.out.print(m_makespan + " | " + m_elapsedTime + " | " + m_backtracksNum + " | " + m_adjustements + "\t \t");
    }
    public String getResults() {
        return m_makespan + " | " + m_elapsedTime + " | " + m_backtracksNum + " | " + m_adjustements + "\t \t";
    }
    public void printSolution() {
        for (int i = 0; i < m_numberOfTasks; i++){
            System.out.print("s["+ i + "] = "+ m_solution[i] + " , ");
        }
        System.out.println();
    }
    public String parameter() {
        return propagator +  " | " + search +  " | " + timeLimit ;
    }
    public void printAllResults() {
        System.out.println(fileName +  " | " +  m_makespan + " | " + m_elapsedTime + " | " + m_backtracksNum + " | " + m_adjustements + " | " + Arrays.toString(m_solution) + " | " +  parameter() + "\t \t");
    }
    public String AllResults() {
        return fileName +  " | " +  m_makespan + " | " + m_elapsedTime + " | " + m_backtracksNum + " | " + m_visitedNodes + " | " + m_adjustements + " | " + Arrays.toString(m_solution) + " | " +  parameter() + " | ";
    }
}

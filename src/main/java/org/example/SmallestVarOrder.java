package org.example;

import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableEvaluator;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelector;
import org.chocosolver.solver.variables.IntVar;

public class SmallestVarOrder implements VariableSelector<IntVar>, VariableEvaluator<IntVar> {
    private final IStateInt lastIdx; // index of the last non-instantiated variable

    /**
     * <b>Synamic Variable Order</b> variable selector.
     * @param model reference to the model (does not define the variable scope)
     */
    public SmallestVarOrder(Model model){
        lastIdx = model.getEnvironment().makeInt(0);
    }


    @Override
    public IntVar getVariable(IntVar[] variables) {
        IntVar uninstantiatedVar  = null;
        IntVar smallestVariable = null;
        int smallValue = Integer.MAX_VALUE;
        // get and update the index of the first uninstantiated variable with minimum lower bound value
        int idx = lastIdx.get();
        while(idx < variables.length && variables[idx].isInstantiated()){
            idx++;
        }
        lastIdx.set(idx);
        while(idx < variables.length) {
            if (!variables[idx].isInstantiated() && variables[idx].getLB() < smallValue){
                smallValue = variables[idx].getLB();
                smallestVariable = variables[idx];
            }
            idx++;
        }
        return smallestVariable;
    }

    @Override
    public double evaluate(IntVar variable) {
        return variable.getDomainSize();
    }
}

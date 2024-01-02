package org.example;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.variables.IntVar;

public abstract class AdjustmentsPropagator extends Propagator<IntVar> {
    public static final boolean COUNT_ADJUSTMENTS_AFTER_FIXPOINT = true;

    public AdjustmentsPropagator(IntVar[] vars) {
        super(vars);
    }
    public abstract int getNbAdjustments();

}

/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Gabor Bergmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.incquery.runtime.rete.construction.psystem;

import java.util.Set;

import org.eclipse.incquery.runtime.rete.construction.QueryPlannerException;
import org.eclipse.incquery.runtime.rete.construction.SubPlan;

/**
 * Any constraint that can only be checked on certain stubs (e.g. those stubs that already contain some variables).
 * 
 * @author Gabor Bergmann
 * 
 */
public abstract class DeferredPConstraint extends BasePConstraint {

    public DeferredPConstraint(PSystem pSystem, Set<PVariable> affectedVariables) {
        super(pSystem, affectedVariables);
    }

    /**
     * Decides whether it is possible to evaluate the PConstraint at a selected {@link SubPlan}. No cost model is included
     * when calculating the readiness of a constraint.
     * 
     * @param subPlan
     * @return
     */
    public abstract boolean isReadyAt(SubPlan subPlan);

    /**
     * @pre this.isReadyAt(stub);
     */
    public SubPlan checkOn(SubPlan subPlan) throws QueryPlannerException {
        SubPlan newStub = doCheckOn(subPlan);
        newStub.addConstraint(this);
        return newStub;
    }

    protected abstract SubPlan doCheckOn(SubPlan subPlan) throws QueryPlannerException;

    /**
     * Called when the constraint is not ready, but cannot be deferred further.
     * 
     * @param subPlan
     * @throws QueryPlannerException
     *             to indicate the error in detail. PRE: !isReady(stub)
     */
    public abstract void raiseForeverDeferredError(SubPlan subPlan) throws QueryPlannerException;
}

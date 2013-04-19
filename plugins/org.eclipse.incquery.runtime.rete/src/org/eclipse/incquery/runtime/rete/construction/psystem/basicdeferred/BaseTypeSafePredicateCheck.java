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

package org.eclipse.incquery.runtime.rete.construction.psystem.basicdeferred;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.incquery.runtime.rete.collections.CollectionsFactory;
import org.eclipse.incquery.runtime.rete.construction.QueryPlannerException;
import org.eclipse.incquery.runtime.rete.construction.SubPlan;
import org.eclipse.incquery.runtime.rete.construction.helpers.TypeHelper;
import org.eclipse.incquery.runtime.rete.construction.psystem.PSystem;
import org.eclipse.incquery.runtime.rete.construction.psystem.PVariable;
import org.eclipse.incquery.runtime.rete.construction.psystem.VariableDeferredPConstraint;

/**
 * @author Gabor Bergmann
 * 
 */
public abstract class BaseTypeSafePredicateCheck extends VariableDeferredPConstraint {
    private Map<PVariable, Set<Object>> allTypeRestrictions;

    /**
     * @param buildable
     * @param affectedVariables
     */
    public BaseTypeSafePredicateCheck(PSystem pSystem, Set<PVariable> affectedVariables) {
        super(pSystem, affectedVariables);
    }

    @Override
    public Set<PVariable> getDeducedVariables() {
        return Collections.emptySet();
    }

    @Override
    protected Set<PVariable> getDeferringVariables() {
        return getAffectedVariables();
    }

    @Override
    public boolean isReadyAt(SubPlan subPlan) {
        // if (super.isReadyAt(stub)) {
        // return checkTypeSafety(stub) == null;
        // }
        return false;
    }

    /**
     * Checks whether all type restrictions are already enforced on affected variables.
     * 
     * @param subPlan
     * @return a variable whose type safety is not enforced yet, or null if the stub is typesafe
     */
    protected PVariable checkTypeSafety(SubPlan subPlan) {
        for (PVariable pVariable : getAffectedVariables()) {
            Set<Object> allTypeRestrictionsForVariable = getAllTypeRestrictions().get(pVariable);
            Set<Object> checkedTypeRestrictions = TypeHelper.inferTypes(pVariable, subPlan.getAllEnforcedConstraints());
            Set<Object> uncheckedTypeRestrictions = TypeHelper.subsumeTypes(allTypeRestrictionsForVariable,
                    checkedTypeRestrictions, this.pSystem.getContext());
            if (!uncheckedTypeRestrictions.isEmpty())
                return pVariable;
        }
        return null;
    }

    /**
     * @return the allTypeRestrictions
     */
    public Map<PVariable, Set<Object>> getAllTypeRestrictions() {
        if (allTypeRestrictions == null) {
            allTypeRestrictions = CollectionsFactory.getMap();//new HashMap<PVariable, Set<Object>>();
            for (PVariable pVariable : getAffectedVariables()) {
                allTypeRestrictions.put(pVariable,
                        TypeHelper.inferTypes(pVariable, pVariable.getReferringConstraints()));
            }
        }
        return allTypeRestrictions;
    }

    @Override
    public void raiseForeverDeferredError(SubPlan subPlan) throws QueryPlannerException {
        if (!super.isReadyAt(subPlan)) {
            super.raiseForeverDeferredError(subPlan);
        } else {
            String[] args = { toString(), checkTypeSafety(subPlan).toString() };
            String msg = "The checking of pattern constraint {1} cannot be deferred further, but variable {2} is still not type safe. "
                    + "HINT: the incremental matcher is not an equation solver, please make sure that all variable values are deducible.";
            String shortMsg = "Could not check all constraints due to undeducible type restrictions";
            throw new QueryPlannerException(msg, args, shortMsg, null);
        }

    }
}

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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.incquery.runtime.rete.collections.CollectionsFactory;
import org.eclipse.incquery.runtime.rete.construction.QueryPlannerException;
import org.eclipse.incquery.runtime.rete.construction.SubPlan;
import org.eclipse.incquery.runtime.rete.construction.psystem.DeferredPConstraint;
import org.eclipse.incquery.runtime.rete.construction.psystem.PSystem;
import org.eclipse.incquery.runtime.rete.construction.psystem.PVariable;

/**
 * @author Gabor Bergmann
 * 
 */
public class Equality extends DeferredPConstraint {

    private PVariable who;
    private PVariable withWhom;

    /**
     * @param buildable
     * @param affectedVariables
     */
    public Equality(PSystem pSystem, PVariable who, PVariable withWhom) {
        super(pSystem, buildSet(who, withWhom));
        this.who = who;
        this.withWhom = withWhom;
    }

    private static Set<PVariable> buildSet(PVariable who, PVariable withWhom) {
        Set<PVariable> set = CollectionsFactory.getSet();//new HashSet<PVariable>();
        set.add(who);
        set.add(withWhom);
        return set;
    }

    public boolean isMoot() {
        return who.equals(withWhom);
    }

    @Override
    public void doReplaceVariable(PVariable obsolete, PVariable replacement) {
        if (obsolete.equals(who))
            who = replacement;
        if (obsolete.equals(withWhom))
            withWhom = replacement;
    }

    @Override
    protected String toStringRest() {
        return who.getName() + "=" + withWhom.getName();
    }

    /**
     * @return the who
     */
    public PVariable getWho() {
        return who;
    }

    /**
     * @return the withWhom
     */
    public PVariable getWithWhom() {
        return withWhom;
    }

    @Override
    public Set<PVariable> getDeducedVariables() {
        return Collections.emptySet();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.incquery.runtime.rete.construction.psystem.BasePConstraint#getFunctionalKeys()
     */
    @Override
    public Map<Set<PVariable>, Set<PVariable>> getFunctionalDependencies() {
    	final HashMap<Set<PVariable>, Set<PVariable>> result = new HashMap<Set<PVariable>, Set<PVariable>>();
    	result.put(Collections.singleton(who), Collections.singleton(withWhom));
    	result.put(Collections.singleton(withWhom), Collections.singleton(who));
		return result;
    }

    @Override
    public boolean isReadyAt(SubPlan subPlan) {
        return subPlan.getVariablesIndex().containsKey(who) && subPlan.getVariablesIndex().containsKey(withWhom);
        // will be replaced by || if copierNode is available;
        // until then, LayoutHelper.unifyVariablesAlongEqualities(PSystem<PatternDescription, StubHandle, Collector>) is
        // recommended.
    }

    @Override
    protected SubPlan doCheckOn(SubPlan subPlan) throws QueryPlannerException {
        if (isMoot())
            return subPlan;

        Integer index1 = subPlan.getVariablesIndex().get(who);
        Integer index2 = subPlan.getVariablesIndex().get(withWhom);
        if (index1 != null && index2 != null) {
            if (index1.equals(index2))
                return subPlan;
            else {
                // return buildable.buildEqualityChecker(stub, new int[] { index1, index2 });
            }
        } else if (index1 == null) {
            // TODO build copierNode here
        }
        return null;
    }

    @Override
    public void raiseForeverDeferredError(SubPlan subPlan) throws QueryPlannerException {
        String[] args = { who.toString(), withWhom.toString() };
        String msg = "Cannot express equality of variables {1} and {2} if neither of them is deducable.";
        String shortMsg = "Equality between undeducible variables.";
        throw new QueryPlannerException(msg, args, shortMsg, null);
    }
}

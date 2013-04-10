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
import java.util.Set;

import org.eclipse.incquery.runtime.rete.construction.RetePatternBuildException;
import org.eclipse.incquery.runtime.rete.construction.Stub;
import org.eclipse.incquery.runtime.rete.construction.helpers.BuildHelper;
import org.eclipse.incquery.runtime.rete.construction.psystem.PSystem;
import org.eclipse.incquery.runtime.rete.construction.psystem.PVariable;
import org.eclipse.incquery.runtime.rete.tuple.Tuple;

/**
 * @author Gabor Bergmann
 * 
 */
public class NegativePatternCall<PatternDescription> extends PatternCallBasedDeferred<PatternDescription> {

    /**
     * @param buildable
     * @param affectedVariables
     */
    public NegativePatternCall(PSystem<PatternDescription> pSystem, Tuple actualParametersTuple,
            PatternDescription pattern) {
        super(pSystem, actualParametersTuple, pattern);
    }

    @Override
    public Set<PVariable> getDeducedVariables() {
        return Collections.emptySet();
    }

    /**
     * @return all variables that may potentially be quantified they are not used anywhere else
     */
    @Override
    protected Set<PVariable> getCandidateQuantifiedVariables() {
        return getAffectedVariables();
    }

    @Override
    protected Stub doCheckOn(Stub stub) throws RetePatternBuildException {
        Stub sideStub = getSideStub();
        BuildHelper.JoinHelper joinHelper = getJoinHelper(stub, sideStub);
        // return buildable.buildBetaNode(stub, sideStub, joinHelper.getPrimaryMask(), joinHelper.getSecondaryMask(),
        // joinHelper.getComplementerMask(), true);
        return null;
    }

    @Override
    protected void doDoReplaceVariables(PVariable obsolete, PVariable replacement) {
    }

    @Override
    protected String toStringRest() {
        return "!" + pSystem.getContext().printPattern(pattern) + "@" + actualParametersTuple.toString();
    }

}

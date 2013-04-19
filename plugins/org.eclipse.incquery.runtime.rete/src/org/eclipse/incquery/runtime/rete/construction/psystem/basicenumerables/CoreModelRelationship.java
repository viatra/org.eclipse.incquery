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

package org.eclipse.incquery.runtime.rete.construction.psystem.basicenumerables;

import org.eclipse.incquery.runtime.rete.construction.QueryPlannerException;
import org.eclipse.incquery.runtime.rete.construction.SubPlan;
import org.eclipse.incquery.runtime.rete.construction.psystem.EnumerablePConstraint;
import org.eclipse.incquery.runtime.rete.construction.psystem.PSystem;
import org.eclipse.incquery.runtime.rete.construction.psystem.PVariable;
import org.eclipse.incquery.runtime.rete.tuple.FlatTuple;

/**
 * @author Gabor Bergmann
 * 
 * @param <PatternDescription>
 * @param <StubHandle>
 */
public abstract class CoreModelRelationship extends EnumerablePConstraint {

    protected boolean transitive;

    protected abstract SubPlan doCreateTransitiveStub();

    protected abstract SubPlan doCreateDirectStub();

    /**
     * @param buildable
     * @param variablesTuple
     */
    public CoreModelRelationship(PSystem pSystem, PVariable parent, PVariable child,
            boolean transitive) {
        super(pSystem, new FlatTuple(parent, child));
        this.transitive = transitive;
    }

    @Override
    public SubPlan doCreateStub() throws QueryPlannerException {
        return isTransitive() ? doCreateTransitiveStub() : doCreateDirectStub();
    }

    @Override
    protected String toStringRestRest() {
        return transitive ? "transitive" : "direct";
    }

    /**
     * @return the transitive
     */
    public boolean isTransitive() {
        return transitive;
    }

}
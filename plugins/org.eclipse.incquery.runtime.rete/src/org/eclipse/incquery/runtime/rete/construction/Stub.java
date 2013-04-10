/*******************************************************************************
 * Copyright (c) 2004-2008 Gabor Bergmann and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Gabor Bergmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.incquery.runtime.rete.construction;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.incquery.runtime.rete.collections.CollectionsFactory;
import org.eclipse.incquery.runtime.rete.construction.psystem.PConstraint;
import org.eclipse.incquery.runtime.rete.construction.psystem.PVariable;
import org.eclipse.incquery.runtime.rete.tuple.Tuple;

/**
 * 
 * @author Gabor Bergmann
 * 
 */
public class Stub {
    private Tuple variablesTuple;
    private Map<Object, Integer> variablesIndex;
    private Set<PConstraint> constraints;
    private Stub primaryParentStub;
    private Stub secondaryParentStub;
	private final Set<PVariable> variablesSet;

    private Stub(Map<Object, Integer> variablesIndex, Tuple variablesTuple) {
        super();
        this.variablesIndex = variablesIndex;
        this.variablesTuple = variablesTuple;
        this.constraints = CollectionsFactory.getSet();//new HashSet<PConstraint>();
		variablesSet = new HashSet<PVariable>();
		for (Object pVar : variablesIndex.keySet()) {
			variablesSet.add((PVariable) pVar);
		}
    }

    public Stub(Tuple variablesTuple, Object handle) {
        this(variablesTuple.invertIndex(), variablesTuple);
    }

    // public Stub(Stub<HandleType> template) {
    // this(template.variablesIndex, template.variablesTuple, template.getHandle());
    // }
    public Stub(Stub primaryParent, Object handle) {
        this(primaryParent.variablesIndex, primaryParent.variablesTuple);
        this.primaryParentStub = primaryParent;
        constraints.addAll(primaryParent.getAllEnforcedConstraints());
    }

    public Stub(Stub primaryParent, Tuple variablesTuple, Object handle) {
        this(variablesTuple.invertIndex(), variablesTuple);
        this.primaryParentStub = primaryParent;
        constraints.addAll(primaryParent.getAllEnforcedConstraints());
    }

    public Stub(Stub primaryParent, Stub secondaryParent, Tuple variablesTuple, Object handle) {
        this(variablesTuple.invertIndex(), variablesTuple);
        this.primaryParentStub = primaryParent;
        this.secondaryParentStub = secondaryParent;
        constraints.addAll(primaryParent.getAllEnforcedConstraints());
        constraints.addAll(secondaryParent.getAllEnforcedConstraints());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Stub(" + getVariablesTuple() + "@" + "|");
        for (PConstraint constraint : constraints)
            sb.append(constraint.toString() + "&");
        sb.append(")");
        return sb.toString();
    }

    /**
     * @return the tuple of variables that define the scheme emanating from the handle
     */
    public Tuple getVariablesTuple() {
        return variablesTuple;
    }

    /**
     * @return the index of the variable within variablesTuple
     */
    public Map<Object, Integer> getVariablesIndex() {
        return variablesIndex;
    }

    /**
     * @return the set of variables involved
     */
    public Set<PVariable> getVariablesSet() {
        return variablesSet;
    }

    /**
     * @return all constraints already enforced at this handle
     */
    public Set<PConstraint> getAllEnforcedConstraints() {
        return constraints;
    }

    /**
     * @return the new constraints enforced at this handle, that aren't yet enforced at parents
     */
    public Set<PConstraint> getDeltaEnforcedConstraints() {
        Set<PConstraint> result = CollectionsFactory.getSet(constraints);//new HashSet<PConstraint>(constraints);
        if (primaryParentStub != null)
            result.removeAll(primaryParentStub.getAllEnforcedConstraints());
        if (secondaryParentStub != null)
            result.removeAll(secondaryParentStub.getAllEnforcedConstraints());
        return result;
    }

    /**
     * @return the constraints
     */
    public void addConstraint(PConstraint constraint) {
        constraints.add(constraint);
    }

    /**
     * @return the primaryParentStub
     */
    public Stub getPrimaryParentStub() {
        return primaryParentStub;
    }

    /**
     * @return the secondaryParentStub
     */
    public Stub getSecondaryParentStub() {
        return secondaryParentStub;
    }

}

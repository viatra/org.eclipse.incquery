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

import org.eclipse.incquery.runtime.rete.boundary.AbstractEvaluator;
import org.eclipse.incquery.runtime.rete.tuple.Tuple;
import org.eclipse.incquery.runtime.rete.tuple.TupleMask;

/**
 * 
 * An implicit common parameter is the "effort" PatternDescription. This indicates that the build request is part of an
 * effort to build the matcher of the given pattern; it it important to record this during code generation so that the
 * generated code can be separated according to patterns.
 * 
 * @param <PatternDescription>
 *            the description of a pattern
 * @param <StubHandle>
 *            the handle of a continuable supplier-like RETE ending with associated semantics
 * @param <Collector>
 *            the handle of a receiver-like RETE ending to which stubs can be connected
 * @author Gabor Bergmann
 */
public interface POperationCompiler<Collector> {

    public Collector patternCollector(Object pattern) throws QueryPlannerException;

    public SubPlan patternCallStub(Tuple nodes, Object supplierKey)
            throws QueryPlannerException;

    public SubPlan instantiationTransitiveStub(Tuple nodes);

    public SubPlan instantiationDirectStub(Tuple nodes);

    public SubPlan generalizationTransitiveStub(Tuple nodes);

    public SubPlan generalizationDirectStub(Tuple nodes);

    public SubPlan containmentTransitiveStub(Tuple nodes);

    public SubPlan containmentDirectStub(Tuple nodes);

    public SubPlan binaryEdgeTypeStub(Tuple nodes, Object supplierKey);

    public SubPlan ternaryEdgeTypeStub(Tuple nodes, Object supplierKey);

    public SubPlan unaryTypeStub(Tuple nodes, Object supplierKey);

    public void buildConnection(SubPlan subPlan, Collector collector);

    public SubPlan buildStartStub(Object[] constantValues, Object[] constantNames);

    public SubPlan buildEqualityChecker(SubPlan subPlan, int[] indices);

    public SubPlan buildInjectivityChecker(SubPlan subPlan, int subject, int[] inequalIndices);

    public SubPlan buildTransitiveClosure(SubPlan subPlan);

    public SubPlan buildTrimmer(SubPlan subPlan, TupleMask trimMask);

    public SubPlan buildBetaNode(SubPlan primaryStub, SubPlan sideStub,
            TupleMask primaryMask, TupleMask sideMask, TupleMask complementer, boolean negative);

    public SubPlan buildCounterBetaNode(SubPlan primaryStub, SubPlan sideStub,
            TupleMask primaryMask, TupleMask originalSideMask, TupleMask complementer,
            Object aggregateResultCalibrationElement);

    public SubPlan buildCountCheckBetaNode(SubPlan primaryStub, SubPlan sideStub,
            TupleMask primaryMask, TupleMask originalSideMask, int resultPositionInSignature);

    public SubPlan buildScopeConstrainer(SubPlan subPlan, boolean transitive, Object unwrappedContainer,
            int constrainedIndex);

    public SubPlan buildPredicateChecker(AbstractEvaluator evaluator, Integer rhsIndex, int[] affectedIndices, SubPlan subPlan);

    /**
     * @return a buildable that potentially acts on a separate container
     */
    public POperationCompiler<Collector> getNextContainer();

    /**
     * @return a buildable that puts build actions on the tab of the given pattern
     */
    public POperationCompiler<Collector> putOnTab(Object effort);

    public void reinitialize();
}
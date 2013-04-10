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
public interface Buildable<PatternDescription, Collector> {

    public Collector patternCollector(PatternDescription pattern) throws RetePatternBuildException;

    public Stub patternCallStub(Tuple nodes, PatternDescription supplierKey)
            throws RetePatternBuildException;

    public Stub instantiationTransitiveStub(Tuple nodes);

    public Stub instantiationDirectStub(Tuple nodes);

    public Stub generalizationTransitiveStub(Tuple nodes);

    public Stub generalizationDirectStub(Tuple nodes);

    public Stub containmentTransitiveStub(Tuple nodes);

    public Stub containmentDirectStub(Tuple nodes);

    public Stub binaryEdgeTypeStub(Tuple nodes, Object supplierKey);

    public Stub ternaryEdgeTypeStub(Tuple nodes, Object supplierKey);

    public Stub unaryTypeStub(Tuple nodes, Object supplierKey);

    public void buildConnection(Stub stub, Collector collector);

    public Stub buildStartStub(Object[] constantValues, Object[] constantNames);

    public Stub buildEqualityChecker(Stub stub, int[] indices);

    public Stub buildInjectivityChecker(Stub stub, int subject, int[] inequalIndices);

    public Stub buildTransitiveClosure(Stub stub);

    public Stub buildTrimmer(Stub stub, TupleMask trimMask);

    public Stub buildBetaNode(Stub primaryStub, Stub sideStub,
            TupleMask primaryMask, TupleMask sideMask, TupleMask complementer, boolean negative);

    public Stub buildCounterBetaNode(Stub primaryStub, Stub sideStub,
            TupleMask primaryMask, TupleMask originalSideMask, TupleMask complementer,
            Object aggregateResultCalibrationElement);

    public Stub buildCountCheckBetaNode(Stub primaryStub, Stub sideStub,
            TupleMask primaryMask, TupleMask originalSideMask, int resultPositionInSignature);

    public Stub buildScopeConstrainer(Stub stub, boolean transitive, Object unwrappedContainer,
            int constrainedIndex);

    public Stub buildPredicateChecker(AbstractEvaluator evaluator, Integer rhsIndex, int[] affectedIndices, Stub stub);

    /**
     * @return a buildable that potentially acts on a separate container
     */
    public Buildable<PatternDescription, Collector> getNextContainer();

    /**
     * @return a buildable that puts build actions on the tab of the given pattern
     */
    public Buildable<PatternDescription, Collector> putOnTab(PatternDescription effort);

    public void reinitialize();
}
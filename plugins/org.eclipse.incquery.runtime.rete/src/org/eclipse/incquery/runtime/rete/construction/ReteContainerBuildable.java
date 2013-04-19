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
import org.eclipse.incquery.runtime.rete.boundary.PredicateEvaluatorNode;
import org.eclipse.incquery.runtime.rete.boundary.ReteBoundary;
import org.eclipse.incquery.runtime.rete.index.DualInputNode;
import org.eclipse.incquery.runtime.rete.index.Indexer;
import org.eclipse.incquery.runtime.rete.index.IterableIndexer;
import org.eclipse.incquery.runtime.rete.matcher.ReteEngine;
import org.eclipse.incquery.runtime.rete.network.Library;
import org.eclipse.incquery.runtime.rete.network.Network;
import org.eclipse.incquery.runtime.rete.network.Receiver;
import org.eclipse.incquery.runtime.rete.network.ReteContainer;
import org.eclipse.incquery.runtime.rete.network.Supplier;
import org.eclipse.incquery.runtime.rete.remote.Address;
import org.eclipse.incquery.runtime.rete.single.EqualityFilterNode;
import org.eclipse.incquery.runtime.rete.single.InequalityFilterNode;
import org.eclipse.incquery.runtime.rete.single.TransitiveClosureNode;
import org.eclipse.incquery.runtime.rete.single.TrimmerNode;
import org.eclipse.incquery.runtime.rete.tuple.FlatTuple;
import org.eclipse.incquery.runtime.rete.tuple.LeftInheritanceTuple;
import org.eclipse.incquery.runtime.rete.tuple.Tuple;
import org.eclipse.incquery.runtime.rete.tuple.TupleMask;
import org.eclipse.incquery.runtime.rete.util.Options;

/**
 * The buildable interface of a rete container.
 * 
 * @author Gabor Bergmann
 * 
 */
public class ReteContainerBuildable implements POperationCompiler<Address<? extends Receiver>> {

    protected Library library;
    protected ReteContainer targetContainer;
    protected Network reteNet;
    protected ReteBoundary boundary;
    protected ReteEngine engine;
    protected boolean headAttached = false;

    /**
     * Constructs the builder attached to a specified container. Prerequisite: engine has its network and boundary
     * fields initialized.
     * 
     * @param targetContainer
     */
    public ReteContainerBuildable(ReteEngine engine, ReteContainer targetContainer) {
        super();
        this.engine = engine;
        this.reteNet = engine.getReteNet();
        this.boundary = engine.getBoundary();
        this.targetContainer = targetContainer;
        this.library = targetContainer.getLibrary();
        this.headAttached = false;
    }

    /**
     * Constructs the builder attached to the head container. Prerequisite: engine has its network and boundary fields
     * initialized
     */
    public ReteContainerBuildable(ReteEngine engine) {
        super();
        this.engine = engine;
        this.reteNet = engine.getReteNet();
        this.boundary = engine.getBoundary();
        this.targetContainer = reteNet.getHeadContainer();
        this.library = targetContainer.getLibrary();
        this.headAttached = true;
    }

    public void reinitialize() {
        this.reteNet = engine.getReteNet();
        this.boundary = engine.getBoundary();
        this.targetContainer = headAttached ? reteNet.getHeadContainer() : reteNet.getNextContainer();
        this.library = targetContainer.getLibrary();
    }

    public SubPlan buildTrimmer(SubPlan subPlan, TupleMask trimMask) {
        // Address<TrimmerNode> bodyTerminator = library.accessTrimmerNode(stub.getHandle(), trimMask);
        Address<TrimmerNode> bodyTerminator = library.accessTrimmerNode(null, trimMask);
        return new SubPlan(subPlan, trimMask.transform(subPlan.getVariablesTuple()), bodyTerminator);
    }

    public void buildConnection(SubPlan subPlan, Address<? extends Receiver> collector) {
        // reteNet.connectRemoteNodes(stub.getHandle(), collector, true);
        reteNet.connectRemoteNodes(null, collector, true);
        boundary.registerParentStubForReceiver(collector, subPlan);
    }

    public SubPlan buildStartStub(Object[] constantValues, Object[] constantNames) {
        return new SubPlan(new FlatTuple(constantNames), library.accessConstantNode(boundary
                .wrapTuple(new FlatTuple(constantValues))));
    }

    public SubPlan buildEqualityChecker(SubPlan subPlan, int[] indices) {
        // Address<EqualityFilterNode> checker = library.accessEqualityFilterNode(stub.getHandle(), indices);
        Address<EqualityFilterNode> checker = library.accessEqualityFilterNode(null, indices);
        return new SubPlan(subPlan, checker);
    }

    public SubPlan buildInjectivityChecker(SubPlan subPlan,
            int subject, int[] inequalIndices) {
        // Address<InequalityFilterNode> checker = library.accessInequalityFilterNode(stub.getHandle(), subject,
        Address<InequalityFilterNode> checker = library.accessInequalityFilterNode(null, subject,
                new TupleMask(inequalIndices, subPlan.getVariablesTuple().getSize()));
        return new SubPlan(subPlan, checker);
    }

    @Override
    public SubPlan buildTransitiveClosure(SubPlan subPlan) {
        // Address<TransitiveClosureNode> checker = library.accessTransitiveClosureNode(stub.getHandle());
        Address<TransitiveClosureNode> checker = library.accessTransitiveClosureNode(null);
        return new SubPlan(subPlan, checker);
    }

    public SubPlan patternCallStub(Tuple nodes, Object supplierKey)
            throws QueryPlannerException {
        return new SubPlan(nodes, boundary.accessProduction(supplierKey));
    }

    public SubPlan instantiationTransitiveStub(Tuple nodes) {
        return new SubPlan(nodes, boundary.accessInstantiationTransitiveRoot());
    }

    public SubPlan instantiationDirectStub(Tuple nodes) {
        return new SubPlan(nodes, boundary.accessInstantiationRoot());
    }

    public SubPlan generalizationTransitiveStub(Tuple nodes) {
        return new SubPlan(nodes, boundary.accessGeneralizationTransitiveRoot());
    }

    public SubPlan generalizationDirectStub(Tuple nodes) {
        return new SubPlan(nodes, boundary.accessGeneralizationRoot());
    }

    public SubPlan containmentTransitiveStub(Tuple nodes) {
        return new SubPlan(nodes, boundary.accessContainmentTransitiveRoot());
    }

    public SubPlan containmentDirectStub(Tuple nodes) {
        return new SubPlan(nodes, boundary.accessContainmentRoot());
    }

    public SubPlan binaryEdgeTypeStub(Tuple nodes, Object supplierKey) {
        return new SubPlan(nodes, boundary.accessBinaryEdgeRoot(supplierKey));
    }

    public SubPlan ternaryEdgeTypeStub(Tuple nodes, Object supplierKey) {
        return new SubPlan(nodes, boundary.accessTernaryEdgeRoot(supplierKey));
    }

    public SubPlan unaryTypeStub(Tuple nodes, Object supplierKey) {
        return new SubPlan(nodes, boundary.accessUnaryRoot(supplierKey));
    }

    public SubPlan buildBetaNode(SubPlan primaryStub, SubPlan sideStub, TupleMask primaryMask, TupleMask sideMask,
            TupleMask complementer, boolean negative) {
        // Address<? extends IterableIndexer> primarySlot = library.accessProjectionIndexer(primaryStub.getHandle(),
        Address<? extends IterableIndexer> primarySlot = library.accessProjectionIndexer(
                (Address<? extends Supplier>) null,
                primaryMask);
        // Address<? extends Indexer> sideSlot = library.accessProjectionIndexer(sideStub.getHandle(), sideMask);
        Address<? extends Indexer> sideSlot = library.accessProjectionIndexer((Address<? extends Supplier>) null,
                sideMask);

        if (negative) {
            Address<? extends DualInputNode> checker = library.accessExistenceNode(primarySlot, sideSlot, true);
            return new SubPlan(primaryStub, checker);
        } else {
            Address<? extends DualInputNode> checker = library.accessJoinNode(primarySlot, sideSlot, complementer);
            Tuple newCalibrationPattern = complementer.combine(primaryStub.getVariablesTuple(),
                    sideStub.getVariablesTuple(), Options.enableInheritance, true);
            return new SubPlan(primaryStub, sideStub, newCalibrationPattern, checker);
        }
    }

    public SubPlan buildCounterBetaNode(SubPlan primaryStub, SubPlan sideStub, TupleMask primaryMask,
            TupleMask originalSideMask,
            TupleMask complementer, Object aggregateResultCalibrationElement) {
        // Address<? extends IterableIndexer> primarySlot = library.accessProjectionIndexer(primaryStub.getHandle(),
        Address<? extends IterableIndexer> primarySlot = library.accessProjectionIndexer(
                (Address<? extends Supplier>) null,
                primaryMask);
        // Address<? extends Indexer> sideSlot = library.accessCountOuterIndexer(sideStub.getHandle(),
        // originalSideMask);
        Address<? extends Indexer> sideSlot = library.accessCountOuterIndexer(null, originalSideMask);

        Address<? extends DualInputNode> checker = library.accessJoinNode(primarySlot, sideSlot,
                TupleMask.selectSingle(originalSideMask.indices.length, originalSideMask.indices.length + 1));

        Object[] newCalibrationElement = { aggregateResultCalibrationElement };
        Tuple newCalibrationPattern = new LeftInheritanceTuple(primaryStub.getVariablesTuple(), newCalibrationElement);

        SubPlan result = new SubPlan(primaryStub,
                newCalibrationPattern, checker);

        return result;
    }

    public SubPlan buildCountCheckBetaNode(SubPlan primaryStub, SubPlan sideStub, TupleMask primaryMask,
            TupleMask originalSideMask,
            int resultPositionInSignature) {
        Address<? extends IterableIndexer> primarySlot = library.accessProjectionIndexer(
                (Address<? extends Supplier>) null,
                primaryMask);
        // Address<? extends Indexer> sideSlot = library.accessCountOuterIdentityIndexer(sideStub.getHandle(),
        Address<? extends Indexer> sideSlot = library.accessCountOuterIdentityIndexer(null,
                originalSideMask, resultPositionInSignature);

        Address<? extends DualInputNode> checker = library.accessJoinNode(primarySlot, sideSlot,
                TupleMask.empty(originalSideMask.indices.length + 1));

        Tuple newCalibrationPattern = primaryStub.getVariablesTuple();

        SubPlan result = new SubPlan(primaryStub,
                newCalibrationPattern, checker);

        return result;
    }

    public SubPlan buildPredicateChecker(AbstractEvaluator evaluator, Integer rhsIndex, int[] affectedIndices, SubPlan subPlan) {
        PredicateEvaluatorNode ten = new PredicateEvaluatorNode(engine, targetContainer, rhsIndex, affectedIndices,
                subPlan.getVariablesTuple().getSize(), evaluator);
        Address<PredicateEvaluatorNode> checker = Address.of(ten);

        // reteNet.connectRemoteNodes(stub.getHandle(), checker, true);
        reteNet.connectRemoteNodes(null, checker, true);

        SubPlan result = new SubPlan(subPlan, checker);

        return result;
    }

    /**
     * @return a buildable that potentially acts on a separate container
     */
    public ReteContainerBuildable getNextContainer() {
        return new ReteContainerBuildable(engine, reteNet.getNextContainer());
    }

    public SubPlan buildScopeConstrainer(SubPlan subPlan,
            boolean transitive, Object unwrappedContainer, int constrainedIndex) {
        Address<? extends Supplier> root = (transitive) ? boundary.accessContainmentTransitiveRoot() : boundary
                .accessContainmentRoot();
        // bind the container element
        Address<? extends Supplier> filteredRoot = targetContainer.getLibrary().accessValueBinderFilterNode(root,
                0/* container */, boundary.wrapElement(unwrappedContainer));
        // build secondary indexer
        int[] secondaryIndices = { 1 /* contained element */};
        Address<? extends Indexer> secondary = targetContainer.getLibrary().accessProjectionIndexer(filteredRoot,
                new TupleMask(secondaryIndices, 2));
        // build primary indexer
        int[] primaryIndices = { constrainedIndex };
        TupleMask primaryMask = new TupleMask(primaryIndices, subPlan.getVariablesTuple().getSize());
        Address<? extends IterableIndexer> primary = targetContainer.getLibrary().accessProjectionIndexer(
                (Address<? extends Supplier>) null, primaryMask);
        // stub.getHandle(), primaryMask);
        // build checker
        subPlan = new SubPlan(subPlan, targetContainer.getLibrary().accessExistenceNode(primary,
                secondary, false));
        return subPlan;
    }

    public Address<? extends Receiver> patternCollector(Object pattern) throws QueryPlannerException {
        return engine.getBoundary().createProductionInternal(pattern);
    }

    /**
     * No need to distinguish
     */
    public ReteContainerBuildable putOnTab(Object effort) {
        return this;
    }

}

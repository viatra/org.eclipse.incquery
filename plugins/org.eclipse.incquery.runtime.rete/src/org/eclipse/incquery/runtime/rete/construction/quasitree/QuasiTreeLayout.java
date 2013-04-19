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

package org.eclipse.incquery.runtime.rete.construction.quasitree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.incquery.runtime.rete.construction.POperationCompiler;
import org.eclipse.incquery.runtime.rete.construction.IQueryPlanner;
import org.eclipse.incquery.runtime.rete.construction.QueryPlannerException;
import org.eclipse.incquery.runtime.rete.construction.SubPlan;
import org.eclipse.incquery.runtime.rete.construction.helpers.BuildHelper;
import org.eclipse.incquery.runtime.rete.construction.helpers.LayoutHelper;
import org.eclipse.incquery.runtime.rete.construction.psystem.DeferredPConstraint;
import org.eclipse.incquery.runtime.rete.construction.psystem.EnumerablePConstraint;
import org.eclipse.incquery.runtime.rete.construction.psystem.PSystem;
import org.eclipse.incquery.runtime.rete.matcher.IPatternMatcherContext;
import org.eclipse.incquery.runtime.rete.util.Options;

/**
 * Layout ideas: see https://bugs.eclipse.org/bugs/show_bug.cgi?id=398763
 * 
 * @author Gabor Bergmann
 * 
 */
public class QuasiTreeLayout<Collector> implements IQueryPlanner {

    @Override
    public SubPlan layout(PSystem pSystem)
            throws QueryPlannerException {
        return new Scaffold(pSystem).run();
    }

    public class Scaffold {
        PSystem pSystem;
        Object pattern;
        IPatternMatcherContext context;
        POperationCompiler<Collector> pOperationCompiler;

        Set<DeferredPConstraint> deferredConstraints = null;
        Set<SubPlan> forefront = new LinkedHashSet<SubPlan>();

        Scaffold(PSystem pSystem) {
            this.pSystem = pSystem;
            pattern = pSystem.getPattern();
            context = pSystem.getContext();
            pOperationCompiler = null;// pSystem.getBuildable();
        }

        /**
         * @return
         */
        public SubPlan run() throws QueryPlannerException {
            try {
                context.logDebug(getClass().getSimpleName() + ": patternbody build started");

                // UNIFICATION AND WEAK INEQUALITY ELMINATION
                LayoutHelper.unifyVariablesAlongEqualities(pSystem);
                LayoutHelper.eliminateWeakInequalities(pSystem);

                // UNARY ELIMINATION WITH TYPE INFERENCE
                if (Options.calcImpliedTypes) {
                    LayoutHelper.eliminateInferrableUnaryTypes(pSystem, context);
                }

                // PREVENTIVE CHECKS
                LayoutHelper.checkSanity(pSystem);

                // PROCESS CONSTRAINTS
                deferredConstraints = pSystem.getConstraintsOfType(DeferredPConstraint.class);
                Set<EnumerablePConstraint> enumerables = pSystem.getConstraintsOfType(EnumerablePConstraint.class);
                for (EnumerablePConstraint enumerable : enumerables) {
                    SubPlan subPlan = enumerable.getStub();
                    admitStub(subPlan);
                }
                if (enumerables.isEmpty()) { // EXTREME CASE
                    SubPlan subPlan = pOperationCompiler.buildStartStub(new Object[] {}, new Object[] {});
                    admitStub(subPlan);
                }

                // JOIN FOREFRONT STUBS WHILE POSSIBLE
                while (forefront.size() > 1) {
                    // TODO QUASI-TREE TRIVIAL JOINS?

                    List<JoinCandidate> candidates = generateJoinCandidates();
                    JoinOrderingHeuristics<Collector> ordering = new JoinOrderingHeuristics<Collector>();
                    JoinCandidate selectedJoin = Collections.min(candidates, ordering);
                    doJoin(selectedJoin.getPrimary(), selectedJoin.getSecondary());
                }

                // FINAL CHECK, whether all exported variables are present
                assert (forefront.size() == 1);
                SubPlan finalStub = forefront.iterator().next();
                LayoutHelper.finalCheck(pSystem, finalStub);

                context.logDebug(getClass().getSimpleName() + ": patternbody build concluded");
                return finalStub;
            } catch (QueryPlannerException ex) {
                ex.setPatternDescription(pattern);
                throw ex;
            }
        }

        public List<JoinCandidate> generateJoinCandidates() {
            List<JoinCandidate> candidates = new ArrayList<JoinCandidate>();
            int bIndex = 0;
            for (SubPlan b : forefront) {
                int aIndex = 0;
                for (SubPlan a : forefront) {
                    if (aIndex++ >= bIndex)
                        break;
                    candidates.add(new JoinCandidate(a, b));
                }
                bIndex++;
            }
            return candidates;
        }

        private void admitStub(SubPlan subPlan) throws QueryPlannerException {
            for (DeferredPConstraint deferred : deferredConstraints) {
                if (!subPlan.getAllEnforcedConstraints().contains(deferred)) {
                    if (deferred.isReadyAt(subPlan)) {
                        admitStub(deferred.checkOn(subPlan));
                        return;
                    }
                }
            }
            forefront.add(subPlan);
        }

        private void doJoin(SubPlan primaryStub, SubPlan secondaryStub)
                throws QueryPlannerException {
            SubPlan joinedStub = BuildHelper.naturalJoin(pOperationCompiler, primaryStub, secondaryStub);
            forefront.remove(primaryStub);
            forefront.remove(secondaryStub);
            admitStub(joinedStub);
        }

    }

}

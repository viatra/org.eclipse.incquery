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

package org.eclipse.incquery.runtime.internal.matcherbuilder;

import org.eclipse.incquery.patternlanguage.patternLanguage.Pattern;
import org.eclipse.incquery.patternlanguage.patternLanguage.PatternBody;
import org.eclipse.incquery.runtime.rete.construction.POperationCompiler;
import org.eclipse.incquery.runtime.rete.construction.QueryPlannerException;
import org.eclipse.incquery.runtime.rete.construction.SubPlan;
import org.eclipse.incquery.runtime.rete.construction.helpers.BuildHelper;
import org.eclipse.incquery.runtime.rete.matcher.IPatternMatcherContext;
import org.eclipse.incquery.runtime.rete.util.Options;
import org.eclipse.incquery.runtime.rete.util.Options.BuilderMethod;

/**
 * @author Bergmann Gabor
 * 
 */
public class EPMBuildScaffold<Collector> {

    protected POperationCompiler<Collector> baseBuildable;
    protected IPatternMatcherContext context;

    /**
     * @param baseBuildable
     * @param context
     */
    public EPMBuildScaffold(POperationCompiler<Collector> baseBuildable, IPatternMatcherContext context) {
        super();
        this.baseBuildable = baseBuildable;
        this.context = context;
    }

    public Collector construct(Pattern pattern) throws QueryPlannerException {
        Collector production = baseBuildable.putOnTab(pattern).patternCollector(pattern);
        // TODO check annotations for reinterpret

        context.logDebug("EPMBuilder starts construction of: " + pattern.getName());
        for (PatternBody body : pattern.getBodies()) {
            POperationCompiler<Collector> currentBuildable = baseBuildable.getNextContainer().putOnTab(
                    pattern);
            if (Options.builderMethod == BuilderMethod.LEGACY) {
                throw new UnsupportedOperationException();
            } else {
                EPMBodyToPSystem<Collector> converter = new EPMBodyToPSystem<Collector>(
                        pattern, body, context, currentBuildable);
                SubPlan bodyFinal = Options.builderMethod.<Collector> layoutStrategy()
                        .layout(converter.toPSystem());
                BuildHelper.projectIntoCollector(currentBuildable, bodyFinal, production,
                        converter.symbolicParameterArray());
            }

        }

        return null;
    }

}

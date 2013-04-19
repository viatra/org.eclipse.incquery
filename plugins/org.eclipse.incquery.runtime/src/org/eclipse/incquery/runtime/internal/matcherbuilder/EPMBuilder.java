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

import java.util.HashMap;

import org.eclipse.emf.common.util.EList;
import org.eclipse.incquery.patternlanguage.patternLanguage.Pattern;
import org.eclipse.incquery.patternlanguage.patternLanguage.Variable;
import org.eclipse.incquery.runtime.rete.construction.POperationCompiler;
import org.eclipse.incquery.runtime.rete.construction.IRetePatternBuilder;
import org.eclipse.incquery.runtime.rete.construction.QueryPlannerException;
import org.eclipse.incquery.runtime.rete.matcher.IPatternMatcherContext;

import com.google.common.base.Preconditions;

/**
 * @author Bergmann Gábor
 * 
 */
public class EPMBuilder<Collector> implements IRetePatternBuilder<Collector> {
    protected POperationCompiler<Collector> baseBuildable;
    protected IPatternMatcherContext context;

    /**
     * @param baseBuildable
     * @param context
     */
    public EPMBuilder(POperationCompiler<Collector> baseBuildable, IPatternMatcherContext context) {
        super();
        this.baseBuildable = baseBuildable;
        this.context = context;
    }

    @Override
    public IPatternMatcherContext getContext() {
        return context;
    }

    @Override
    public void refresh() {
        baseBuildable.reinitialize();
    }

    @Override
    public Collector construct(Object _pattern) throws QueryPlannerException {
        Preconditions.checkArgument(_pattern instanceof Pattern, "Invalid parameter type");
        Pattern pattern = (Pattern) _pattern;
        try {
            EPMBuildScaffold<Collector> epmBuildScaffold = new EPMBuildScaffold<Collector>(
                    baseBuildable, context);
            return epmBuildScaffold.construct(pattern);
        } catch (RuntimeException ex) {
            throw new QueryPlannerException(
                    "Error during constructing Rete pattern matcher; please review Error Log and consult developers",
                    new String[0], "Error during pattern matcher construction", pattern, ex);
        }
    }

    @Override
    public HashMap<Object, Integer> getPosMapping(Object _pattern) {
        Preconditions.checkArgument(_pattern instanceof Pattern, "Invalid parameter type");
        Pattern pattern = (Pattern) _pattern;
        HashMap<Object, Integer> result = new HashMap<Object, Integer>();
        EList<Variable> parameters = pattern.getParameters();
        for (int i = 0; i < parameters.size(); ++i)
            result.put(parameters.get(i), i);
        return result;
    }
}

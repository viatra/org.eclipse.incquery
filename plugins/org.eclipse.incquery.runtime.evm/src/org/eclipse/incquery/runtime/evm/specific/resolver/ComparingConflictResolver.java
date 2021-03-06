/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Abel Hegedus - initial API and implementation
 *******************************************************************************/
package org.eclipse.incquery.runtime.evm.specific.resolver;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.incquery.runtime.evm.api.Activation;
import org.eclipse.incquery.runtime.evm.api.resolver.ChangeableConflictSet;
import org.eclipse.incquery.runtime.evm.api.resolver.ConflictResolver;

import com.google.common.collect.Sets;

/**
 * @author Abel Hegedus
 *
 */
public class ComparingConflictResolver implements ConflictResolver {

    private final Comparator<Activation<?>> comparator;
    
    public ComparingConflictResolver(Comparator<Activation<?>> comparator) {
        this.comparator = comparator;
    }
    
    @Override
    public ComparingConflictSet createConflictSet() {
        return new ComparingConflictSet(comparator);
    }

    /**
     * @return the comparator
     */
    public Comparator<Activation<?>> getComparator() {
        return comparator;
    }
    
    public class ComparingConflictSet implements ChangeableConflictSet {
    
        private SortedSet<Activation<?>> set;
        
        protected ComparingConflictSet(Comparator<Activation<?>> comparator) {
            checkArgument(comparator != null, "Comparator cannot be null!");
            set = Sets.newTreeSet(comparator);
        }
        
        @Override
        public Activation<?> getNextActivation() {
            if(!set.isEmpty()) {
                return set.first();
            }
            return null;
        }
    
        @Override
        public boolean addActivation(Activation<?> activation) {
            checkArgument(activation != null, "Activation cannot be null!");
            return set.add(activation);
        }
    
        @Override
        public boolean removeActivation(Activation<?> activation) {
            checkArgument(activation != null, "Activation cannot be null!");
            return set.remove(activation);
        }

        @Override
        public ComparingConflictResolver getConflictResolver() {
            return ComparingConflictResolver.this;
        }

        @Override
        public Set<Activation<?>> getNextActivations() {
            Set<Activation<?>> hashSet = new HashSet<Activation<?>>();
            hashSet.add(getNextActivation());
            return Collections.unmodifiableSet(hashSet);
        }

        @Override
        public Set<Activation<?>> getConflictingActivations() {
            return Collections.unmodifiableSet(set);
        }
        
    }

}

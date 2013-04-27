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
package org.eclipse.incquery.runtime.evm.api;

import java.util.Set;

/**
 * A conflict set is responsible for managing and ordering the set of enabled activations.
 * 
 * @author Abel Hegedus
 *
 */
public interface ConflictSet {

    /**
     * 
     * @return the resolver corresponding to the conflict set
     */
    ConflictResolver<?> getConflictResolver();
    
    /**
     * 
     * @return the next activation chosen by the resolver
     */
    Activation getNextActivation();
    
    /**
     * 
     * @return the set of activations that are considered as equal by the resolver
     */
    Set<Activation> getNextActivations();
    
    /**
     * 
     * @return the set of all activations that are in conflict (all enabled activations)
     */
    Set<Activation> getConflictingActivations();
    
    /**
     * This method is called by the Agenda when an activation changes state and becomes or is still enabled.
     * 
     * <p/>NOTE: The ConflictSet is responsible for handling that <code>add</code> may be called
     *  multiple times on an Activation already in the conflict set! 
     * 
     * @param activation the activation that should be added to the conflict set
     * @return true, if the conflict set changed
     */
    boolean addActivation(Activation activation);
    
    /**
     * This method is called by the Agenda when an activation changes state and becomes or is still disabled.
     * 
     * <p/>NOTE: The ConflictSet is responsible for handling that <code>remove</code> may be called
     * on Activations that are not in the conflict set! 
     * 
     * @param activation the activation that should be removed from the conflict set
     * @return true, if the conflict set changed
     */
    boolean removeActivation(Activation activation);
    
}

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

package org.eclipse.incquery.runtime.rete.network;

import java.util.Collection;

import org.eclipse.incquery.runtime.matchers.tuple.Tuple;

/**
 * ALL METHODS: FOR INTERNAL USE ONLY; ONLY INVOKE FROM {@link ReteContainer}
 * 
 * @author Gabor Bergmann
 */
public interface Receiver extends Node {

    /**
     * updates the receiver with a newly found or lost partial matching
     */
    public void update(Direction direction, Tuple updateElement);

    /**
     * appends a parent that will continously send insert and revoke updates to this supplier
     */
    void appendParent(Supplier supplier);

    /**
     * removes a parent
     */
    void removeParent(Supplier supplier);

    /**
     * access active parent
     */
    Collection<Supplier> getParents();

}

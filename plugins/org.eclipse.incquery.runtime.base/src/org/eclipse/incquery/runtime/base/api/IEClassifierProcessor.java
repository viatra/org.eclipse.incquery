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
package org.eclipse.incquery.runtime.base.api;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;

/**
 * @author Abel Hegedus
 *
 */
public interface IEClassifierProcessor<ClassType, InstanceType> {
    
    void process(ClassType type, InstanceType instance);
    
    public interface IEClassProcessor extends IEClassifierProcessor<EClass, EObject>{}
    public interface IEDataTypeProcessor extends IEClassifierProcessor<EDataType, Object>{}
}

/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi - initial API and implementation
 *******************************************************************************/
package org.eclipse.incquery.patternlanguage.emf.ui.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.incquery.patternlanguage.emf.ui.EMFPatternLanguageExecutableExtensionFactory;
import org.osgi.framework.Bundle;

/**
 * @author Zoltan Ujhelyi
 *
 */
public class IncQueryExecutableExtensionFactory extends EMFPatternLanguageExecutableExtensionFactory {

    private String contributorName;
    @Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
            throws CoreException {
        super.setInitializationData(config, propertyName, data);
        contributorName = config.getContributor().getName();
    }
    @Override
    protected Bundle getBundle() {
        final Bundle bundle = Platform.getBundle(contributorName);
        if (bundle != null) {
            return bundle;
        }
        return super.getBundle();
    }


}

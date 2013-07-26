/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zoltan Ujhelyi - initial API and implementation
 *******************************************************************************/
package org.eclipse.incquery.viewers.runtime;

import org.eclipse.incquery.viewers.runtime.model.ViewerDataFilter;
import org.eclipse.incquery.viewers.runtime.model.ViewerDataModel;
import org.eclipse.incquery.viewers.runtime.model.ViewerState;
import org.eclipse.incquery.viewers.runtime.model.ViewerState.ViewerStateFeature;
import org.eclipse.incquery.viewers.runtime.sources.ListContentProvider;
import org.eclipse.incquery.viewers.runtime.sources.QueryLabelProvider;
import org.eclipse.incquery.viewers.runtime.sources.TreeContentProvider;
import org.eclipse.jface.viewers.AbstractListViewer;
import org.eclipse.jface.viewers.AbstractTreeViewer;

import com.google.common.collect.ImmutableSet;

/**
 * @author Zoltan Ujhelyi
 * 
 */
public class IncQueryViewerSupport {

	/**
	 * 
	 * @deprecated Use {@link #bind(AbstractListViewer, ViewerStateSet)} where
	 *             {@link ViewerStateSet} consists of the shared data between
	 *             various viewers.
	 */
	public static void bind(AbstractListViewer viewer, ViewerDataModel model) {
		bind(viewer, model, ViewerDataFilter.UNFILTERED);
	}

	/**
	 * 
	 * @deprecated Use {@link #bind(AbstractListViewer, ViewerStateSet)} where
	 *             {@link ViewerStateSet} consists of the shared data between
	 *             various viewers.
	 */
	public static void bind(AbstractListViewer viewer, ViewerDataModel model,
			ViewerDataFilter filter) {
		bind(viewer, ViewerState.newInstance(model, filter, ImmutableSet
						.of(ViewerStateFeature.CONTAINMENT)));
	}

	public static void bind(AbstractListViewer viewer, ViewerState state) {
		if (!(viewer.getContentProvider() instanceof ListContentProvider)) {
			viewer.setContentProvider(new ListContentProvider());
		}
		if (!(viewer.getLabelProvider() instanceof QueryLabelProvider)) {
			viewer.setLabelProvider(new QueryLabelProvider());
		}
		viewer.setInput(state);
		viewer.refresh();
	}

	/**
	 * 
	 * @deprecated Use {@link #bind(AbstractTreeViewer, ViewerStateSet)} where
	 *             {@link ViewerStateSet} consists of the shared data between
	 *             various viewers.
	 */
	public static void bind(AbstractTreeViewer viewer, ViewerDataModel model) {
		bind(viewer, model, ViewerDataFilter.UNFILTERED);
	}

	/**
	 * 
	 * @deprecated Use {@link #bind(AbstractTreeViewer, ViewerStateSet)} where
	 *             {@link ViewerStateSet} consists of the shared data between
	 *             various viewers.
	 */
	public static void bind(AbstractTreeViewer viewer, ViewerDataModel model,
			ViewerDataFilter filter) {
		ViewerState state = ViewerState.newInstance(model, filter, ImmutableSet
				.of(ViewerStateFeature.CONTAINMENT));
		bind(viewer, state);
	}

	public static void bind(AbstractTreeViewer viewer, ViewerState state) {
		if (!(viewer.getContentProvider() instanceof TreeContentProvider)) {
			viewer.setContentProvider(new TreeContentProvider());
		}
		if (!(viewer.getLabelProvider() instanceof QueryLabelProvider)) {
			viewer.setLabelProvider(new QueryLabelProvider());
		}

		viewer.setInput(state);
		viewer.refresh();
	}
}

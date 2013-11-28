/*******************************************************************************
 * Copyright (c) 2010-2012, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Tamas Szabo, Gabor Bergmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.incquery.runtime.base.core;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.NotifyingList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.incquery.runtime.base.api.BaseIndexOptions;
import org.eclipse.incquery.runtime.base.api.DataTypeListener;
import org.eclipse.incquery.runtime.base.api.FeatureListener;
import org.eclipse.incquery.runtime.base.api.IEStructuralFeatureProcessor;
import org.eclipse.incquery.runtime.base.api.IncQueryBaseIndexChangeListener;
import org.eclipse.incquery.runtime.base.api.InstanceListener;
import org.eclipse.incquery.runtime.base.api.LightweightEObjectObserver;
import org.eclipse.incquery.runtime.base.api.NavigationHelper;
import org.eclipse.incquery.runtime.base.comprehension.EMFModelComprehension;
import org.eclipse.incquery.runtime.base.exception.IncQueryBaseException;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

public class NavigationHelperImpl implements NavigationHelper {

    protected boolean inWildcardMode;

    protected Notifier notifier;
    protected Set<Notifier> modelRoots;
    private boolean expansionAllowed;
    // protected NavigationHelperVisitor visitor;
    protected NavigationHelperContentAdapter contentAdapter;

    private final Logger logger;

    // type object or String id
    protected Set<Object> directlyObservedClasses = new HashSet<Object>();
    protected Set<Object> allObservedClasses = null; // including subclasses; if null, must be recomputed
    protected Set<Object> observedDataTypes;
    protected Set<Object> observedFeatures;

    /**
     * Feature registration and model traversal is delayed while true
     */
    protected boolean delayTraversals = false;
    /**
     * Classes (or String ID in dynamic mode) to be registered once the coalescing period is over
     */
   protected Set<Object> delayedClasses;
    /**
     * EStructuralFeatures (or String ID in dynamic mode) to be registered once the coalescing period is over
     */
    protected Set<Object> delayedFeatures;
    /**
     * EDataTypes (or String ID in dynamic mode) to be registered once the coalescing period is over
     */
    protected Set<Object> delayedDataTypes;
    
    /**
     * These global listeners will be called after updates.
     */
    //private final Set<Runnable> afterUpdateCallbacks;
    private final Set<IncQueryBaseIndexChangeListener> baseIndexChangeListeners;
    private final Map<LightweightEObjectObserver, Collection<EObject>> lightweightObservers;

    // These are the user subscriptions to notifications
    private final Map<InstanceListener, Set<EClass>> subscribedInstanceListeners;
    private final Map<FeatureListener, Set<EStructuralFeature>> subscribedFeatureListeners;
    private final Map<DataTypeListener, Set<EDataType>> subscribedDataTypeListeners;

    // these are the internal notification tables
    // (element Type or String id) -> listener -> (subscription types)
    // if null, must be recomputed from subscriptions
    // potentially multiple subscription types for each element type because (a) nsURI collisions, (b) multiple supertypes
    private Table<Object, InstanceListener, Set<EClass>> instanceListeners;
    private Table<Object, FeatureListener, Set<EStructuralFeature>> featureListeners;
    private Table<Object, DataTypeListener, Set<EDataType>> dataTypeListeners;

    private final BaseIndexOptions baseIndexOptions;

    private EMFModelComprehension comprehension;

    <T> Set<T> setMinus(Collection<T> a, Collection<T> b) {
        Set<T> result = new HashSet<T>(a);
        result.removeAll(b);
        return result;
    }

    @SuppressWarnings("unchecked")
    <T extends EObject> Set<T> resolveAllInternal(Set<T> a) {
    	if (a==null) a = Collections.emptySet();
        Set<T> result = new HashSet<T>();
        for (T t : a) {
            if (t.eIsProxy()) {
                result.add((T) EcoreUtil.resolve(t, (ResourceSet) null));
            } else {
                result.add(t);
            }
        }
        return result;
    }
    Set<Object> resolveClassifiersToKey(Set<? extends EClassifier> classes) {
    	Set<? extends EClassifier> resolveds = resolveAllInternal(classes);
        Set<Object> result = new HashSet<Object>();
        for (EClassifier resolved : resolveds) {
            result.add(toKey(resolved));
        }
        return result;
    }
    Set<Object> resolveFeaturesToKey(Set<EStructuralFeature> features) {
    	Set<EStructuralFeature> resolveds = resolveAllInternal(features);
        Set<Object> result = new HashSet<Object>();
        for (EStructuralFeature resolved : resolveds) {
            result.add(toKey(resolved));
        }
        return result;
    }

    @Override
    public boolean isInWildcardMode() {
        return baseIndexOptions.isWildcardMode();
    }
    
    @Override
    public boolean isInDynamicEMFMode() {
    	return baseIndexOptions.isDynamicEMFMode();
    }
    
    /**
     * @return the baseIndexOptions
     */
    public BaseIndexOptions getBaseIndexOptions() {
        return baseIndexOptions.copy();
    }
    
    /**
     * @return the comprehension
     */
    public EMFModelComprehension getComprehension() {
        return comprehension;
    }
    
    public NavigationHelperImpl(Notifier emfRoot, BaseIndexOptions options, Logger logger) throws IncQueryBaseException {
        this.baseIndexOptions = options.copy();
        this.logger = logger;
        assert (logger != null);
        
        this.comprehension = new EMFModelComprehension(baseIndexOptions);
        this.subscribedInstanceListeners = new HashMap<InstanceListener, Set<EClass>>();
        this.subscribedFeatureListeners = new HashMap<FeatureListener, Set<EStructuralFeature>>();
        this.subscribedDataTypeListeners = new HashMap<DataTypeListener, Set<EDataType>>();
        this.lightweightObservers = new HashMap<LightweightEObjectObserver, Collection<EObject>>();
        this.observedFeatures = new HashSet<Object>();
        this.observedDataTypes = new HashSet<Object>();
        this.contentAdapter = new NavigationHelperContentAdapter(this);
        this.baseIndexChangeListeners = new HashSet<IncQueryBaseIndexChangeListener>();

        this.notifier = emfRoot;
        this.modelRoots = new HashSet<Notifier>();
        this.expansionAllowed = false;
        
        if (emfRoot != null) {
            addRootInternal(emfRoot);
        }
    }
    

    public NavigationHelperContentAdapter getContentAdapter() {
        return contentAdapter;
    }

    public Set<Object> getObservedFeaturesInternal() {
        return observedFeatures;
    }

    @Override
    public void dispose() {
    	ensureNoListenersForDispose();
        for (Notifier root : modelRoots) {
            contentAdapter.removeAdapter(root);
        }
    }

    @Override
    public Set<Object> getDataTypeInstances(EDataType type) {
    	Object typeKey = toKey(type);
        Map<Object, Integer> valMap = contentAdapter.getDataTypeMap(typeKey);
        if (valMap != null) {
            return Collections.unmodifiableSet(valMap.keySet());
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public Set<Setting> findByAttributeValue(Object value) {
        Set<Setting> retSet = new HashSet<Setting>();
        Map<Object, Set<EObject>> valMap = contentAdapter.getValueToFeatureToHolderMap().row(value);

        for (Entry<Object, Set<EObject>> entry : valMap.entrySet()) {
            for (EObject holder : entry.getValue()) {
                EStructuralFeature feature = getKnownFeature(entry.getKey());
                retSet.add(new NavigationHelperSetting(feature, holder, value));
            }
        }

        return retSet;
    }

    private EStructuralFeature getKnownFeature(Object featureKey) {
        EStructuralFeature feature;
        if (contentAdapter.isDynamicModel()) {
            feature = contentAdapter.getKnownFeature((String) featureKey);
        } else {
            feature = (EStructuralFeature) featureKey;
        }
        return feature;
    }

    @Override
    public Set<Setting> findByAttributeValue(Object value, Collection<EAttribute> attributes) {
        Set<Setting> retSet = new HashSet<Setting>();
        Map<Object, Set<EObject>> valMap = contentAdapter.getValueToFeatureToHolderMap().row(value);

        for (EAttribute attr : attributes) {
            Object feature = toKey(attr);
            if (valMap.get(feature) != null) {
                for (EObject holder : valMap.get(feature)) {
                    retSet.add(new NavigationHelperSetting(attr, holder, value));
                }
            }
        }

        return retSet;
    }

    @Override
    public Set<EObject> findByAttributeValue(Object value, EAttribute attribute) {
        Map<Object, Set<EObject>> valMap = contentAdapter.getValueToFeatureToHolderMap().row(value);
        Object feature = toKey(attribute);
        if (valMap.get(feature) == null) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(valMap.get(feature));
        }
    }
        
    @Override
    public void processAllFeatureInstances(EStructuralFeature feature, IEStructuralFeatureProcessor processor) {
       final Map<Object, Set<EObject>> instanceMap = contentAdapter.getValueToFeatureToHolderMap().column(toKey(feature));
        for (Entry<Object, Set<EObject>> entry : instanceMap.entrySet()) {
            for (EObject src : entry.getValue()) {
                processor.process(feature, src, entry.getKey());
            }
        }
    }

    @Override
    public Set<Setting> getInverseReferences(EObject target) {
        Set<Setting> retSet = new HashSet<Setting>();
        Map<Object, Set<EObject>> valMap = contentAdapter.getValueToFeatureToHolderMap().row(target);

        for (Entry<Object, Set<EObject>> entry : valMap.entrySet()) {
            for (EObject source : entry.getValue()) {
                EStructuralFeature feature = getKnownFeature(entry.getKey());
                retSet.add(new NavigationHelperSetting(feature, source, target));
            }
        }

        return retSet;
    }

    @Override
    public Set<Setting> getInverseReferences(EObject target, Collection<EReference> references) {
        Set<Setting> retSet = new HashSet<Setting>();
        Map<Object, Set<EObject>> valMap = contentAdapter.getValueToFeatureToHolderMap().row(target);

        for (EReference ref : references) {
            Object feature = toKey(ref);
            if (valMap.get(feature) != null) {
                for (EObject source : valMap.get(feature)) {
                    retSet.add(new NavigationHelperSetting(ref, source, target));
                }
            }
        }

        return retSet;
    }

    @Override
    public Set<EObject> getInverseReferences(EObject target, EReference reference) {
        Object feature = toKey(reference);
        Map<Object, Set<EObject>> valMap = contentAdapter.getValueToFeatureToHolderMap().row(target);
        if (valMap.get(feature) == null) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(valMap.get(feature));
        }
    }

	@Override
	@SuppressWarnings("unchecked")
    public Set<EObject> getReferenceValues(EObject source, EReference reference) {
    	Set<Object> targets = getFeatureTargets(source, reference);
        return (Set<EObject>)(Set<?>) targets; // this is known to be safe, as EReferences can only point to EObjects
    }

    @Override
    public Set<Object> getFeatureTargets(EObject source, EStructuralFeature _feature) {
        Object feature = toKey(_feature);
        final Set<Object> valSet = contentAdapter.getHolderToFeatureToValueMap().get(source, feature);
        if (valSet == null) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(valSet);
        }
    }
    
    @Override
    public Map<EObject, Set<Object>> getFeatureInstances(EStructuralFeature _feature) {
        Object feature = toKey(_feature);
    	final Map<EObject, Set<Object>> valMap = contentAdapter.getHolderToFeatureToValueMap().column(feature);
        if (valMap == null) {
            return Collections.emptyMap();
        } else {
            return Collections.unmodifiableMap(valMap);
        }
    }

    @Override
    public Set<EObject> getDirectInstances(EClass type) {
    	Object typeKey = toKey(type);
        Set<EObject> valSet = contentAdapter.getInstanceSet(typeKey);
        if (valSet == null) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(valSet);
        }
    }

	private Object toKey(EClassifier eClassifier) {
		return contentAdapter.toKey(eClassifier);
	}
	private Object toKey(EStructuralFeature feature) {
		return contentAdapter.toKey(feature);
	}

    @Override
    public Set<EObject> getAllInstances(EClass type) {
    	Object typeKey = toKey(type);
        Set<EObject> retSet = new HashSet<EObject>();

        Set<Object> subTypes = contentAdapter.getSubTypeMap().get(typeKey);
        if (subTypes != null) {
            for (Object subTypeKey : subTypes) {
                final Set<EObject> instances = contentAdapter.getInstanceSet(subTypeKey);
                if (instances != null) {
                    retSet.addAll(instances);
                }
            }
        } 
        final Set<EObject> instances = contentAdapter.getInstanceSet(typeKey);
        if (instances != null) {
            retSet.addAll(instances);
        }

        return retSet;
    }

    @Override
    public Set<EObject> findByFeatureValue(Object value, EStructuralFeature _feature) {
        Object feature = toKey(_feature);
        Set<EObject> retSet = new HashSet<EObject>();
        Map<Object, Set<EObject>> valMap = contentAdapter.getValueToFeatureToHolderMap().row(value);
        if (valMap.get(feature) != null) {
            retSet.addAll(valMap.get(feature));
        }
        return retSet;
    }

    @Override
    public Set<EObject> getHoldersOfFeature(EStructuralFeature _feature) {
        Object feature = toKey(_feature);
        Multiset<EObject> holders = contentAdapter.getFeatureToHolderMap().get(feature);
        if (holders == null) {
           return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(holders.elementSet());
        }
    }

    @Override
    public void addInstanceListener(Collection<EClass> classes, InstanceListener listener) {
        Set<EClass> registered = this.subscribedInstanceListeners.get(listener);
        if (registered == null) {
            registered = new HashSet<EClass>();
            this.subscribedInstanceListeners.put(listener, registered);
        }
        Set<EClass> delta = setMinus(classes, registered);
        if (!delta.isEmpty()) {
        	registered.addAll(delta);
        	if (instanceListeners!= null) { // if already computed
    	        for (EClass subscriptionType : delta) {
    	        	final Object superElementTypeKey = toKey(subscriptionType);
					addInstanceListenerInternal(listener, subscriptionType, superElementTypeKey);
					final Set<Object> subTypeKeys = contentAdapter.getSubTypeMap().get(superElementTypeKey);
					if (subTypeKeys != null) for (Object subTypeKey : subTypeKeys) {
						addInstanceListenerInternal(listener, subscriptionType, subTypeKey);
					}
    	        }
        	}
        }
    }

    @Override
    public void removeInstanceListener(Collection<EClass> classes, InstanceListener listener) {
    	Set<EClass> restriction = this.subscribedInstanceListeners.get(listener);
        if (restriction != null) {
            boolean changed = restriction.removeAll(classes);
            if (restriction.size() == 0) {
                this.subscribedInstanceListeners.remove(listener);
            }
            if (changed) instanceListeners = null; // recompute later on demand
        }
    }

    @Override
    public void addFeatureListener(Collection<EStructuralFeature> features, FeatureListener listener) {
        Set<EStructuralFeature> registered = this.subscribedFeatureListeners.get(listener);
        if (registered == null) {
            registered = new HashSet<EStructuralFeature>();
            this.subscribedFeatureListeners.put(listener, registered);
        }
        Set<EStructuralFeature> delta = setMinus(features, registered);
        if (!delta.isEmpty()) {
        	registered.addAll(delta);
        	if (featureListeners!= null) { // if already computed
    	        for (EStructuralFeature subscriptionType : delta) {
    	        	addFeatureListenerInternal(listener, subscriptionType, toKey(subscriptionType));
    	        }
        	}
        }
    }

    @Override
    public void removeFeatureListener(Collection<EStructuralFeature> features, FeatureListener listener) {
        Collection<EStructuralFeature> restriction = this.subscribedFeatureListeners.get(listener);
        if (restriction != null) {
        	boolean changed = restriction.removeAll(features);
            if (restriction.size() == 0) {
                this.subscribedFeatureListeners.remove(listener);
            }
            if (changed) featureListeners = null; // recompute later on demand
        }
    }

    @Override
    public void addDataTypeListener(Collection<EDataType> types, DataTypeListener listener) {
        Set<EDataType> registered = this.subscribedDataTypeListeners.get(listener);
        if (registered == null) {
            registered = new HashSet<EDataType>();
            this.subscribedDataTypeListeners.put(listener, registered);
        }
        Set<EDataType> delta = setMinus(types, registered);
        if (!delta.isEmpty()) {
        	registered.addAll(delta);
        	if (dataTypeListeners!= null) { // if already computed
    	        for (EDataType subscriptionType : delta) {
    	        	addDatatypeListenerInternal(listener, subscriptionType, toKey(subscriptionType));
    	        }
        	}
        }
    }

    @Override
    public void removeDataTypeListener(Collection<EDataType> types, DataTypeListener listener) {
        Collection<EDataType> restriction = this.subscribedDataTypeListeners.get(listener);
        if (restriction != null) {
        	boolean changed = restriction.removeAll(types);
            if (restriction.size() == 0) {
                this.subscribedDataTypeListeners.remove(listener);
            }
            if (changed) dataTypeListeners = null; // recompute later on demand
        }
    }

    /**
     * @return the observedDataTypes
     */
    public Set<Object> getObservedDataTypesInternal() {
        return observedDataTypes;
    }
    
    @Override
    public void addLightweightEObjectObserver(LightweightEObjectObserver observer, EObject observedObject){
        Collection<EObject> observedObjects = lightweightObservers.get(observer);
        if(observedObjects == null) {
            observedObjects = new HashSet<EObject>();
            lightweightObservers.put(observer, observedObjects);
        }
        observedObjects.add(observedObject);
    }
    
    @Override
    public void removeLightweightEObjectObserver(LightweightEObjectObserver observer, EObject observedObject) {
        Collection<EObject> observedObjects = lightweightObservers.get(observer);
        if(observedObjects != null) {
            observedObjects.remove(observedObject);
            if(observedObjects.isEmpty()) {
                lightweightObservers.remove(observer);
            }
        }
    }
    
    /**
     * @return the lightweightObservers
     */
    public Map<LightweightEObjectObserver, Collection<EObject>> getLightweightObservers() {
        return lightweightObservers;
    }

    /**
     * This will run after updates.
     */
    protected void notifyBaseIndexChangeListeners(boolean baseIndexChanged) {
        if (!baseIndexChangeListeners.isEmpty()) {
            for (IncQueryBaseIndexChangeListener listener : new ArrayList<IncQueryBaseIndexChangeListener>(baseIndexChangeListeners)) {
                try {
                    if(!listener.onlyOnIndexChange() || baseIndexChanged) {
                        listener.notifyChanged(baseIndexChanged);
                    }
                } catch (Exception ex) {
                    logger.fatal("EMF-IncQuery Base encountered an error in delivering notifications about changes. ",
                            ex);
                }
            }
        }
    }

    @Override
    public void addBaseIndexChangeListener(IncQueryBaseIndexChangeListener listener) {
        checkArgument(listener != null, "Cannot add null listener!");
        baseIndexChangeListeners.add(listener);
    }

    @Override
    public void removeBaseIndexChangeListener(IncQueryBaseIndexChangeListener listener) {
        checkArgument(listener != null, "Cannot remove null listener!");
        baseIndexChangeListeners.remove(listener);
    }

    protected void considerForExpansion(EObject obj) {
        if (expansionAllowed) {
            Resource eResource = obj.eResource();
            if (eResource != null && eResource.getResourceSet() == null) {
                expandToAdditionalRoot(eResource);
            }
        }
    }

    protected void expandToAdditionalRoot(Notifier root) {
        if (modelRoots.add(root)) {
            if (root instanceof ResourceSet) {
                expansionAllowed = true;
            }
            contentAdapter.addAdapter(root);
            contentAdapter.notifyBaseIndexChangeListeners();
        }
    }

    /**
     * @return the expansionAllowed
     */
    public boolean isExpansionAllowed() {
        return expansionAllowed;
    }

    /**
     * @return the directlyObservedClasses
     */
    public Set<Object> getDirectlyObservedClassesInternal() {
        return directlyObservedClasses;
    }

    boolean isObservedInternal(Object clazzKey) {      
    	return inWildcardMode || getAllObservedClassesInternal().contains(clazzKey);
    }  
    
    /**
     * not just the directly observed classes, but also their known subtypes
     */
    public Set<Object> getAllObservedClassesInternal() {
        if (allObservedClasses == null) {
            allObservedClasses = new HashSet<Object>();
            for (Object eClassKey : directlyObservedClasses) {
                allObservedClasses.add(eClassKey);
                final Set<Object> subTypes = contentAdapter.getSubTypeMap().get(eClassKey);
                if (subTypes != null) {
                    allObservedClasses.addAll(subTypes);
                }
            }
        }
        return allObservedClasses;
    }
    
    /**
	 * @return the instanceListeners
	 */
	Table<Object, InstanceListener, Set<EClass>> getInstanceListeners() {
		if (instanceListeners == null) {
			instanceListeners = HashBasedTable.create(100,1);
			for (Entry<InstanceListener, Set<EClass>> subscription : subscribedInstanceListeners.entrySet()) {
				final InstanceListener listener = subscription.getKey();
				for (EClass subscriptionType : subscription.getValue()) {
					final Object superElementTypeKey = toKey(subscriptionType);
					addInstanceListenerInternal(listener, subscriptionType, superElementTypeKey);
					final Set<Object> subTypeKeys = contentAdapter.getSubTypeMap().get(superElementTypeKey);
					if (subTypeKeys != null) for (Object subTypeKey : subTypeKeys) {
						addInstanceListenerInternal(listener, subscriptionType, subTypeKey);
					}
				}
			}	
		}
		return instanceListeners;
	}
	Table<Object, InstanceListener, Set<EClass>> peekInstanceListeners() {
		return instanceListeners;
	}

	void addInstanceListenerInternal(final InstanceListener listener,
			EClass subscriptionType, final Object elementTypeKey) {
		Set<EClass> subscriptionTypes = instanceListeners.get(elementTypeKey,listener);
		if (subscriptionTypes == null) {
			subscriptionTypes = new HashSet<EClass>();
			instanceListeners.put(elementTypeKey, listener, subscriptionTypes);
		}
		subscriptionTypes.add(subscriptionType);
	}

	/**
	 * @return the featureListeners
	 */
	Table<Object, FeatureListener, Set<EStructuralFeature>> getFeatureListeners() {
		if (featureListeners == null) {
			featureListeners = HashBasedTable.create(100,1);
			for (Entry<FeatureListener, Set<EStructuralFeature>> subscription : subscribedFeatureListeners.entrySet()) {
				final FeatureListener listener = subscription.getKey();
				for (EStructuralFeature subscriptionType : subscription.getValue()) {
					final Object elementTypeKey = toKey(subscriptionType);
					addFeatureListenerInternal(listener, subscriptionType,
							elementTypeKey);
				}
			}	
		}
		return featureListeners;
	}

	void addFeatureListenerInternal(final FeatureListener listener,
			EStructuralFeature subscriptionType, final Object elementTypeKey) {
		Set<EStructuralFeature> subscriptionTypes = featureListeners.get(elementTypeKey,listener);
		if (subscriptionTypes == null) {
			subscriptionTypes = new HashSet<EStructuralFeature>();
			featureListeners.put(elementTypeKey, listener, subscriptionTypes);
		}
		subscriptionTypes.add(subscriptionType);
	}

	/**
	 * @return the dataTypeListeners
	 */
	Table<Object, DataTypeListener, Set<EDataType>> getDataTypeListeners() {
		if (dataTypeListeners == null) {
			dataTypeListeners = HashBasedTable.create(100,1);
			for (Entry<DataTypeListener, Set<EDataType>> subscription : subscribedDataTypeListeners.entrySet()) {
				final DataTypeListener listener = subscription.getKey();
				for (EDataType subscriptionType : subscription.getValue()) {
					final Object elementTypeKey = toKey(subscriptionType);
					addDatatypeListenerInternal(listener, subscriptionType, elementTypeKey);
				}
			}	
		}
		return dataTypeListeners;
	}

	void addDatatypeListenerInternal(final DataTypeListener listener,
			EDataType subscriptionType, final Object elementTypeKey) {
		Set<EDataType> subscriptionTypes = dataTypeListeners.get(elementTypeKey,listener);
		if (subscriptionTypes == null) {
			subscriptionTypes = new HashSet<EDataType>();
			dataTypeListeners.put(elementTypeKey, listener, subscriptionTypes);
		}
		subscriptionTypes.add(subscriptionType);
	}

	@Override
    public void registerObservedTypes(Set<EClass> classes, Set<EDataType> dataTypes, Set<EStructuralFeature> features) {
        ensureNotInWildcardMode();
        if (classes !=null || features != null || dataTypes!=null) {
			final Set<Object> resolvedFeatures = resolveFeaturesToKey(features);
			final Set<Object> resolvedClasses = resolveClassifiersToKey(classes);
			final Set<Object> resolvedDatatypes = resolveClassifiersToKey(dataTypes);
			
			try {
			     coalesceTraversals(new Callable<Void>() {
			         @Override
			         public Void call() throws Exception {
			         	delayedFeatures.addAll(resolvedFeatures);
			         	delayedDataTypes.addAll(resolvedDatatypes);
			         	delayedClasses.addAll(resolvedClasses);
			         	return null;
			         }
			     });
			 } catch (InvocationTargetException ex) {
			     processingError(ex.getCause(), "register en masse the observed EClasses " + resolvedClasses
			    		 + " and EDatatypes " + resolvedDatatypes
			    		 + " and EStructuralFeatures " + resolvedFeatures);
			 } catch (Exception ex) {
			     processingError(ex, "register en masse the observed EClasses " + resolvedClasses
			    		 + " and EDatatypes " + resolvedDatatypes
			    		 + " and EStructuralFeatures " + resolvedFeatures);
			 }
	     }
	 }

    @Override
    public void unregisterObservedTypes(Set<EClass> classes,
    		Set<EDataType> dataTypes, Set<EStructuralFeature> features) {
    	unregisterEClasses(classes);
    	unregisterEDataTypes(dataTypes);
    	unregisterEStructuralFeatures(features);
    }
    
    @Override
    public void registerEStructuralFeatures(Set<EStructuralFeature> features) {
        ensureNotInWildcardMode();
        if (features != null) {
            final Set<Object> resolved = resolveFeaturesToKey(features);
            
            try {
                coalesceTraversals(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                    	delayedFeatures.addAll(resolved);
                    	return null;
                    }
                });
            } catch (InvocationTargetException ex) {
                processingError(ex.getCause(), "register the observed EStructuralFeatures: " + resolved);
            } catch (Exception ex) {
                processingError(ex, "register the observed EStructuralFeatures: " + resolved);
            }
        }
    }

    @Override
    public void unregisterEStructuralFeatures(Set<EStructuralFeature> features) {
        ensureNotInWildcardMode();
        if (features != null) {
        	final Set<Object> resolved = resolveFeaturesToKey(features);
            ensureNoListeners(resolved, getFeatureListeners());									
            observedFeatures.removeAll(resolved);
            delayedFeatures.removeAll(resolved);
            for (Object f : resolved) {
                contentAdapter.getValueToFeatureToHolderMap().column(f).clear();
                if (contentAdapter.peekFeatureToHolderMap() != null) {
                	contentAdapter.peekFeatureToHolderMap().remove(f);
                }
                if (contentAdapter.peekHolderToFeatureToValueMap() != null) {
                	contentAdapter.peekHolderToFeatureToValueMap().column(f).clear();
                }
            }
        }
    }

	@Override
    public void registerEClasses(Set<EClass> classes) {
        ensureNotInWildcardMode();
        if (classes != null) {
            final Set<Object> resolvedClasses = resolveClassifiersToKey(classes);
            
            try {
                coalesceTraversals(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                    	delayedClasses.addAll(resolvedClasses);
                    	return null;
                    }
                });
            } catch (InvocationTargetException ex) {
                processingError(ex.getCause(), "register the observed EClasses: " + resolvedClasses);
            } catch (Exception ex) {
                processingError(ex, "register the observed EClasses: " + resolvedClasses);
            }
        }
    }

    /**
     * @param classes
     */
    protected void startObservingClasses(Set<Object> classKeys) {
        directlyObservedClasses.addAll(classKeys);
        getAllObservedClassesInternal().addAll(classKeys);
        for (Object classKey : classKeys) {
            final Set<Object> subTypes = contentAdapter.getSubTypeMap().get(classKey);
            if (subTypes != null) {
                allObservedClasses.addAll(subTypes);
            }
        }
    }

    @Override
    public void unregisterEClasses(Set<EClass> classes) {
        ensureNotInWildcardMode();
        if (classes != null) {
        	final Set<Object> resolved = resolveClassifiersToKey(classes);
            ensureNoListeners(resolved, getInstanceListeners());									
            directlyObservedClasses.removeAll(resolved);
            allObservedClasses = null;
            delayedClasses.removeAll(resolved);
            for (Object c : resolved) {
                contentAdapter.removeInstanceSet(c);
            }
        }
    }



    @Override
    public void registerEDataTypes(Set<EDataType> dataTypes) {
        ensureNotInWildcardMode();
        if (dataTypes != null) {
            final Set<Object> resolved = resolveClassifiersToKey(dataTypes);
            
            try {
                coalesceTraversals(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                    	delayedDataTypes.addAll(resolved);
                    	return null;
                    }
                });
            } catch (InvocationTargetException ex) {
                processingError(ex.getCause(), "register the observed EDataTypes: " + resolved);
            } catch (Exception ex) {
                processingError(ex, "register the observed EDataTypes: " + resolved);
            }
        }
    }

    @Override
    public void unregisterEDataTypes(Set<EDataType> dataTypes) {
        ensureNotInWildcardMode();
        if (dataTypes != null) {
        	final Set<Object> resolved = resolveClassifiersToKey(dataTypes);
            ensureNoListeners(resolved, getDataTypeListeners());									
            observedDataTypes.removeAll(resolved);
            delayedDataTypes.removeAll(resolved);
            for (Object dataType : resolved) {
                contentAdapter.removeDataTypeMap(dataType);
            }
        }
    }

    @Override
    public <V> V coalesceTraversals(Callable<V> callable) throws InvocationTargetException {
        V finalResult = null;
        
        if (delayTraversals) { // reentrant case, no special action needed
            try {
            	finalResult = callable.call();
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
            return finalResult;
        }
        
	    boolean firstRun = true;
        while (callable != null) {   // repeat if post-processing needed  	
	        delayedClasses = new HashSet<Object>();
	        delayedFeatures = new HashSet<Object>();
	        delayedDataTypes = new HashSet<Object>();

        	try {
        		try {
        			delayTraversals = true;
        			V result = callable.call();
        			if (firstRun) {
        				firstRun = false;
        				finalResult = result; 
        			}
        		} finally {
        			delayTraversals = false;
        			callable = null;
        			
        			delayedFeatures = setMinus(delayedFeatures, observedFeatures);
        			delayedClasses = setMinus(delayedClasses, directlyObservedClasses);
        			delayedDataTypes = setMinus(delayedDataTypes, observedDataTypes);
        			
        			boolean classesWarrantTraversal = !setMinus(delayedClasses, getAllObservedClassesInternal()).isEmpty();
        			
        			if (!delayedClasses.isEmpty() || !delayedFeatures.isEmpty() || !delayedDataTypes.isEmpty()) {
        				final HashSet<Object> oldClasses = new HashSet<Object>(directlyObservedClasses);
        				startObservingClasses(delayedClasses);
        				observedFeatures.addAll(delayedFeatures);
        				observedDataTypes.addAll(delayedDataTypes);
        				
        				// make copies so that original accumulators can be cleaned for the next cycle
        				// or for the rare case that a coalesced  traversal is invoked during visitation, 
        				// e.g. by a derived feature implementation
        				final HashSet<Object> toGatherClasses = new HashSet<Object>(delayedClasses);
        				final HashSet<Object> toGatherFeatures = new HashSet<Object>(delayedFeatures);
        				final HashSet<Object> toGatherDataTypes = new HashSet<Object>(delayedDataTypes);
        				
        				if (classesWarrantTraversal || !toGatherFeatures.isEmpty() || !toGatherDataTypes.isEmpty()) {
        					// repeat the cycle with this visit
        					final NavigationHelperVisitor visitor = new NavigationHelperVisitor.TraversingVisitor(this,
        							toGatherFeatures, toGatherClasses, oldClasses, toGatherDataTypes);
        					
        					callable = new Callable<V>() {
        						@Override
        						public V call() throws Exception {
        							traverse(visitor);
        							return null;
        						}
							};
        					
        				}
        			}
        		}
	        } catch (Exception e) {
	            getLogger()
	                    .fatal("EMF-IncQuery Base encountered an error while traversing the EMF model to gather new information. ",
	                            e);
	            throw new InvocationTargetException(e);
	        }
        }
        return finalResult;
    }

    private void traverse(final NavigationHelperVisitor visitor) {
        for (Notifier root : modelRoots) {
            comprehension.traverseModel(visitor, root);
        }
        contentAdapter.notifyBaseIndexChangeListeners();
    }

    /**
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void addRoot(Notifier emfRoot) throws IncQueryBaseException {
        addRootInternal(emfRoot);
    }

    
    /* (non-Javadoc)
     * @see org.eclipse.incquery.runtime.base.api.NavigationHelper#cheapMoveTo(org.eclipse.emf.ecore.EObject, org.eclipse.emf.common.util.EList)
     */
    @Override
    public <T extends EObject> void cheapMoveTo(T element, EList<T> targetContainmentReferenceList) {
    	if (element.eAdapters().contains(contentAdapter) && 
    			targetContainmentReferenceList instanceof NotifyingList<?>) {
    		final Object listNotifier = ((NotifyingList<?>)targetContainmentReferenceList).getNotifier();
    		if (listNotifier instanceof Notifier && ((Notifier) listNotifier).eAdapters().contains(contentAdapter)) {
	     		contentAdapter.ignoreInsertionAndDeletion = element;
		    	try {
		    		targetContainmentReferenceList.add(element);
		    	} finally {
		        	contentAdapter.ignoreInsertionAndDeletion = null;
		    	}
    		} else {
    			targetContainmentReferenceList.add(element);
    		}
    	} else {
    		targetContainmentReferenceList.add(element);
    	}
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.incquery.runtime.base.api.NavigationHelper#cheapMoveTo(org.eclipse.emf.ecore.EObject, org.eclipse.emf.ecore.EObject, org.eclipse.emf.ecore.EReference)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void cheapMoveTo(EObject element, EObject parent, EReference containmentFeature) {
    	contentAdapter.maintainMetamodel(containmentFeature);           	
    	if (containmentFeature.isMany())
    		cheapMoveTo(element, (EList)parent.eGet(containmentFeature));
    	else if (element.eAdapters().contains(contentAdapter) &&
    			parent.eAdapters().contains(contentAdapter)) 
    	{
     		contentAdapter.ignoreInsertionAndDeletion = element;
	    	try {
	    		parent.eSet(containmentFeature, element);
	    	} finally {
	        	contentAdapter.ignoreInsertionAndDeletion = null;
	    	}
		} else {
			parent.eSet(containmentFeature, element);
		}
    }
    
    /**
     * @param emfRoot
     * @throws IncQueryBaseException
     */
    private void addRootInternal(Notifier emfRoot) throws IncQueryBaseException {
        if (!((emfRoot instanceof EObject) || (emfRoot instanceof Resource) || (emfRoot instanceof ResourceSet))) {
            throw new IncQueryBaseException(IncQueryBaseException.INVALID_EMFROOT);
        }
        expandToAdditionalRoot(emfRoot);
    }
    
    @Override
    public Set<EClass> getAllCurrentClasses() {
    	return contentAdapter.getAllCurrentClasses();
    }
    
    protected void processingError(Throwable ex, String task) {
        contentAdapter.processingFatal(ex, task);
    }

    private void ensureNotInWildcardMode() {
    	if (inWildcardMode) {
    		throw new IllegalStateException("Cannot register/unregister observed classes in wildcard mode");
    	}
    }
    private <X,Y> void ensureNoListeners(Set<Object> unobservedTypes, final Table<Object, X, Set<Y>> listenerRegistry) {
		if (!Collections.disjoint(unobservedTypes, listenerRegistry.rowKeySet()))
			throw new IllegalStateException("Cannot unregister observed types for which there are active listeners");
    }
    private void ensureNoListenersForDispose() {
    	if (!(baseIndexChangeListeners.isEmpty() && subscribedFeatureListeners.isEmpty() && subscribedDataTypeListeners.isEmpty() && subscribedInstanceListeners.isEmpty()))
    		throw new IllegalStateException("Cannot dispose while there are active listeners");
    }

}

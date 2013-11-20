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

package org.eclipse.incquery.runtime.base.api;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.incquery.runtime.base.exception.IncQueryBaseException;

/**
 * 
 * Using an index of the EMF model, this interface exposes useful query functionality, such as:
 * <ul>
 * <li>
 * Getting all the (direct or descendant) instances of a given {@link EClass}
 * <li>
 * Inverse navigation along arbitrary {@link EReference} instances (heterogenous paths too)
 * <li>
 * Finding model elements by attribute value (i.e. inverse navigation along {@link EAttribute})
 * <li>
 * Querying instances of given data types, or structural features.
 * </ul>
 * As queries are served from an index, results are always instantaneous.
 * 
 * <p>
 * Such indices will be built on an EMF model rooted at an {@link EObject}, {@link Resource} or {@link ResourceSet}. 
 * The boundaries of the model are defined by the containment (sub)tree.
 * The indices will be <strong>maintained incrementally</strong> on changes to the model; these updates can also be
 * observed by registering listeners.
 * </p>
 * 
 * <p>
 * One of the options is to build indices in <em>wildcard mode</em>, meaning that all EClasses, EDataTypes, EReferences
 * and EAttributes are indexed. This is convenient, but comes at a high memory cost. To save memory, one can disable
 * <em>wildcard mode</em> and manually register those EClasses, EDataTypes, EReferences and EAttributes that should be
 * indexed.
 * </p>
 * 
 * <p>
 * Another choice is whether to build indices in <em>dynamic EMF mode</em>, meaning that types are identified by the String IDs 
 * that are ultimately derived from the nsURI of the EPackage. Multiple types with the same ID are treated as the same.
 * This is useful if dynamic EMF is used, where there can be multiple copies (instantiations) of the same EPackage, 
 * representing essentially the same metamodel. If one disables <em>dynamic EMF mode</em>, an error is logged if 
 * duplicate EPackages with the same nsURI are encountered.
 * </p>
 *  
 * <p>
 * Note that none of the defined query methods return null upon empty result sets. All query methods return either a copy of
 * the result sets (where {@link Setting} is instantiated) or an unmodifiable collection of the result view.
 * 
 * <p>
 * Instantiate using {@link IncQueryBaseFactory}
 * 
 * @author Tamas Szabo
 * 
 */
public interface NavigationHelper {

    /**
     * Indicates whether indexing is performed in <em>wildcard mode</em>, where every aspect of the EMF model is
     * automatically indexed.
     * 
     * @return true if everything is indexed, false if manual registration of interesting EClassifiers and
     *         EStructuralFeatures is required.
     */
    public boolean isInWildcardMode();
    
    /**
     * Indicates whether indexing is performed in <em>dynamic EMF mode</em>, i.e. EPackage nsURI collisions are 
     *   tolerated and EPackages with the same URI are automatically considered as equal.
     * 
     * @return true if multiple EPackages with the same nsURI are treated as the same, 
     *         false if an error is logged instead in this case.
     */
    public boolean isInDynamicEMFMode();
    
    /**
     * For a given attribute value <code>value</code>, find each {@link EAttribute} and host {@link EObject} 
     * such that this attribute of the the host object takes the given value. The method will
     * return a set of {@link EStructuralFeature.Setting}s, one for each such host object - EAttribute - value triplet.
     * 
     * <p>
     * <strong>Precondition:</strong> Unset / null attribute values are not indexed, so <code>value!=null</code>
     * 
     * <p>
     * <strong>Precondition:</strong> Will only find those EAttributes that have already been registered using
     * {@link #registerEStructuralFeatures(Set)}, unless running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param value
     *            the value of the attribute
     * @return a set of {@link EStructuralFeature.Setting}s, one for each EObject and EAttribute that have the given value
     * @see #findByAttributeValue(Object)
     */
    public Set<Setting> findByAttributeValue(Object value);

    /**
     * For given <code>attributes</code> and an attribute value <code>value</code>, find each host {@link EObject} 
     * such that any of these attributes of the the host object takes the given value. The method will
     * return a set of {@link EStructuralFeature.Setting}s, one for each such host object - EAttribute - value triplet.
     * 
     * <p>
     * <strong>Precondition:</strong> Unset / null attribute values are not indexed, so <code>value!=null</code>
	 *
     * <p>
     * <strong>Precondition:</strong> Will only find those EAttributes that have already been registered using
     * {@link #registerEStructuralFeatures(Set)}, unless running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param value
     *            the value of the attribute
     * @param attributes
     *            the collection of attributes that should take the given value
     * @return a set of {@link EStructuralFeature.Setting}s, one for each EObject and attribute that have the given value
     */
    public Set<Setting> findByAttributeValue(Object value, Collection<EAttribute> attributes);

    /**
     * Find all {@link EObject}s for which the given <code>attribute</code> takes the given <code>value</code>.
     * 
     * <p>
     * <strong>Precondition:</strong> Unset / null attribute values are not indexed, so <code>value!=null</code>
     * 
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the EAttribute has already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param value
     *            the value of the attribute
     * @param attribute
     *            the EAttribute that should take the given value
     * @return the set of {@link EObject}s for which the given attribute has the given value
     */
    public Set<EObject> findByAttributeValue(Object value, EAttribute attribute);

    /**
     * Returns the set of instances for the given {@link EDataType} that can be found in the model.
     * 
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the EDataType has already been
     * registered using {@link #registerEDataTypes(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param type
     *            the data type
     * @return the set of all attribute values found in the model that are of the given data type
     */
    public Set<Object> getDataTypeInstances(EDataType type);
    
    /**
     * Find all {@link EObject}s that are the target of the EReference <code>reference</code> from the given
     * <code>source</code> {@link EObject}.
     * 
     * <p>
     * Unset / null-valued references are not indexed, and will not be included in the results.
     * 
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the reference has already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param source the host object
     * @param reference an EReference of the host object
     * @return the set of {@link EObject}s that the given reference points to, from the given source object
     */
    public Set<EObject> getReferenceValues(EObject source, EReference reference);
   
    /**
     * Find all {@link Object}s that are the target of the EStructuralFeature <code>feature</code> from the given
     * <code>source</code> {@link EObject}.
     * 
     * <p>
     * Unset / null-valued features are not indexed, and will not be included in the results.
     * 
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the feature has already been
     * registered, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param source the host object
     * @param feature an EStructuralFeature of the host object
     * @return the set of values that the given feature takes at the given source object
     * 
     * @see #getReferenceValues(EObject, EReference)
     */
    public Set<Object> getFeatureTargets(EObject source, EStructuralFeature feature);
   
    /**
     * Find all value assignment instances of the given {@link EStructuralFeature} in the form
     *  of a host {@link EObject} -> value(s) multimap. 
     * 
     * <p>
     * Unset / null-valued features are not indexed, and will not be included in the results.
     * 
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the feature has already been
     * registered, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param feature the EStructuralFeature
     * @return the map from source {@link EObject}s to the value(s) of the given feature
     */
    public Map<EObject, Set<Object>> getFeatureInstances(EStructuralFeature feature);
    
    /**
     * For a given {@link EObject} <code>target</code>, find each {@link EReference} and source {@link EObject} 
     * such that this reference (list) of the the host object points to the given target object. The method will
     * return a set of {@link EStructuralFeature.Setting}s, one for each such source object - EReference - target triplet.
     * 
     * <p>
     * <strong>Precondition:</strong> Unset / null reference values are not indexed, so <code>target!=null</code>
     * 
     * <p>
     * <strong>Precondition:</strong> Results will be returned only for those references that have already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or all references if running in
     * <em>wildcard mode</em> (see {@link #isInWildcardMode()}).
     * 
     * @param target
     *            the EObject pointed to by the references
     * @return a set of {@link EStructuralFeature.Setting}s, one for each source EObject and reference that point to the given target
     */
    public Set<Setting> getInverseReferences(EObject target);

    /**
     * For given <code>references</code> and an {@link EObject} <code>target</code>, find each source {@link EObject} 
     * such that any of these references of the the source object points to the given target object. The method will
     * return a set of {@link EStructuralFeature.Setting}s, one for each such source object - EReference - target triplet.
     * 
     * <p>
     * <strong>Precondition:</strong> Unset / null reference values are not indexed, so <code>target!=null</code>
     * 
     * <p>
     * <strong>Precondition:</strong> Will only find those EReferences that have already been registered using
     * {@link #registerEStructuralFeatures(Set)}, unless running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param target
     *            the EObject pointed to by the references
     * @param references a set of EReferences pointing to the target 
     * @return a set of {@link EStructuralFeature.Setting}s, one for each source EObject and reference that point to the given target
     */
    public Set<Setting> getInverseReferences(EObject target, Collection<EReference> references);

    /**
     * Find all source {@link EObject}s for which the given <code>reference</code> points to the given <code>target</code> object.
     * 
     * <p>
     * <strong>Precondition:</strong> Unset / null reference values are not indexed, so <code>target!=null</code>
     * 
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the reference has already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param target
     *            the EObject pointed to by the references
     * @param reference
     *            an EReference pointing to the target
     * @return the collection of {@link EObject}s for which the given reference points to the given target object
     */
    public Set<EObject> getInverseReferences(EObject target, EReference reference);

    /**
     * Get the direct {@link EObject} instances of the given {@link EClass}. Instances of subclasses will be excluded.
     * 
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the EClass (or any superclass) has
     * already been registered using {@link #registerEClasses(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param clazz
     *            an EClass
     * @return the collection of {@link EObject} direct instances of the given EClass (not of subclasses)
     * 
     * @see #getAllInstances(EClass)
     */
    public Set<EObject> getDirectInstances(EClass clazz);

    /**
     * Get the all {@link EObject} instances of the given {@link EClass}. 
     * This includes instances of subclasses.
     * 
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the EClass (or any superclass) has
     * already been registered using {@link #registerEClasses(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param clazz
     *            an EClass
     * @return the collection of {@link EObject} instances of the given EClass and any of its subclasses
     * 
     * @see #getDirectInstances(EClass)
     */
    public Set<EObject> getAllInstances(EClass clazz);

    /**
     * Find all source {@link EObject}s for which the given <code>feature</code> points to / takes the given <code>value</code>.
     * 
     * <p>
     * <strong>Precondition:</strong> Unset / null-valued features are not indexed, so <code>value!=null</code>
     * 
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the feature has already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param value
     *            the value of the feature
     * @param feature
     *            the feature instance
     * @return the collection of {@link EObject} instances
     */
    public Set<EObject> findByFeatureValue(Object value, EStructuralFeature feature);

    /**
     * Returns those host {@link EObject}s that have a non-null value for the given feature 
     * (at least one, in case of multi-valued references).
     * 
     * <p>
     * Unset / null-valued features are not indexed, and will not be included in the results.
     * 
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the feature has already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param feature
     *            a structural feature 
     * @return the collection of {@link EObject}s that have some value for the given structural feature
     */
    public Set<EObject> getHoldersOfFeature(EStructuralFeature feature);

    /**
     * Call this method to dispose the NavigationHelper. 
     * 
     * <p>After its disposal, the NavigationHelper will no longer listen to EMF change notifications, 
     *   and it will be possible to GC it even if the model is retained in memory.
     * 
     * <dt><b>Precondition:</b><dd> no listeners can be registered at all.
     * @throws IllegalStateException if there are any active listeners
     * 
     */
    public void dispose();

    /**
     * The given <code>listener</code> will be notified from now on whenever instances the given {@link EClass}es 
     * (and any of their subtypes) are added to or removed from the model.  
     *  
     * @param classes
     *            the collection of classes whose instances the listener should be notified of
     * @param listener
     *            the listener instance
     */
    public void addInstanceListener(Collection<EClass> classes, InstanceListener listener);

    /**
     * Unregisters an instance listener for the given classes.
     * 
     * @param classes
     *            the collection of classes
     * @param listener
     *            the listener instance
     */
    public void removeInstanceListener(Collection<EClass> classes, InstanceListener listener);

    /**
     * The given <code>listener</code> will be notified from now on whenever instances the given {@link EDataType}s 
     * are added to or removed from the model.  
     * 
     * @param types
     *            the collection of types associated to the listener
     * @param listener
     *            the listener instance
     */
    public void addDataTypeListener(Collection<EDataType> types, DataTypeListener listener);

    /**
     * Unregisters a data type listener for the given types.
     * 
     * @param types
     *            the collection of data types
     * @param listener
     *            the listener instance
     */
    public void removeDataTypeListener(Collection<EDataType> types, DataTypeListener listener);

    /**
     * The given <code>listener</code> will be notified from now on whenever instances the given {@link EStructuralFeature}s 
     * are added to or removed from the model.  
     * 
     * @param features
     *            the collection of features associated to the listener
     * @param listener
     *            the listener instance
     */
    public void addFeatureListener(Collection<EStructuralFeature> features, FeatureListener listener);

    /**
     * Unregisters a feature listener for the given features.
     * 
     * @param listener
     *            the listener instance
     * @param features
     *            the collection of features
     */
    public void removeFeatureListener(Collection<EStructuralFeature> features, FeatureListener listener);

    /**
     * Register a lightweight observer that is notified if the value of any feature of the given EObject changes.
     * 
     * @param observer the listener instance
     * @param observedObject the observed EObject
     */
    public void addLightweightEObjectObserver(LightweightEObjectObserver observer, EObject observedObject);
    
    /**
     * Unregisters a lightweight observer for the given EObject.
     * 
     * @param observer the listener instance
     * @param observedObjectt the observed EObject
     */
    public void removeLightweightEObjectObserver(LightweightEObjectObserver observer, EObject observedObject);
    
    /**
     * Manually turns on indexing for the given types (indexing of others are unaffected). Note that
     * registering new types will result in a single iteration through the whole attached model.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     * @throws IllegalStateException if in wildcard mode
     * 
     * @param classes
     *            the set of classes to observe (null okay)
     * @param dataTypes
     *            the set of data types to observe (null okay)
     * @param features
     *            the set of features to observe (null okay)
     */
    public void registerObservedTypes(Set<EClass> classes, Set<EDataType> dataTypes, Set<EStructuralFeature> features);
    
    /**
     * Manually turns off indexing for the given types (indexing of others are unaffected). Note that if the
     * unregistered types are re-registered later, the whole attached model needs to be visited again.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     * 
     * <dt><b>Precondition:</b><dd> no listeners can be registered for the given types.
     * @throws IllegalStateException if in wildcard mode, or if there are listeners registered for the given types
     * @param classes
     *            the set of classes that will be ignored again from now on (null okay)
     * @param dataTypes
     *            the set of data types that will be ignored again from now on (null okay)
     * @param features
     *            the set of features that will be ignored again from now on (null okay)
     */
    public void unregisterObservedTypes(Set<EClass> classes, Set<EDataType> dataTypes, Set<EStructuralFeature> features);
   
    
    /**
     * Manually turns on indexing for the given features (indexing of other features are unaffected). Note that
     * registering new features will result in a single iteration through the whole attached model.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     * @throws IllegalStateException if in wildcard mode
     * 
     * @param features
     *            the set of features to observe
     */
    public void registerEStructuralFeatures(Set<EStructuralFeature> features);

    /**
     * Manually turns off indexing for the given features (indexing of other features are unaffected). Note that if the
     * unregistered features are re-registered later, the whole attached model needs to be visited again.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     * 
     * <dt><b>Precondition:</b><dd> no listeners can be registered for the given features.
     * @throws IllegalStateException if in wildcard mode, or if there are listeners registered for the given types
     * 
     * @param features
     *            the set of features that will be ignored again from now on
     */
    public void unregisterEStructuralFeatures(Set<EStructuralFeature> features);

    /**
     * Manually turns on indexing for the given classes (indexing of other classes are unaffected). Instances of
     * subclasses will also be indexed. Note that registering new classes will result in a single iteration through the whole
     * attached model.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     * @throws IllegalStateException if in wildcard mode
     * 
     * @param classes
     *            the set of classes to observe
     */
    public void registerEClasses(Set<EClass> classes);

    /**
     * Manually turns off indexing for the given classes (indexing of other classes are unaffected). Note that if the
     * unregistered classes are re-registered later, the whole attached model needs to be visited again.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     * 
     * <dt><b>Precondition:</b><dd> no listeners can be registered for the given classes.
     * @throws IllegalStateException if in wildcard mode, or if there are listeners registered for the given types
     * @param classes
     *            the set of classes that will be ignored again from now on
     */
    public void unregisterEClasses(Set<EClass> classes);

    /**
     * Manually turns on indexing for the given data types (indexing of other features are unaffected). Note that
     * registering new data types will result in a single iteration through the whole attached model.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     * @throws IllegalStateException if in wildcard mode
     * 
     * @param dataTypes
     *            the set of data types to observe
     */
    public void registerEDataTypes(Set<EDataType> dataTypes);

    /**
     * Manually turns off indexing for the given data types (indexing of other data types are unaffected). Note that if
     * the unregistered data types are re-registered later, the whole attached model needs to be visited again.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     * 
     * <dt><b>Precondition:</b><dd> no listeners can be registered for the given datatypes.
     * @throws IllegalStateException if in wildcard mode, or if there are listeners registered for the given types
     * 
     * @param dataTypes
     *            the set of data types that will be ignored again from now on
     */
    public void unregisterEDataTypes(Set<EDataType> dataTypes);

    /**
     * The given callback will be executed, and all model traversals and index registrations will be delayed until the
     * execution is done. If there are any outstanding feature, class or datatype registrations, a single coalesced model
     * traversal will initialize the caches and deliver the notifications.
     * 
     * @param runnable
     */
    public <V> V coalesceTraversals(Callable<V> callable) throws InvocationTargetException;

    /**
     * Adds a coarse-grained listener that will be invoked after the NavigationHelper index or the underlying model is changed. Can be used
     * e.g. to check model contents. Not intended for general use.
     * 
     * <p/> See {@link #removeBaseIndexChangeListener(IncQueryBaseIndexChangeListener)}
     * @param listener
     */
    public void addBaseIndexChangeListener(IncQueryBaseIndexChangeListener listener);
    
    /**
     * Removes a registered listener.
     * 
     * <p/> See {@link #addBaseIndexChangeListener(IncQueryBaseIndexChangeListener)}
     * 
     * @param listener
     */
    public void removeBaseIndexChangeListener(IncQueryBaseIndexChangeListener listener);
    
    /**
     * Adds an additional EMF model root.
     * 
     * @param emfRoot
     */
    public void addRoot(Notifier emfRoot) throws IncQueryBaseException;
    
    /**
     * Moves an EObject (along with its entire containment subtree) within the containment hierarchy of the EMF model. 
     *   The object will be relocated from the original parent object to a different parent, or a different containment 
     *   list of the same parent. 
     *   
     * <p> When indexing is enabled, such a relocation is costly if performed through normal getters/setters, as the index 
     * for the entire subtree is pruned at the old location and reconstructed at the new one. 
     * This method provides a workaround to keep the operation cheap.
     * 
     * <p> This method is experimental. Re-entrancy not supported.
     * 
     * @param element the eObject to be moved
     * @param the containment list of the new parent object into which the element has to be moved
     * 
     */
    public <T extends EObject> void cheapMoveTo(T element, EList<T> targetContainmentReferenceList);
    
    /**
     * Moves an EObject (along with its entire containment subtree) within the containment hierarchy of the EMF model. 
     *   The object will be relocated from the original parent object to a different parent, or a different containment 
     *   list of the same parent. 
     *   
     * <p> When indexing is enabled, such a relocation is costly if performed through normal getters/setters, as the index 
     * for the entire subtree is pruned at the old location and reconstructed at the new one. 
     * This method provides a workaround to keep the operation cheap.
     * 
     * <p> This method is experimental. Re-entrancy not supported.
     * 
     * @param element the eObject to be moved
     * @param parent  the new parent object under which the element has to be moved
     * @param containmentFeature the kind of containment reference that should be established between the new parent and the element
     * 
     */
    public void cheapMoveTo(EObject element, EObject parent, EReference containmentFeature);

    /**
     * Traverses all instances of a selected feature stored in the base index, and allows executing a custom function on
     * it. There is no guaranteed order in which the processor will be called with the selected features.
     * 
     * <p>
     * <strong>Precondition:</strong> Will only find those EAttributes that have already been registered using
     * {@link #registerEStructuralFeatures(Set)}, unless running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * 
     * @param feature
     * @param processor
     */
    public void processAllFeatureInstances(EStructuralFeature feature, IEStructuralFeatureProcessor processor);
    
    /**
     * Returns all EClasses that currently have direct instances cached by the index. <ul>
     * <li> Supertypes will not be returned, unless they have direct instances in the model as well.
     * <li> If not in <em>wildcard mode</em>, only registered EClasses and their subtypes will be considered.
     * <li> Note for advanced users: if a type is represented by multiple EClass objects, one of them is chosen as representative and returned.
     * </ul>
     */
    public Set<EClass> getAllCurrentClasses();


}

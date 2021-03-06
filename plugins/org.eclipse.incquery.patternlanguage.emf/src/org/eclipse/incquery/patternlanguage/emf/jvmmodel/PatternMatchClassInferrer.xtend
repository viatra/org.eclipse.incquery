/*******************************************************************************
 * Copyright (c) 2010-2012, Mark Czotter, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mark Czotter - initial API and implementation
 *******************************************************************************/

package org.eclipse.incquery.patternlanguage.emf.jvmmodel

import com.google.inject.Inject
import java.util.Arrays
import java.util.List
import org.eclipse.incquery.patternlanguage.emf.util.EMFJvmTypesBuilder
import org.eclipse.incquery.patternlanguage.emf.util.EMFPatternLanguageJvmModelInferrerUtil
import org.eclipse.incquery.patternlanguage.patternLanguage.Pattern
import org.eclipse.incquery.patternlanguage.patternLanguage.Variable
import org.eclipse.incquery.runtime.api.IPatternMatch
import org.eclipse.incquery.runtime.exception.IncQueryException
import org.eclipse.xtext.common.types.JvmDeclaredType
import org.eclipse.xtext.common.types.JvmType
import org.eclipse.xtext.common.types.JvmTypeReference
import org.eclipse.xtext.common.types.JvmVisibility
import org.eclipse.xtext.common.types.util.TypeReferences
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociator
import org.eclipse.xtext.xbase.jvmmodel.JvmAnnotationReferenceBuilder
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder

/**
 * {@link IPatternMatch} implementation inferer.
 *
 * @author Mark Czotter
 */
class PatternMatchClassInferrer {

	@Inject extension EMFJvmTypesBuilder
	@Inject extension IQualifiedNameProvider
	@Inject extension EMFPatternLanguageJvmModelInferrerUtil
	@Inject TypeReferences typeReference
	@Extension private JvmTypeReferenceBuilder builder
	@Extension private JvmAnnotationReferenceBuilder annBuilder
	@Inject extension IJvmModelAssociator associator
	@Inject extension JavadocInferrer
	
	def inferMatchClassElements(JvmDeclaredType it, Pattern pattern, JvmType querySpecificationClass, JvmTypeReferenceBuilder builder, JvmAnnotationReferenceBuilder annBuilder) {
   		this.builder = builder
   		this.annBuilder = annBuilder
	
		documentation = pattern.javadocMatchClass.toString
		abstract = true
		//it.superTypes += pattern.newTypeRef(typeof (IPatternMatch))
		inferMatchClassFields(pattern)
		inferMatchClassConstructors(pattern)
		inferMatchClassGetters(pattern)
		inferMatchClassSetters(pattern)
		inferMatchClassMethods(pattern, typeRef(querySpecificationClass))
		inferMatchInnerClasses(pattern)	
	}
	
   	/**
   	 * Infers fields for Match class based on the input 'pattern'.
   	 */
   	def inferMatchClassFields(JvmDeclaredType matchClass, Pattern pattern) {
   		for (Variable variable : pattern.parameters) {
   			matchClass.members += variable.toField(variable.fieldName, variable.calculateType)
   		}
		matchClass.members += pattern.toField("parameterNames", builder.typeRef(typeof (List), builder.typeRef(typeof (String)))) [
 			static = true
   			initializer = '''makeImmutableList(«FOR variable : pattern.parameters SEPARATOR ', '»"«variable.name»"«ENDFOR»)'''
   		]
   	}

   	/**
   	 * Infers constructors for Match class based on the input 'pattern'.
   	 */
   	def inferMatchClassConstructors(JvmDeclaredType matchClass, Pattern pattern) {
   		matchClass.members += pattern.toConstructor() [
   			simpleName = pattern.matchClassName
   			visibility = JvmVisibility::PRIVATE //DEFAULT
   			for (Variable variable : pattern.parameters) {
   				val javaType = variable.calculateType
   				parameters += variable.toParameter(variable.parameterName, javaType)
   			}
   			body = '''
   				«FOR variable : pattern.parameters»
   				this.«variable.fieldName» = «variable.parameterName»;
   				«ENDFOR»
   			'''
   		]
   	}

   	/**
   	 * Infers getters for Match class based on the input 'pattern'.
   	 */
   	def inferMatchClassGetters(JvmDeclaredType matchClass, Pattern pattern) {
		matchClass.members += pattern.toMethod("get", typeRef(typeof (Object))) [
   			annotations += annotationRef(typeof (Override))
   			parameters += pattern.toParameter("parameterName", typeRef(typeof (String)))
   			body = '''
   				«FOR variable : pattern.parameters»
   				if ("«variable.name»".equals(parameterName)) return this.«variable.fieldName»;
   				«ENDFOR»
   				return null;
   			'''
   		]
   		for (Variable variable : pattern.parameters) {
			 val getter = variable.toMethod(variable.getterMethodName, variable.calculateType) [
	   			body = '''
	   				return this.«variable.fieldName»;
	   			'''
	   		]
	   		matchClass.members += getter
	   		associator.associatePrimary(variable, getter)
   		}
   	}

   	/**
   	 * Infers setters for Match class based on the input 'pattern'.
   	 */
   	def inferMatchClassSetters(JvmDeclaredType matchClass, Pattern pattern) {
   		matchClass.members += pattern.toMethod("set", typeRef(typeof (boolean))) [
   			returnType = typeRef(Boolean::TYPE)
   			annotations += annotationRef(typeof (Override))
   			parameters += pattern.toParameter("parameterName", typeRef(typeof (String)))
   			parameters += pattern.toParameter("newValue", typeRef(typeof (Object)))
   			body = '''
   				if (!isMutable()) throw new java.lang.UnsupportedOperationException();
   				«FOR variable : pattern.parameters»
   				«val type = variable.calculateType»
   				if ("«variable.name»".equals(parameterName) «IF typeReference.is(type, typeof(Object))»&& newValue instanceof «type»«ENDIF») {
   					this.«variable.fieldName» = («type») newValue;
   					return true;
   				}
   				«ENDFOR»
   				return false;
   			'''
   		]
   		for (Variable variable : pattern.parameters) {
   			matchClass.members += pattern.toMethod(variable.setterMethodName, null) [
   				returnType = typeRef(Void::TYPE)
   				parameters += variable.toParameter(variable.parameterName, variable.calculateType)
   				body = '''
   					if (!isMutable()) throw new java.lang.UnsupportedOperationException();
   					this.«variable.fieldName» = «variable.parameterName»;
   				'''
   			]
   		}
   	}

	/**
   	 * Infers methods for Match class based on the input 'pattern'.
   	 */
   	def inferMatchClassMethods(JvmDeclaredType matchClass, Pattern pattern, JvmTypeReference querySpecificationClassRef) {
   		matchClass.members += pattern.toMethod("patternName", typeRef(typeof(String))) [
   			annotations += annotationRef(typeof (Override))
   			body = '''
   				return "«pattern.fullyQualifiedName»";
   			'''
   		]
		// add extra methods like equals, hashcode, toArray, parameterNames
		matchClass.members += pattern.toMethod("parameterNames", typeRef(typeof (List), builder.typeRef(typeof (String)))) [
   			annotations += annotationRef(typeof (Override))
   			body = '''
   				return «pattern.matchClassName».parameterNames;
   			'''
   		]
   		matchClass.members += pattern.toMethod("toArray", typeRef(typeof (Object)).addArrayTypeDimension) [
   			annotations += annotationRef(typeof (Override))
   			body = '''
   				return new Object[]{«FOR variable : pattern.parameters SEPARATOR ', '»«variable.fieldName»«ENDFOR»};
   			'''
   		]
   		matchClass.members += pattern.toMethod("toImmutable", typeRef(matchClass)) [
   			annotations += annotationRef(typeof (Override))
   			body = '''
   				return isMutable() ? newMatch(«FOR variable : pattern.parameters SEPARATOR ', '»«variable.fieldName»«ENDFOR») : this;
   			'''
   		]
		matchClass.members += pattern.toMethod("prettyPrint", typeRef(typeof (String))) [
			annotations += annotationRef(typeof (Override))
			setBody = '''
					«IF pattern.parameters.empty»
						return "[]";
					«ELSE»
					«StringBuilder» result = new «StringBuilder»();
					«FOR variable : pattern.parameters SEPARATOR " + \", \");\n" AFTER ");\n"»
						result.append("\"«variable.name»\"=" + prettyPrintValue(«variable.fieldName»)
					«ENDFOR»
					return result.toString();
					«ENDIF»
				'''
		]
		matchClass.members += pattern.toMethod("hashCode", typeRef(typeof (int))) [
			annotations += annotationRef(typeof (Override))
			body = '''
				final int prime = 31;
				int result = 1;
				«FOR variable : pattern.parameters»
				result = prime * result + ((«variable.fieldName» == null) ? 0 : «variable.fieldName».hashCode());
				«ENDFOR»
				return result;
			'''
		]
		matchClass.members += pattern.toMethod("equals", typeRef(typeof (boolean))) [
			annotations += annotationRef(typeof (Override))
			parameters += pattern.toParameter("obj", typeRef(typeof (Object)))
			body = '''
				if (this == obj)
					return true;
				if (!(obj instanceof «pattern.matchClassName»)) { // this should be infrequent
					if (obj == null) {
						return false;
					}
					if (!(obj instanceof «IPatternMatch»)) {
						return false;
					}
					«IPatternMatch» otherSig  = («IPatternMatch») obj;
					if (!specification().equals(otherSig.specification()))
						return false;
					return «Arrays».deepEquals(toArray(), otherSig.toArray());
				}
				«IF !pattern.parameters.isEmpty»
				«pattern.matchClassName» other = («pattern.matchClassName») obj;
				«FOR variable : pattern.parameters»
				if («variable.fieldName» == null) {if (other.«variable.fieldName» != null) return false;}
				else if (!«variable.fieldName».equals(other.«variable.fieldName»)) return false;
				«ENDFOR»
				«ENDIF»
				return true;
			'''
		]
		matchClass.members += pattern.toMethod("specification", querySpecificationClassRef) [
			annotations += annotationRef(typeof (Override))
			body = '''
				try {
					return «querySpecificationClassRef.type.simpleName».instance();
				} catch («IncQueryException» ex) {
				 	// This cannot happen, as the match object can only be instantiated if the query specification exists
				 	throw new «IllegalStateException» (ex);
				}
			'''
		]
		matchClass.members += pattern.toMethod("newEmptyMatch", typeRef(matchClass)) [
   			static = true
   			documentation = pattern.javadocNewEmptyMatchMethod.toString
   			body = '''
   				return new Mutable(«FOR p : pattern.parameters SEPARATOR ", "»null«ENDFOR»);
   			'''
   		]
		matchClass.members += pattern.toMethod("newMutableMatch", typeRef(matchClass)) [
   			static = true
   			parameters += pattern.parameters.map[it.toParameter(parameterName, calculateType)]
   			documentation = pattern.javadocNewMutableMatchMethod.toString
   			body = '''
   				return new Mutable(«FOR p : pattern.parameters SEPARATOR ", "»«p.parameterName»«ENDFOR»);
   			'''
   		]
		matchClass.members += pattern.toMethod("newMatch", typeRef(matchClass)) [
   			static = true
   			parameters += pattern.parameters.map[it.toParameter(parameterName, calculateType)]
   			documentation = pattern.javadocNewMatchMethod.toString
   			body = '''
   				return new Immutable(«FOR p : pattern.parameters SEPARATOR ", "»«p.parameterName»«ENDFOR»);
   			'''
   		]
  	}

 	/**
   	 * Infers inner classes for Match class based on the input 'pattern'.
   	 */
  	def inferMatchInnerClasses(JvmDeclaredType matchClass, Pattern pattern) {
  		matchClass.members += matchClass.makeMatchInnerClass(pattern, pattern.matchMutableInnerClassName, true);
  		matchClass.members += matchClass.makeMatchInnerClass(pattern, pattern.matchImmutableInnerClassName, false);
	}

 	/**
   	 * Infers a single inner class for Match class
   	 */
	def makeMatchInnerClass(JvmDeclaredType matchClass, Pattern pattern, String innerClassName, boolean isMutable) {
		pattern.toClass(innerClassName) [
			visibility = JvmVisibility::PRIVATE
			static = true
			final = true
			superTypes += typeRef(matchClass)

			members+= pattern.toConstructor() [
	   			simpleName = innerClassName
	   			visibility = JvmVisibility::DEFAULT
	   			for (Variable variable : pattern.parameters) {
	   				val javaType = variable.calculateType
	   				parameters += variable.toParameter(variable.parameterName, javaType)
	   			}
	   			body = '''
	   				super(«FOR variable : pattern.parameters SEPARATOR ", "»«variable.parameterName»«ENDFOR»);
	   			'''
			]
			members += pattern.toMethod("isMutable", typeRef(typeof (boolean))) [
				visibility = JvmVisibility::PUBLIC
				annotations += annotationRef(typeof (Override))
				body = '''return «isMutable»;'''
			]
		]
	}



}
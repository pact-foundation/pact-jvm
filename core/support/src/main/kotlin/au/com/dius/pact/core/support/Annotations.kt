package au.com.dius.pact.core.support

import mu.KLogging
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

object Annotations : KLogging() {
  /**
   * Searches for the given annotation, first up the class hierarchy, then on enclosing classes if there are any
   */
  fun searchForAnnotation(clazz: Class<*>, annotation: Class<*>) =
    searchForAnnotation(clazz.kotlin, annotation.kotlin)

  /**
   * Searches for the given annotation, first up the class hierarchy, then on enclosing classes if there are any
   */
  fun searchForAnnotation(clazz: KClass<*>, annotation: KClass<*>): KClass<*>? {
    logger.trace { "searchForAnnotation($clazz, $annotation)" }
    var result = searchForAnnotationOnClassHierarchy(clazz, annotation)
    if (result == null && clazz.isInner) {
      result = searchForAnnotationOnOuterClass(clazz, annotation)
    }
    return result
  }

  private fun searchForAnnotationOnOuterClass(clazz: KClass<*>, annotation: KClass<*>): KClass<*>? {
    logger.trace { "searchForAnnotationOnOuterClass($clazz, $annotation)" }
    val outer = clazz.java.enclosingClass.kotlin
    return searchForAnnotation(outer, annotation)
  }

  private fun searchForAnnotationOnClassHierarchy(clazz: KClass<*>, annotation: KClass<*>): KClass<*>? {
    logger.trace { "searchForAnnotationOnClassHierarchy($clazz, $annotation)" }
    return if (classHasAnnotation(clazz, annotation)) {
      clazz
    } else {
      clazz.superclasses.fold(null) { acc: KClass<*>?, value ->
        acc ?: searchForAnnotationOnClassHierarchy(value, annotation)
      }
    }
  }

  private fun classHasAnnotation(clazz: KClass<*>, annotation: KClass<*>) =
    clazz.annotations.find { it.annotationClass == annotation } != null
}

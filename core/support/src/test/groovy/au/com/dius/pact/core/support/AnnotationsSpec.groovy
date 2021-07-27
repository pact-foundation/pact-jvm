package au.com.dius.pact.core.support

import spock.lang.Specification

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface ToFind {}

class AnnotationsSpec extends Specification {
  @ToFind
  static class WithAnnotation {}

  static class ChildClass extends WithAnnotation {}

  @ToFind
  static class OuterClass {
    class InnerClass {
      class InnerClass2 {}
    }
  }

  static class OuterChildClass extends WithAnnotation {
    class InnerClass {
      class InnerClass2 {}
    }
  }

  def 'finds annotation on provided class'() {
    expect:
    Annotations.INSTANCE.searchForAnnotation(WithAnnotation, ToFind).toString()
      == 'class au.com.dius.pact.core.support.AnnotationsSpec$WithAnnotation'
  }

  def 'finds annotation on parent class'() {
    expect:
    Annotations.INSTANCE.searchForAnnotation(ChildClass, ToFind).toString()
      == 'class au.com.dius.pact.core.support.AnnotationsSpec$WithAnnotation'
  }

  def 'finds annotation on outer class'() {
    expect:
    Annotations.INSTANCE.searchForAnnotation(OuterClass.InnerClass.InnerClass2, ToFind).toString()
      == 'class au.com.dius.pact.core.support.AnnotationsSpec$OuterClass'
  }

  def 'finds annotation on outer class parent'() {
    expect:
    Annotations.INSTANCE.searchForAnnotation(OuterChildClass.InnerClass.InnerClass2, ToFind).toString()
      == 'class au.com.dius.pact.core.support.AnnotationsSpec$WithAnnotation'
  }

  def 'returns null if the annotation is not found'() {
    expect:
    Annotations.INSTANCE.searchForAnnotation(AnnotationsSpec, ToFind) == null
  }
}

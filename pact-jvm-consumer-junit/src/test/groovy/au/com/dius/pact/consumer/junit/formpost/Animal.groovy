package au.com.dius.pact.consumer.junit.formpost

import groovy.transform.Canonical

@Canonical
class Animal {
  String animalType
  String name
  List feedingLog = []
}

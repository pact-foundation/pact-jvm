package au.com.dius.pact.provider

import au.com.dius.pact.provider.junitsupport.Provider

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Provider('TestConsumer')
@Target(value = ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface IsTestConsumer {

}

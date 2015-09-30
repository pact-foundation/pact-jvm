/**
 * 
 */
package au.com.dius.pact.consumer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import au.com.dius.pact.model.v3.messaging.Message;
import au.com.dius.pact.model.v3.messaging.MessagePact;

/**
 *
 */
public class MessagePactProviderRule extends ExternalResource {
	
	private Map<String, Message> providerStateMessages;
	private Object testClassInstance;
	public static VerificationResult PACT_VERIFIED = PactVerified$.MODULE$;
	private MessagePact messagePact;
	
	/**
	 * @param testClassInstance
	 */
	public MessagePactProviderRule(Object testClassInstance) {
		this.testClassInstance = testClassInstance;
	}

	/* (non-Javadoc)
	 * @see org.junit.rules.ExternalResource#apply(org.junit.runners.model.Statement, org.junit.runner.Description)
	 */
	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				PactVerification pactDef = description.getAnnotation(PactVerification.class);
				if (pactDef == null) {
					base.evaluate();
					return;
				} 
				
				
				Message providedMessage = null;
				Map<String, Message> pacts = parsePacts();
				if (pactDef.value().length == 2 && !pactDef.value()[1].trim().isEmpty()) {
					providedMessage = pacts.get(pactDef.value()[1].trim());
				} else if (!pacts.isEmpty()) {
					providedMessage = pacts.values().iterator().next();
				}
				
				if (providedMessage == null) {
					base.evaluate();
					return;
				} 
				
				Method messageSetter = null ;
				try {
					messageSetter = description.getTestClass().getMethod("setMessage", byte[].class);
				} catch (Exception e) {
					//ignore 
					base.evaluate();
					return;
				}
				
				if (messageSetter != null) {
					messageSetter.invoke(testClassInstance, providedMessage.contentsAsBytes());
				}
				
				try {
					base.evaluate();
					messagePact.write(PactConsumerConfig$.MODULE$.pactRootDir());
				} catch (Throwable t) {
					throw t;
				}
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Message> parsePacts() {
        if (providerStateMessages == null) {
        	providerStateMessages = new HashMap <String, Message> ();
            for (Method m: testClassInstance.getClass().getMethods()) {
                if (conformsToSignature(m)) {
	                Pact pact = m.getAnnotation(Pact.class);
	                if (pact != null) {
	                	String provider = pact.provider();
	                	if (provider != null && !provider.trim().isEmpty()) {
	                		MessagePactBuilder builder = MessagePactBuilder.consumer(pact.consumer()).hasPactWith(provider);
	                		List<Message> messages = null;
	                		try {
	                			messagePact = (MessagePact) m.invoke(testClassInstance, builder);
		                		messages = messagePact.getMessages();
	                		} catch (Exception e) {
		                        throw new RuntimeException("Failed to invoke pact method", e);
		                    }
	                		
	                		for (Message message : messages) {
	                			providerStateMessages.put(message.getProviderState(), message);
	                		}

	                	}
	                }
                }
            }
        }
        
        return providerStateMessages;
	}

    /**
     * validates method signature as described at {@link Pact}
     */
    private boolean conformsToSignature(Method m) {
        Pact pact = m.getAnnotation(Pact.class);
        boolean conforms =
            pact != null
            && MessagePact.class.isAssignableFrom(m.getReturnType())
            && m.getParameterTypes().length == 1
            && m.getParameterTypes()[0].isAssignableFrom(MessagePactBuilder.class);

        if (!conforms && pact != null) {
            throw new UnsupportedOperationException("Method " + m.getName() +
                " does not conform required method signature 'public PactFragment xxx(PactDslWithProvider builder)'");
        }
        return conforms;
    }

}

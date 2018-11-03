package au.com.dius.pact.consumer;

import au.com.dius.pact.consumer.junit.JUnitTestSupport;
import au.com.dius.pact.model.PactSpecVersion;
import au.com.dius.pact.model.v3.messaging.Message;
import au.com.dius.pact.model.v3.messaging.MessagePact;
import org.apache.commons.lang3.StringUtils;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A junit rule that wraps every test annotated with {@link PactVerification}.
 */
public class MessagePactProviderRule extends ExternalResource {
	
	private final String provider;
	private final Object testClassInstance;
	private byte[] message;
	private Map<String, Message> providerStateMessages;
	private MessagePact messagePact;
	private Map<String, String> metadata;

	/**
	 * @param testClassInstance
	 */
	public MessagePactProviderRule(Object testClassInstance) {
		this(null, testClassInstance);
	}

	public MessagePactProviderRule(String provider, Object testClassInstance) {
		this.provider = provider;
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
				PactVerifications pactVerifications = description.getAnnotation(PactVerifications.class);
				if (pactVerifications != null) {
					evaluatePactVerifications(pactVerifications, base, description);
					return;
				}

				PactVerification pactDef = description.getAnnotation(PactVerification.class);
				// no pactVerification? execute the test normally
				if (pactDef == null) {
					base.evaluate();
					return;
				}

				Message providedMessage = null;
				Map<String, Message> pacts;
				if (StringUtils.isNoneEmpty(pactDef.fragment())) {
          Optional<Method> possiblePactMethod = findPactMethod(pactDef);
          if (!possiblePactMethod.isPresent()) {
            base.evaluate();
            return;
          }

          pacts = new HashMap<>();
          Method method = possiblePactMethod.get();
          Pact pact = method.getAnnotation(Pact.class);
          MessagePactBuilder builder = MessagePactBuilder.consumer(pact.consumer()).hasPactWith(provider);
          messagePact = (MessagePact) method.invoke(testClassInstance, builder);
          for (Message message : messagePact.getMessages()) {
            pacts.put(message.getProviderState(), message);
          }
        } else {
          pacts = parsePacts();
        }

        if (pactDef.value().length == 2 && !pactDef.value()[1].trim().isEmpty()) {
          providedMessage = pacts.get(pactDef.value()[1].trim());
        } else if (!pacts.isEmpty()) {
          providedMessage = pacts.values().iterator().next();
        }

				if (providedMessage == null) {
					base.evaluate();
					return;
				}

				setMessage(providedMessage, description);
				try {
					base.evaluate();
					messagePact.write(PactConsumerConfig.INSTANCE.getPactDirectory(), PactSpecVersion.V3);
				} catch (Throwable t) {
					throw t;
				}
			}
		};
	}

	private void evaluatePactVerifications(PactVerifications pactVerifications, Statement base, Description description)
			throws Throwable {

		if (provider == null) {
			throw new UnsupportedOperationException("This provider name cannot be null when using @PactVerifications");
		}

		Optional<PactVerification> possiblePactVerification = findPactVerification(pactVerifications);
		if (!possiblePactVerification.isPresent()) {
			base.evaluate();
			return;
		}

		PactVerification pactVerification = possiblePactVerification.get();
		Optional<Method> possiblePactMethod = findPactMethod(pactVerification);
		if (!possiblePactMethod.isPresent()) {
			throw new UnsupportedOperationException("Could not find method with @Pact for the provider " + provider);
		}

		Method method = possiblePactMethod.get();
		Pact pact = method.getAnnotation(Pact.class);
		MessagePactBuilder builder = MessagePactBuilder.consumer(pact.consumer()).hasPactWith(provider);
		MessagePact messagePact = (MessagePact) method.invoke(testClassInstance, builder);
		setMessage(messagePact.getMessages().get(0), description);
		base.evaluate();
		messagePact.write(PactConsumerConfig.INSTANCE.getPactDirectory(), PactSpecVersion.V3);
	}

	private Optional<PactVerification> findPactVerification(PactVerifications pactVerifications) {
		PactVerification[] pactVerificationValues = pactVerifications.value();
		return Arrays.stream(pactVerificationValues).filter(p -> {
			String[] providers = p.value();
			if (providers.length != 1) {
				throw new IllegalArgumentException(
						"Each @PactVerification must specify one and only provider when using @PactVerifications");
			}
			String provider = providers[0];
			return provider.equals(this.provider);
		}).findFirst();
	}

	private Optional<Method> findPactMethod(PactVerification pactVerification) {
		String pactFragment = pactVerification.fragment();
		for (Method method : testClassInstance.getClass().getMethods()) {
			Pact pact = method.getAnnotation(Pact.class);
			if (pact != null && pact.provider().equals(provider)
					&& (pactFragment.isEmpty() || pactFragment.equals(method.getName()))) {
				JUnitTestSupport.conformsToMessagePactSignature(method);
				return Optional.of(method);
			}
		}
		return Optional.empty();
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
                " does not conform required method signature 'public MessagePact xxx(MessagePactBuilder builder)'");
        }
        return conforms;
    }

	public byte[] getMessage() {
		if (message == null) {
			throw new UnsupportedOperationException("Message was not created and cannot be retrieved." +
								" Check @Pact and @PactVerification match.");
		}
		return message;
	}

	public Map<String, String> getMetadata() {
		if (metadata == null) {
			throw new UnsupportedOperationException("Message metadata was not created and cannot be retrieved." +
								" Check @Pact and @PactVerification match.");
		}
		return metadata;
	}

	private void setMessage(Message message, Description description)
			throws InvocationTargetException, IllegalAccessException {

		this.message = message.contentsAsBytes();
		this.metadata = message.getMetaData();
		Method messageSetter;
		try {
			messageSetter = description.getTestClass().getMethod("setMessage", byte[].class);
		} catch (Exception e) {
			//ignore
			return;
		}
		messageSetter.invoke(testClassInstance, message.contentsAsBytes());
	}
}

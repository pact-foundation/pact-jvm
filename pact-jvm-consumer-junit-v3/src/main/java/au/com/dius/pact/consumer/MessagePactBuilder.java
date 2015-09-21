package au.com.dius.pact.consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.entity.ContentType;

import au.com.dius.pact.model.Consumer;
import au.com.dius.pact.model.Provider;
import au.com.dius.pact.model.v3.messaging.Message;
import au.com.dius.pact.model.v3.messaging.MessagePact;

/**
 * 
 * PACT DSL builder for v3 specification
 *
 */
public class MessagePactBuilder {
	/**
	 * The consumer for the pact.
	 */
	private Consumer consumer;
	
	/**
	 * The producer for the pact.
	 */
	private Provider provider;
	
	/**
	 * Producer state
	 */
	private String providerState;
	
	/**
	 * Messages for the pact
	 */
	private List<Message> messages;

	/**
	 * Creates a new instance of {@link MessagePactBuilder}
	 * @param consumer
	 */
	private MessagePactBuilder(String consumer) {
		this.consumer = new Consumer(consumer);
	}
	
    /**
     * Name the consumer of the pact
     * @param consumer Consumer name
     */
    public static MessagePactBuilder consumer(String consumer) {
        return new MessagePactBuilder(consumer);
    }

    /**
     * Name the provider that the consumer has a pact with.
     * 
     * @param provider provider name
     * @return this builder.
     */
    public MessagePactBuilder hasPactWith(String provider) {
    	this.provider = new Provider(provider);
    	return this;
    }

    /**
     * Sets the provider state.
     * 
     * @param providerState state of the provider
     * @return this builder.
     */
    public MessagePactBuilder given(String providerState) {
    	this.providerState = providerState;
    	return this;
    }
    
    /**
     * Adds a message expectation in the pact.
     * 
     * @param description message description.
     */
    public MessagePactBuilder expectsToReceive(String description) {
    	Message message = new Message(description, providerState);
    	if (messages == null) {
    		messages = new ArrayList<Message>();
    	}
    	
    	messages.add(message);
    	
    	return this;
    }
    
    /**
     * 
     */
    public MessagePactBuilder withMetadata(Map<String, String> metadata) {
    	if (messages == null || messages.isEmpty()) {
    		throw new InvalidPactException("expectsToReceive is required before withMetaData");
    	}
    	
    	messages.get(messages.size()-1).setMetaData(metadata);
    	return this;
    }

    public MessagePactBuilder withContent(PactDslJsonBody body) {
    	if (messages == null || messages.isEmpty()) {
    		throw new InvalidPactException("expectsToReceive is required before withMetaData");
    	}
    	
    	Message message = messages.get(messages.size()-1);
    	@SuppressWarnings("unchecked")
		Map<String, String> metadata = message.getMetaData();
    	if (metadata == null) {
    		metadata = new HashMap<String, String>(1);
    		metadata.put("Content-Type", ContentType.APPLICATION_JSON.toString());
    	} else if (!metadata.containsKey("Content-Type")) {
    		metadata.put("Content-Type", ContentType.APPLICATION_JSON.toString());
    	}
    	
    	message.setContents(body.getBody());
    	message.setMatchingRules(body.getMatchers());
    	
    	return this;
    }	
    
    public MessagePact toPact() {
    	MessagePact pact = new MessagePact();
    	pact.setConsumer(consumer);
    	pact.setProvider(provider);
    	pact.setMessages(messages);
    	
    	return pact;
    }
}

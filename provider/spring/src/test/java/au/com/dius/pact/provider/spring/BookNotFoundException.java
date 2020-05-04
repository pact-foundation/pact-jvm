package au.com.dius.pact.provider.spring;

import java.util.UUID;

/**
 * Created by raceconditions on 5/17/17.
 */
public class BookNotFoundException extends Exception {
    public BookNotFoundException(UUID id) {
        super(String.format("No book with ID: %s", id.toString()));
    }
}

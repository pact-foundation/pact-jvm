package au.com.dius.pact.provider.spring;

import java.util.UUID;

/**
 * Created by raceconditions on 5/17/17.
 */
public class BookValidationException extends Exception {
    public BookValidationException(Book book) {
        super(String.format("Could not create book with ID: %s", book.getId().toString()));
    }
}

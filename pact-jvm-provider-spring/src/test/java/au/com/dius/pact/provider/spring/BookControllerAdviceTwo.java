package au.com.dius.pact.provider.spring;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class BookControllerAdviceTwo {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BookValidationException.class)
    public void handle(BookValidationException ex) {
        System.err.println("Caught BookValidationException, mapping to HTTP 400.");
    }
}

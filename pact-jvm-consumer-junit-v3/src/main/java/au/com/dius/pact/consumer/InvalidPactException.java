/**
 * 
 */
package au.com.dius.pact.consumer;

/**
 * Exception class to indicate an invalid pact specification
 *
 */
public class InvalidPactException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2976572483298516932L;

	/**
	 * Create a new instance of {@link InvalidPactException}.
	 * 
	 * @param message the exception message
	 */
	public InvalidPactException(String message) {
		super(message);
	}
}

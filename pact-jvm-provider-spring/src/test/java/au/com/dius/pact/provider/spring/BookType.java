package au.com.dius.pact.provider.spring;

public class BookType {

  private final String type;

  public BookType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}

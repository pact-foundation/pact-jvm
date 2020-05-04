package au.com.dius.pact.provider.spring;

import au.com.dius.pact.provider.junitsupport.State;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class BooksPactProviderStates {

  private final BookLogic bookLogic;
  private final DateTime dateTime;

  public BooksPactProviderStates(BookLogic bookLogic, DateTime dateTime) {
    this.bookLogic = bookLogic;
    this.dateTime = dateTime;
  }

  @State("book-exists")
  public void bookFound() {
    when(bookLogic.getBookById(any(UUID.class)))
      .thenReturn(new Book(UUID.randomUUID(), "Nick Hoftsettler", true, dateTime));
  }

  @State("book-not-found")
  public void bookNotFound() {
    when(bookLogic.getBookById(any(UUID.class)))
      .then(i -> { throw new BookNotFoundException((UUID) i.getArguments()[0]); });
  }

  @State("create-book")
  public void createBook() {
    // no setup needed
  }

  @State("create-book-bad-data")
  public void createBookBadData() {
    when(bookLogic.createBook(any(Book.class)))
      .then(i -> { throw new BookValidationException((Book) i.getArguments()[0]); });
  }

  @State("update-book")
  public void updateBook() {
    // no setup needed
  }

  @State("delete-book")
  public void deleteBook() {
    // no setup needed
  }

  @State("update-book-no-content-type")
  public void updateBookNoContentType() {
    // no setup needed
  }

  @State("get-books")
  public void getAllBooks() {

    List<Book> bookList = new ArrayList<Book>();

    bookList.add(new Book(UUID.randomUUID(), "Bob Jones", true, dateTime));
    bookList.add(new Book(UUID.randomUUID(), "Jerry Duff", false, dateTime.plusDays(1)));
    bookList.add(new Book(UUID.randomUUID(), "Eric Reynolds", true, dateTime.plusDays(2)));

    when(bookLogic.getBooks())
      .thenReturn(bookList);
  }

  @State("get-best-selling-books")
  public void getBestSellingBooks() {

    List<Book> bookList = new ArrayList<Book>();

    bookList.add(new Book(UUID.randomUUID(), "Bob Jones", true, dateTime));
    bookList.add(new Book(UUID.randomUUID(), "Eric Reynolds", true, dateTime.plusDays(1)));

    when(bookLogic.getBooks(true))
      .thenReturn(bookList);
  }

}

package au.com.dius.pact.provider.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
public class BookController {
    @Autowired
    BookLogic bookLogic;

    @RequestMapping(value = "/books", method = RequestMethod.POST)
    ResponseEntity create(@RequestBody Book book) throws Exception {
        bookLogic.createBook(book);
        return new ResponseEntity(HttpStatus.CREATED);
    }

    @RequestMapping(value = "/books/{id}", method = RequestMethod.PUT)
    ResponseEntity updateById(@RequestBody Book book, @PathVariable UUID id) throws Exception {
        bookLogic.updateBook(book);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/books/{id}", method = RequestMethod.DELETE)
    ResponseEntity<Book> deleteByID(@PathVariable UUID id) throws Exception {
        bookLogic.deleteById(id);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/books/{id}", method = RequestMethod.GET)
    ResponseEntity<Book> getByID(@PathVariable UUID id) throws Exception {
        return new ResponseEntity(bookLogic.getBookById(id), HttpStatus.OK);
    }

    @RequestMapping(value = {"/books"}, method = RequestMethod.GET)
    ResponseEntity<List<Book>> getAll(@RequestParam(value = "bestSeller", required = false) Boolean bestSeller) throws Exception {
        if(bestSeller == null)
            return new ResponseEntity(bookLogic.getBooks(), HttpStatus.OK);
        else {
            return new ResponseEntity(bookLogic.getBooks(bestSeller), HttpStatus.OK);
        }
    }

    @RequestMapping(value = {"/books"}, params = "type", method = RequestMethod.GET)
    ResponseEntity<List<Book>> getAllForType(BookType bookType) throws Exception {
        return new ResponseEntity(bookLogic.getBooks(bookType), HttpStatus.OK);
    }
}

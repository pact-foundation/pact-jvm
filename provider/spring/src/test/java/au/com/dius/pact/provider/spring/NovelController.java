package au.com.dius.pact.provider.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
public class NovelController {
    @Autowired
    BookLogic bookLogic;

    @RequestMapping(value = "/novels/{id}", method = RequestMethod.GET)
    ResponseEntity<Book> getByID(@PathVariable UUID id) throws Exception {
        return new ResponseEntity(bookLogic.getBookById(id), HttpStatus.OK);
    }
}

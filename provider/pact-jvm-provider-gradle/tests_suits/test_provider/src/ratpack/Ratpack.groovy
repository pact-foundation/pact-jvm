import ratpack.groovy.template.MarkupTemplateModule

import static ratpack.groovy.Groovy.groovyMarkupTemplate
import static ratpack.groovy.Groovy.ratpack
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ratpack.handling.RequestLogger

final Logger logger = LoggerFactory.getLogger(Ratpack.class)

ratpack {
  bindings {
    module MarkupTemplateModule
  }

  handlers {
    all RequestLogger.ncsa(logger)

    get {
      render groovyMarkupTemplate("index.gtpl", title: "My Ratpack App")
    }

    get('nullfields') {
      response.contentType('application/json; charset=UTF-8')
      response.status(201)
      response.headers.add('HEADER-X', 'Y')
      render('''
        [
            {
                doesNotExist: "Test", 
                "documentId": 0,
                "documentCategoryId": 5,
                "documentCategoryCode": null,
                "contentLength": 0,
                "tags": null
            },
            {
                doesNotExist: "Test",
                "documentId": 1,
                "documentCategoryId": 5,
                "documentCategoryCode": null,
                "contentLength": 0,
                "tags": null
            }
        ]
      ''')
    }

    files { dir "public" }
  }
}

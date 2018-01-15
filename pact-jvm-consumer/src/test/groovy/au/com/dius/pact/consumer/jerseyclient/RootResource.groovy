package au.com.dius.pact.consumer.jerseyclient

import groovy.transform.Canonical

import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Canonical
class BeanIn {
  boolean test
}

@Canonical
class BeanOut {
  boolean result
}

@Path('/hello')
interface RootResource {

  @POST
  @Produces('application/json')
  @Consumes('application/json')
  BeanOut getTest(BeanIn bean)

}

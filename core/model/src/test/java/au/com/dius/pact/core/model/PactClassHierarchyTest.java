package au.com.dius.pact.core.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class PactClassHierarchyTest {

  @Test
  public void simpleTraitsBoundsTest() {
    Provider provider = new Provider();
    Consumer consumer = new Consumer();
    RequestResponsePact pactA = new RequestResponsePact(provider, consumer, new ArrayList<>());
    RequestResponsePact pactB = new RequestResponsePact(provider, consumer,
      Collections.singletonList(new RequestResponseInteraction("Test Merge Interaction",
        new Request(), new Response())));
    pactA.mergeInteractions(pactB.getInteractions());

    assertThat(pactA.getInteractions(), hasSize(1));
  }

}

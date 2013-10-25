The ideals behind pact are too valuable to leave to a single language

This project aims to make the ideals available to microservices built with scala, clojure, groovy and other JVM languages (even java if you're that way inclined)

The hope is to maximise compatibility, however where ruby specific features are coupled with a pact definition, a more generic alternitive will be found.

It is not acceptible to have native dependencies in this project.


Example of running a tests against the consumer (what we are aiming for):

    class ConsumerSpec
      pact_with 'alligator_service.pact'

      it 'should get alligators' with 'provider state' do
        MakeTheServiceCall.call('blah').should == 'Bob'
      end

    end

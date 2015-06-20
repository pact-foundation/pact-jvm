
Pact server
===========

The pact server is a stand-alone interactions recorder and verifier, aimed at clients that are non-JVM or non-Ruby based.

The pact client for that platform will need to be implemented, but it only be responsible for generating the `JSON`
interactions, running the tests and communicating with the server.

The server implements a `JSON` `REST` Admin API with the following endpoints.

    /         -> For diagnostics, currently returns a list of ports of the running mock servers.
    /create   -> For initialising a test server and submitting the JSON interactions. It returns a port
    /complete -> For finalising and verifying the interactions with the server.  It writes the `JSON` pact file to disk.

## Running the server

You can download a [distribution from maven central](http://search.maven.org/remotecontent?filepath=au/com/dius/pact-jvm-server_2.11/2.2.4/).
There is both a ZIP and TAR archive. Unpack it to a directory of choice and then run the script in the bin directory.

You can build an application bundle with gradle by running (for 2.11 version):

    $ ./gradlew :pact-jvm-server_2.11:installdist

This will create an app bundle in `build/2.11/install/pact-jvm-server_2.11`. You can then execute it with:

    $ java -jar pact-jvm-server/build/2.10/install/pact-jvm-server_2.11/lib/pact-jvm-server_2.11-2.2.4.jar

or with the generated bundle script file:

    $ pact-jvm-server/build/2.11/install/pact-jvm-server_2.11/bin/pact-jvm-server_2.11

By default will run on port `29999` but a port number can be optionally supplied.

## Life cycle

The following actions are expected to occur

 * The client calls `/create` to initialise a server with the expected `JSON` interactions and state
 * The admin server will start a mock server on a random port and return the port number in the response
 * The client will execute its interaction tests against the mock server with the supplied port
 * Once finished, the client will call `/complete' on the Admin API, posting the port number
 * The pact server will verify the interactions and write the `JSON` `pact` file to disk under `/target`
 * The mock server running on the supplied port will be shutdown.

## Endpoints

### /create

The client will need `POST` to `/create` the generated `JSON` interactions, also providing a state as a query parameter.

For example:

    POST http://localhost:29999/create?state=NoUsers '{ "provider": { "name": "Animal_Service"}, ... }'

This will create a new running mock service provider on a randomly generated port.  The port will be returned in the
`201` response:

    { "port" : 34423 }

### /complete

Once the client has finished running its tests against the mock server on the supplied port (in this example port
`34423`) the client will need to `POST` to `/complete` the port number of the mock server that was used.

For example:

    POST http://localhost:29999/complete '{ "port" : 34423 }'

This will cause the Pact server to verify the interactions, shutdown the mock server running on that port and writing
the pact `JSON` file to disk under the `target` directory.

### /

The `/` endpoint is for diagnostics and to check that the pact server is running.  It will return all the currently
running mock servers port numbers.

For example:

    GET http://localhost:29999/

        '{ "ports": [23443,43232] }'

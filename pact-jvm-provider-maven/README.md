Maven plugin to verify a provider
=================================

Maven plugin for verifying pacts against a provider.

The Maven plugin provides a `verify` goal which will verify all configured pacts against your provider.

## To Use It

### 1. Add the pact-jvm-provider-maven plugin to your `build` section of your pom file.

```xml
<build>
    [...]
    <plugins>
      [...]
      <plugin>
        <groupId>au.com.dius</groupId>
        <artifactId>pact-jvm-provider-maven_2.12</artifactId>
        <version>3.5.11</version>
      </plugin>
      [...]
    </plugins>
    [...]
  </build>
```

### 2. Define the pacts between your consumers and providers

You define all the providers and consumers within the configuration element of the maven plugin.

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <!-- You can define as many as you need, but each must have a unique name -->
        <serviceProvider>
          <name>provider1</name>
          <!-- All the provider properties are optional, and have sensible defaults (shown below) -->
          <protocol>http</protocol>
          <host>localhost</host>
          <port>8080</port>
          <path>/</path>
          <consumers>
            <!-- Again, you can define as many consumers for each provider as you need, but each must have a unique name -->
            <consumer>
              <name>consumer1</name>
              <!--  currently supports a file path using pactFile or a URL using pactUrl -->
              <pactFile>path/to/provider1-consumer1-pact.json</pactFile>
            </consumer>
          </consumers>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

### 3. Execute `mvn pact:verify`

You will have to have your provider running for this to pass.

## Verifying all pact files in a directory for a provider

You can specify a directory that contains pact files, and the Pact plugin will scan for all pact files that match that
provider and define a consumer for each pact file in the directory. Consumer name is read from contents of pact file.

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <!-- You can define as many as you need, but each must have a unique name -->
        <serviceProvider>
          <name>provider1</name>
          <!-- All the provider properties are optional, and have sensible defaults (shown below) -->
          <protocol>http</protocol>
          <host>localhost</host>
          <port>8080</port>
          <path>/</path>
          <pactFileDirectory>path/to/pacts</pactFileDirectory>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

### Verifying all pact files from multiple directories for a provider [3.5.18+]

If you want to specify multiple directories, you can use `pactFileDirectories`. The plugin will only fail the build if
no pact files are loaded after processing all the directories in the list.

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.18</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>provider1</name>
          <pactFileDirectories>
            <pactFileDirectory>path/to/pacts1</pactFileDirectory>
            <pactFileDirectory>path/to/pacts2</pactFileDirectory>
          </pactFileDirectories>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

## Enabling insecure SSL

For providers that are running on SSL with self-signed certificates, you need to enable insecure SSL mode by setting
`<insecure>true</insecure>` on the provider.

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>provider1</name>
          <pactFileDirectory>path/to/pacts</pactFileDirectory>
          <insecure>true</insecure>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

## Specifying a custom trust store

For environments that are running their own certificate chains:

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>provider1</name>
          <pactFileDirectory>path/to/pacts</pactFileDirectory>
          <trustStore>relative/path/to/trustStore.jks</trustStore>
          <trustStorePassword>changeit</trustStorePassword>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

`trustStore` is either relative to the current working (build) directory. `trustStorePassword` defaults to `changeit`.

NOTE: The hostname will still be verified against the certificate.

## Modifying the requests before they are sent

Sometimes you may need to add things to the requests that can't be persisted in a pact file. Examples of these would
be authentication tokens, which have a small life span. The Pact Maven plugin provides a request filter that can be
set to a Groovy script on the provider that will be called before the request is made. This script will receive the HttpRequest
bound to a variable named `request` prior to it being executed.

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>provider1</name>
          <requestFilter>
            // This is a Groovy script that adds an Authorization header to each request
            request.addHeader('Authorization', 'oauth-token eyJhbGciOiJSUzI1NiIsIm...')
          </requestFilter>
          <consumers>
            <consumer>
              <name>consumer1</name>
              <pactFile>path/to/provider1-consumer1-pact.json</pactFile>
            </consumer>
          </consumers>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

__*Important Note:*__ You should only use this feature for things that can not be persisted in the pact file. By modifying
the request, you are potentially modifying the contract from the consumer tests!

## Modifying the HTTP Client Used

The default HTTP client is used for all requests to providers (created with a call to `HttpClients.createDefault()`).
This can be changed by specifying a closure assigned to createClient on the provider that returns a CloseableHttpClient.
For example:

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>provider1</name>
          <createClient>
            // This is a Groovy script that will enable the client to accept self-signed certificates
            import org.apache.http.ssl.SSLContextBuilder
            import org.apache.http.conn.ssl.NoopHostnameVerifier
            import org.apache.http.impl.client.HttpClients
            HttpClients.custom().setSSLHostnameVerifier(new NoopHostnameVerifier())
                .setSslcontext(new SSLContextBuilder().loadTrustMaterial(null, { x509Certificates, s -> true })
                    .build())
            .build()
          </createClient>
          <consumers>
            <consumer>
              <name>consumer1</name>
              <pactFile>path/to/provider1-consumer1-pact.json</pactFile>
            </consumer>
          </consumers>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

## Turning off URL decoding of the paths in the pact file

By default the paths loaded from the pact file will be decoded before the request is sent to the provider. To turn this
behaviour off, set the system property `pact.verifier.disableUrlPathDecoding` to `true`.

__*Important Note:*__ If you turn off the url path decoding, you need to ensure that the paths in the pact files are 
correctly encoded. The verifier will not be able to make a request with an invalid encoded path.

## Plugin Properties

The following plugin properties can be specified with `-Dproperty=value` on the command line or in the configuration section:

|Property|Description|
|--------|-----------|
|pact.showStacktrace|This turns on stacktrace printing for each request. It can help with diagnosing network errors|
|pact.showFullDiff|This turns on displaying the full diff of the expected versus actual bodies|
|pact.filter.consumers|Comma separated list of consumer names to verify|
|pact.filter.description|Only verify interactions whose description match the provided regular expression|
|pact.filter.providerState|Only verify interactions whose provider state match the provided regular expression. An empty string matches interactions that have no state|
|pact.verifier.publishResults|Publishing of verification results will be skipped unless this property is set to `true` [version 3.5.18+]|
|pact.matching.wildcard|Enables matching of map values ignoring the keys when this property is set to `true`|
|pact.verifier.disableUrlPathDecoding|Disables decoding of request paths|
|pact.pactbroker.httpclient.usePreemptiveAuthentication|Enables preemptive authentication with the pact broker when set to `true`|

Example in the configuration section:

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>provider1</name>
          <consumers>
            <consumer>
              <name>consumer1</name>
              <pactFile>path/to/provider1-consumer1-pact.json</pactFile>
            </consumer>
          </consumers>
        </serviceProvider>
      </serviceProviders>
      <configuration>
        <pact.showStacktrace>true</pact.showStacktrace>
      </configuration>
    </configuration>
</plugin>
```

## Provider States

For each provider you can specify a state change URL to use to switch the state of the provider. This URL will
receive the providerState description and parameters from the pact file before each interaction via a POST. The stateChangeUsesBody
controls if the state is passed in the request body or as query parameters.

These values can be set at the provider level, or for a specific consumer. Consumer values take precedent if both are given.

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>provider1</name>
          <stateChangeUrl>http://localhost:8080/tasks/pactStateChange</stateChangeUrl>
          <stateChangeUsesBody>false</stateChangeUsesBody> <!-- defaults to true -->
          <consumers>
            <consumer>
              <name>consumer1</name>
              <pactFile>path/to/provider1-consumer1-pact.json</pactFile>
              <stateChangeUrl>http://localhost:8080/tasks/pactStateChangeForConsumer1</stateChangeUrl>
              <stateChangeUsesBody>false</stateChangeUsesBody> <!-- defaults to true -->
            </consumer>
          </consumers>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

If the `stateChangeUsesBody` is not specified, or is set to true, then the provider state description and parameters will be sent as
 JSON in the body of the request. If it is set to false, they will passed as query parameters.

As for normal requests (see Modifying the requests before they are sent), a state change request can be modified before
it is sent. Set `stateChangeRequestFilter` to a Groovy script on the provider that will be called before the request is made.

#### Teardown calls for state changes

You can enable teardown state change calls by setting the property `<stateChangeTeardown>true</stateChangeTeardown>` on the provider. This
will add an `action` parameter to the state change call. The setup call before the test will receive `action=setup`, and
then a teardown call will be made afterwards to the state change URL with `action=teardown`.

## Verifying pact files from a pact broker

You can setup your build to validate against the pacts stored in a pact broker. The pact plugin will query
the pact broker for all consumers that have a pact with the provider based on its name. To use it, just configure the
`pactBrokerUrl` or `pactBroker` value for the provider with the base URL to the pact broker.

For example:

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>provider1</name>
          <stateChangeUrl>http://localhost:8080/tasks/pactStateChange</stateChangeUrl>
          <pactBrokerUrl>http://pact-broker:5000/</pactBrokerUrl>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

### Verifying pacts from an authenticated pact broker

If your pact broker requires authentication (basic authentication is only supported), you can configure the username
and password to use by configuring the `authentication` element of the `pactBroker` element of your provider.

For example:

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>provider1</name>
          <stateChangeUrl>http://localhost:8080/tasks/pactStateChange</stateChangeUrl>
          <pactBroker>
              <url>http://pactbroker:1234</url>
              <authentication>
                  <username>test</username>
                  <password>test</password>
              </authentication>
          </pactBroker>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

Preemptive Authentication can be enabled by setting the `pact.pactbroker.httpclient.usePreemptiveAuthentication` Java
system property to `true`.

#### Using the Maven servers configuration [version 3.5.6+]

From version 3.5.6, you can use the servers setup in the Maven settings. To do this, setup a server as per the 
[Maven Server Settings](https://maven.apache.org/settings.html#Servers). Then set the server ID in the pact broker
configuration in your POM.
 
```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.6</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>provider1</name>
          <stateChangeUrl>http://localhost:8080/tasks/pactStateChange</stateChangeUrl>
          <pactBroker>
              <url>http://pactbroker:1234</url>
              <serverId>test-pact-broker</serverId> <!-- This must match the server id in the maven settings -->
          </pactBroker>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

### Verifying pacts from an pact broker that match particular tags

If your pacts in your pact broker have been tagged, you can set the tags to fetch by configuring the `tags` 
element of the `pactBroker` element of your provider.

For example:

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>provider1</name>
          <stateChangeUrl>http://localhost:8080/tasks/pactStateChange</stateChangeUrl>
          <pactBroker>
              <url>http://pactbroker:1234</url>
              <tags>
                  <tag>TEST</tag>
                  <tag>DEV</tag>
              </tags>
          </pactBroker>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

This example will fetch and validate the pacts for the TEST and DEV tags.

## Filtering the interactions that are verified

You can filter the interactions that are run using three properties: `pact.filter.consumers`, `pact.filter.description` and `pact.filter.providerState`.
Adding `-Dpact.filter.consumers=consumer1,consumer2` to the command line or configuration section will only run the pact files for those
consumers (consumer1 and consumer2). Adding `-Dpact.filter.description=a request for payment.*` will only run those interactions
whose descriptions start with 'a request for payment'. `-Dpact.filter.providerState=.*payment` will match any interaction that
has a provider state that ends with payment, and `-Dpact.filter.providerState=` will match any interaction that does not have a
provider state.

## Not failing the build if no pact files are found [version 3.5.19+]

By default, if there are no pact files to verify, the plugin will raise an exception. This is to guard against false
positives where the build is passing but nothing has been verified due to mis-configuration.

To disable this behaviour, set the `failIfNoPactsFound` parameter to `false`.

# Verifying a message provider

The Maven plugin has been updated to allow invoking test methods that can return the message contents from a message
producer. To use it, set the way to invoke the verification to `ANNOTATED_METHOD`. This will allow the pact verification
 task to scan for test methods that return the message contents.

Add something like the following to your maven pom file:

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>messageProvider</name>
          <verificationType>ANNOTATED_METHOD</verificationType>
          <!-- packagesToScan is optional, but leaving it out will result in the entire
          test classpath being scanned. Set it to the packages where your annotated test method
          can be found. -->
          <packagesToScan>
              <packageToScan>au.com.example.messageprovider.*</packageToScan>
          </packagesToScan>
          <consumers>
            <consumer>
              <name>consumer1</name>
              <pactFile>path/to/messageprovider-consumer1-pact.json</pactFile>
            </consumer>
          </consumers>
        </serviceProvider>
      </serviceProviders>
    </configuration>
</plugin>
```

Now when the pact verify task is run, will look for methods annotated with `@PactVerifyProvider` in the test classpath
that have a matching description to what is in the pact file.

```groovy
class ConfirmationKafkaMessageBuilderTest {

  @PactVerifyProvider('an order confirmation message')
  String verifyMessageForOrder() {
      Order order = new Order()
      order.setId(10000004)
      order.setExchange('ASX')
      order.setSecurityCode('CBA')
      order.setPrice(BigDecimal.TEN)
      order.setUnits(15)
      order.setGst(new BigDecimal('15.0'))
      odrer.setFees(BigDecimal.TEN)

      def message = new ConfirmationKafkaMessageBuilder()
              .withOrder(order)
              .build()

      JsonOutput.toJson(message)
  }

}
```

It will then validate that the returned contents matches the contents for the message in the pact file.

## Changing the class path that is scanned

By default, the test classpath is scanned for annotated methods. You can override this by setting
 the `classpathElements` property:

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <serviceProviders>
        <serviceProvider>
          <name>messageProvider</name>
          <verificationType>ANNOTATED_METHOD</verificationType>
          <consumers>
            <consumer>
              <name>consumer1</name>
              <pactFile>path/to/messageprovider-consumer1-pact.json</pactFile>
            </consumer>
          </consumers>
        </serviceProvider>
      </serviceProviders>
      <classpathElements>
          <classpathElement>
              build/classes/test
          </classpathElement>
      </classpathElements>
    </configuration>
</plugin>
```

# Publishing pact files to a pact broker

The pact maven plugin provides a `publish` mojo that can publish all pact files in a directory
to a pact broker. To use it, you need to add a publish configuration to the POM that defines the
directory where the pact files are and the URL to the pact broker.

For example:

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <pactDirectory>path/to/pact/files</pactDirectory> <!-- Defaults to ${project.build.directory}/pacts -->
      <pactBrokerUrl>http://pactbroker:1234</pactBrokerUrl>
      <projectVersion>1.0.100</projectVersion> <!-- Defaults to ${project.version} -->
      <trimSnapshot>true</trimSnapshot> <!-- Defaults to false -->
    </configuration>
</plugin>
```
You can now execute `mvn pact:publish` to publish the pact files.

_NOTE:_ The pact broker requires a version for all published pacts. The `publish` task will use the version of the
project by default, but can be overwritten with the `projectVersion` property. Make sure you have set one otherwise the broker will reject the pact files.

_NOTE_: By default, the pact broker has issues parsing `SNAPSHOT` versions.  You can configure the publisher to 
automatically remove `-SNAPSHOT` from your version number by setting `trimSnapshot` to true. This setting does not modify non-snapshot versions.

You can set any tags that the pacts should be published with by setting the `tags` list property (version 3.5.12+). A common use of this
is setting the tag to the current source control branch. This supports using pact with feature branches.

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.12</version>
    <configuration>
      <pactDirectory>path/to/pact/files</pactDirectory> <!-- Defaults to ${project.build.directory}/pacts -->
      <pactBrokerUrl>http://pactbroker:1234</pactBrokerUrl>
      <projectVersion>1.0.100</projectVersion> <!-- Defaults to ${project.version} -->
      <tags>
        <tag>feature/feature_name</tag>
      </tags>
    </configuration>
</plugin>
```

## Publishing to an authenticated pact broker

For an authenticated pact broker, you can pass in the credentials with the `pactBrokerUsername` and `pactBrokerPassword`
properties. Currently it only supports basic authentication.

For example:

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <pactBrokerUrl>http://pactbroker:1234</pactBrokerUrl>
      <pactBrokerUsername>USERNAME</pactBrokerUsername>
      <pactBrokerPassword>PASSWORD</pactBrokerPassword>
    </configuration>
</plugin>
```

Or to use a bearer token:

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.11</version>
    <configuration>
      <pactBrokerUrl>http://pactbroker:1234</pactBrokerUrl>
      <pactBrokerToken>TOKEN</pactBrokerToken> <!-- Replace TOKEN with the actual token -->
      <pactBrokerAuthenticationScheme>Bearer</pactBrokerAuthenticationScheme>
    </configuration>
</plugin>
```

#### Using the Maven servers configuration [version 3.5.6+]

From version 3.5.6, you can use the servers setup in the Maven settings. To do this, setup a server as per the 
[Maven Server Settings](https://maven.apache.org/settings.html#Servers). Then set the server ID in the pact broker
configuration in your POM.

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.11</artifactId>
    <version>3.5.19</version>
    <configuration>
      <pactBrokerUrl>http://pactbroker:1234</pactBrokerUrl>
      <pactBrokerServerId>test-pact-broker</pactBrokerServerId>  <!-- This must match the server id in the maven settings -->
    </configuration>
</plugin>
```

## Excluding pacts from being published [version 3.5.19+]

You can exclude some of the pact files from being published by providing a list of regular expressions that match
against the base names of the pact files.

For example:

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.19</version>
    <configuration>
      <pactBrokerUrl>http://pactbroker:1234</pactBrokerUrl>
      <excludes>
        <exclude>.*\\-\\d+$</exclude> <!-- exclude pact files where the name ends in a dash followed by a number -->
      </excludes>
    </configuration>
</plugin>
```

# Publishing verification results to a Pact Broker [version 3.5.4+]

For pacts that are loaded from a Pact Broker, the results of running the verification can be published back to the
 broker against the URL for the pact. You will be able to then see the result on the Pact Broker home screen.
 
To turn on the verification publishing, set the system property `pact.verifier.publishResults` to `true` in the pact maven plugin, not surefire, configuration.

# Enabling other verification reports [version 3.5.20+]

By default the verification report is written to the console. You can also enable a JSON or Markdown report by setting
the `reports` configuration list.

```xml
<plugin>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider-maven_2.12</artifactId>
    <version>3.5.20</version>
    <configuration>
      <reports>
          <report>console</report>
          <report>json</report>
          <report>markdown</report>
      </reports>
    </configuration>
</plugin>
```

These reports will be written to `target/reports/pact`.

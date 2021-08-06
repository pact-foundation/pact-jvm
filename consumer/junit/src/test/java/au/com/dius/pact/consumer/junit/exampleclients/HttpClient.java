package au.com.dius.pact.consumer.junit.exampleclients;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class HttpClient {
  static CloseableHttpClient insecureHttpClient() {
    SSLContext sslContext = null;
    try {
      sslContext = SSLContexts.custom().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
    } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
      throw new RuntimeException(e);
    }
    SSLConnectionSocketFactory socketFactory = SSLConnectionSocketFactoryBuilder.create()
        .setSslContext(sslContext).build();
      CloseableHttpClient httpClient = HttpClientBuilder.create()
        .setConnectionManager(
          new BasicHttpClientConnectionManager(
            RegistryBuilder.<ConnectionSocketFactory>create()
              .register("http", PlainConnectionSocketFactory.getSocketFactory())
              .register("https", socketFactory)
              .build()
          )
        )
        .build();
      return httpClient;
  }
}

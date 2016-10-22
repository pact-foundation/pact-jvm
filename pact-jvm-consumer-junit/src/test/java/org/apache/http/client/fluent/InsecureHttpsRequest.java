package org.apache.http.client.fluent;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class InsecureHttpsRequest extends Request {
  private CloseableHttpClient httpclient;

  InsecureHttpsRequest(InternalHttpRequest request) {
    super(request);
  }

  private void setupInsecureSSL() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    HttpClientBuilder b = HttpClientBuilder.create();

    // setup a Trust Strategy that allows all certificates.
    //
    TrustStrategy trustStrategy = new TrustStrategy() {
      @Override
      public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        return true;
      }
    };
    SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build();
    b.setSSLContext(sslContext);
    // don't check Hostnames, either.
    //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
    HostnameVerifier hostnameVerifier = new NoopHostnameVerifier();

    // here's the special part:
    //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
    //      -- and create a Registry, to register it.
    //
    SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
    Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
      .register("http", PlainConnectionSocketFactory.getSocketFactory())
      .register("https", sslSocketFactory)
      .build();

    // now, we create connection-manager using our Registry.
    //      -- allows multi-threaded use
    PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
    b.setConnectionManager(connMgr);

    // finally, build the HttpClient;
    //      -- done!
    this.httpclient = b.build();
  }

  @Override
  public Response execute() throws IOException {
    if (httpclient == null) {
      try {
        setupInsecureSSL();
      } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
        throw new IOException(e);
      }
    }
    return new Response(internalExecute(httpclient, null));
  }

  public static Request options(final URI uri) {
    return new InsecureHttpsRequest(new InternalHttpRequest(HttpOptions.METHOD_NAME, uri));
  }

  public static Request options(final String uri) {
    return new InsecureHttpsRequest(new InternalHttpRequest(HttpOptions.METHOD_NAME, URI.create(uri)));
  }

  public static Request post(final URI uri) {
    return new InsecureHttpsRequest(new InternalEntityEnclosingHttpRequest(HttpPost.METHOD_NAME, uri));
  }

  public static Request post(final String uri) {
    return new InsecureHttpsRequest(new InternalEntityEnclosingHttpRequest(HttpPost.METHOD_NAME, URI.create(uri)));
  }

  public static Request put(final URI uri) {
    return new InsecureHttpsRequest(new InternalEntityEnclosingHttpRequest(HttpPut.METHOD_NAME, uri));
  }

  public static Request put(final String uri) {
    return new InsecureHttpsRequest(new InternalEntityEnclosingHttpRequest(HttpPut.METHOD_NAME, URI.create(uri)));
  }

  public static Request get(final URI uri) {
    return new InsecureHttpsRequest(new InternalHttpRequest(HttpGet.METHOD_NAME, uri));
  }

  public static Request get(final String uri) {
    return new InsecureHttpsRequest(new InternalHttpRequest(HttpGet.METHOD_NAME, URI.create(uri)));
  }
}

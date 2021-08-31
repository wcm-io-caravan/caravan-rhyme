package io.wcm.caravan.ryhme.testing.client;

import static com.google.common.base.Charsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

@Component
public class ApacheHttpClientResourceLoader implements HalResourceLoader {

  private static final Logger log = LoggerFactory.getLogger(ApacheHttpClientResourceLoader.class);

  private static final JsonFactory JSON_FACTORY = new JsonFactory(new ObjectMapper());

  private final Cache<String, Single<HalResponse>> cache = CacheBuilder.newBuilder()
      .expireAfterWrite(60, TimeUnit.SECONDS)
      .build();


  @Override
  public Single<HalResponse> getHalResource(String uri) {

    try {
      return cache.get(uri, () -> {

        Single<HalResponse> single = Single.create(source -> {

          try {
            source.onSuccess(getHalResourceBlocking(uri));
          }
          catch (RuntimeException ex) {
            source.onError(ex);
          }
        });

        return single.cache();
      });
    }
    catch (ExecutionException ex) {
      return Single.error(ex);
    }

  }

  private HalResponse getHalResourceBlocking(String uri) {
    Stopwatch sw = Stopwatch.createStarted();

    CredentialsProvider provider = new BasicCredentialsProvider();
    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "admin");
    provider.setCredentials(AuthScope.ANY, credentials);

    Integer statusCode = null;
    CloseableHttpResponse httpResponse = null;
    try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build()) {

      HttpGet httpRequest = new HttpGet();
      httpRequest.setURI(URI.create(uri));

      httpResponse = client.execute(httpRequest);

      statusCode = httpResponse.getStatusLine().getStatusCode();

      log.info("Received response with status {} from {} with {} latency", statusCode, uri, sw);
      if (statusCode == HttpStatus.SC_OK) {

        ObjectNode responseJson = parseResponseBody(httpResponse);

        HalResponse response = new HalResponse()
            .withContentType(httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue())
            .withStatus(statusCode)
            .withBody(responseJson)
            .withMaxAge(CacheControlUtil.parseMaxAge(httpResponse.getHeaders(HttpHeaders.CACHE_CONTROL)));

        httpResponse.close();

        return response;
      }

      try {
        ObjectNode responseJson = parseResponseBody(httpResponse);

        HalResponse errorResponse = new HalResponse()
            .withContentType(httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue())
            .withStatus(statusCode)
            .withBody(responseJson)
            .withMaxAge(CacheControlUtil.parseMaxAge(httpResponse.getHeaders(HttpHeaders.CACHE_CONTROL)));

        throw new HalApiClientException(errorResponse, uri, null);
      }
      catch (IOException ex) {
        throw new HalApiClientException("HTTP request failed with statusCode " + statusCode + " (and body couldn't be parsed as JSON)", statusCode, uri, null);
      }

    }
    catch (HalApiClientException ex) {
      throw ex;
    }
    catch (JsonProcessingException ex) {
      log.error("Failed to parse JSON resource fetched from {}", uri, ex);
      throw new HalApiClientException("Failed to parse JSON resource", null, uri, ex);
    }
    catch (IOException | RuntimeException ex) {
      log.error("Failed to fetch resource from {}", uri, ex);
      throw new HalApiClientException("Failed to fetch resource from " + uri, statusCode, uri, ex);
    }
    finally {
      if (httpResponse != null) {
        try {
          httpResponse.close();
        }
        catch (IOException ex) {
          log.warn("Failed to close HTTP response", ex);
        }
      }
    }
  }

  private ObjectNode parseResponseBody(CloseableHttpResponse httpResponse) throws IOException, JsonParseException {
    String responseBody = IOUtils.toString(httpResponse.getEntity().getContent(), UTF_8);
    return JSON_FACTORY.createParser(responseBody).readValueAs(ObjectNode.class);
  }

}

package io.wcm.caravan.rhyme.osgi.it.extensions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.SingleSubject;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.JsonResourceLoader;
import io.wcm.caravan.rhyme.osgi.it.TestEnvironmentConstants;

public class HalApiClientExtension implements ParameterResolver {

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(HalApiClient.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {

    ApacheAsyncJsonResourceLoader loader = new ApacheAsyncJsonResourceLoader();

    RequestMetricsCollector metrics = RequestMetricsCollector.create();

    return HalApiClient.create(loader, metrics);
  }

  private static class ApacheAsyncJsonResourceLoader implements JsonResourceLoader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JsonFactory JSON_FACTORY = new JsonFactory(OBJECT_MAPPER);

    private static final CloseableHttpAsyncClient HTTP_CLIENT = HttpAsyncClientBuilder.create().build();

    static {
      HTTP_CLIENT.start();
    }

    @Override
    public Single<HalResponse> loadJsonResource(String uri) {

      SingleSubject<HalResponse> responseSubject = SingleSubject.create();

      HttpGet request = createRequest(uri);
      HTTP_CLIENT.execute(request, new FutureCallback<HttpResponse>() {

        @Override
        public void failed(Exception ex) {
          responseSubject.onError(new HalApiClientException("HTTP request failed", null, uri, ex));
        }

        @Override
        public void completed(HttpResponse result) {


          JsonNode json = parseJson(result);
          HalResource hal = new HalResource(json);

          int statusCode = result.getStatusLine().getStatusCode();
          String contentType = result.getEntity().getContentType().getValue();

          HalResponse response = new HalResponse().withStatus(statusCode).withContentType(contentType).withBody(hal);

          if (statusCode == 200) {
            if (!StringUtils.equals(contentType, HalResource.CONTENT_TYPE)) {
              responseSubject.onError(new RuntimeException("Unexpected content type " + contentType));
              return;
            }
            responseSubject.onSuccess(response);
          }
          else {
            responseSubject.onError(new HalApiClientException(response, uri, null));
          }
        }

        @Override
        public void cancelled() {

        }
      });

      return responseSubject;
    }

    private HttpGet createRequest(String uri) {
      try {
        HttpGet request = new HttpGet();
        request.setURI(new URI(TestEnvironmentConstants.SERVER_URL + uri));
        return request;
      }
      catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }

    private JsonNode parseJson(HttpResponse result) {
      try {
        return JSON_FACTORY.createParser(result.getEntity().getContent()).readValueAsTree();
      }
      catch (UnsupportedOperationException | IOException e) {
        e.printStackTrace();
      }
      return null;
    }
  }
}

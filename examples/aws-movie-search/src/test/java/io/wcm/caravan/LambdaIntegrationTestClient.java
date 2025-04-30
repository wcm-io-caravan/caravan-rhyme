package io.wcm.caravan;

import static org.mockito.Mockito.mock;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent.ProxyRequestContext;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.client.HalApiClientBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;

public class LambdaIntegrationTestClient {

  public static final String STAGE = "testStage";

  private final Context context = mock(Context.class);

  private final Map<String, String> stageVariables = new LinkedHashMap<>();

  private final RequestMetricsCollector metrics = RequestMetricsCollector.createEssentialCollector();

  private final LambdaClientSupport clientSupport;

  private final HalResourceLoader loader;

  private final HalApiClient client;

  private RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> requestHandler;

  public LambdaIntegrationTestClient(RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> requestHandler) {

    this.clientSupport = new LambdaClientSupport();

    this.loader = HalResourceLoader.create(clientSupport);

    this.client = HalApiClientBuilder.create()
        .withResourceLoader(loader)
        .withMetrics(metrics)
        .build();

    this.requestHandler = requestHandler;
  }

  public Context getContextMock() {
    return context;
  }

  public Map<String, String> getStageVariables() {
    return stageVariables;
  }

  public HalResponse getResponse(String pathWithoutStage) {

    return loader.getHalResource("/" + STAGE + pathWithoutStage).blockingGet();
  }

  public HalResponse getResponse(LinkableResource resource) {

    String resourceUrl = resource.createLink().getHref();

    return getResponse(stripStagePrefix(resourceUrl));
  }

  public APIGatewayProxyResponseEvent getGatewayProxyResponse(String absoluteUrl) {

    return clientSupport.executeRequest(URI.create(absoluteUrl));
  }

  public <T> T getEntryPoint(Class<T> halApiInterface) {

    return client.getRemoteResource("/" + STAGE, halApiInterface);
  }

  public Integer getMaxAge() {

    return metrics.getResponseMaxAge();
  }

  private String stripStagePrefix(String pathWithStage) {

    return StringUtils.substringAfter(pathWithStage, STAGE);
  }


  private class LambdaClientSupport implements HttpClientSupport {

    @Override
    public void executeGetRequest(URI uri, HttpClientCallback callback) {

      try {
        APIGatewayProxyResponseEvent response = executeRequest(uri);

        callback.onHeadersAvailable(response.getStatusCode(), convertHeaderMap(response));

        callback.onBodyAvailable(IOUtils.toInputStream(response.getBody(), StandardCharsets.UTF_8));
      }
      catch (Exception ex) {

        callback.onExceptionCaught(ex);
      }
    }

    private APIGatewayProxyResponseEvent executeRequest(URI uri) {

      APIGatewayProxyRequestEvent request = createRequest(uri);

      return requestHandler.handleRequest(request, context);
    }

    private APIGatewayProxyRequestEvent createRequest(URI uri) {

      String pathWithoutStage = stripStagePrefix(uri.getPath());
      String path = StringUtils.defaultIfEmpty(pathWithoutStage, "/");

      Map<String, List<String>> multiValueQueryStringParameters = parseQueryParameters(uri);

      Map<String, String> queryStringParameters = createMapWithFirstValues(multiValueQueryStringParameters);

      return new APIGatewayProxyRequestEvent()
          .withRequestContext(new ProxyRequestContext().withStage(STAGE).withPath(path))
          .withStageVariables(stageVariables)
          .withQueryStringParameters(queryStringParameters)
          .withMultiValueQueryStringParameters(multiValueQueryStringParameters)
          .withPath(path);
    }

    private Map<String, List<String>> parseQueryParameters(URI uri) {

      if (StringUtils.isBlank(uri.getQuery())) {
        return Collections.emptyMap();
      }

      Map<String, List<String>> params = new LinkedHashMap<String, List<String>>();

      for (String queryPart : StringUtils.split(uri.getQuery(), "&")) {

        String name = StringUtils.substringBefore(queryPart, "=");
        String value = StringUtils.substringAfter(queryPart, "=");

        params.putIfAbsent(name, new ArrayList<>());
        params.get(name).add(value);
      }

      return params;
    }

    private Map<String, String> createMapWithFirstValues(Map<String, List<String>> multiValueQueryStringParameters) {

      return multiValueQueryStringParameters.entrySet().stream()
          .map(entry -> Pair.of(entry.getKey(), entry.getValue().get(0)))
          .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    private Map<String, Collection<String>> convertHeaderMap(APIGatewayProxyResponseEvent response) {

      Multimap<String, String> headers = LinkedHashMultimap.create();

      response.getHeaders().forEach(headers::put);

      return headers.asMap();
    }
  }
}

package io.wcm.caravan.rhyme.examples.movies.impl;

import java.util.Optional;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.net.HttpHeaders;

import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.client.CachingConfiguration;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.RhymeMetadataConfiguration;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.awslambda.api.LambdaResourceRouting;
import io.wcm.caravan.rhyme.awslambda.api.LambdaRhyme;
import io.wcm.caravan.rhyme.awslambda.api.RhymeRequestHandler;

/**
 * Implementation of the AWS Lambda {@link RequestHandler} interface that serves all resources from this example,
 * and configures how upstream resources are loaded and cached.
 */
public class MovieSearchRequestHandler implements LambdaResourceRouting, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final RhymeRequestHandler requestHandler;

  /**
   * Default constructor that is called for each cold start of your lambda
   */
  public MovieSearchRequestHandler() {

    requestHandler = new RhymeRequestHandler(this)
        .withResourceLoader(configureResourceLoader().build())
        .withCustomizedRhymeBuilder(this::configureRhymeBuilder);
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

    APIGatewayProxyResponseEvent response = requestHandler.handleRequest(input, context);

    // additional CORS headers required to load resources from an externally hosted HAL Explorer frontend
    response.getHeaders().put(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS");
    response.getHeaders().put(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

    return response;
  }

  @Override
  public LinkableResource createRequestedResource(LambdaRhyme rhyme) {

    String path = rhyme.getRequest().getPath();

    if (MovieSearchRootResource.PATH.equals(path)) {
      return new MovieSearchRootResource(rhyme);
    }

    if (MovieSearchResultResource.PATH.equals(path)) {
      return MovieSearchResultResource.createWithRequestParametersFrom(rhyme);
    }

    return null;
  }

  /**
   * Creates and configures the {@link HalResourceLoader} to be used for any upstream requests.
   * @return a configured {@link HalResourceLoaderBuilder} instance
   */
  protected HalResourceLoaderBuilder configureResourceLoader() {

    return HalResourceLoaderBuilder.create()
        // we want to cache all responses from the upstream API
        .withMemoryCache()
        // because the responses from the Hypermedia Movie Demo do not set any "max-age" cache control directives,
        // we are using a custom configuration to ensure that those responses are cached for one hour
        .withCachingConfiguration(new CachingConfiguration() {

          @Override
          public boolean isCachingOfHalApiClientExceptionsEnabled() {
            return false;
          }

          @Override
          public int getDefaultMaxAge(Optional<Integer> statusCode) {
            return 3600;
          }
        });
  }

  /**
   * Allows further customization of the {@link RhymeBuilder} instance for each incoming request
   * @param builder as pre-configured by {@link RhymeRequestHandler}
   * @param request the incoming request
   * @return a {@link RhymeBuilder} instance with additional customizations
   */
  protected RhymeBuilder configureRhymeBuilder(RhymeBuilder builder, APIGatewayProxyRequestEvent request) {

    // in this example we always want to show the embedded "rhyme:metadata" resource so you can
    // easily inspect which upstream resources are requested in the background
    return builder.withMetadataConfiguration(new RhymeMetadataConfiguration() {

      @Override
      public boolean isMetadataGenerationEnabled() {
        return true;
      }
    });
  }
}

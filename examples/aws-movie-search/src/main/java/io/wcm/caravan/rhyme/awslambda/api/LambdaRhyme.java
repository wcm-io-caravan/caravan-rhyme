package io.wcm.caravan.rhyme.awslambda.api;

import java.time.Duration;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

/**
 * This is the main point of interaction of your server-side resource implementations with the Rhyme framework's AWS
 * lambda integration code. One instance of this interface is created for each incoming request, and needs to be passed
 * around to your resource implementation constructors.
 * The methods in this interface will allow you to
 * <ul>
 * <li>access the incoming {@link APIGatewayProxyRequestEvent} and {@link Context}</li>
 * <li>build {@link Link} instances to your resource implementations</li>
 * <li>request remote resources with a {@link HalApiClient}</li>
 * <li>modify the max-age cache directive for the current response</li>
 * </ul>
 * <p>
 * Implementations of this interface are automatically created by
 * {@link RhymeRequestHandler#handleRequest(APIGatewayProxyRequestEvent, Context)}
 * and will be passed on to {@link LambdaResourceRouting#createRequestedResource(LambdaRhyme)}
 * when your resource implementations will be created.
 * </p>
 * <p>
 * If you do need to apply any customizations to the underlying {@link Rhyme} instance,
 * you can use {@link RhymeRequestHandler#withResourceLoader(HalResourceLoader)} or
 * {@link RhymeRequestHandler#withCustomizedRhymeBuilder(java.util.function.BiFunction)}
 * </p>
 * @see RhymeRequestHandler
 */
public interface LambdaRhyme {

  /**
   * @return the {@link APIGatewayProxyRequestEvent} instance that was passed to
   *         {@link RequestHandler#handleRequest(Object, Context)}
   */
  APIGatewayProxyRequestEvent getRequest();

  /**
   * @return the {@link Context} instance that was passed to {@link RequestHandler#handleRequest(Object, Context)}
   */
  Context getContext();

  /**
   * @param resourcePath the absolute path (without stage prefix) of the resource to link to
   * @return a fluent {@link LambdaLinkBuilder} to add additional variables or attributes to the link
   */
  LambdaLinkBuilder buildLinkTo(String resourcePath);

  /**
   * Create a dynamic client proxy to load and navigate through HAL+JSON resources from an upstream service.
   * Any interaction with the proxies will be recorded and used to generate embedded metadata about the upstream
   * requests when the response is rendered later.
   * @param <T> an interface annotated with {@link HalApiInterface}
   * @param uri the URI of the entry point, in any format that the {@link HalResourceLoader} being used can understand
   * @param halApiInterface an interface annotated with {@link HalApiInterface}
   * @return a dynamic proxy instance of the provided interface that you can use to navigate through the
   *         resources of the service
   * @see HalApiClient
   */
  <T> T getRemoteResource(String uri, Class<T> halApiInterface);

  /**
   * Limit the maximum time for which the response should be cached by clients and downstream services. Note that
   * calling this method only sets the upper limit: if other upstream resource fetched during the current request
   * indicate a lower max-age value in their header, that lower value will be used instead.
   * @param duration the max cache time
   */
  void setResponseMaxAge(Duration duration);

  /**
   * Provides access to the underlying {@link Rhyme} instance of the core framework to be used for the current request,
   * just in case you need to call one of the methods for which there doesn't exist a delegate in the
   * {@link LambdaRhyme} interface
   * @return the single {@link Rhyme} instance that is used throughout the incoming request
   */
  Rhyme getCoreRhyme();
}

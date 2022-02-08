package io.wcm.caravan.rhyme.awslambda.impl;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.net.HttpHeaders;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.awslambda.api.LambdaLinkBuilder;
import io.wcm.caravan.rhyme.awslambda.api.LambdaResourceRouting;
import io.wcm.caravan.rhyme.awslambda.api.LambdaRhyme;
import io.wcm.caravan.rhyme.awslambda.api.RhymeRequestHandler;
import software.amazon.awssdk.http.HttpStatusCode;

/**
 * The core class of Rhyme's AWS lambda integration that handles the main request cycle (from resource instance
 * creation to rendering success and error responses).
 * <p>
 * You shouldn't need to interact with this implementation directly, as instances are automatically created
 * from within {@link RhymeRequestHandler#handleRequest(APIGatewayProxyRequestEvent, Context)} and then passed
 * on to your server-side resource implementations via
 * {@link LambdaResourceRouting#createRequestedResource(LambdaRhyme)}
 * </p>
 * @see RhymeRequestHandler
 * @see LambdaResourceRouting
 */
public class LambdaRhymeImpl implements LambdaRhyme {

  private final APIGatewayProxyRequestEvent request;
  private final Context context;

  private final LambdaRhymeDocsSupport rhymeDocs;

  private final Rhyme rhyme;

  /**
   * @param request the incoming request
   * @param context the {@link Context} instance passed to {@link RequestHandler#handleRequest(Object, Context)}
   * @param rhymeBuilder a {@link RhymeBuilder} instance (that can be customized by
   *          {@link RhymeRequestHandler#withCustomizedRhymeBuilder(java.util.function.BiFunction)}
   * @param rhymeDocs the {@link LambdaRhymeDocsSupport} class (to serve the HTML documentation from classpath
   *          resources)
   */
  public LambdaRhymeImpl(APIGatewayProxyRequestEvent request, Context context, RhymeBuilder rhymeBuilder, LambdaRhymeDocsSupport rhymeDocs) {

    this.request = request;
    this.context = context;
    this.rhymeDocs = rhymeDocs;

    String incomingRequestUrl = buildAbsoluteUrl(request.getPath());
    this.rhyme = rhymeBuilder.buildForRequestTo(incomingRequestUrl);
  }

  private String buildAbsoluteUrl(String resourcePath) {

    return getBaseUrl(request) + resourcePath;
  }

  static String getBaseUrl(APIGatewayProxyRequestEvent request) {

    String apiGatewayUrl = request.getStageVariables().getOrDefault("apiGatewayUrl", "");

    return apiGatewayUrl + "/" + request.getRequestContext().getStage();
  }

  @Override
  public APIGatewayProxyRequestEvent getRequest() {
    return request;
  }

  @Override
  public Context getContext() {
    return context;
  }

  @Override
  public <T> T getRemoteResource(String uri, Class<T> halApiInterface) {
    return rhyme.getRemoteResource(uri, halApiInterface);
  }

  @Override
  public void setResponseMaxAge(Duration duration) {
    rhyme.setResponseMaxAge(duration);
  }

  @Override
  public Rhyme getCoreRhyme() {
    return rhyme;
  }

  @Override
  public LambdaLinkBuilder buildLinkTo(String resourcePath) {

    return new LambdaLinkBuilderImpl(buildAbsoluteUrl(resourcePath));
  }

  /**
   * Handles the main request cycle for the incoming request:
   * <ul>
   * <li>if HTML documentation is requested, it is returned by {@link LambdaRhymeDocsSupport}</li>
   * <li>otherwise the server-side resource implementation for the current request is created by the given
   * {@link LambdaResourceRouting} strategy</li>
   * <li>the HAL+JSON response will be rendered using the core Rhyme framework's {@link AsyncHalResponseRenderer}</li>
   * <li>the response is converted to an {@link APIGatewayProxyRequestEvent}</li>
   * <li>any exceptions thrown and caught in this whole process will be rendered as a vnd.error resource</li>
   * </ul>
   * @param routing will be called to create the server-side resource implementation
   * @return a {@link APIGatewayProxyRequestEvent} with status, headers and body already set
   */
  public APIGatewayProxyResponseEvent createResourceAndRenderResponse(LambdaResourceRouting routing) {
    try {
      if (rhymeDocs.isRhymeDocsRequest()) {
        return rhymeDocs.createHtmlResponse();
      }

      LinkableResource resource = routing.createRequestedResource(this);
      if (resource == null) {
        throw new HalApiServerException(HttpStatusCode.NOT_FOUND, "No resource implementation was found for relative path " + request.getPath());
      }

      HalResponse response = rhyme.renderResponse(resource).blockingGet();

      return convertResponse(response);
    }
    catch (Exception ex) {
      return convertResponse(rhyme.renderVndErrorResponse(ex));
    }
  }

  private APIGatewayProxyResponseEvent convertResponse(HalResponse halResponse) {

    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, halResponse.getContentType());

    if (halResponse.getMaxAge() != null) {
      headers.put(HttpHeaders.CACHE_CONTROL, "max-age=" + halResponse.getMaxAge());
    }

    return new APIGatewayProxyResponseEvent()
        .withStatusCode(halResponse.getStatus())
        .withHeaders(headers)
        .withBody(halResponse.getBody().getModel().toString());
  }
}

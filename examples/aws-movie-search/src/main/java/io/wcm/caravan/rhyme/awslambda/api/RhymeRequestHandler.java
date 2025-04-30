package io.wcm.caravan.rhyme.awslambda.api;

import static io.wcm.caravan.rhyme.api.common.RequestMetricsCollector.EMBED_RHYME_METADATA;

import java.util.function.BiFunction;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.server.RhymeMetadataConfiguration;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.awslambda.impl.LambdaRhymeDocsSupport;
import io.wcm.caravan.rhyme.awslambda.impl.LambdaRhymeImpl;

/**
 * A base implementation of AWS Lambda's {@link RequestHandler} interface to be used with the API Gateway's proxy
 * integration.
 * <p>
 * It takes care of creating a single {@link LambdaRhyme} instance for each incoming request,
 * and then use the given {@link LambdaResourceRouting} strategy object to create the requested server-side resource
 * implementation.
 * </p>
 * <p>
 * That resource will then be rendered as a {@link APIGatewayProxyResponseEvent},
 * and any exceptions that were thrown and caught during the overall process will be rendered as a vnd.error response.
 * </p>
 * @see LambdaResourceRouting
 * @see LambdaRhyme
 */
public class RhymeRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final LambdaResourceRouting routing;

  private HalResourceLoader resourceLoader = HalResourceLoader.create();

  private BiFunction<RhymeBuilder, APIGatewayProxyRequestEvent, RhymeBuilder> customization = (builder, request) -> builder;

  /**
   * @param routing the strategy to create the server-side resource implementation to be rendered
   */
  public RhymeRequestHandler(LambdaResourceRouting routing) {
    this.routing = routing;
  }

  /**
   * @param customLoader a custom {@link HalResourceLoader} implementation to use for all upstream requests initiated
   *          via {@link LambdaRhyme#getRemoteResource(String, Class)}
   * @return this
   */
  public RhymeRequestHandler withResourceLoader(HalResourceLoader customLoader) {

    this.resourceLoader = customLoader;
    return this;
  }

  /**
   * An extension point that allows you to modify the {@link RhymeBuilder} instance that builds the
   * single {@link Rhyme} instance which is then used throughout each incoming request
   * @param function that can return a customized {@link RhymeBuilder} instance
   * @return this
   */
  public RhymeRequestHandler withCustomizedRhymeBuilder(BiFunction<RhymeBuilder, APIGatewayProxyRequestEvent, RhymeBuilder> function) {

    this.customization = function;
    return this;
  }

  private RhymeBuilder createRhymeBuilder(APIGatewayProxyRequestEvent request, LambdaRhymeDocsSupport rhymeDocs) {

    RhymeBuilder rhymeBuilder = RhymeBuilder.withResourceLoader(resourceLoader)
        .withRhymeDocsSupport(rhymeDocs)
        .withMetadataConfiguration(new RhymeMetadataConfiguration() {

          @Override
          public boolean isMetadataGenerationEnabled() {
            return request.getQueryStringParameters().containsKey(EMBED_RHYME_METADATA);
          }

        });

    return customization.apply(rhymeBuilder, request);
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

    LambdaRhymeDocsSupport rhymeDocs = new LambdaRhymeDocsSupport(request);

    RhymeBuilder rhymeBuilder = createRhymeBuilder(request, rhymeDocs);

    LambdaRhymeImpl lambdaRhyme = new LambdaRhymeImpl(request, context, rhymeBuilder, rhymeDocs);

    return lambdaRhyme.createResourceAndRenderResponse(routing);
  }
}

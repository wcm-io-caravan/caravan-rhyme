package io.wcm.caravan.rhyme.awslambda.api;

import static io.wcm.caravan.rhyme.api.common.RequestMetricsCollector.EMBED_RHYME_METADATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.LambdaIntegrationTestClient;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.RhymeMetadataConfiguration;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

@ExtendWith(MockitoExtension.class)
class RhymeRequestHandlerTest {

  @Mock
  private HalResourceLoader resourceLoader;

  @HalApiInterface
  public interface TestResource extends LinkableResource {

    @ResourceProperty
    Optional<String> getText();
  }

  private HalResponse renderResponse(LambdaResourceRouting routing) {

    return renderResponse("/", routing);
  }

  private HalResponse renderResponse(String resourcePath, LambdaResourceRouting routing) {

    RhymeRequestHandler handler = new RhymeRequestHandler(routing)
        .withResourceLoader(resourceLoader);

    LambdaIntegrationTestClient testClient = new LambdaIntegrationTestClient(handler);

    return testClient.getResponse(resourcePath);
  }

  @Test
  void should_render_resource_with_response_headers() {

    int maxAge = 123;

    LambdaResourceRouting routing = rhyme -> new TestResource() {

      @Override
      public Optional<String> getText() {

        rhyme.getCoreRhyme().setResponseMaxAge(Duration.ofSeconds(maxAge));
        return Optional.of("foo");
      }

      @Override
      public Link createLink() {
        return new Link("/");
      }

    };

    HalResponse response = renderResponse(routing);

    assertThat(response.getStatus())
        .isEqualTo(200);

    assertThat(response.getContentType())
        .isEqualTo(HalResource.CONTENT_TYPE);

    assertThat(response.getMaxAge())
        .isEqualTo(maxAge);

    assertThat(response.getBody().getModel().path("text").asText())
        .isEqualTo("foo");
  }

  @Test
  void should_render_error_resource_with_response_headers_if_exception_is_thrown() {

    int statusCode = 403;
    String errorMessage = "You are not allowed to do that";

    LambdaResourceRouting routing = rhyme -> new TestResource() {

      @Override
      public Optional<String> getText() {
        throw new HalApiServerException(statusCode, errorMessage);
      }

      @Override
      public Link createLink() {
        return new Link("/");
      }

    };

    HalApiClientException ex = assertThrows(HalApiClientException.class, () -> renderResponse(routing));

    assertThat(ex.getStatusCode())
        .isEqualTo(statusCode);

    assertThat(ex.getErrorResponse().getContentType())
        .isEqualTo(VndErrorResponseRenderer.CONTENT_TYPE);

    assertThat(ex.getErrorResponse().getBody().getModel().path("message").asText())
        .isEqualTo(errorMessage);
  }

  @Test
  void should_render_404_error_response_if_LambdaResourceRouting_returns_null_for_given_path() {

    LambdaResourceRouting routing = rhyme -> null;

    HalApiClientException ex = assertThrows(HalApiClientException.class, () -> renderResponse(routing));

    assertThat(ex.getStatusCode())
        .isEqualTo(404);

    assertThat(ex.getErrorResponse().getContentType())
        .isEqualTo(VndErrorResponseRenderer.CONTENT_TYPE);

    assertThat(ex.getErrorResponse().getBody().getModel().path("message").asText())
        .isEqualTo("No resource implementation was found for relative path /");
  }

  @Test
  void should_use_given_resource_loader_to_fetch_remote_resources() {

    when(resourceLoader.getHalResource("/foo"))
        .thenReturn(Single.just(new HalResponse()
            .withStatus(200)
            .withBody(JsonNodeFactory.instance.objectNode().put("text", "foo"))));

    LambdaResourceRouting routing = rhyme -> new TestResource() {

      @Override
      public Optional<String> getText() {

        TestResource remote = rhyme.getCoreRhyme().getRemoteResource("/foo", TestResource.class);

        return remote.getText();
      }

      @Override
      public Link createLink() {
        return new Link("/");
      }
    };

    HalResponse response = renderResponse(routing);

    assertThat(response.getBody().getModel().path("text").asText())
        .isEqualTo("foo");
  }

  private static final class TestResourceImpl implements TestResource {

    @Override
    public Optional<String> getText() {
      return Optional.of("foo");
    }

    @Override
    public Link createLink() {
      return new Link("/");
    }
  }

  @Test
  void should_not_include_metadata_by_default() {

    LambdaResourceRouting routing = rhyme -> new TestResourceImpl();

    HalResponse response = renderResponse("/", routing);

    assertThat(response.getBody().getEmbeddedResource("rhyme:metadata"))
        .isNull();
  }

  @Test
  void should_include_metadata_if_request_param_is_present() {

    LambdaResourceRouting routing = rhyme -> new TestResourceImpl();

    HalResponse response = renderResponse("/?" + EMBED_RHYME_METADATA, routing);

    assertThat(response.getBody().getEmbeddedResource("rhyme:metadata"))
        .isNotNull();
  }


  @Test
  void should_enable_metadata_generation_through_RhymeBuilder_customization() {

    LambdaResourceRouting routing = rhyme -> new TestResourceImpl();

    RhymeRequestHandler handler = new RhymeRequestHandler(routing)
        .withCustomizedRhymeBuilder((builder, request) -> builder.withMetadataConfiguration(
            new RhymeMetadataConfiguration() {

              @Override
              public boolean isMetadataGenerationEnabled() {
                return true;
              }
            }));

    LambdaIntegrationTestClient testClient = new LambdaIntegrationTestClient(handler);

    HalResponse response = testClient.getResponse("/");

    assertThat(response.getBody().getEmbeddedResource("rhyme:metadata"))
        .isNotNull();
  }

  @Test
  void should_make_context_available_to_resource_routing() {

    String functionName = "foo";

    LambdaResourceRouting routing = rhyme -> {

      assertThat(rhyme.getContext().getFunctionName())
          .isEqualTo(functionName);

      return new TestResourceImpl();
    };

    RhymeRequestHandler handler = new RhymeRequestHandler(routing);

    LambdaIntegrationTestClient testClient = new LambdaIntegrationTestClient(handler);

    when(testClient.getContextMock().getFunctionName())
        .thenReturn(functionName);

    testClient.getResponse("/");
  }
}

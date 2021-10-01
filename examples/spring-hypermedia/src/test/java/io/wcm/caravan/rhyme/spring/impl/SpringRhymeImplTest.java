package io.wcm.caravan.rhyme.spring.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.relations.VndErrorRelations;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;


@ExtendWith(MockitoExtension.class)
public class SpringRhymeImplTest {

  private static final String REQUEST_PATH = "/foo";
  private static final String REQUEST_QUERY = "bar=123";

  @Mock
  private HttpServletRequest request;

  @Mock
  private HalResourceLoader resourceLoader;

  @Mock
  private SpringRhymeDocsIntegration rhymeDocs;

  private SpringRhymeImpl rhyme;

  @BeforeEach
  void setUp() {

    when(request.getRequestURL())
        .thenReturn(new StringBuffer(REQUEST_PATH));

    when(request.getQueryString())
        .thenReturn(REQUEST_QUERY);

    rhyme = new SpringRhymeImpl(request, resourceLoader, rhymeDocs);
  }

  @HalApiInterface
  public interface MinimalTestResource extends LinkableResource {

    @ResourceState
    ObjectNode getState();
  }

  @Test
  public void getRemoteResource_should_use_HalResourceLoader() throws Exception {

    String resourceUri = "/bar";

    when(resourceLoader.getHalResource(resourceUri))
        .thenReturn(Single.just(new HalResponse().withStatus(200).withBody(new HalResource())));

    MinimalTestResource resource = rhyme.getRemoteResource(resourceUri, MinimalTestResource.class);

    ObjectNode state = resource.getState();

    assertThat(state).isNotNull();

    verify(resourceLoader).getHalResource(resourceUri);
  }

  @Test
  public void renderVndErrorResponse_should_use_request_url() throws Exception {

    ResponseEntity<JsonNode> errorEntity = rhyme.renderVndErrorResponse(new RuntimeException());

    HalResource hal = new HalResource(errorEntity.getBody());

    Link aboutLink = hal.getLink(VndErrorRelations.ABOUT);

    assertThat(aboutLink).isNotNull();
    assertThat(aboutLink.getHref()).isEqualTo(REQUEST_PATH + "?" + REQUEST_QUERY);
  }

  @Test
  public void getCoreRhyme_should_return_the_same_core_Rhyme_instance_for_multiple_calls() throws Exception {

    Rhyme coreRhyme = rhyme.getCoreRhyme();

    assertThat(coreRhyme).isNotNull();

    assertThat(rhyme.getCoreRhyme()).isSameAs(coreRhyme);
  }

}

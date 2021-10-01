package io.wcm.caravan.rhyme.spring.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.spring.impl.SpringRhymeImplTest.MinimalTestResource;

@ExtendWith(MockitoExtension.class)
public class LinkableResourceMessageConverterTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String REQUEST_PATH = "/foo";
  private static final String REQUEST_QUERY = "bar=123";
  private static final String REQUEST_URL = REQUEST_PATH + "?" + REQUEST_QUERY;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HalResourceLoader resourceLoader;

  @Mock
  private SpringRhymeDocsIntegration rhymeDocs;

  private SpringRhymeImpl rhyme;

  private LinkableResourceMessageConverter converter;

  @BeforeEach
  void setUp() {

    when(request.getRequestURL())
        .thenReturn(new StringBuffer(REQUEST_PATH));

    when(request.getQueryString())
        .thenReturn(REQUEST_QUERY);

    rhyme = new SpringRhymeImpl(request, resourceLoader, rhymeDocs);

    converter = new LinkableResourceMessageConverter(rhyme);
  }

  @Test
  public void supports_should_return_true_for_interface() throws Exception {

    assertThat(converter.supports(LinkableResource.class))
        .isTrue();
  }

  @Test
  public void supports_should_return_true_for_implementation() throws Exception {

    assertThat(converter.supports(MinimalTestResourceImpl.class))
        .isTrue();
  }

  @Test
  public void read_should_always_throw_HttpMessageNotReadableException() throws Exception {

    MockHttpInputMessage mockInput = new MockHttpInputMessage(new byte[0]);
    Throwable ex = catchThrowable(() -> converter.read(MinimalTestResource.class, mockInput));

    assertThat(ex)
        .isInstanceOf(HttpMessageNotReadableException.class)
        .hasMessage("Parsing " + LinkableResource.class.getSimpleName() + " instances is not implemented");
  }

  @Test
  public void write_should_add_headers() throws Exception {

    MinimalTestResource resource = new MinimalTestResourceImpl();

    MockHttpOutputMessage mockOutput = new MockHttpOutputMessage();
    converter.write(resource, MediaTypes.HAL_JSON, mockOutput);

    assertThat(mockOutput.getHeaders())
        .isNotEmpty()
        .containsKey("Content-Type");
  }

  @Test
  public void write_should_serialize_body() throws Exception {

    MinimalTestResource resource = new MinimalTestResourceImpl();

    MockHttpOutputMessage mockOutput = new MockHttpOutputMessage();
    converter.write(resource, MediaTypes.HAL_JSON, mockOutput);

    HalResource hal = parseResponse(mockOutput);

    assertThat(hal.getStateFieldNames())
        .containsExactly("foo");

    assertThat(hal.getLink().getHref())
        .isEqualTo(REQUEST_URL);
  }

  private HalResource parseResponse(MockHttpOutputMessage mockOutput) throws IOException, JsonParseException {

    ObjectNode jsonResponse = new JsonFactory(OBJECT_MAPPER).createParser(mockOutput.getBodyAsBytes()).readValueAsTree();

    return new HalResource(jsonResponse);
  }

  private final class MinimalTestResourceImpl implements MinimalTestResource {

    ObjectNode state = JsonNodeFactory.instance.objectNode()
        .put("foo", "bar");

    @Override
    public ObjectNode getState() {
      return state;
    }

    @Override
    public Link createLink() {
      return new Link(REQUEST_URL);
    }
  }

}

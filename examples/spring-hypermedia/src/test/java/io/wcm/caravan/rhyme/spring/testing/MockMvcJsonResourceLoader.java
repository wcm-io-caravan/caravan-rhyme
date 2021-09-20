package io.wcm.caravan.rhyme.spring.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.relations.VndErrorRelations;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.spring.impl.CacheControlUtil;

@Component
@Primary
public class MockMvcJsonResourceLoader implements HalResourceLoader {

  private static final Logger log = LoggerFactory.getLogger(MockMvcJsonResourceLoader.class);

  private static final JsonFactory JSON_FACTORY = new JsonFactory(new ObjectMapper());;

  private final MockMvc mockMvc;

  public MockMvcJsonResourceLoader(@Autowired WebApplicationContext wac) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Override
  public Single<HalResponse> getHalResource(String uriString) {
    try {

      URI uri = URI.create(uriString);

      MockHttpServletResponse mvcResponse = getServletResponse(uri);

      HalResponse halResponse = convertToHalResponse(mvcResponse);

      if (halResponse.getStatus() >= 400) {
        String causes = extractCauses(halResponse);
        String msg = "Request for " + uri + " has failed with status " + halResponse.getStatus() + "\n" + causes;
        log.warn(msg);
        return Single.error(new HalApiClientException(msg, halResponse.getStatus(), uriString, null));
      }

      return Single.just(halResponse);

    }
    catch (Exception e) {
      return Single.error(new HalApiClientException("An exception occured when calling the controller via MockMvc", 500, uriString, e));
    }
  }

  private String extractCauses(HalResponse halResponse) {
    StringBuilder causes = new StringBuilder();
    HalResource body = halResponse.getBody();

    boolean isVndError = VndErrorResponseRenderer.CONTENT_TYPE.contentEquals(halResponse.getContentType());
    if (body != null && isVndError) {
      appendTitle(causes, body);
      body.getEmbedded(VndErrorRelations.ERRORS).forEach(hal -> appendTitle(causes, hal));
    }
    return causes.toString();
  }

  private void appendTitle(StringBuilder causes, HalResource hal) {
    causes.append(hal.getModel().path("title").asText() + "\n");
  }

  private MockHttpServletResponse getServletResponse(URI uri) throws Exception {

    MvcResult mvcResult = mockMvc.perform(get(uri))
        .andReturn();

    return mvcResult.getResponse();
  }

  private MockHttpServletResponse getAsyncServletResponse(URI uri) throws Exception {

    MvcResult asyncResult = mockMvc.perform(get(uri)).andExpect(request().asyncStarted())
        .andReturn();

    MvcResult mvcResult = mockMvc.perform(asyncDispatch(asyncResult))
        .andExpect(status().isOk())
        .andExpect(content().contentType(HalResource.CONTENT_TYPE))
        .andReturn();

    return mvcResult.getResponse();
  }

  private HalResource parseHalResource(MockHttpServletResponse mvcResponse)
      throws UnsupportedEncodingException, IOException, JsonParseException {

    String jsonString = mvcResponse.getContentAsString();
    assertThat(jsonString).isNotNull();

    JsonNode jsonNode = JSON_FACTORY.createParser(jsonString).readValueAsTree();

    return new HalResource(jsonNode);
  }

  private HalResponse convertToHalResponse(MockHttpServletResponse mvcResponse)
      throws UnsupportedEncodingException, IOException, JsonParseException {

    HalResource hal = parseHalResource(mvcResponse);

    HalResponse halResponse = new HalResponse()
        .withBody(hal)
        .withStatus(mvcResponse.getStatus())
        .withContentType(mvcResponse.getContentType());

    String cacheControl = mvcResponse.getHeader("Cache-Control");
    if (cacheControl != null) {
      halResponse = halResponse.withMaxAge(CacheControlUtil.parseMaxAge(cacheControl));
    }
    return halResponse;
  }

}

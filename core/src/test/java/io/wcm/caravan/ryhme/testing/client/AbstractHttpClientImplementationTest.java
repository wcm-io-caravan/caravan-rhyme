package io.wcm.caravan.ryhme.testing.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.URISyntaxException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.common.net.HttpHeaders;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientImplementation;
import wiremock.org.apache.http.client.utils.URIBuilder;

public abstract class AbstractHttpClientImplementationTest {

  private static final String UNKNOWN_HOST_URL = "http://foo.bar";

  private static final String TEST_PATH = "/test";

  private static WireMockServer wireMockServer;

  private static String testUrl;
  private static String sslTestUrl;

  @BeforeAll
  static void init() throws URISyntaxException {
    wireMockServer = new WireMockServer(new WireMockConfiguration().dynamicPort().dynamicHttpsPort());
    wireMockServer.start();

    testUrl = buildTestUrl(false);
    sslTestUrl = buildTestUrl(true);
  }

  private static String buildTestUrl(boolean useSsl) throws URISyntaxException {

    return new URIBuilder()
        .setScheme(useSsl ? "https" : "http")
        .setHost("localhost")
        .setPort(useSsl ? wireMockServer.httpsPort() : wireMockServer.port())
        .setPath(TEST_PATH)
        .build().toString();
  }

  @AfterEach
  void tearDown() {
    wireMockServer.resetAll();
  }


  protected abstract HttpClientImplementation createImplementationUnderTest();


  private static HalResource createHalResource() {

    return new HalResource(TEST_PATH).addState(createJsonResource());
  }

  private static ObjectNode createJsonResource() {

    return JsonNodeFactory.instance.objectNode().put("föö", "官ar");
  }

  private static ResponseDefinitionBuilder get200HalResponseWithMaxAge(Integer maxAge) {

    HalResource hal = createHalResource();

    ResponseDefinitionBuilder response = aResponse()
        .withHeader(HttpHeaders.CONTENT_TYPE, HalResource.CONTENT_TYPE)
        .withStatus(200)
        .withBody(hal.getModel().toString());

    if (maxAge != null) {
      response = response.withHeader(HttpHeaders.CACHE_CONTROL, "max-age=" + maxAge);
    }

    return response;
  }

  private static void stub200HalResponseWithMaxAge(Integer maxAge) {

    wireMockServer.stubFor(get(urlEqualTo(TEST_PATH))
        .willReturn(get200HalResponseWithMaxAge(maxAge)));
  }

  private static void stubHtmlResponseWithStatusCode(int statusCode) {

    wireMockServer.stubFor(get(urlEqualTo(TEST_PATH))
        .willReturn(aResponse()
            .withStatus(statusCode)
            .withHeader(HttpHeaders.CONTENT_TYPE, "text/html")
            .withBody("<h1>This is an HTML document</h1>")));
  }

  private static void stubJsonResponseWithStatusCode(int statusCode) {

    wireMockServer.stubFor(get(urlEqualTo(TEST_PATH))
        .willReturn(aResponse()
            .withStatus(statusCode)
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(createJsonResource().toString())));
  }

  private static void stubErrorResponseWithoutContentType(int statusCode) {

    wireMockServer.stubFor(get(urlEqualTo(TEST_PATH))
        .willReturn(aResponse()
            .withStatus(statusCode)
            .withBody("Foo")));
  }


  private void stubEmptyResponseWithStatusCode(int statusCode) {

    wireMockServer.stubFor(get(urlEqualTo(TEST_PATH))
        .willReturn(aResponse()
            .withStatus(statusCode)));
  }

  private HalResourceLoader createLoader() {

    HttpClientImplementation clientImpl = createImplementationUnderTest();

    return HalResourceLoader.withCustomHttpClient(clientImpl);
  }

  private HalResponse loadResource() {

    String uri = testUrl;

    return createLoader().getHalResource(uri).blockingGet();
  }
  private HalApiClientException loadResourceAndExpectClientException() {

    return loadResourceAndExpectClientException(testUrl);
  }

  private HalApiClientException loadResourceAndExpectClientException(String url) {
    Single<HalResponse> rxResponse = createLoader().getHalResource(url);

    Throwable ex = catchThrowable(() -> rxResponse.blockingGet());

    assertThat(ex)
        .isInstanceOf(HalApiClientException.class)
        .hasMessageStartingWith("HTTP request failed with status code");

    return (HalApiClientException)ex;
  }

  @Test
  public void response_code_should_be_set_for_200_hal_response() throws Exception {

    stub200HalResponseWithMaxAge(null);

    HalResponse response = loadResource();

    assertThat(response.getStatus())
        .isEqualTo(200);
  }

  @Test
  public void content_type_should_be_set_for_200_hal_response() throws Exception {

    stub200HalResponseWithMaxAge(null);

    HalResponse response = loadResource();

    assertThat(response.getContentType())
        .isEqualTo(HalResource.CONTENT_TYPE);
  }

  @Test
  public void body_should_be_set_for_200_hal_response() throws Exception {

    stub200HalResponseWithMaxAge(null);

    HalResponse response = loadResource();

    assertThat(response.getBody())
        .isNotNull();

    assertThat(response.getBody().getModel())
        .isEqualTo(createHalResource().getModel());
  }

  @Test
  public void maxAge_should_be_null_for_200_hal_response_without_cache_control() throws Exception {

    stub200HalResponseWithMaxAge(null);

    HalResponse response = loadResource();

    assertThat(response.getMaxAge())
        .isNull();
  }

  @Test
  public void non_zero_maxAge_should_be_taken_from_200_hal_response() throws Exception {

    stub200HalResponseWithMaxAge(100);

    HalResponse response = loadResource();

    assertThat(response.getMaxAge())
        .isEqualTo(100);
  }

  @Test
  public void zero_maxAge_should_be_taken_from_200_hal_response() throws Exception {

    stub200HalResponseWithMaxAge(0);

    HalResponse response = loadResource();

    assertThat(response.getMaxAge())
        .isEqualTo(0);
  }

  @Test
  public void status_code_should_be_present_in_HalApiClientException_for_non_ok_response() throws Exception {

    stubHtmlResponseWithStatusCode(503);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getStatusCode())
        .isEqualTo(503);
  }

  @Test
  public void request_url_should_be_present_in_HalApiClientException_for_non_ok_response() throws Exception {

    stubHtmlResponseWithStatusCode(503);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getRequestUrl())
        .isEqualTo(testUrl);
  }

  @Test
  public void error_body_should_be_null_in_HalApiClientException_for_non_ok_html_response() throws Exception {

    stubHtmlResponseWithStatusCode(503);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getErrorResponse())
        .isNotNull();
    assertThat(ex.getErrorResponse().getBody())
        .isNull();
  }

  @Test
  public void error_body_should_be_null_in_HalApiClientException_for_non_ok_response_without_body() throws Exception {

    stubEmptyResponseWithStatusCode(204);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getErrorResponse())
        .isNotNull();
    assertThat(ex.getErrorResponse().getBody())
        .isNull();
  }

  @Test
  public void error_body_should_be_present_in_HalApiClientException_for_non_ok_json_response() throws Exception {

    stubJsonResponseWithStatusCode(503);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getErrorResponse())
        .isNotNull();

    HalResource body = ex.getErrorResponse().getBody();

    assertThat(body)
        .isNotNull();
    assertThat(body.getModel())
        .isEqualTo(createJsonResource());
  }

  @Test
  public void status_code_should_be_null_in_HalApiClientException_for_failure_to_parse_json() throws Exception {

    stubHtmlResponseWithStatusCode(200);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getStatusCode())
        .isNull();
  }

  @Test
  public void request_url_should_be_present_in_HalApiClientException_for_failure_to_parse_json() throws Exception {

    stubHtmlResponseWithStatusCode(200);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getRequestUrl())
        .isEqualTo(testUrl);
  }

  @Test
  public void cause_should_be_present_in_HalApiClientException_for_failure_to_parse_json() throws Exception {

    stubHtmlResponseWithStatusCode(200);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex)
        .hasRootCauseInstanceOf(JsonProcessingException.class);

    assertThat(ex.getCause())
        .hasMessageStartingWith("Failed to read or parse JSON response");
  }

  @Test
  public void status_code_should_be_null_in_HalApiClientException_for_network_errors() throws Exception {

    HalApiClientException ex = loadResourceAndExpectClientException(UNKNOWN_HOST_URL);

    assertThat(ex.getStatusCode())
        .isNull();
  }

  @Test
  public void request_url_should_be_present_in_HalApiClientException_for_network_errors() throws Exception {

    HalApiClientException ex = loadResourceAndExpectClientException(UNKNOWN_HOST_URL);

    assertThat(ex.getRequestUrl())
        .isEqualTo(UNKNOWN_HOST_URL);
  }

  @Test
  public void error_body_should_be_null_in_HalApiClientException_for_network_errors() throws Exception {

    HalApiClientException ex = loadResourceAndExpectClientException(UNKNOWN_HOST_URL);

    assertThat(ex.getErrorResponse())
        .isNotNull();
    assertThat(ex.getErrorResponse().getBody())
        .isNull();
  }

  @Test
  public void cause_should_be_present_in_HalApiClientException_for_for_network_errors() throws Exception {

    HalApiClientException ex = loadResourceAndExpectClientException(UNKNOWN_HOST_URL);

    assertThat(ex)
        .hasCauseInstanceOf(UnknownHostException.class);
  }

  @Test
  public void cause_should_be_present_in_HalApiClientException_for_for_ssl_errors() throws Exception {

    HalApiClientException ex = loadResourceAndExpectClientException(sslTestUrl);

    assertThat(ex)
        .hasCauseInstanceOf(SSLHandshakeException.class);
  }

}

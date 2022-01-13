package io.wcm.caravan.rhyme.testing.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Duration;

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
import com.github.tomakehurst.wiremock.http.Fault;
import com.google.common.net.HttpHeaders;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;
import wiremock.org.apache.http.client.utils.URIBuilder;

/**
 * A base class that you can extend if you are implementing your own {@link HalResourceLoader}
 * or {@link HttpClientSupport} classes.
 * It will run a extensive suite of tests where a {@link WireMockServer} will respond
 * with a variety of responses, including client/server errors and corrupt responses.
 */
public abstract class AbstractHalResourceLoaderTest {

  protected static final String UNKNOWN_HOST_URL = "http://foo.bar";

  protected static final String TEST_PATH = "/test";

  private static WireMockServer wireMockServer;

  protected static String testUrl;
  protected static String sslTestUrl;

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

  /**
   * Implement this to return your implementation of {@link HalResourceLoader} to be tested.
   * If you are implementing {@link HttpClientSupport}) then use
   * {@link HalResourceLoader#create()} to create the instance under test
   * @return the instance to be tested
   */
  protected abstract HalResourceLoader createLoaderUnderTest();

  private static HalResource createHalResource() {

    return new HalResource(TEST_PATH).addState(createJsonResource());
  }

  private static ObjectNode createJsonResource() {

    return JsonNodeFactory.instance.objectNode().put("föö", "官ar");
  }

  private static ResponseDefinitionBuilder get200HalResponseWithCacheControl(String... values) {

    HalResource hal = createHalResource();

    ResponseDefinitionBuilder response = aResponse()
        .withHeader(HttpHeaders.CONTENT_TYPE, HalResource.CONTENT_TYPE)
        .withStatus(200)
        .withBody(hal.getModel().toString());

    for (String value : values) {
      response = response.withHeader(HttpHeaders.CACHE_CONTROL, value);
    }

    return response;
  }

  private static void stub200HalResponseWithMaxAge(Integer maxAge) {

    wireMockServer.stubFor(get(urlEqualTo(TEST_PATH))
        .willReturn(get200HalResponseWithCacheControl("max-age=" + maxAge)));
  }

  private static void stub200HalResponseWithCacheControl(String... cacheControl) {

    wireMockServer.stubFor(get(urlEqualTo(TEST_PATH))
        .willReturn(get200HalResponseWithCacheControl(cacheControl)));
  }

  private static void stub200HalResponse() {

    wireMockServer.stubFor(get(urlEqualTo(TEST_PATH))
        .willReturn(get200HalResponseWithCacheControl()));
  }

  private static void stubHtmlResponseWithStatusCode(int statusCode) {

    stubResponseWithContentTypeAndBody(statusCode, "text/html", "<h1>This is an HTML document</h1>");
  }

  private static void stubResponseWithContentTypeAndBody(int statusCode, String contentType, String body) {

    wireMockServer.stubFor(get(urlEqualTo(TEST_PATH))
        .willReturn(aResponse()
            .withStatus(statusCode)
            .withHeader(HttpHeaders.CONTENT_TYPE, contentType)
            .withBody(body)));
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

  private static void stub500VndErrorResponse() {

    stubResponseWithContentTypeAndBody(500, VndErrorResponseRenderer.CONTENT_TYPE, "{}");
  }

  private void stubEmptyResponseWithStatusCode(int statusCode) {

    wireMockServer.stubFor(get(urlEqualTo(TEST_PATH))
        .willReturn(aResponse()
            .withStatus(statusCode)));
  }

  protected void stubFaultyResponseWithStatusCode(int statusCode, Fault fault) {

    wireMockServer.stubFor(get(urlEqualTo(TEST_PATH))
        .willReturn(aResponse()
            .withStatus(statusCode)
            .withFault(fault)));
  }

  protected HalResponse loadResource() {

    String uri = testUrl;

    return createLoaderUnderTest().getHalResource(uri).blockingGet();
  }

  protected HalApiClientException loadResourceAndExpectClientException() {

    return loadResourceAndExpectClientException(testUrl);
  }

  protected HalApiClientException loadResourceAndExpectClientException(String url) {
    Single<HalResponse> rxResponse = createLoaderUnderTest().getHalResource(url);

    Throwable ex = catchThrowable(() -> rxResponse.blockingGet());

    assertThat(ex)
        .isInstanceOf(HalApiClientException.class)
        .hasMessageStartingWith("HAL client request to")
        .hasMessageContaining("has failed");

    return (HalApiClientException)ex;
  }

  @Test
  public void response_code_should_be_set_for_200_hal_response() throws Exception {

    stub200HalResponse();

    HalResponse response = loadResource();

    assertThat(response.getStatus())
        .isEqualTo(200);
  }

  @Test
  public void content_type_should_be_set_for_200_hal_response() throws Exception {

    stub200HalResponse();

    HalResponse response = loadResource();

    assertThat(response.getContentType())
        .isEqualTo(HalResource.CONTENT_TYPE);
  }

  @Test
  public void body_should_be_set_for_200_hal_response() throws Exception {

    stub200HalResponse();

    HalResponse response = loadResource();

    assertThat(response.getBody())
        .isNotNull();

    assertThat(response.getBody().getModel())
        .isEqualTo(createHalResource().getModel());
  }

  @Test
  public void maxAge_should_be_null_for_200_hal_response_without_cache_control() throws Exception {

    stub200HalResponseWithCacheControl();

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
  public void maxAge_should_be_found_if_seperated_in_multiple_cache_control_headers() throws Exception {

    stub200HalResponseWithCacheControl("public", "no-transform", "max-age=86400");

    HalResponse response = loadResource();

    assertThat(response.getMaxAge())
        .isEqualTo(86400);
  }

  @Test
  public void immutable_should_override_max_age() throws Exception {

    stub200HalResponseWithCacheControl("public", "max-age=86400", "immutable");

    HalResponse response = loadResource();

    assertThat(response.getMaxAge())
        .isEqualTo(Duration.ofDays(365).getSeconds());
  }

  @Test
  public void maxAge_should_be_ignored_if_it_contains_invalid_value() throws Exception {

    stub200HalResponseWithCacheControl("max-age=foo");

    HalResponse response = loadResource();

    assertThat(response.getMaxAge())
        .isNull();
  }

  @Test
  public void maxAge_should_be_properly_capped_to_max_int_value() throws Exception {

    stub200HalResponseWithCacheControl("max-age=" + (1000l + Integer.MAX_VALUE));

    HalResponse response = loadResource();

    assertThat(response.getMaxAge())
        .isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void status_code_should_be_present_in_HalApiClientException_for_non_ok_response() throws Exception {

    stubHtmlResponseWithStatusCode(503);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getStatusCode())
        .isEqualTo(503);
  }

  @Test
  public void content_type_should_be_set_for_500_vnd_error_response() throws Exception {

    stub500VndErrorResponse();

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getErrorResponse().getContentType())
        .isEqualTo(VndErrorResponseRenderer.CONTENT_TYPE);
  }

  @Test
  public void content_type_should_be_null_if_not_present_in_error_response() throws Exception {

    stubErrorResponseWithoutContentType(501);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getErrorResponse().getContentType())
        .isNull();
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
  public void error_body_should_be_null_in_HalApiClientException_for_non_ok_text_response() throws Exception {

    stubResponseWithContentTypeAndBody(500, "text/json", "500");

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
  public void status_code_should_be_200_in_HalApiClientException_for_failure_to_parse_json() throws Exception {

    stubHtmlResponseWithStatusCode(200);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getStatusCode())
        .isEqualTo(200);
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
        .hasMessage("An HTTP response with status code 200 was retrieved, but the body could not be successfully read and parsed as a JSON document")
        .hasRootCauseInstanceOf(IOException.class);
  }

  @Test
  public void cause_should_be_present_in_HalApiClientException_for_failure_to_parse_empty_string() throws Exception {

    stubEmptyResponseWithStatusCode(200);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex)
        .hasMessageContaining("has failed because the response body is malformed");

    assertThat(ex.getCause())
        .hasMessage("An HTTP response with status code 200 was retrieved, but the body could not be successfully read and parsed as a JSON document")
        .hasRootCauseMessage("The response body was completely empty (or consisted only of whitespace)");
  }

  @Test
  public void cause_should_be_present_in_HalApiClientException_for_malformed_200_response() throws Exception {

    stubFaultyResponseWithStatusCode(200, Fault.MALFORMED_RESPONSE_CHUNK);

    HalApiClientException ex = loadResourceAndExpectClientException();

    // the behaviour for this edge cases varies between implementations:
    // sometimes the response code is successfully extracted, sometimes not.
    // the only thing they have in common is that at some point, an Exception is caught
    assertThat(ex.getCause())
        .isNotNull();
  }

  @Test
  public void cause_should_be_present_in_HalApiClientException_for_connection_reset_on_200_response() throws Exception {

    stubFaultyResponseWithStatusCode(200, Fault.CONNECTION_RESET_BY_PEER);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getStatusCode())
        .isEqualTo(null);

    assertThat(ex)
        .hasMessageContaining("has failed before a status code was available")
        .hasRootCauseInstanceOf(IOException.class);
  }

  @Test
  public void cause_should_be_present_in_HalApiClientException_for_random_data_on_200_response() throws Exception {

    stubFaultyResponseWithStatusCode(200, Fault.RANDOM_DATA_THEN_CLOSE);

    HalApiClientException ex = loadResourceAndExpectClientException();

    assertThat(ex.getStatusCode())
        .isEqualTo(null);

    assertThat(ex)
        .hasMessageContaining("has failed before a status code was available");
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

package io.wcm.caravan.rhyme.awslambda.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.net.HttpHeaders;

import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;
import io.wcm.caravan.rhyme.awslambda.api.LambdaResourceRouting;

/**
 * AWS Lambda specific implementation of {@link RhymeDocsSupport} that enables
 * generation of "curies" links to the HAL responses. The generated HTML documentation
 * is embedded and served when a request with the documentation base URI is handled by
 * {@link LambdaRhymeImpl#createResourceAndRenderResponse(LambdaResourceRouting)}
 */
public class LambdaRhymeDocsSupport implements RhymeDocsSupport {

  private static final String BASE_PATH = "/docs/rhyme/api/";

  private final APIGatewayProxyRequestEvent request;
  private final String baseUrl;

  /**
   * @param request the incoming request
   */
  public LambdaRhymeDocsSupport(APIGatewayProxyRequestEvent request) {
    this.request = request;
    this.baseUrl = LambdaRhymeImpl.getBaseUrl(request) + BASE_PATH;
  }

  @Override
  public String getRhymeDocsBaseUrl() {
    return baseUrl;
  }

  @Override
  public InputStream openResourceStream(String resourcePath) throws IOException {

    return LambdaRhymeDocsSupport.class.getResourceAsStream(resourcePath);
  }

  @Override
  public boolean isFragmentAppendedToCuriesLink() {
    return false;
  }

  boolean isRhymeDocsRequest() {

    return request.getPath().startsWith(BASE_PATH);
  }

  APIGatewayProxyResponseEvent createHtmlResponse() {

    String fileName = StringUtils.substringAfter(request.getPath(), BASE_PATH);

    String htmlString = RhymeDocsSupport.loadGeneratedHtml(this, fileName);

    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, "text/html");

    return new APIGatewayProxyResponseEvent()
        .withStatusCode(200)
        .withHeaders(headers)
        .withBody(htmlString);
  }
}

package io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;

import com.google.common.collect.LinkedHashMultimap;

import io.wcm.caravan.rhyme.aem.impl.HalApiServlet;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;

public class ServletIntegrationTestSupport {


  public static HalApiClient createHalApiClient(HalApiServlet servlet, ResourceResolver resolver) {

    HttpClientSupport clientSupport = new HttpClientSupport() {

      @Override
      public void executeGetRequest(URI uri, HttpClientCallback callback) {

        MockSlingHttpServletRequest request = createRequestWithPathInfo(resolver, uri);

        MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        try {
          servlet.service(request, response);

          LinkedHashMultimap<String, String> headers = LinkedHashMultimap.create();

          callback.onHeadersAvailable(response.getStatus(), headers.asMap());

          callback.onBodyAvailable(new ByteArrayInputStream(response.getOutput()));
        }
        catch (ServletException | IOException | RuntimeException ex) {

          callback.onExceptionCaught(ex);
        }
      }
    };

    HalResourceLoader resourceLoader = HalResourceLoader.create(clientSupport);

    return HalApiClient.create(resourceLoader);
  }

  private static MockSlingHttpServletRequest createRequestWithPathInfo(ResourceResolver resolver, URI uri) {

    MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(resolver);

    MockRequestPathInfo pathInfo = (MockRequestPathInfo)request.getRequestPathInfo();

    String[] parts = StringUtils.split(uri.getPath(), ".");

    String resourcePath = parts[0];
    String selectors = Stream.of(parts).skip(1).limit(parts.length - 2).collect(Collectors.joining("."));
    String extension = parts[parts.length - 1];

    pathInfo.setResourcePath(resourcePath);
    pathInfo.setSelectorString(selectors);
    pathInfo.setExtension(extension);

    request.setResource(resolver.getResource(resourcePath));
    if (StringUtils.isNotBlank(uri.getRawQuery())) {
      request.setQueryString(uri.getRawQuery());
    }
    return request;
  }
}

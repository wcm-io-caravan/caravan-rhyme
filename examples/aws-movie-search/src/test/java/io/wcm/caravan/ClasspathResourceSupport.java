package io.wcm.caravan;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;

public class ClasspathResourceSupport implements HttpClientSupport {

  private Map<String, String> uriToPathMapping = new HashMap<>();

  public void addStubResponseMapping(String requestUri, String testResourcePath) {

    uriToPathMapping.put(requestUri, testResourcePath);
  }

  @Override
  public void executeGetRequest(URI uri, HttpClientCallback callback) {

    try {
      int status = 200;
      InputStream stream = getStubResponseFromTestResources(uri);
      if (stream == null) {
        status = 404;
        stream = new ByteArrayInputStream(new byte[0]);
      }

      callback.onHeadersAvailable(status, Collections.emptyMap());
      callback.onBodyAvailable(stream);
    }
    catch (Exception ex) {

      callback.onExceptionCaught(ex);
    }
  }

  private InputStream getStubResponseFromTestResources(URI uri) {

    String resourcePath = uriToPathMapping.get(uri.toString());
    if (resourcePath == null) {
      return null;
    }

    return getClass().getResourceAsStream(resourcePath);
  }
}

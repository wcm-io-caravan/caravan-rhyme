package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.WebRequest;

@Component
@RequestScope
public class CompanyApiStickyParameters {

  static final String USE_EMBEDDED_RESOURCES = "useEmbeddedResources";
  static final String USE_FINGERPRINTING = "useFingerprinting";


  private final Map<String, Boolean> booleanParameters = new LinkedHashMap<>();

  CompanyApiStickyParameters(@Autowired WebRequest request) {

    addBooleanParamIfPresentInRequest(request, USE_EMBEDDED_RESOURCES);
    addBooleanParamIfPresentInRequest(request, USE_FINGERPRINTING);
  }

  private void addBooleanParamIfPresentInRequest(WebRequest request, String name) {

    String value = request.getParameter(name);
    if (StringUtils.isNotBlank(value)) {
      booleanParameters.put(name, Boolean.valueOf(value));
    }
  }

  public Boolean getUseEmbeddedResources() {

    return booleanParameters.getOrDefault(USE_EMBEDDED_RESOURCES, true);
  }

  public Boolean getUseFingerprinting() {

    return booleanParameters.getOrDefault(USE_FINGERPRINTING, true);
  }

  Map<String, ? extends Object> getParameterMap() {

    return booleanParameters;
  }
}

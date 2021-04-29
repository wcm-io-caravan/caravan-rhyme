package io.wcm.caravan.rhyme.aem.integration.impl;

import static org.apache.sling.api.servlets.ServletResolverConstants.DEFAULT_RESOURCE_TYPE;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_SELECTORS;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.rhyme.aem.api.AemAsset;
import io.wcm.caravan.rhyme.aem.api.AemPage;
import io.wcm.caravan.rhyme.aem.api.AemRendition;
import io.wcm.caravan.rhyme.aem.api.SlingResource;
import io.wcm.caravan.rhyme.aem.impl.resources.AemAssetImpl;
import io.wcm.caravan.rhyme.aem.impl.resources.AemPageImpl;
import io.wcm.caravan.rhyme.aem.impl.resources.AemRenditionImpl;
import io.wcm.caravan.rhyme.aem.impl.resources.SlingResourceImpl;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;


@Component(service = Servlet.class, property = {
    SERVICE_DESCRIPTION + "=JSON Servlet to read the data from the external webservice",
    SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
    SLING_SERVLET_RESOURCE_TYPES + "=" + DEFAULT_RESOURCE_TYPE,
    SLING_SERVLET_SELECTORS + "=" + HalApiServlet.HAL_API_SELECTOR })
public class HalApiServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -7540592969300324670L;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static final String HAL_API_SELECTOR = "halapi";


  protected Map<String, Class<? extends LinkableResource>> getSelectorModelClassMap() {

    return new ImmutableMap.Builder<String, Class<? extends LinkableResource>>()
        .put(AemPageImpl.SELECTOR, AemPage.class)
        .put(SlingResourceImpl.SELECTOR, SlingResource.class)
        .put(AemAssetImpl.SELECTOR, AemAsset.class)
        .put(AemRenditionImpl.SELECTOR, AemRendition.class)
        .build();
  }


  @Override
  protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
      throws ServletException, IOException {

    SlingRhyme rhyme = request.adaptTo(SlingRhyme.class);
    if (rhyme == null) {
      throw new RuntimeException("request could not be adapted to " + SlingRhymeImpl.class);
    }

    Map<String, Class<? extends LinkableResource>> selectorModelClassMap = getSelectorModelClassMap();

    HalResponse halResponse = rhyme.renderRequestedResource(selectorModelClassMap);

    writeHalResponse(halResponse, response);
  }


  private void writeHalResponse(HalResponse halResponse, SlingHttpServletResponse servletResponse)
      throws IOException, JsonGenerationException, JsonMappingException {

    servletResponse.setCharacterEncoding(Charsets.UTF_8.name());
    servletResponse.setContentType(halResponse.getContentType());
    servletResponse.setStatus(halResponse.getStatus());
    if (halResponse.getMaxAge() != null) {
      servletResponse.setHeader("cache-control", "max-age=" + halResponse.getMaxAge());
    }

    OBJECT_MAPPER.writeValue(servletResponse.getWriter(), halResponse.getBody().getModel());
  }

}

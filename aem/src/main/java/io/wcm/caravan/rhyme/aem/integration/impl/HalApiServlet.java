package io.wcm.caravan.rhyme.aem.integration.impl;

import static org.apache.sling.api.servlets.ServletResolverConstants.DEFAULT_RESOURCE_TYPE;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;


@Component(service = Servlet.class, property = {
    SERVICE_DESCRIPTION + "=Servlet to render HAL responses using the Rhyme framework",
    SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
    SLING_SERVLET_RESOURCE_TYPES + "=" + DEFAULT_RESOURCE_TYPE,
    SLING_SERVLET_EXTENSIONS + "=" + HalApiServlet.EXTENSION })
public class HalApiServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -7540592969300324670L;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static final String EXTENSION = "rhyme";

  static final String QUERY_PARAM_EMBED_METADATA = "embedMetadata";

  @Reference
  private ResourceSelectorRegistry registry;

  @Override
  protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
      throws ServletException, IOException {

    SlingRhymeImpl rhyme = request.adaptTo(SlingRhymeImpl.class);

    try {
      ensureThatRequestIsValid(request);

      LinkableResource requestedResource = adaptRequestedResourceToSlingModel(request, rhyme);

      HalResponse halResponse = rhyme.renderResource(requestedResource);

      writeHalResponse(request, halResponse, response);
    }
    catch (RuntimeException ex) {

      HalResponse errorResponse = rhyme.renderVndErrorResponse(ex);

      writeHalResponse(request, errorResponse, response);
    }
  }

  private void ensureThatRequestIsValid(SlingHttpServletRequest request) {

    Resource resource = request.getResource();

    if (Resource.RESOURCE_TYPE_NON_EXISTING.equals(resource.getResourceType())) {
      throw new HalApiServerException(HttpStatus.SC_NOT_FOUND,
          "There does not exist any resource at " + resource.getPath());
    }
  }

  private LinkableResource adaptRequestedResourceToSlingModel(SlingHttpServletRequest request, SlingRhymeImpl rhyme) {

    List<String> selectors = Arrays.asList(request.getRequestPathInfo().getSelectors());

    Class<? extends LinkableResource> modelClass = registry.getModelClassForSelectors(selectors);

    return rhyme.adaptResource(request.getResource(), modelClass);
  }

  private void writeHalResponse(SlingHttpServletRequest request, HalResponse halResponse, SlingHttpServletResponse servletResponse)
      throws IOException, JsonGenerationException, JsonMappingException {

    servletResponse.setContentType(halResponse.getContentType());
    servletResponse.setCharacterEncoding(Charsets.UTF_8.name());
    servletResponse.setStatus(halResponse.getStatus());
    if (halResponse.getMaxAge() != null) {
      servletResponse.setHeader("cache-control", "max-age=" + halResponse.getMaxAge());
    }

    HalResource responseBody = halResponse.getBody();

    if (shouldMetadataBeRemoved(request)) {
      responseBody.removeEmbedded("caravan:metadata");
    }

    OBJECT_MAPPER.writeValue(servletResponse.getWriter(), responseBody.getModel());
  }

  private boolean shouldMetadataBeRemoved(SlingHttpServletRequest request) {
    return !request.getParameterMap().containsKey(QUERY_PARAM_EMBED_METADATA);
  }

}

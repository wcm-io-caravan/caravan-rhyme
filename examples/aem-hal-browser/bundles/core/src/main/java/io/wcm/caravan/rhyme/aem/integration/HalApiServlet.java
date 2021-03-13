package io.wcm.caravan.rhyme.aem.integration;

import static org.apache.sling.api.servlets.ServletResolverConstants.DEFAULT_RESOURCE_TYPE;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_SELECTORS;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;


@Component(service = Servlet.class, property = {
    SERVICE_DESCRIPTION + "=JSON Servlet to read the data from the external webservice",
    SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
    SLING_SERVLET_RESOURCE_TYPES + "=" + DEFAULT_RESOURCE_TYPE,
    SLING_SERVLET_SELECTORS + "=" + HalApiServlet.HAL_API_SELECTOR })
public class HalApiServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -7540592969300324670L;

  static final String HAL_API_SELECTOR = "halapi";

  @Override
  protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
      throws ServletException, IOException {

    response.setContentType("text/plain");
    response.getWriter().write("Foo!");

  }

}

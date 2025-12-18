/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.osgi.sampleservice.impl.jaxrs;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Servlet filter to exclude specific paths from being handled by the JAX-RS application.
 * This filter checks if the request path starts with any of the configured excluded paths,
 * and if so, returns a 404 error before the JAX-RS application can handle it.
 */
@Component(service = Filter.class, property = {
    "osgi.http.whiteboard.filter.pattern=/*",
    "osgi.http.whiteboard.filter.name=ExampleServicePathExclusionFilter",
    "osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=*)"
})
@Designate(ocd = PathExclusionFilter.Config.class)
public class PathExclusionFilter implements Filter {

  @ObjectClassDefinition(
      name = "Caravan Rhyme Example Service - Path Exclusion Filter",
      description = "Filter to exclude specific paths from being handled by the JAX-RS application"
  )
  @interface Config {

    @AttributeDefinition(
        name = "Excluded Paths",
        description = "Paths that should NOT be handled by the JAX-RS application. "
            + "Requests starting with these paths will receive a 404 response."
    )
    String[] excludedPaths() default {
        "/system",
        "/admin",
        "/bin",
        "/content"
    };

    @AttributeDefinition(
        name = "Enabled",
        description = "Enable or disable the path exclusion filter"
    )
    boolean enabled() default true;
  }

  private String[] excludedPaths;
  private boolean enabled;

  @Activate
  @Modified
  protected void activate(Config config) {
    this.excludedPaths = config.excludedPaths();
    this.enabled = config.enabled();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (!enabled || !(request instanceof HttpServletRequest)) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletRequest httpRequest = (HttpServletRequest)request;
    HttpServletResponse httpResponse = (HttpServletResponse)response;
    String requestPath = httpRequest.getRequestURI();

    // Remove context path if present
    String contextPath = httpRequest.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && requestPath.startsWith(contextPath)) {
      requestPath = requestPath.substring(contextPath.length());
    }

    // Check if the request path starts with any excluded path
    for (String excludedPath : excludedPaths) {
      if (excludedPath != null && !excludedPath.isEmpty() && requestPath.startsWith(excludedPath)) {
        httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND,
            "Path is excluded from JAX-RS application: " + excludedPath);
        return;
      }
    }

    // Path is not excluded, continue with the filter chain
    chain.doFilter(request, response);
  }

  @Override
  public void init(javax.servlet.FilterConfig filterConfig) throws ServletException {
    // No initialization required
  }

  @Override
  public void destroy() {
    // No cleanup required
  }
}

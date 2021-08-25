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
package io.wcm.caravan.rhyme.aem.integration.impl.docs;

import static org.apache.sling.api.servlets.ServletResolverConstants.DEFAULT_RESOURCE_TYPE;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_SELECTORS;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;

@Component(service = Servlet.class, property = {
    SERVICE_DESCRIPTION + "=Servlet to render HAL responses using the Rhyme framework",
    SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
    SLING_SERVLET_RESOURCE_TYPES + "=" + DEFAULT_RESOURCE_TYPE,
    SLING_SERVLET_EXTENSIONS + "=" + RhymeDocsServlet.EXTENSION,
    SLING_SERVLET_SELECTORS + "=" + RhymeDocsServlet.SELECTOR })
public class RhymeDocsServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 6259103543958524803L;

  private static final Logger log = LoggerFactory.getLogger(RhymeDocsServlet.class);

  static final String SELECTOR = "rhymedocs";
  static final String EXTENSION = "html";

  @Reference
  private RhymeDocsOsgiBundleSupport rhymeDocs;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

    try {
      String fileName = validateAndGetFileNameFromSuffix(request.getRequestPathInfo());

      String html = RhymeDocsSupport.loadGeneratedHtml(rhymeDocs, fileName);

      writeHtmlResponse(html, response);
    }
    catch (HalApiServerException ex) {
      log.warn("Failed to render documentation from file name {}", request.getRequestPathInfo().getSuffix(), ex);
      response.setStatus(ex.getStatusCode());
    }
  }

  private String validateAndGetFileNameFromSuffix(RequestPathInfo pathInfo) {

    String suffix = pathInfo.getSuffix();

    if (StringUtils.isBlank(suffix) || suffix.contains("..") || !suffix.startsWith("/")) {
      throw new HalApiServerException(HttpStatus.SC_BAD_REQUEST, "Invalid or no file name was provide in the suffix " + suffix);
    }

    return StringUtils.substringAfter(suffix, "/");
  }

  private void writeHtmlResponse(String html, SlingHttpServletResponse response) throws IOException {

    response.setStatus(HttpStatus.SC_OK);
    response.setContentType("text/html; charset=UTF-8");

    IOUtils.write(html, response.getWriter());
  }

}

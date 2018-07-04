/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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
package io.wcm.caravan.hal.api.server.jaxrs;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Path;

import org.apache.commons.lang3.StringUtils;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;
import com.google.common.base.Preconditions;

import io.wcm.caravan.hal.api.common.LinkableResource;
import io.wcm.caravan.hal.api.server.impl.reflection.JaxRsReflectionUtils;
import io.wcm.caravan.hal.resource.Link;

public class JaxRsLinkBuilder {

  private final String baseUrl;
  private final LinkableResource resource;

  /**
   * @param baseUrl the base path (or full URI) for which this bundle is registered
   * @param targetResource the resource instance for which a link should be generated
   */
  public JaxRsLinkBuilder(String baseUrl, LinkableResource targetResource) {

    Preconditions.checkArgument(StringUtils.isNotBlank(baseUrl), "A contextPath must be provided");
    Preconditions.checkArgument(targetResource != null, "the targetResource must not be null");

    this.baseUrl = baseUrl;
    this.resource = targetResource;
  }


  public Link build() {

    // first build the resource path (from the context path and the resource's @Path annotation)
    String resourcePath = buildResourcePath();

    // create a URI template from this path
    UriTemplateBuilder uriTemplateBuilder = UriTemplate.buildFromTemplate(resourcePath);

    // get parameter values from resource, and append query parameter names to the URI template
    Map<String, Object> parametersThatAreSet = collectAndAppendParameters(uriTemplateBuilder);

    // finally build the URI template
    String uriTemplate = uriTemplateBuilder.build().getTemplate();

    // then expand the uri template partially (i.e. keep variables that are null in the resource implementation)
    String partiallyExpandedUriTemplate = UriTemplate.expandPartial(uriTemplate, parametersThatAreSet);

    // and finally construct and return the link
    return new Link(partiallyExpandedUriTemplate);
  }


  /**
   * build the resource path template (by concatenating the baseUrl of the service,
   * and the value of the linked resource's @Path annotation (that may contain path parameter variables)
   * @return an absolute path template to the resource
   */
  private String buildResourcePath() {

    Path pathAnnotation = resource.getClass().getAnnotation(Path.class);
    Preconditions.checkNotNull(pathAnnotation,
        "A @Path annotation must be present on the resource implementation class (" + resource.getClass().getName() + ")");

    String pathTemplate = pathAnnotation.value();

    String resourcePath = baseUrl;
    if (StringUtils.isNotBlank(pathTemplate)) {
      resourcePath += pathTemplate;
    }

    return resourcePath;
  }

  private Map<String, Object> collectAndAppendParameters(UriTemplateBuilder uriTemplateBuilder) {

    // use reflection to find the names and values of all fields annotated with JAX-RS @PathParam and @QueryParam annotations
    Map<String, Object> pathParams = JaxRsReflectionUtils.getPathParameterMap(resource);
    Map<String, Object> queryParams = JaxRsReflectionUtils.getQueryParameterMap(resource);

    // add all available query parameters to the URI template
    String[] queryParamNames = queryParams.keySet().stream().toArray(String[]::new);
    if (queryParamNames.length > 0) {
      uriTemplateBuilder.query(queryParamNames);
    }

    // now merge the template variables from the query and path parameters
    Map<String, Object> parameters = new HashMap<>();
    parameters.putAll(pathParams);
    parameters.putAll(queryParams);

    // and filter only the parameters that have a non-null value
    Map<String, Object> parametersThatAreSet = parameters.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .collect(Collectors.toMap(entry -> entry.getKey(), e -> e.getValue()));

    return parametersThatAreSet;
  }

}

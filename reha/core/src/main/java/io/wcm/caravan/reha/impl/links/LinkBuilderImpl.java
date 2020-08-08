/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
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
package io.wcm.caravan.reha.impl.links;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.client.HalApiDeveloperException;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.api.server.LinkBuilder;
import io.wcm.caravan.reha.api.server.LinkBuilderSupport;

/**
 * Implementation of {@link LinkBuilder} that constructs links and link templates using the {@link UriTemplateBuilder}.
 * The actual discovery of parameters and their values is delegated to a {@link LinkBuilderSupport} instance to be
 * supplied in the constructor.
 */
public class LinkBuilderImpl implements LinkBuilder {

  private final String baseUrl;

  private final LinkBuilderSupport support;

  private final Map<String, Object> additionalParameters = new LinkedHashMap<>();

  /**
   * @param baseUrl the base path (or full URI) for which the current service bundle is registered
   * @param support implements the logic of extracting resource path and template variables from a server-side
   *          resource instance
   */
  public LinkBuilderImpl(String baseUrl, LinkBuilderSupport support) {

    Preconditions.checkNotNull(baseUrl, "A baseUrl must be provided");
    Preconditions.checkNotNull(support, "a " + LinkBuilderSupport.class.getSimpleName() + " must be specified");

    this.baseUrl = baseUrl;

    this.support = support;
  }

  @Override
  public LinkBuilder withAdditionalParameters(Map<String, Object> parameters) {
    this.additionalParameters.putAll(parameters);
    return this;
  }

  /**
   * @param resource the resource instance for which a link should be generated
   * @return a Link instance where the href property is already set
   */
  @Override
  public Link buildLinkTo(LinkableResource resource) {

    // first build the resource path (from the context path and the resource's @Path annotation)
    String resourcePath = buildResourcePath(resource);

    // create a URI template from this path
    UriTemplateBuilder uriTemplateBuilder = UriTemplate.buildFromTemplate(resourcePath);

    // get parameter values from resource, and append query parameter names to the URI template
    Map<String, Object> parametersThatAreSet = collectAndAppendParameters(uriTemplateBuilder, resource);

    // finally build the URI template
    String uriTemplate = uriTemplateBuilder.build().getTemplate();

    // then expand the uri template partially (i.e. keep variables that are null in the resource implementation)
    String partiallyExpandedUriTemplate = UriTemplate.expandPartial(uriTemplate, parametersThatAreSet);

    // and finally construct and return the link
    return new Link(partiallyExpandedUriTemplate);
  }

  /**
   * build the resource path template by concatenating the baseUrl of the service,
   * and the value of the linked resource's relative path template (that may contain path parameter variables)
   * @param resource target resource for the link
   * @return an absolute path template to the resource
   */
  private String buildResourcePath(LinkableResource resource) {

    String pathTemplate = support.getResourcePathTemplate(resource);

    String resourcePath = baseUrl;
    if (StringUtils.isNotBlank(pathTemplate)) {
      resourcePath += pathTemplate;
    }

    return resourcePath;
  }

  /**
   * @param uriTemplateBuilder to which append all query parameter template variables
   * @param resource target resource for the link
   * @return a map of all non-null path and query variables that should be expanded in the template
   */
  private Map<String, Object> collectAndAppendParameters(UriTemplateBuilder uriTemplateBuilder, LinkableResource resource) {

    // use reflection to find the names and values of all fields annotated with JAX-RS @PathParam and @QueryParam annotations
    Map<String, Object> pathParams = support.getPathParameters(resource);
    Map<String, Object> queryParams = support.getQueryParameters(resource);

    // make sure that
    ensureThatNamesAreUnique(pathParams, queryParams, "Duplicate names detected in path and query params");
    ensureThatNamesAreUnique(pathParams, additionalParameters, "Duplicate names detected in path and additional params");
    ensureThatNamesAreUnique(queryParams, additionalParameters, "Duplicate names detected in query and additional params");

    // add all parameters specified in #withAdditionalParameters to the query parameter map
    if (!additionalParameters.isEmpty()) {
      queryParams = new LinkedHashMap<>(queryParams);
      queryParams.putAll(additionalParameters);
    }

    // add all available query parameters to the URI template
    if (!queryParams.isEmpty()) {
      String[] queryParamNames = queryParams.keySet().stream().toArray(String[]::new);
      uriTemplateBuilder.query(queryParamNames);
    }

    // now merge the template variables from the query and path parameters
    Map<String, Object> parametersThatAreSet = Stream.concat(queryParams.entrySet().stream(), pathParams.entrySet().stream())
        // and filter only the parameters that have a non-null value
        .filter(entry -> entry.getValue() != null)
        .collect(Collectors.toMap(entry -> entry.getKey(), e -> e.getValue()));

    return parametersThatAreSet;
  }

  private static void ensureThatNamesAreUnique(Map<String, Object> params1, Map<String, Object> params2, String message) {

    Set<String> commonKeys = Sets.intersection(params1.keySet(), params2.keySet());
    if (!commonKeys.isEmpty()) {
      String names = commonKeys.stream().collect(Collectors.joining(","));
      throw new HalApiDeveloperException(message + ": " + names);
    }
  }

}

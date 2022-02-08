package io.wcm.caravan.rhyme.awslambda.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.awslambda.api.LambdaLinkBuilder;

/**
 * Implements the link (template) building using a {@link UriTemplateBuilder}
 */
class LambdaLinkBuilderImpl implements LambdaLinkBuilder {

  private final List<String> variableNames = new ArrayList<>();
  private final Map<String, Object> nonNullVariableValues = new LinkedHashMap<>();

  private final String absolutePath;

  LambdaLinkBuilderImpl(String absolutePath) {

    this.absolutePath = absolutePath;
  }

  @Override
  public LambdaLinkBuilder addQueryVariable(String name, Object value) {

    variableNames.add(name);

    if (value != null) {
      nonNullVariableValues.put(name, value);
    }
    return this;
  }

  @Override
  public Link build() {

    if (variableNames.isEmpty()) {
      return new Link(absolutePath);
    }

    UriTemplate template = UriTemplate.buildFromTemplate(absolutePath)
        .query(variableNames.toArray(new String[0]))
        .build();

    String expandedTemplate = template
        .set(nonNullVariableValues)
        .expandPartial();

    return new Link(expandedTemplate);
  }
}

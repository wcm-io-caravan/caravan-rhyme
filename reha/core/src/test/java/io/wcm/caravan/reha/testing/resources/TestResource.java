/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.reha.testing.resources;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

public class TestResource {

  public static final String FLAG = "flag";
  public static final String TEXT = "text";
  public static final String NUMBER = "number";
  public static final String ARRAY = "array";

  private final TestResourceTree tree;

  private final HalResource halResource;

  private Integer maxAge;
  private Integer status = 200;

  TestResource(TestResourceTree tree) {
    this.tree = tree;
    this.halResource = new HalResource();
  }

  public TestResource createLinked(String relation) {
    return createLinked(relation, null);
  }

  public TestResource createLinked(String relation, String name) {
    TestResource linkedResource = new TestResource(tree);
    tree.assignSelfLink(linkedResource);
    halResource.addLinks(relation, new Link(linkedResource.getUrl()).setName(name));
    return linkedResource;
  }

  public TestResource createEmbedded(String relation) {
    TestResource embeddedResource = new TestResource(tree);
    halResource.addEmbedded(relation, embeddedResource.asHalResource());
    return embeddedResource;
  }

  public Link addLinkTo(String relation, TestResource other) {
    String otherUrl = other.getUrl();
    assertThat(otherUrl).isNotNull();
    Link link = new Link(otherUrl);
    halResource.addLinks(relation, link);
    return link;
  }

  public HalResource asHalResource() {
    return this.halResource;
  }

  public String getUrl() {
    return halResource.getLink().getHref();
  }

  public TestResource setFlag(Boolean value) {
    getJson().put(FLAG, value);
    return this;
  }

  public TestResource setText(String value) {
    getJson().put(TEXT, value);
    return this;
  }

  public TestResource setNumber(Integer value) {
    getJson().put(NUMBER, value);
    return this;
  }

  public TestResource setArray(String... values) {

    ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
    for (String value : values) {
      arrayNode.add(value);
    }
    getJson().set(ARRAY, arrayNode);
    return this;
  }

  public ObjectNode getJson() {
    return halResource.getModel();
  }


  public Integer getMaxAge() {
    return this.maxAge;
  }

  public TestResource withMaxAge(int value) {
    this.maxAge = value;
    return this;
  }

  public Integer getStatus() {
    return this.status;
  }

  public TestResource withStatus(Integer value) {
    this.status = value;
    return this;
  }

}

/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
package io.wcm.caravan.rhyme.jaxrs.impl;

import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;

import javax.ws.rs.core.MultivaluedHashMap;

import org.apache.commons.io.Charsets;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.HalResource;


public class HalResourceMessageBodyWriterTest {

  private final HalResourceMessageBodyWriter messageBodyWriter = new HalResourceMessageBodyWriter();

  @Test
  public void isWriteable_should_return_true_for_HalResource() throws Exception {

    boolean writeable = messageBodyWriter.isWriteable(HalResource.class, null, new Annotation[0], WILDCARD_TYPE);

    assertThat(writeable).isTrue();
  }

  @Test
  public void isWriteable_should_return_fals_for_other_class() throws Exception {

    boolean writeable = messageBodyWriter.isWriteable(JsonNode.class, null, new Annotation[0], WILDCARD_TYPE);

    assertThat(writeable).isFalse();
  }

  @Test
  public void getSize_should_not_be_able_do_termine_size() throws Exception {

    long size = messageBodyWriter.getSize(new HalResource(), HalResource.class, null, new Annotation[0], WILDCARD_TYPE);

    assertThat(size).isEqualTo(-1);
  }

  @Test
  public void writeTo_should_serialize_json() throws Exception {

    ObjectNode json = JsonNodeFactory.instance.objectNode().put("föö", "bär");
    HalResource hal = new HalResource(json);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    messageBodyWriter.writeTo(hal, HalResource.class, null, new Annotation[0], WILDCARD_TYPE, new MultivaluedHashMap<>(), baos);

    String jsonString = baos.toString(Charsets.UTF_8.name());

    assertThat(jsonString).isEqualTo(json.toString());
  }

}

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
package io.wcm.caravan.reha.api.common;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;


public class HalResponseTest {

  @Test
  public void withBody_should_allow_null_json_nodes() throws Exception {

    JsonNode jsonNode = null;

    HalResponse hal = new HalResponse().withBody(jsonNode);

    Assertions.assertThat(hal.getBody()).isNull();
  }

  @Test
  public void withBody_should_wrap_json_node() throws Exception {

    JsonNode jsonNode = JsonNodeFactory.instance.objectNode();

    HalResponse hal = new HalResponse().withBody(jsonNode);

    Assertions.assertThat(hal.getBody().getModel()).isSameAs(jsonNode);
  }
}

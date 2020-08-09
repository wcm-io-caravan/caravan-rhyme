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
package io.wcm.caravan.reha.impl.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.reha.testing.LinkableTestResource;


public class HalApiReflectionUtilsTest {

  private static final HalApiAnnotationSupport ANNOTATION_SUPPORT = new DefaultHalApiTypeSupport();

  private String getSimpleClassName(LinkableResource resourceImpl) {

    return HalApiReflectionUtils.getSimpleClassName(resourceImpl, ANNOTATION_SUPPORT);
  }

  static class InnerTestResourceImpl implements LinkableTestResource {

    @Override
    public Link createLink() {
      return new Link("/foo");
    }

  }

  @Test
  public void getSimpleClassName_should_return_inner_class_name() throws Exception {

    InnerTestResourceImpl resourceImpl = new InnerTestResourceImpl();

    String name = getSimpleClassName(resourceImpl);

    assertThat(name).isEqualTo("InnerTestResourceImpl");
  }


  @Test
  public void getSimpleClassName_should_return_a_readable_representation_for_anonymous_classes() throws Exception {

    LinkableTestResource resourceImpl = new LinkableTestResource() {

      @Override
      public Link createLink() {
        return new Link("/foo");
      }
    };

    String name = getSimpleClassName(resourceImpl);

    assertThat(name).isEqualTo("anonymous TestResource (defined in HalApiReflectionUtilsTest)");
  }
}

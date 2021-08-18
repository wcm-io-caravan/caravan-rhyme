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
package io.wcm.caravan.maven.plugins.rhymedocs.interfaces;

import java.util.List;
import java.util.Optional;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;

@HalApiInterface
public interface ResourceWithNestedProperties {

  @ResourceState
  Optional<NestedProperties> getState();

  class NestedProperties {

    public InnerBeanProperties bean;

    public InnerFieldProperties field;

    public List<InnerBeanProperties> beanList;

  }

  class InnerBeanProperties {

    private InnerFieldProperties innerField;

    private List<InnerFieldProperties> fieldList;

    public InnerFieldProperties getInnerField() {
      return this.innerField;
    }

    public void setInnerField(InnerFieldProperties innerField) {
      this.innerField = innerField;
    }

    public List<InnerFieldProperties> getFieldList() {
      return this.fieldList;
    }

    public void setFieldList(List<InnerFieldProperties> fieldList) {
      this.fieldList = fieldList;
    }
  }

  class InnerFieldProperties {

    public String foo;
  }

}

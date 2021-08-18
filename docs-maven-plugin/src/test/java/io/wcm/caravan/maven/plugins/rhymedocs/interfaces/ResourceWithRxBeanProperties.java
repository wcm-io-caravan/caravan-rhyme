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

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;

@HalApiInterface
public interface ResourceWithRxBeanProperties {

  @ResourceState
  Single<BeanProperties> getState();

  public static class BeanProperties {

    private String foo;
    private Integer bar;
    private List<Boolean> list;

    public String getFoo() {
      return this.foo;
    }

    /**
     * Javadoc for #getBar()
     * @return
     */
    public Integer getBar() {
      return this.bar;
    }

    /**
     * @return a list of boolean flags
     */
    public List<Boolean> getList() {
      return this.list;
    }

    public void setFoo(String foo) {
      this.foo = foo;
    }

    public void setBar(Integer bar) {
      this.bar = bar;
    }

    public void setList(List<Boolean> list) {
      this.list = list;
    }

  }
}

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
package io.wcm.caravan.rhyme.aem.impl;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

@Component(service = ResourceLoaderRegistry.class)
public class ResourceLoaderRegistry {

  @Reference(cardinality = ReferenceCardinality.MULTIPLE,
      policy = ReferencePolicy.STATIC,
      policyOption = ReferencePolicyOption.GREEDY)
  private List<HalResourceLoader> resourceLoaders;

  private ImplementationPickingHalResourceLoader pickingLoader;

  void activate() {
    pickingLoader = new ImplementationPickingHalResourceLoader();
  }

  public HalResourceLoader getResourceLoader() {

    return pickingLoader;
  }

  class ImplementationPickingHalResourceLoader implements HalResourceLoader {

    @Override
    public Single<HalResponse> getHalResource(String uri) {

      if (resourceLoaders.isEmpty()) {
        String msg = "No OSGi services implementing " + HalResourceLoader.class + " are running in this container";
        return Single.error(new HalApiClientException(msg, null, uri, null));
      }

      return resourceLoaders.get(0).getHalResource(uri);
    }

  }
}

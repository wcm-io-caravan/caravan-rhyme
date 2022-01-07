/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
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
package io.wcm.caravan.rhyme.impl;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.client.HalApiClientBuilder;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.api.server.HalResponseRendererBuilder;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

public final class RhymeDirector {

  private RhymeDirector() {

  }

  public static HalApiClientBuilder buildClient() {
    return new HalApiClientBuilderImpl();
  }

  public static HalResponseRendererBuilder buildRenderer() {
    return new HalResponseRenderBuilderImpl();
  }

  public static RhymeBuilder buildRhyme() {
    return new RhymeBuilderImpl();
  }

  public static RhymeBuilder buildRhyme(HalResourceLoader loader) {
    return new RhymeBuilderImpl().withResourceLoader(loader);
  }

  static class HalApiClientBuilderImpl extends CommonRhymeBuilderImpl<HalApiClientBuilder> implements HalApiClientBuilder {

    @Override
    public HalApiClient build() {
      return buildApiClient();
    }

  }

  static class HalResponseRenderBuilderImpl extends CommonRhymeBuilderImpl<HalResponseRendererBuilder> implements HalResponseRendererBuilder {

    @Override
    public AsyncHalResponseRenderer build() {
      return buildAsyncRenderer();
    }

  }

  static class RhymeBuilderImpl extends CommonRhymeBuilderImpl<RhymeBuilder> implements RhymeBuilder {

    @Override
    public Rhyme buildForRequestTo(String incomingRequestUri) {
      return buildRhyme(incomingRequestUri);
    }
  }
}

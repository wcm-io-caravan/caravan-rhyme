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

/**
 * <b>Internal</b> definitions of the default implementations of the {@link RhymeBuilder}, {@link HalApiClientBuilder}
 * and {@link HalResponseRendererBuilder} interfaces.
 * <p>
 * You should be using the static method in the interfaces instead.
 * </p>
 * @see HalApiClientBuilder#create()
 * @see HalResponseRendererBuilder#create()
 * @see RhymeBuilder#create()
 * @see RhymeBuilder#withResourceLoader(HalResourceLoader)
 */
public final class RhymeBuilders {

  private RhymeBuilders() {
    // only static methods
  }

  public static HalApiClientBuilder client() {
    return new HalApiClientBuilderImpl();
  }

  public static HalResponseRendererBuilder renderer() {
    return new HalResponseRenderBuilderImpl();
  }

  public static RhymeBuilder rhyme() {
    return new RhymeBuilderImpl();
  }

  public static RhymeBuilder rhyme(HalResourceLoader loader) {
    return new RhymeBuilderImpl().withResourceLoader(loader);
  }

  private static class HalApiClientBuilderImpl extends AbstractRhymeBuilder<HalApiClientBuilder> implements HalApiClientBuilder {

    @Override
    public HalApiClient build() {
      return buildApiClient();
    }
  }

  private static class HalResponseRenderBuilderImpl extends AbstractRhymeBuilder<HalResponseRendererBuilder> implements HalResponseRendererBuilder {

    @Override
    public AsyncHalResponseRenderer build() {
      return buildAsyncRenderer();
    }
  }

  private static class RhymeBuilderImpl extends AbstractRhymeBuilder<RhymeBuilder> implements RhymeBuilder {

    @Override
    public Rhyme buildForRequestTo(String incomingRequestUri) {
      return buildRhyme(incomingRequestUri);
    }
  }
}

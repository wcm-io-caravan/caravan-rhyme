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
package io.wcm.caravan.rhyme.impl.metadata;

/**
 * Constants for the relation used in the embedded metadata resource created by
 * {@link FullMetadataGenerator#createMetadataResource(io.wcm.caravan.rhyme.api.resources.LinkableResource)}
 */
public final class ResponseMetadataRelations {

  /** the relation used to embed information on upstream resources and performance in every rendered resource */
  public static final String RHYME_METADATA_RELATION = "rhyme:metadata";

  static final String MAX_AGE = "metrics:maxAge";
  static final String RESPONSE_TIMES = "metrics:responseTimes";
  static final String RENDERING_TIMES = "metrics:renderer";
  static final String PROXY_TIMES = "metrics:clientProxies";
  static final String SLING_MODELS = "metrics:slingModels";
  static final String EMISSION_TIMES = "metrics:observableEmissions";
  static final String SOURCE_LINKS = "metrics:sourceLinks";
  public static final String INVOCATION_TIMES = "metrics:invocationTimes";

  private ResponseMetadataRelations() {
    // constants only
  }

}

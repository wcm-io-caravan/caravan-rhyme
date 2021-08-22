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
package io.wcm.caravan.rhyme.osgi.sampleservice.api.errors;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariables;

/**
 * Examples for error handling that show how exceptions are rendered as
 * <a href="https://github.com/blongden/vnd.error">vnd.error</a> resources, and how
 * detailed error information from an upstream response is retained over service boundaries.
 */
@HalApiInterface
public interface ErrorExamplesResource {

  /**
   * A link template that will trigger an exception with the given parameters to be thrown by the server-side resource
   * implementation. The exception will be rendered as a vnd.error resource.
   * @param parameters defined in {@link ErrorParameters}
   * @return a {@link Single} that will emit the linked {@link ErrorResource}
   */
  @Related("error:server")
  Single<ErrorResource> simulateErrorOnServer(@TemplateVariables ErrorParameters parameters);

  /**
   * A link template that will trigger a HTTP request with the given parameters to the resource
   * linked with "error:server" relation, triggering an exception and a vnd.error response.
   * The request is executed with Rhyme's client proxies that will catch and re-throw the exception
   * as a HalApiClientException. The vnd.error resource for that exception will also include
   * the error information from the upstream vnd.error resource.
   * @param parameters defined in {@link ErrorParameters}
   * @return a {@link Single} that will emit the linked {@link ErrorResource}
   */
  @Related("error:client")
  Single<ErrorResource> testClientErrorHandling(@TemplateVariables ErrorParameters parameters);
}

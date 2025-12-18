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
package io.wcm.caravan.rhyme.osgi.it.tests;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.VIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.reactivex.rxjava3.core.Maybe;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.relations.VndErrorRelations;
import io.wcm.caravan.rhyme.osgi.it.IntegrationTestEnvironment;
import io.wcm.caravan.rhyme.osgi.it.extensions.WaitForServerStartupExtension;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorParameters;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.errors.ErrorParametersBean;


@ExtendWith({ WaitForServerStartupExtension.class })
public class HttpErrorResourcesIT {

  private final ExamplesEntryPointResource entryPoint = IntegrationTestEnvironment.createEntryPointProxy();

  private final ErrorParametersBean defaultParams;

  public HttpErrorResourcesIT() {

    this.defaultParams = new ErrorParametersBean()
        .withStatusCode(503)
        .withMessage("Something went wrong")
        .withWrapException(false);
  }

  HalApiClientException catchExceptionForRequestWith(ErrorParameters parameters) {

    Maybe<String> propertyThatShouldFailToLoad = entryPoint.getErrorExamples()
        .flatMap(errors -> errors.testClientErrorHandling(parameters))
        .flatMapMaybe(ErrorResource::getTitle);

    return assertThrows(HalApiClientException.class, propertyThatShouldFailToLoad::blockingGet);
  }

  @Test
  void should_respond_with_specified_status_code() {

    ErrorParameters params = defaultParams.withStatusCode(403);

    HalApiClientException ex = catchExceptionForRequestWith(params);

    assertThat(ex.getStatusCode())
        .isEqualTo(params.getStatusCode());
  }

  @Test
  void should_respond_with_vnd_error_resource() {

    HalApiClientException ex = catchExceptionForRequestWith(defaultParams);

    assertThat(ex.getErrorResponse().getContentType())
        .isEqualTo("application/vnd.error+json");
  }

  @Test
  void error_response_should_contain_about_link() {

    HalApiClientException ex = catchExceptionForRequestWith(defaultParams);

    Link aboutLink = ex.getErrorResponse().getBody().getLink(VndErrorRelations.ABOUT);

    assertThat(aboutLink)
        .isNotNull();
    assertThat(aboutLink.getHref())
        .isEqualTo(ex.getRequestUrl());
  }

  @Test
  void error_response_should_not_contain_embedded_metadata_if_request_param_is_not_set() {

    HalApiClientException ex = catchExceptionForRequestWith(defaultParams);

    HalResource metadata = ex.getErrorResponse().getBody().getEmbeddedResource("rhyme:metadata");

    assertThat(metadata)
        .isNull();
  }

  @Test
  void error_response_should_contain_embedded_cause() {

    ErrorParameters params = defaultParams.withWrapException(true);

    HalApiClientException ex = catchExceptionForRequestWith(params);

    HalResource vndBody = ex.getErrorResponse().getBody();

    List<HalResource> errors = vndBody.getEmbedded(VndErrorRelations.ERRORS);

    assertThat(errors)
        .hasSize(4);

    assertThat(errors.get(3).getModel().path("message").asText())
        .isEqualTo(params.getMessage());
  }

  @Test
  void error_response_should_have_via_link() {

    HalApiClientException ex = catchExceptionForRequestWith(defaultParams);

    Link via = ex.getErrorResponse().getBody().getLink(VIA);

    assertThat(via)
        .isNotNull();

    assertThat(via.getHref())
        .startsWith("/errors/server?");
  }
}

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

import static io.wcm.caravan.rhyme.osgi.it.TestEnvironmentConstants.ENTRY_POINT_PATH;
import static io.wcm.caravan.rhyme.osgi.it.TestEnvironmentConstants.SERVER_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.relations.VndErrorRelations;
import io.wcm.caravan.rhyme.osgi.it.extensions.HalApiClientExtension;
import io.wcm.caravan.rhyme.osgi.it.extensions.WaitForServerStartupExtension;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorParameters;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.errors.ErrorParametersBean;


@ExtendWith({ WaitForServerStartupExtension.class, HalApiClientExtension.class })
public class ServerSideErrorResourcesIT {

  private final ExamplesEntryPointResource entryPoint;

  private final ErrorParametersBean defaultParams;

  public ServerSideErrorResourcesIT(HalApiClient halApiClient) {

    this.entryPoint = halApiClient.getRemoteResource(ENTRY_POINT_PATH, ExamplesEntryPointResource.class);

    this.defaultParams = new ErrorParametersBean()
        .withStatusCode(503)
        .withMessage("Something went wrong")
        .withWrapException(false);
  }

  private HalApiClientException executeRequestAndGetExpectedHalApiClientException(Integer statusCode, String message, Boolean wrapException) {

    ErrorParameters parameters = new ErrorParameters() {

      @Override
      public Boolean getWrapException() {
        return wrapException;
      }

      @Override
      public Integer getStatusCode() {
        return statusCode;
      }

      @Override
      public String getMessage() {
        return message;
      }
    };

    return catchExceptionForRequestWith(parameters);
  }

  HalApiClientException catchExceptionForRequestWith(ErrorParameters parameters) {
    return assertThrows(HalApiClientException.class, () -> {
      entryPoint.getErrorExamples()
          .flatMap(errors -> errors.simulateErrorOnServer(parameters))
          .flatMapMaybe(ErrorResource::getProperties)
          .blockingGet();
    });
  }

  @Test
  public void should_respond_with_specified_status_code() {

    ErrorParameters params = defaultParams.withStatusCode(501);

    HalApiClientException ex = catchExceptionForRequestWith(params);

    assertThat(ex.getStatusCode())
        .isEqualTo(params.getStatusCode());
  }

  @Test
  public void should_respond_with_vnd_error_resource() {

    HalApiClientException ex = catchExceptionForRequestWith(defaultParams);

    assertThat(ex.getErrorResponse().getContentType())
        .isEqualTo("application/vnd.error+json");
  }

  @Test
  public void error_response_should_contain_about_link() {

    HalApiClientException ex = catchExceptionForRequestWith(defaultParams);

    Link aboutLink = ex.getErrorResponse().getBody().getLink(VndErrorRelations.ABOUT);

    assertThat(aboutLink)
        .isNotNull();
    assertThat(aboutLink.getHref())
        .isEqualTo(SERVER_URL + ex.getRequestUrl());
  }

  @Test
  public void error_response_should_contain_embedded_metadata() {

    HalApiClientException ex = catchExceptionForRequestWith(defaultParams);

    HalResource metadata = ex.getErrorResponse().getBody().getEmbeddedResource("rhyme:metadata");

    assertThat(metadata)
        .isNotNull();
  }

  @Test
  public void error_response_should_contain_embedded_cause() {

    ErrorParameters params = defaultParams.withWrapException(true);

    HalApiClientException ex = catchExceptionForRequestWith(params);

    HalResource cause = ex.getErrorResponse().getBody().getEmbeddedResource(VndErrorRelations.ERRORS);

    assertThat(cause)
        .isNotNull();

    assertThat(cause.getModel().path("message").asText())
        .isEqualTo(params.getMessage());
  }
}

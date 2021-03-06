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
import static io.wcm.caravan.rhyme.osgi.it.TestEnvironmentConstants.SERVER_URL;
import static io.wcm.caravan.rhyme.osgi.it.TestEnvironmentConstants.SERVICE_ID;
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
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorResource;


@ExtendWith({ WaitForServerStartupExtension.class, HalApiClientExtension.class })
public class HttpErrorResourcesIT {

  private final ExamplesEntryPointResource entryPoint;

  public HttpErrorResourcesIT(HalApiClient halApiClient) {
    this.entryPoint = halApiClient.getRemoteResource(SERVICE_ID, ExamplesEntryPointResource.class);
  }

  private HalApiClientException executeRequestAndGetExpectedHalApiClientException(Integer statusCode, String message, Boolean withCause) {

    return assertThrows(HalApiClientException.class, () -> {
      entryPoint.getErrorExamples()
          .flatMap(errors -> errors.provokeHttpClientError(statusCode, message, withCause))
          .flatMapMaybe(ErrorResource::getState)
          .blockingGet();
    });
  }

  @Test
  public void should_respond_with_specified_status_code() {

    Integer statusCode = 403;
    String message = "Something went wrong";
    Boolean withCause = false;

    HalApiClientException ex = executeRequestAndGetExpectedHalApiClientException(statusCode, message, withCause);

    assertThat(ex.getStatusCode()).isEqualTo(statusCode);
  }

  @Test
  public void should_respond_with_vnd_error_resource() {

    Integer statusCode = 403;
    String message = "Something went wrong";
    Boolean withCause = false;

    HalApiClientException ex = executeRequestAndGetExpectedHalApiClientException(statusCode, message, withCause);

    assertThat(ex.getErrorResponse().getContentType()).isEqualTo("application/vnd.error+json");
  }

  @Test
  public void error_response_should_contain_about_link() {

    Integer statusCode = 403;
    String message = "Something went wrong";
    Boolean withCause = false;

    HalApiClientException ex = executeRequestAndGetExpectedHalApiClientException(statusCode, message, withCause);

    Link aboutLink = ex.getErrorResponse().getBody().getLink(VndErrorRelations.ABOUT);
    assertThat(aboutLink).isNotNull();
    assertThat(aboutLink.getHref()).isEqualTo(SERVER_URL + ex.getRequestUrl());
  }

  @Test
  public void error_response_should_contain_embedded_metadata() {

    Integer statusCode = 403;
    String message = "Something went wrong";
    Boolean withCause = false;

    HalApiClientException ex = executeRequestAndGetExpectedHalApiClientException(statusCode, message, withCause);

    HalResource metadata = ex.getErrorResponse().getBody().getEmbeddedResource("caravan:metadata");
    assertThat(metadata).isNotNull();
  }

  @Test
  public void error_response_should_contain_embedded_cause() {

    Integer statusCode = 403;
    String message = "Something went wrong";
    Boolean withCause = true;

    HalApiClientException ex = executeRequestAndGetExpectedHalApiClientException(statusCode, message, withCause);

    HalResource vndBody = ex.getErrorResponse().getBody();

    HalResource cause = vndBody.getEmbeddedResource(VndErrorRelations.ERRORS);
    assertThat(cause).isNotNull();

    Link viaLink = vndBody.getLink(VIA);
    assertThat(viaLink).isNotNull();
    assertThat(viaLink.getHref()).startsWith("/caravan/hal/sample-service/errors/serverSide?");
  }
}

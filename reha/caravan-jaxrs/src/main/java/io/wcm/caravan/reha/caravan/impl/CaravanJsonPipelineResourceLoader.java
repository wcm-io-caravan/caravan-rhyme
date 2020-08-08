/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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
package io.wcm.caravan.reha.caravan.impl;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import hu.akarnokd.rxjava3.interop.RxJavaInterop;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.pipeline.JsonPipeline;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.JsonPipelineInputException;
import io.wcm.caravan.pipeline.JsonPipelineOutput;
import io.wcm.caravan.pipeline.cache.CacheStrategies;
import io.wcm.caravan.reha.api.client.HalApiClientException;
import io.wcm.caravan.reha.api.client.JsonResourceLoader;
import io.wcm.caravan.reha.api.common.HalResponse;


class CaravanJsonPipelineResourceLoader implements JsonResourceLoader {

  private final JsonPipelineFactory pipelineFactory;
  private final String serviceId;

  CaravanJsonPipelineResourceLoader(JsonPipelineFactory pipelineFactory, String serviceId) {
    this.pipelineFactory = pipelineFactory;
    this.serviceId = serviceId;
  }

  @Override
  public Single<HalResponse> loadJsonResource(String uri) {

    CaravanHttpRequest request = createRequest(uri);

    return getPipelineOutput(request)
        .map(this::createSuccessResponse)
        .onErrorResumeNext(ex -> rethrowAsHalApiClientException(ex, uri));
  }

  private CaravanHttpRequest createRequest(String uri) {

    CaravanHttpRequestBuilder requestBuilder = new CaravanHttpRequestBuilder(serviceId);

    requestBuilder.append(uri);

    return requestBuilder.build();
  }

  private Single<JsonPipelineOutput> getPipelineOutput(CaravanHttpRequest request) {

    JsonPipeline pipeline = pipelineFactory.create(request)
        .addCachePoint(CacheStrategies.timeToIdle(60, TimeUnit.SECONDS));

    return RxJavaInterop.toV3Single(pipeline.getOutput().toSingle());
  }

  private HalResponse createSuccessResponse(JsonPipelineOutput pipelineOutput) {
    HalResponse response = new HalResponse()
        .withStatus(pipelineOutput.getStatusCode())
        .withBody(pipelineOutput.getPayload())
        .withMaxAge(pipelineOutput.getMaxAge());

    return response;
  }

  private SingleSource<HalResponse> rethrowAsHalApiClientException(Throwable ex, String uri) {
    if (!(ex instanceof JsonPipelineInputException)) {
      return Single.error(new HalApiClientException("An unexpected exception occured trying to load " + uri, null, uri, ex));
    }

    JsonPipelineInputException jpie = (JsonPipelineInputException)ex;

    JsonNode responseNode = tryToReadResponseBodyFromException(jpie);

    HalResponse errorResponse = new HalResponse()
        .withStatus(jpie.getStatusCode())
        .withBody(responseNode)
        .withReason(jpie.getReason());

    return Single.error(new HalApiClientException(errorResponse, uri, ex));
  }

  private JsonNode tryToReadResponseBodyFromException(JsonPipelineInputException jpie) {

    JsonNode responseNode = JsonNodeFactory.instance.objectNode();

    Throwable cause = jpie.getCause();
    if (cause instanceof IllegalResponseRuntimeException) {
      IllegalResponseRuntimeException irre = ((IllegalResponseRuntimeException)cause);
      String responseBody = irre.getResponseBody();
      if (responseBody != null) {
        try {
          responseNode = new JsonFactory(new ObjectMapper()).createParser(responseBody).readValueAs(JsonNode.class);
        }
        // CHECKSTYLE:OFF - we really want to just try to parse the response body as JSON if possible
        catch (Exception ex) {
          // CHECKSTYLE:ON - it's absolutely no deal if this doesn't work for whatever reason
        }
      }
    }

    return responseNode;
  }
}

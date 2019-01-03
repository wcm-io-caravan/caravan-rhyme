/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2015 wcm.io
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
package io.wcm.caravan.jaxrs.publisher.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;

import io.wcm.caravan.hal.microservices.api.client.HalApiClient;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.resource.HalResource;

public class HalApiClientRule implements TestRule {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final JsonFactory JSON_FACTORY = new JsonFactory(OBJECT_MAPPER);

	static final String SERVER_URL = "http://localhost:8080"; // System.getProperty("launchpad.http.server.url");
	static final String SERVICE_ID = "/caravan/hal/sample-service";

	static WaitUntilEntryPointIsAvailable waitUntilAvailable = new WaitUntilEntryPointIsAvailable();

	static class WaitUntilEntryPointIsAvailable extends ExternalResource {

		private boolean isAvailable = false;

		@Override
		protected void before() throws Throwable {

			int maxWaitSeconds = 30;
			Stopwatch sw = Stopwatch.createStarted();
			while (!isAvailable && sw.elapsed(TimeUnit.SECONDS) < maxWaitSeconds) {
				try {
					assertHalResourceFoundAt(SERVICE_ID);
					isAvailable = true;
					return;
				} catch (Exception e) {
					System.err.println("Caught " + e.getClass().getSimpleName()
							+ " waiting for server to start up before starting tests");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
			if (!isAvailable) {
				throw new IllegalStateException("Entry point at " + SERVER_URL + SERVICE_ID
						+ " still fails to load after waiting " + maxWaitSeconds + " seconds");
			}
		}
	}

	private static HalResource assertHalResourceFoundAt(String url) throws IOException {

		String fullUrl = SERVER_URL + url;
		HttpResponse response = getResponse(fullUrl);

		assertThat(response.getStatusLine().getStatusCode()).as("Response code for " + fullUrl)
				.isEqualTo(HttpServletResponse.SC_OK);

		assertThat(response.getFirstHeader("Content-Type").getValue()).as("Content type for " + fullUrl)
				.isEqualTo(HalResource.CONTENT_TYPE);

		String jsonString = EntityUtils.toString(response.getEntity());
		assertThat(jsonString).as("JSON response").isNotBlank();

		JsonNode json = JSON_FACTORY.createParser(jsonString).readValueAsTree();
		HalResource halResource = new HalResource(json);

		assertThat(halResource.getLink()).as("self link").isNotNull();
		assertThat(halResource.getLink().getHref()).as("self URL").startsWith(url);

		return halResource;
	}

	private static HttpResponse getResponse(String fullUrl) throws IOException, ClientProtocolException {
		HttpGet get = new HttpGet(fullUrl);

		CloseableHttpClient client = HttpClientBuilder.create().build();
		return client.execute(get);
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return waitUntilAvailable.apply(base, description);
	}
}

package io.wcm.caravan.hal.it.extensions;

import static io.wcm.caravan.hal.it.TestEnvironmentConstants.SERVER_URL;
import static io.wcm.caravan.hal.it.TestEnvironmentConstants.SERVICE_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;

import io.wcm.caravan.hal.it.TestEnvironmentConstants;
import io.wcm.caravan.hal.resource.HalResource;

public class WaitForServerStartupExtension implements BeforeAllCallback {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final JsonFactory JSON_FACTORY = new JsonFactory(OBJECT_MAPPER);

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {

		int maxWaitSeconds = 30;
		Stopwatch sw = Stopwatch.createStarted();
		while (sw.elapsed(TimeUnit.SECONDS) < maxWaitSeconds) {
			try {
				assertHalResourceFoundAt(SERVICE_ID);
				return;
			} catch (Exception e) {
				System.err.println("Caught " + e.getClass().getSimpleName()
						+ " waiting for server to start up before starting tests, will try again...");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		throw new IllegalStateException("Entry point at " + TestEnvironmentConstants.SERVER_URL + SERVICE_ID
				+ " still fails to load after waiting " + maxWaitSeconds + " seconds");
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

}
package io.wcm.caravan.rhyme.impl.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport.MockClientTestSupport;
import io.wcm.caravan.rhyme.testing.resources.TestResourceState;

class HalApiClientProxyFactoryUriTemplateTest {

    private final MockClientTestSupport client = ClientTestSupport.withMocking();

    @HalApiInterface
    interface ResourceWithSingleState {
        @ResourceState
        Single<TestResourceState> getProperties();
    }

    @Test
    void createProxyFromUrl_should_expand_templated_url_with_empty_map() {

        TestResourceState state = new TestResourceState();
        state.text = "test";

        // The link is templated but no variables are provided, so it strips the
        // variable placeholders
        String templatedUrl = "/resource/{id}";
        String resolvedUrl = "/resource/";

        client.mockHalResponse(resolvedUrl, new HalResource(state, resolvedUrl));

        TestResourceState result = client.getHalApiClient()
                .getRemoteResource(templatedUrl, ResourceWithSingleState.class)
                .getProperties()
                .blockingGet();

        assertThat(result.text).isEqualTo("test");
    }

    @Test
    void createProxyFromUrl_should_strip_query_templates() {

        TestResourceState state = new TestResourceState();
        state.text = "test";

        // The link is templated but no variables are provided, so it strips the
        // variable placeholders
        String templatedUrl = "/resource{?id}";
        String resolvedUrl = "/resource";

        client.mockHalResponse(resolvedUrl, new HalResource(state, resolvedUrl));

        TestResourceState result = client.getHalApiClient()
                .getRemoteResource(templatedUrl, ResourceWithSingleState.class)
                .getProperties()
                .blockingGet();

        assertThat(result.text).isEqualTo("test");
    }
}

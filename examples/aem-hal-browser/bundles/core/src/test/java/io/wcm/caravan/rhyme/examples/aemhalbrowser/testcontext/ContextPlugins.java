package io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext;

import org.apache.sling.testing.mock.osgi.context.AbstractContextPlugin;
import org.apache.sling.testing.mock.osgi.context.ContextPlugin;
import org.jetbrains.annotations.NotNull;

import io.wcm.caravan.commons.httpclient.impl.HttpClientFactoryImpl;
import io.wcm.caravan.rhyme.aem.impl.client.ResourceLoaderManager;
import io.wcm.caravan.rhyme.aem.impl.docs.RhymeDocsOsgiBundleSupport;
import io.wcm.caravan.rhyme.aem.impl.parameters.QueryParamInjector;
import io.wcm.testing.mock.aem.context.AemContextImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;

/**
 * AEM Mock context plugins (to be used with {@link AemContextBuilder} or {@link AemContext})
 */
public final class ContextPlugins {

  private ContextPlugins() {
    // constants only
  }

  /**
   * Context plugin that sets up all mandatory OSGi services for Rhyme's AEM integration.
   */
  public static final @NotNull ContextPlugin<AemContextImpl> CARAVAN_RHYME = new AbstractContextPlugin<AemContextImpl>() {

    @Override
    public void afterSetUp(@NotNull AemContextImpl context) throws Exception {

      context.registerInjectActivateService(new HttpClientFactoryImpl());
      context.registerInjectActivateService(new ResourceLoaderManager());

      context.registerInjectActivateService(new RhymeDocsOsgiBundleSupport());
      context.registerInjectActivateService(new QueryParamInjector());

    }
  };
}

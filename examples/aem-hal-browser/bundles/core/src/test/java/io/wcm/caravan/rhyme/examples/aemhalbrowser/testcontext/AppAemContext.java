package io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext;

import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.ContextPlugins.CARAVAN_RHYME;
import static io.wcm.testing.mock.wcmio.caconfig.ContextPlugins.WCMIO_CACONFIG;
import static io.wcm.testing.mock.wcmio.handler.ContextPlugins.WCMIO_HANDLER;
import static io.wcm.testing.mock.wcmio.sling.ContextPlugins.WCMIO_SLING;
import static io.wcm.testing.mock.wcmio.wcm.ContextPlugins.WCMIO_WCM;
import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;

import org.jetbrains.annotations.NotNull;

import io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.AemHalBrowserResourceRegistration;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;

/**
 * Sets up {@link AemContext} for unit tests in this application.
 */
public class AppAemContext extends AemContext {

  private AppAemContext() {
    // static methods only
  }

  /**
   * @return {@link AemContext}
   */
  public static AemContext newAemContext() {

    return new AemContextBuilder()
        .plugin(CACONFIG)
        .plugin(WCMIO_SLING, WCMIO_WCM, WCMIO_CACONFIG, WCMIO_HANDLER)
        .plugin(CARAVAN_RHYME)
        .afterSetUp(AppAemContext::registerOsgiServices)
        .build();
  }

  /**
   * Custom set up rules required in all unit tests.
   */
  static void registerOsgiServices(@NotNull AemContext context) {

    context.registerInjectActivateService(new AemHalBrowserResourceRegistration());
  }
}

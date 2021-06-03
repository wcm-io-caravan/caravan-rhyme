package io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext;

import static io.wcm.testing.mock.wcmio.caconfig.ContextPlugins.WCMIO_CACONFIG;
import static io.wcm.testing.mock.wcmio.handler.ContextPlugins.WCMIO_HANDLER;
import static io.wcm.testing.mock.wcmio.sling.ContextPlugins.WCMIO_SLING;
import static io.wcm.testing.mock.wcmio.wcm.ContextPlugins.WCMIO_WCM;
import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;

import java.io.IOException;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;

import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextCallback;

/**
 * Sets up {@link AemContext} for unit tests in this application.
 */
public final class AppAemContext {

  private AppAemContext() {
    // static methods only
  }

  /**
   * @return {@link AemContext}
   */
  public static AemContext newAemContext() {
    AemContext context = new AemContextBuilder()
        .plugin(CACONFIG)
        .plugin(WCMIO_SLING, WCMIO_WCM, WCMIO_CACONFIG, WCMIO_HANDLER)
        .afterSetUp(SETUP_CALLBACK)
        .registerSlingModelsFromClassPath(true)
        .build();

    context.addModelsForPackage("io.wcm.caravan.rhyme.aem");

    return context;
  }

  /**
   * Custom set up rules required in all unit tests.
   */
  private static final AemContextCallback SETUP_CALLBACK = new AemContextCallback() {

    @Override
    public void execute(@NotNull AemContext context) throws PersistenceException, IOException {

      // setup handler
      //      context.registerInjectActivateService(new LinkHandlerConfigImpl());
      //      context.registerInjectActivateService(new MediaHandlerConfigImpl());

    }
  };


  public static SlingRhyme createRhymeInstance(AemContext context, String resourcePath) {

    Resource content = context.create().resource(resourcePath);

    context.currentResource(content);
    context.request().setResource(content);

    return context.request().adaptTo(SlingRhyme.class);
  }

}

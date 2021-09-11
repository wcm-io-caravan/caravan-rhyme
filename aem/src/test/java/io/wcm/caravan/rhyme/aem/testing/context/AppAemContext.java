package io.wcm.caravan.rhyme.aem.testing.context;

import static io.wcm.testing.mock.wcmio.caconfig.ContextPlugins.WCMIO_CACONFIG;
import static io.wcm.testing.mock.wcmio.handler.ContextPlugins.WCMIO_HANDLER;
import static io.wcm.testing.mock.wcmio.sling.ContextPlugins.WCMIO_SLING;
import static io.wcm.testing.mock.wcmio.wcm.ContextPlugins.WCMIO_WCM;
import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;

import java.io.IOException;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.jetbrains.annotations.NotNull;

import io.wcm.caravan.commons.httpclient.impl.HttpClientFactoryImpl;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.impl.RhymeResourceRegistry;
import io.wcm.caravan.rhyme.aem.impl.client.ResourceLoaderManager;
import io.wcm.caravan.rhyme.aem.impl.docs.RhymeDocsOsgiBundleSupport;
import io.wcm.caravan.rhyme.aem.impl.parameters.QueryParamInjector;
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

    registerRequiredServices(context);

    return context;
  }

  public static AemContext newAemContextWithJcrMock() {

    AemContext context = new AemContextBuilder(ResourceResolverType.JCR_MOCK)
        .plugin(WCMIO_SLING, WCMIO_WCM)
        .registerSlingModelsFromClassPath(true)
        .build();

    registerRequiredServices(context);

    return context;
  }

  private static void registerRequiredServices(AemContext context) {

    context.registerInjectActivateService(new HttpClientFactoryImpl());
    context.registerInjectActivateService(new ResourceLoaderManager());

    context.registerInjectActivateService(new RhymeDocsOsgiBundleSupport());

    context.registerInjectActivateService(new RhymeResourceRegistry());

    context.registerInjectActivateService(new QueryParamInjector());
  }

  /**
   * Custom set up rules required in all unit tests.
   */
  private static final AemContextCallback SETUP_CALLBACK = new AemContextCallback() {

    @Override
    public void execute(@NotNull AemContext context) throws PersistenceException, IOException {

      // setup handler
      //context.registerInjectActivateService(new LinkHandlerConfigImpl());
      //context.registerInjectActivateService(new MediaHandlerConfigImpl());

    }
  };


  public static SlingRhyme createRhymeInstance(AemContext context, String resourcePath) {

    Resource content = context.resourceResolver().getResource(resourcePath);
    if (content == null) {
      content = context.create().resource(resourcePath);
    }

    context.currentResource(content);
    context.request().setResource(content);

    return context.request().adaptTo(SlingRhyme.class);
  }

}

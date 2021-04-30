package io.wcm.caravan.rhyme.aem.integration.impl;

import java.util.Map;

import org.osgi.service.component.annotations.Component;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.rhyme.aem.impl.resources.AemAssetImpl;
import io.wcm.caravan.rhyme.aem.impl.resources.AemPageImpl;
import io.wcm.caravan.rhyme.aem.impl.resources.AemRenditionImpl;
import io.wcm.caravan.rhyme.aem.impl.resources.SlingResourceImpl;
import io.wcm.caravan.rhyme.aem.integration.ResourceSelectorProvider;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Component
public class HalBrowserResourceSelectorProvider implements ResourceSelectorProvider {

  @Override
  public Map<Class<? extends LinkableResource>, String> getModelClassesWithSelectors() {

    return ImmutableMap.of(
        AemPageImpl.class, "aempage",
        SlingResourceImpl.class, "slingresource",
        AemAssetImpl.class, "aemasset",
        AemRenditionImpl.class, "aemrendition");
  }

}

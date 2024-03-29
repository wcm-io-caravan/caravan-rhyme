package io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.rhyme.aem.api.RhymeResourceRegistration;
import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.assets.AemAssetImpl;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.assets.AemRenditionImpl;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.generic.SlingResourceImpl;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.sites.AemPageImpl;

@Component
public class AemHalBrowserResourceRegistration implements RhymeResourceRegistration {

  @Override
  public Map<Class<? extends LinkableResource>, String> getModelClassesWithSelectors() {

    return ImmutableMap.of(
        AemRepositoryImpl.class, "aemrepository",
        AemPageImpl.class, "aempage",
        SlingResourceImpl.class, "slingresource",
        AemAssetImpl.class, "aemasset",
        AemRenditionImpl.class, "aemrendition");
  }

  @Override
  public Optional<? extends LinkableResource> getApiEntryPoint(SlingResourceAdapter adapter) {

    return adapter
        .selectResourceAt("/")
        .adaptTo(AemRepositoryImpl.class)
        .withLinkName("rhyme.examples.aem-hal-browser")
        .getOptional();
  }

}

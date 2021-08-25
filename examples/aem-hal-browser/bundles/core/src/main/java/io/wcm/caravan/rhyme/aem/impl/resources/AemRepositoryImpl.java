package io.wcm.caravan.rhyme.aem.impl.resources;

import java.util.Optional;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;

import io.wcm.caravan.rhyme.aem.api.AemRepository;
import io.wcm.caravan.rhyme.aem.api.assets.AemAsset;
import io.wcm.caravan.rhyme.aem.api.assets.AemRendition;
import io.wcm.caravan.rhyme.aem.api.generic.SlingResource;
import io.wcm.caravan.rhyme.aem.api.sites.AemPage;
import io.wcm.caravan.rhyme.aem.impl.resources.assets.AemRenditionImpl;
import io.wcm.caravan.rhyme.aem.integration.AbstractLinkableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = Resource.class, adapters = { LinkableResource.class, AemRepository.class })
public class AemRepositoryImpl extends AbstractLinkableResource implements AemRepository {

  @Override
  public SlingResource getRoot() {

    return resourceAdapter.selectResourceAt("/")
        .adaptTo(SlingResource.class)
        .withLinkTitle("The root resource of the repository")
        .getInstance();
  }

  @Override
  public Optional<SlingResource> getResource(String path) {

    return resourceAdapter.selectResourceAt(path)
        .adaptTo(SlingResource.class)
        .withLinkTitle("Get a generic view of the resource at the given path")
        .getOptional();
  }

  @Override
  public Optional<AemPage> getPage(String path) {

    return resourceAdapter.selectResourceAt(path)
        .filterAdaptableTo(Page.class)
        .adaptTo(AemPage.class)
        .withLinkTitle("Get information on an AEM page at the given path")
        .getOptional();
  }

  @Override
  public Optional<AemAsset> getAsset(String path) {

    return resourceAdapter.selectResourceAt(path)
        .filterAdaptableTo(Asset.class)
        .adaptTo(AemAsset.class)
        .withLinkTitle("Get information on an asset at the given path")
        .getOptional();
  }

  @Override
  public Optional<AemRendition> getRendition(String path, Integer width, Integer height) {

    if (path == null) {
      return resourceAdapter.selectResourceAt(null)
          .adaptTo(AemRendition.class)
          .withLinkTitle("Get a dynamic (cropped) rendition of an asset with the specified width and height")
          .withQueryParameterTemplate(AemRenditionImpl.WIDTH, AemRenditionImpl.HEIGHT)
          .getOptional();
    }

    return getAsset(path)
        .flatMap(asset -> asset.getRendition(width, height));
  }

  @Override
  protected String getDefaultLinkTitle() {
    return "The entry point to the AEM Repository HAL API";
  }

}

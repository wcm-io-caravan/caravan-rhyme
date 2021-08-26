package io.wcm.caravan.rhyme.aem.impl.resources.generic;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.rhyme.aem.api.assets.AemAsset;
import io.wcm.caravan.rhyme.aem.api.generic.InfinityJsonResource;
import io.wcm.caravan.rhyme.aem.api.generic.SlingResource;
import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.api.sites.AemPage;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = Resource.class, adapters = { LinkableResource.class, SlingResource.class })
public class SlingResourceImpl extends AbstractLinkableResource implements SlingResource {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

  @Self
  private Resource resource;

  @Override
  public ObjectNode getProperties() {

    return OBJECT_MAPPER.convertValue(resource.getValueMap(), ObjectNode.class);
  }

  @Override
  public Optional<AemPage> asAemPage() {

    return resourceAdapter
        .selectCurrentResource()
        .filterAdaptableTo(Page.class)
        .adaptTo(AemPage.class)
        .withLinkTitle("Show the specific HAL representation for this AEM page")
        .getOptional();
  }

  @Override
  public Optional<AemAsset> asAemAsset() {

    return resourceAdapter
        .selectCurrentResource()
        .filterAdaptableTo(Asset.class)
        .adaptTo(AemAsset.class)
        .getOptional();
  }

  @Override
  public Optional<SlingResource> getParent() {

    return resourceAdapter
        .selectParentResource()
        .adaptTo(SlingResource.class)
        .getOptional();
  }

  @Override
  public Stream<SlingResource> getChildren() {

    return resourceAdapter
        .selectChildResources()
        .adaptTo(SlingResource.class)
        .getStream();
  }

  @Override
  public Optional<InfinityJsonResource> getJcrContentAsJson() {

    return resourceAdapter
        .selectChildResource(JcrConstants.JCR_CONTENT)
        .adaptTo(InfinityJsonResource.class)
        .getOptional();
  }

  @Override
  protected String getDefaultLinkTitle() {

    String resourceType = defaultIfNull(resource.getResourceType(), "unknown");

    String titleSuffix = getResourceTitleSuffix();

    return resourceType + " resource " + titleSuffix;
  }

  private String getResourceTitleSuffix() {

    Resource contentResource = resource.getChild(JcrConstants.JCR_CONTENT);
    if (contentResource != null) {
      String jcrTitle = contentResource.getValueMap().get(JcrConstants.JCR_TITLE, String.class);
      if (StringUtils.isNotBlank(jcrTitle)) {
        return "with title '" + jcrTitle + "'";
      }
    }
    return "without a title";
  }

}

package io.wcm.caravan.rhyme.aem.impl.resources;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.dam.api.Rendition;

import io.wcm.caravan.rhyme.aem.api.AemRendition;
import io.wcm.caravan.rhyme.aem.api.AemRenditionProperties;
import io.wcm.caravan.rhyme.aem.integration.impl.ResourceUtils;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.media.Media;
import io.wcm.handler.media.MediaBuilder;
import io.wcm.handler.media.MediaHandler;

@Model(adaptables = Resource.class, adapters = { AemRendition.class })
public class AemRenditionImpl implements AemRendition {

  @Self
  private MediaHandler mediaHandler;

  @Self
  private Resource resource;

  @Self
  private Rendition rendition;

  @Override
  public AemRenditionProperties getProperties() {

    Resource assetResource = ResourceUtils.getParentAssetResource(resource);

    MediaBuilder builder = mediaHandler.get(assetResource.getPath())
        .includeAssetThumbnails(true)
        .includeAssetWebRenditions(true);

    Media media = builder.build();

    System.out.println("this resource is " + resource.getPath());
    System.out.println("asset resource is " + assetResource.getPath());

    System.out.println("it has " + media.getRenditions().size() + " renditions");

    io.wcm.handler.media.Rendition wcmIoRendition = media.getRenditions().stream()
        .peek(r -> System.out.println("rendition path is " + r.getPath()))
        .filter(r -> r.getPath().equals(resource.getPath()))
        .findFirst().orElse(null);

    return new AemRenditionProperties() {

      @Override
      public String getTitle() {
        return getName() + " (" + getResourceType() + ")";
      }

      @Override
      public String getName() {
        return rendition.getName();
      }

      @Override
      public String getResourceType() {

        return rendition.getResourceType();
      }

      @Override
      public String getMimeType() {
        return rendition.getMimeType();
      }
    };
  }

  @Override
  public LinkableResource getBinaryResource() {

    return new BinaryAssetResource(mediaHandler, rendition);
  }


}

package io.wcm.caravan.rhyme.aem.integration.impl;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;

public class ResourceUtils {

  private static final String ASSET_NODE_TYPE = "dam:Asset";

  public static Stream<Resource> getStreamOfChildren(Resource res) {

    return StreamSupport.stream(res.getChildren().spliterator(), false);
  }

  public static Stream<Resource> getStreamOfChildPages(Resource res) {

    return getStreamOfChildren(res)
        .filter(child -> isPage(child));
  }

  static boolean isPage(Resource page) {
    boolean isPage = "cq:Page".equals(page.getResourceType());
    System.out.println("Is " + page.getPath() + " a page? " + isPage);
    return isPage;
  }

  public static Resource getParentAssetResource(Resource resource) {

    Resource res = resource;
    while (res != null && !res.getResourceType().equals(ASSET_NODE_TYPE)) {
      res = res.getParent();
    }

    return res;
  }

}

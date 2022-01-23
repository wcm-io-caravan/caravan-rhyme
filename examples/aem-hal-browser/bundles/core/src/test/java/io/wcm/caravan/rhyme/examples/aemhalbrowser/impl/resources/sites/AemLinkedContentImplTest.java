package io.wcm.caravan.rhyme.examples.aemhalbrowser.impl.resources.sites;

import static com.day.cq.commons.jcr.JcrConstants.JCR_CONTENT;
import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.createRhymeInstance;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites.AemLinkedContent;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class AemLinkedContentImplTest {

  private static final String PAGE_PATH = "/content/foo";
  private static final String CONTENT_PATH = PAGE_PATH + "/" + JCR_CONTENT;

  private static final String TEMPLATE = "foo/templates/page";

  private static final String LINKED_PAGE_PATH = "/content/page";
  private static final String LINKED_ASSET_PATH = "/content/asset";
  private static final String LINKED_RESOURCE_PATH = "/content/resource";

  private AemContext context = AppAemContext.newAemContext();

  private AemLinkedContent createSlingModel() {

    SlingRhyme slingRhyme = createRhymeInstance(context, CONTENT_PATH);

    AemLinkedContent linkedContent = slingRhyme.adaptTo(AemLinkedContent.class);

    assertThat(linkedContent)
        .isNotNull();

    return linkedContent;
  }

  private void assertHasNoLinksAtAll(AemLinkedContent linkedContent) {

    assertThat(linkedContent.getLinkedPages())
        .isEmpty();

    assertThat(linkedContent.getLinkedAssets())
        .isEmpty();

    assertThat(linkedContent.getOtherLinkedResources())
        .isEmpty();
  }

  private void assertHasOnlyLinkToPage(AemLinkedContent linkedContent) {

    assertThat(linkedContent.getLinkedPages())
        .hasSize(1)
        .first()
        .extracting(page -> page.createLink().getHref())
        .isEqualTo(LINKED_PAGE_PATH + ".aempage.rhyme");

    assertThat(linkedContent.getLinkedAssets())
        .isEmpty();

    assertThat(linkedContent.getOtherLinkedResources())
        .isEmpty();
  }

  private void assertHasOnlyLinkToAsset(AemLinkedContent linkedContent) {

    assertThat(linkedContent.getLinkedPages())
        .isEmpty();

    assertThat(linkedContent.getLinkedAssets())
        .hasSize(1)
        .first()
        .extracting(page -> page.createLink().getHref())
        .isEqualTo(LINKED_ASSET_PATH + ".aemasset.rhyme");

    assertThat(linkedContent.getOtherLinkedResources())
        .isEmpty();
  }

  private void assertHasOnlyLinkToResource(AemLinkedContent linkedContent) {

    assertThat(linkedContent.getLinkedPages())
        .isEmpty();

    assertThat(linkedContent.getLinkedAssets())
        .isEmpty();

    assertThat(linkedContent.getOtherLinkedResources())
        .hasSize(1)
        .first()
        .extracting(page -> page.createLink().getHref())
        .isEqualTo(LINKED_RESOURCE_PATH + ".slingresource.rhyme");
  }

  @Test
  void has_no_links_if_content_is_empty() {

    context.create().page(PAGE_PATH);

    AemLinkedContent linkedContent = createSlingModel();

    assertHasNoLinksAtAll(linkedContent);
  }


  @Test
  void getLinkedPages_returns_links_to_pages_from_page_properties() {

    context.create().page(LINKED_PAGE_PATH);

    context.create().page(PAGE_PATH, TEMPLATE, "linkRef", LINKED_PAGE_PATH);

    AemLinkedContent linkedContent = createSlingModel();

    assertHasOnlyLinkToPage(linkedContent);
  }

  @Test
  void getLinkedPages_can_return_multiple_links() {

    context.create().page(LINKED_PAGE_PATH);

    String linkedPath1 = LINKED_PAGE_PATH + "/1";
    String linkedPath2 = LINKED_PAGE_PATH + "/2";

    context.create().page(linkedPath1);
    context.create().page(linkedPath2);

    context.create().page(PAGE_PATH, TEMPLATE, "linkRef", LINKED_PAGE_PATH, "linkRef1", linkedPath1, "linkRef2", linkedPath2);

    AemLinkedContent linkedContent = createSlingModel();

    assertThat(linkedContent.getLinkedPages())
        .hasSize(3);
  }

  @Test
  void getLinkedPages_returns_links_to_pages_from_child_properties() {

    context.create().page(LINKED_PAGE_PATH);

    context.create().page(PAGE_PATH);
    context.create().resource(CONTENT_PATH + "/components/123", "linkRef", LINKED_PAGE_PATH);

    AemLinkedContent linkedContent = createSlingModel();

    assertHasOnlyLinkToPage(linkedContent);
  }

  @Test
  void getLinkedPages_ignores_links_to_non_existent_page() {

    context.create().page(PAGE_PATH, TEMPLATE, "linkRef", LINKED_PAGE_PATH);

    AemLinkedContent linkedContent = createSlingModel();

    assertHasNoLinksAtAll(linkedContent);
  }

  @Test
  void getLinkedAssets_returns_links_to_assets_from_page_properties() {

    context.create().asset(LINKED_ASSET_PATH, 400, 100, "image/jpeg");

    context.create().page(PAGE_PATH, TEMPLATE, "mediaRef", LINKED_ASSET_PATH);

    AemLinkedContent linkedContent = createSlingModel();

    assertHasOnlyLinkToAsset(linkedContent);
  }

  @Test
  void getLinkedAssets_returns_links_to_assets_from_child_properties() {

    context.create().asset(LINKED_ASSET_PATH, 400, 100, "image/jpeg");

    context.create().page(PAGE_PATH);
    context.create().resource(CONTENT_PATH + "/components/123", "mediaRef", LINKED_ASSET_PATH);

    AemLinkedContent linkedContent = createSlingModel();

    assertHasOnlyLinkToAsset(linkedContent);
  }

  @Test
  void getLinkedAssets_ignores_links_to_non_existent_asset() {

    context.create().page(PAGE_PATH, TEMPLATE, "mediaRef", LINKED_ASSET_PATH);

    AemLinkedContent linkedContent = createSlingModel();

    assertHasNoLinksAtAll(linkedContent);
  }

  @Test
  void getOtherLinkedResources_returns_links_to_resources_from_page_properties() {

    context.create().resource(LINKED_RESOURCE_PATH);

    context.create().page(PAGE_PATH, TEMPLATE, "otherRef", LINKED_RESOURCE_PATH);

    AemLinkedContent linkedContent = createSlingModel();

    assertHasOnlyLinkToResource(linkedContent);
  }

  @Test
  void getOtherLinkedResources_returns_links_to_resources_from_child_properties() {

    context.create().resource(LINKED_RESOURCE_PATH);

    context.create().page(PAGE_PATH);
    context.create().resource(CONTENT_PATH + "/components/123", "otherRef", LINKED_RESOURCE_PATH);

    AemLinkedContent linkedContent = createSlingModel();

    assertHasOnlyLinkToResource(linkedContent);
  }

  @Test
  void getOtherLinkedResources_ignores_links_to_non_existentresources() {

    context.create().page(PAGE_PATH, TEMPLATE, "otherRef", LINKED_RESOURCE_PATH);

    AemLinkedContent linkedContent = createSlingModel();

    assertHasNoLinksAtAll(linkedContent);
  }

  @Test
  void ignores_properties_not_containing_absolute_paths() {

    context.create().page(PAGE_PATH, TEMPLATE, "linkRef", "components/123", "mediaRef", "foo");
    context.create().resource(CONTENT_PATH + "/components/123");

    AemLinkedContent linkedContent = createSlingModel();

    assertHasNoLinksAtAll(linkedContent);
  }
}

package io.wcm.caravan.rhyme.examples.aemhalbrowser.impl.resources.generic;

import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.assertLinkHasHref;
import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.assertResourceCanBeRendered;
import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.createRhymeInstance;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemAsset;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.generic.InfinityJsonResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.generic.SlingResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites.AemPage;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class SlingResourceImplTest {

  private static final String PATH = "/content/foo";

  private AemContext context = AppAemContext.newAemContext();

  private SlingResource createSlingModel(String path) {

    SlingRhyme slingRhyme = createRhymeInstance(context, path);

    SlingResource slingResource = slingRhyme.adaptTo(SlingResource.class);

    assertThat(slingResource)
        .isNotNull();

    return slingResource;
  }

  @Test
  void can_render_minimal_resource() {

    context.create().resource(PATH);

    SlingResource slingModel = createSlingModel(PATH);

    assertResourceCanBeRendered(slingModel);
  }


  @Test
  void getProperties_returns_properties_from_sling_resource() {

    context.create().resource(PATH, ImmutableMap.of("foo", "bar", "test", 123));

    ObjectNode properties = createSlingModel(PATH).getProperties();

    assertThat(properties.fieldNames())
        .toIterable()
        .containsExactlyInAnyOrder("foo", "test");

    assertThat(properties.path("foo").asText())
        .isEqualTo("bar");

    assertThat(properties.path("test").asInt())
        .isEqualTo(123);
  }

  @Test
  void getProperties_handles_empty_resource_properties_map() {

    context.create().resource(PATH, emptyMap());

    ObjectNode properties = createSlingModel(PATH).getProperties();

    assertThat(properties)
        .isEmpty();
  }

  @Test
  void asAemPage_adapts_page_resource() {

    context.create().page(PATH);

    Optional<AemPage> page = createSlingModel(PATH).asAemPage();

    assertLinkHasHref("/content/foo.aempage.rhyme", page);

    assertThat(page.get().createLink().getTitle())
        .contains("specific to this AEM page");
  }

  @Test
  void asAemPage_returns_empty_for_other_resource() {

    context.create().resource(PATH);

    Optional<AemPage> page = createSlingModel(PATH).asAemPage();

    assertThat(page)
        .isEmpty();
  }

  @Test
  void asAemAsset_adapts_asset_resource() {

    context.create().asset(PATH, 400, 100, "image/jpeg");

    Optional<AemAsset> asset = createSlingModel(PATH).asAemAsset();

    assertLinkHasHref("/content/foo.aemasset.rhyme", asset);

    assertThat(asset.get().createLink().getTitle())
        .contains("specific to this AEM asset");
  }

  @Test
  void asAemAsset_returns_empty_for_other_resource() {

    context.create().resource(PATH);

    Optional<AemAsset> asset = createSlingModel(PATH).asAemAsset();

    assertThat(asset)
        .isEmpty();
  }

  @Test
  void getJcrContentAsJson_adapts_resource_with_content() {

    context.build().resource(PATH).resource("jcr:content");

    Optional<InfinityJsonResource> infinityJson = createSlingModel(PATH).getJcrContentAsJson();

    assertLinkHasHref("/content/foo/_jcr_content.infinity.json", infinityJson);

    assertThat(infinityJson.get().createLink().getTitle())
        .contains("AEM's built-in infinity.json representation");
  }

  @Test
  void getJcrContentAsJson_returns_empty_for_resource_without_content() {

    context.create().resource(PATH);

    Optional<InfinityJsonResource> infinityJson = createSlingModel(PATH).getJcrContentAsJson();

    assertThat(infinityJson)
        .isEmpty();
  }

  @Test
  void getChildren_lists_children() {

    ResourceBuilder parent = context.build().resource(PATH);
    parent.resource("child1");
    parent.resource("child2");

    List<SlingResource> children = createSlingModel(PATH).getChildren()
        .collect(Collectors.toList());

    assertThat(children)
        .hasSize(2)
        .extracting(child -> child.createLink().getName())
        .containsExactly("child1", "child2");
  }

  @Test
  void getChildren_handles_leaf_node() {

    context.build().resource(PATH);

    Stream<SlingResource> children = createSlingModel(PATH).getChildren();

    assertThat(children)
        .isEmpty();
  }

  @Test
  void getParent_finds_existing_parent() {

    context.build().resource(PATH);

    Optional<SlingResource> parent = createSlingModel(PATH).getParent();

    assertLinkHasHref("/content.slingresource.rhyme", parent);
  }

  @Test
  void getParent_handles_root_node() {

    Optional<SlingResource> parent = createSlingModel("/").getParent();

    assertThat(parent)
        .isEmpty();
  }

  private void assertThatLinkHasTitle(String title) {

    SlingResource slingModel = createSlingModel(PATH);

    assertThat(slingModel.createLink().getTitle())
        .isEqualTo(title);
  }

  @Test
  void createLink_adds_title_for_empty_unstructured_node() {

    context.create().resource(PATH);

    assertThatLinkHasTitle("nt:unstructured resource without a title");
  }

  @Test
  void createLink_adds_title_for_node_with_resource_type() {

    String resourceType = "foo/components/bar";

    context.build().resource(PATH, "sling:resourceType", resourceType);

    assertThatLinkHasTitle(resourceType + " resource without a title");
  }

  @Test
  void createLink_adds_title_from_jcr_content_title() {

    String pageTitle = "Foo Bar";

    context.create().page(PATH, "apps/templates/page", pageTitle);

    assertThatLinkHasTitle("cq:Page resource with title '" + pageTitle + "'");
  }

  @Test
  void createLink_adds_title_for_resource_with_jcr_content_but_no_title() {

    context.create().asset(PATH, 400, 100, "image/jpeg");

    assertThatLinkHasTitle("dam:Asset resource without a title");
  }
}

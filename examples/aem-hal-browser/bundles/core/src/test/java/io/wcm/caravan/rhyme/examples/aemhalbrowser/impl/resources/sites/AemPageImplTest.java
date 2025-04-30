package io.wcm.caravan.rhyme.examples.aemhalbrowser.impl.resources.sites;

import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.assertLinkHasHref;
import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.assertResourceCanBeRendered;
import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.createRhymeInstance;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.generic.SlingResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites.AemLinkedContent;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites.AemPage;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class AemPageImplTest {

  private static final String PATH = "/content/foo";

  private AemContext context = AppAemContext.newAemContext();

  private AemPage createSlingModel(String path) {

    SlingRhyme slingRhyme = createRhymeInstance(context, path);

    AemPage page = slingRhyme.adaptTo(AemPage.class);

    assertThat(page)
        .isNotNull();

    return page;
  }

  @Test
  void can_render_minimal_page() {

    context.create().page(PATH);

    AemPage page = createSlingModel(PATH);

    assertResourceCanBeRendered(page);
  }

  @Test
  void getTitle_returns_page_title() {

    String pageTitle = "Foo Page Title";

    context.create().page(PATH, "foo/templates/page", JcrConstants.JCR_TITLE, pageTitle);

    AemPage slingModel = createSlingModel(PATH);

    assertThat(slingModel.getTitle())
        .isEqualTo(pageTitle);
  }

  @Test
  void getTitle_returns_page_name_if_title_is_null() {

    Page page = context.create().page(PATH);

    AemPage slingModel = createSlingModel(PATH);

    assertThat(slingModel.getTitle())
        .isEqualTo(page.getName());
  }

  @Test
  void asSlingResource_returns_link_to_generic_sling_resource() {

    context.create().page(PATH);

    SlingResource slingModel = createSlingModel(PATH).asSlingResource();

    assertThat(slingModel.createLink().getHref())
        .isEqualTo("/content/foo.slingresource.rhyme");
  }

  @Test
  void getParent_returns_page_if_parent_is_a_page() {

    String childPath = PATH + "/bar";

    context.create().page(PATH);
    context.create().page(childPath);

    Optional<AemPage> parentPage = createSlingModel(childPath).getParentPage();

    assertLinkHasHref("/content/foo.aempage.rhyme", parentPage);
  }

  @Test
  void getParent_is_not_present_if_parent_is_not_a_page() {

    String childPath = PATH + "/bar";

    context.create().resource(PATH);
    context.create().page(childPath);

    Optional<AemPage> parentPage = createSlingModel(childPath).getParentPage();

    assertThat(parentPage)
        .isEmpty();
  }

  @Test
  void getChildren_returns_child_pages() {

    context.create().page(PATH);
    context.create().page(PATH + "/child1");
    context.create().page(PATH + "/child2");

    List<AemPage> childPages = createSlingModel(PATH).getChildPages()
        .collect(Collectors.toList());

    assertThat(childPages)
        .hasSize(2)
        .extracting(AemPage::getTitle)
        .containsExactly("child1", "child2");
  }

  @Test
  void getChildren_does_not_return_other_child_resources() {

    context.create().page(PATH);
    context.create().resource(PATH + "/child1");
    context.create().resource(PATH + "/child2");

    List<AemPage> childPages = createSlingModel(PATH).getChildPages()
        .collect(Collectors.toList());

    assertThat(childPages)
        .isEmpty();
  }

  @Test
  void getLinkedContent_returns_embedded_resource_even_for_minimal_page() {

    context.create().page(PATH);

    AemLinkedContent linkedContent = createSlingModel(PATH).getLinkedContent();

    assertThat(linkedContent.isEmbedded())
        .isTrue();
  }

  @Test
  void createLink_uses_page_names_and_title() {

    String pageTitle = "Foo Page Title";

    context.create().page(PATH, "foo/templates/page", JcrConstants.JCR_TITLE, pageTitle);

    Link selfLink = createSlingModel(PATH).createLink();

    assertThat(selfLink.getTitle())
        .isEqualTo(pageTitle);

    assertThat(selfLink.getName())
        .isEqualTo("foo");
  }
}

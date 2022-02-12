package io.wcm.caravan.rhyme.examples.aemhalbrowser.impl.resources;

import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.assertLinkHasHref;
import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.assertResourceCanBeRendered;
import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.createRhymeInstance;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.AemRepository;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemAsset;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemRendition;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.generic.SlingResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites.AemPage;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class AemRepositoryImplTest {

  private static final String CONTENT_PATH = "/content/foo";

  private AemContext context = AppAemContext.newAemContext();

  private AemRepository repository;

  @BeforeEach
  void setUp() {
    SlingRhyme slingRhyme = createRhymeInstance(context, "/");

    repository = slingRhyme.adaptTo(AemRepository.class);

    assertThat(repository)
        .isNotNull();
  }

  @Test
  void can_be_rendererd() {

    assertResourceCanBeRendered(repository);
  }

  @Test
  void getRoot_should_return_linkable_root_resource() {

    SlingResource root = repository.getRoot();

    assertThat(root)
        .isNotNull();

    assertThat(root.createLink().getHref())
        .isEqualTo("/.slingresource.rhyme");
  }

  @Test
  void getResource_with_null_path() {

    Optional<SlingResource> resource = repository.getResource(null);

    assertLinkHasHref("{+path}.slingresource.rhyme", resource);
  }

  @Test
  void getResource_with_valid_path() {

    context.create().resource(CONTENT_PATH);

    Optional<SlingResource> resource = repository.getResource(CONTENT_PATH);

    assertLinkHasHref("/content/foo.slingresource.rhyme", resource);
  }

  @Test
  void getResource_with_non_existent_path() {

    Optional<SlingResource> resource = repository.getResource(CONTENT_PATH);

    assertThat(resource)
        .isNotPresent();
  }

  @Test
  void getPage_with_null_path() {

    Optional<AemPage> page = repository.getPage(null);

    assertLinkHasHref("{+path}.aempage.rhyme", page);
  }

  @Test
  void getPage_with_valid_path() {

    context.create().page(CONTENT_PATH);

    Optional<AemPage> page = repository.getPage(CONTENT_PATH);

    assertLinkHasHref("/content/foo.aempage.rhyme", page);
  }

  @Test
  void getPage_with_path_of_different_type() {

    context.create().resource(CONTENT_PATH);

    Optional<AemPage> page = repository.getPage(CONTENT_PATH);

    assertThat(page)
        .isNotPresent();
  }

  @Test
  void getPage_with_non_existent_path() {

    Optional<AemPage> page = repository.getPage(CONTENT_PATH);

    assertThat(page)
        .isNotPresent();
  }

  @Test
  void getAsset_with_null_value() {

    Optional<AemAsset> asset = repository.getAsset(null);

    assertLinkHasHref("{+path}.aemasset.rhyme", asset);
  }

  @Test
  void getAsset_with_valid_path() {

    context.create().asset(CONTENT_PATH, 400, 100, "image/jpeg");

    Optional<AemAsset> asset = repository.getAsset(CONTENT_PATH);

    assertLinkHasHref("/content/foo.aemasset.rhyme", asset);
  }

  @Test
  void getAsset_with_path_of_different_type() {

    context.create().resource(CONTENT_PATH);

    Optional<AemAsset> asset = repository.getAsset(CONTENT_PATH);

    assertThat(asset)
        .isNotPresent();
  }

  @Test
  void getAsset_with_non_existent_path() {

    Optional<AemAsset> asset = repository.getAsset(CONTENT_PATH);

    assertThat(asset)
        .isNotPresent();
  }

  @Test
  void getRendition_with_null_path() {

    Optional<AemRendition> rendition = repository.getRendition(null, null, null);

    assertLinkHasHref("{+path}.aemrendition.rhyme{?width,height}", rendition);
  }

  @Test
  void getRendition_with_valid_path() {

    context.create().asset(CONTENT_PATH, 400, 100, "image/jpeg");

    Optional<AemRendition> rendition = repository.getRendition(CONTENT_PATH, 123, 456);

    assertLinkHasHref("/content/foo.aemrendition.rhyme?width=123&height=456", rendition);
  }

  @Test
  void getRendition_with_path_of_different_type() {

    context.create().resource(CONTENT_PATH);

    Optional<AemRendition> rendition = repository.getRendition(CONTENT_PATH, 123, 456);

    assertThat(rendition)
        .isEmpty();
  }

  @Test
  void getRendition_with_non_existent_path() {

    Optional<AemRendition> rendition = repository.getRendition(CONTENT_PATH, 123, 456);

    assertThat(rendition)
        .isEmpty();
  }
}

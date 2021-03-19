package io.wcm.caravan.rhyme.aem.integration.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;


public class RhymeObjectsTest {

  private AemContext context = AppAemContext.newAemContext();

  @Test
  public void injectIntoSlingModel_should_inject_SlingRhyme_instance() throws Exception {

    ModelWithSlingRhymeField slingModel = new ModelWithSlingRhymeField();

    RhymeObjects.injectIntoSlingModel(slingModel, () -> new SlingRhymeImpl(context.request()));

    assertThat(slingModel.slingRhyme).isNotNull();
  }

  class ModelWithSlingRhymeField {

    @RhymeObject
    private SlingRhyme slingRhyme;
  }

  @Test
  public void injectIntoSlingModel_should_inject_SlingLinkBuilder_instance() throws Exception {

    ModelWithSlingLinkBuilderField slingModel = new ModelWithSlingLinkBuilderField();

    RhymeObjects.injectIntoSlingModel(slingModel, () -> new SlingRhymeImpl(context.request()));

    assertThat(slingModel.linkBuilder).isNotNull();
  }


  class ModelWithSlingLinkBuilderField {

    @RhymeObject
    private SlingLinkBuilder linkBuilder;
  }
}

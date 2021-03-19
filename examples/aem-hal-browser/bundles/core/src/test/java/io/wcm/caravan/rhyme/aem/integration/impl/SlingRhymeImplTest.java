package io.wcm.caravan.rhyme.aem.integration.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;


public class SlingRhymeImplTest {

  private AemContext context = AppAemContext.newAemContext();

  private SlingRhyme slingRhyme;

  public SlingRhymeImplTest() {
    slingRhyme = context.request().adaptTo(SlingRhymeImpl.class);

    assertThat(slingRhyme).isNotNull();
  }

  @Test
  public void can_be_adapted_to_SlingLinkBuilder() throws Exception {

    SlingLinkBuilder linkBuilder = slingRhyme.adaptTo(SlingLinkBuilder.class);

    assertThat(linkBuilder).isNotNull();
  }

}

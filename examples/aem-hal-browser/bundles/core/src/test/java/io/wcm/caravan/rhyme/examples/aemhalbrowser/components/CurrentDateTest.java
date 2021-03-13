package io.wcm.caravan.rhyme.examples.aemhalbrowser.components;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.sling.commons.adapter.AdaptTo;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;

@ExtendWith(AemContextExtension.class)
class CurrentDateTest {

  private final AemContext context = AppAemContext.newAemContext();

  private CurrentDate underTest;

  @BeforeEach
  void setUp() {
    underTest = AdaptTo.notNull(context.request(), CurrentDate.class);
  }

  @Test
  void testYear() {
    assertTrue(underTest.getYear() >= 2018);
  }

}

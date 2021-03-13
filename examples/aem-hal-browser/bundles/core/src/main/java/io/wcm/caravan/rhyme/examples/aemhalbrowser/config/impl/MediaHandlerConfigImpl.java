package io.wcm.caravan.rhyme.examples.aemhalbrowser.config.impl;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

import com.day.cq.wcm.api.Page;
import com.google.common.collect.ImmutableList;

import io.wcm.handler.media.spi.MediaHandlerConfig;
import io.wcm.handler.media.spi.MediaSource;
import io.wcm.handler.mediasource.dam.DamMediaSource;
import io.wcm.handler.mediasource.inline.InlineMediaSource;

/**
 * Media handler configuration.
 */
@Component(service = MediaHandlerConfig.class)
public class MediaHandlerConfigImpl extends MediaHandlerConfig {

  static final String DAM_ROOT = "/content/dam/aem-hal-browser";

  private static final List<Class<? extends MediaSource>> MEDIA_SOURCES = ImmutableList.<Class<? extends MediaSource>>of(
      DamMediaSource.class,
      InlineMediaSource.class);

  @Override
  public @NotNull List<Class<? extends MediaSource>> getSources() {
    return MEDIA_SOURCES;
  }

  @Override
  public boolean useAdobeStandardNames() {
    // use standard names for asset references as used by the core components
    return true;
  }

  @Override
  public @NotNull String getDamRootPath(@NotNull Page page) {
    return DAM_ROOT;
  }

}

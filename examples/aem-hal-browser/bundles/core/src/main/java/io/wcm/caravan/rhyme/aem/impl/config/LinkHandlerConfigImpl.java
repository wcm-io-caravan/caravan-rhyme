package io.wcm.caravan.rhyme.aem.impl.config;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;

import com.day.cq.wcm.api.Page;
import com.google.common.collect.ImmutableList;

import io.wcm.handler.link.spi.LinkHandlerConfig;
import io.wcm.handler.link.spi.LinkType;
import io.wcm.handler.link.type.ExternalLinkType;
import io.wcm.handler.link.type.InternalCrossContextLinkType;
import io.wcm.handler.link.type.InternalLinkType;
import io.wcm.handler.link.type.MediaLinkType;

/**
 * Link handler configuration.
 */
@Component(service = LinkHandlerConfig.class)
public class LinkHandlerConfigImpl extends LinkHandlerConfig {

  private static final List<Class<? extends LinkType>> DEFAULT_LINK_TYPES = ImmutableList.<Class<? extends LinkType>>of(
      InternalLinkType.class,
      InternalCrossContextLinkType.class,
      ExternalLinkType.class,
      MediaLinkType.class);

  @Override
  public @NotNull List<Class<? extends LinkType>> getLinkTypes() {
    return DEFAULT_LINK_TYPES;
  }

  @Override
  public boolean isValidLinkTarget(@NotNull Page page) {
    return true;
  }

  @Override
  public boolean isRedirect(@NotNull Page page) {
    return false;
  }

  @Override
  public @Nullable String getLinkRootPath(@NotNull Page page, @NotNull String linkTypeId) {
    if (StringUtils.equals(linkTypeId, MediaLinkType.ID)) {
      return MediaHandlerConfigImpl.DAM_ROOT;
    }
    return super.getLinkRootPath(page, linkTypeId);
  }

}

/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.rhyme.spring.api;

import java.time.Duration;


public interface SpringRhyme {

  <T> T getRemoteResource(String uri, Class<T> halApiInterface);

  void setResponseMaxAge(Duration duration);

}

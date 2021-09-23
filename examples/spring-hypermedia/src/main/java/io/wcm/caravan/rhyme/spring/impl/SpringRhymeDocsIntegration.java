/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.spring.impl;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;

/**
 * A {@link RestController} that serves the Rhyme HTML API documentation files which are generated
 * into the application jar files. It's also passed to {@link RhymeBuilder#withRhymeDocsSupport(RhymeDocsSupport)}
 * by {@link SpringRhymeImpl} to enable generation of curies link using the same base path.
 */
@RestController
@RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
public class SpringRhymeDocsIntegration implements RhymeDocsSupport {

  private static final String BASE_PATH = "/docs/rhyme/api/";

  /**
   * @param fileName of a HTML documentation file
   * @return the content of that HTML file from the class path
   */
  @GetMapping(path = BASE_PATH + "{fileName}")
  String getHtml(@PathVariable("fileName") String fileName) {

    return RhymeDocsSupport.loadGeneratedHtml(this, fileName);
  }

  @Override
  public String getRhymeDocsBaseUrl() {

    return BASE_PATH;
  }

  @Override
  public InputStream openResourceStream(String resourcePath) throws IOException {

    Resource resource = new ClassPathResource(resourcePath);

    return resource.getInputStream();
  }

  @Override
  public boolean isFragmentAppendedToCuriesLink() {

    // even though it would be nice for the curies links to point directly to the relevant relation
    // using a link fragment, this is causing some weird scrolling issues in the HAL browser, so it's disabled for now
    return false;
  }

}

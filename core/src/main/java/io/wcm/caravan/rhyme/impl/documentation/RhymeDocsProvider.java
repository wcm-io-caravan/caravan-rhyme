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
package io.wcm.caravan.rhyme.impl.documentation;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;

import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;

public class RhymeDocsProvider {

  private final RhymeDocsSupport support;

  public RhymeDocsProvider(RhymeDocsSupport support) {
    this.support = support;
  }

  public String loadGeneratedHtmlFrom(String fileName) {

    if (support == null) {
      throw new HalApiDeveloperException("HTML documentation can only be served if rhyme docs support was activated through the RhymeBuilder interface");
    }

    String resourcePath = "/" + RhymeDocsSupport.FOLDER + "/" + fileName;

    try (InputStream is = support.openResourceStream(resourcePath)) {
      if (is == null) {
        throw new HalApiServerException(404, "No HTML documentation was generated for " + fileName);
      }
      return IOUtils.toString(is, Charsets.UTF_8);
    }
    catch (HalApiServerException ex) {
      throw ex;
    }
    catch (IOException | RuntimeException ex) {
      throw new HalApiServerException(500, "Failed to load documentation from " + fileName, ex);
    }
  }
}

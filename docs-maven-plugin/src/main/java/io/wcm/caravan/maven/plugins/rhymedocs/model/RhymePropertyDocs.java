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
package io.wcm.caravan.maven.plugins.rhymedocs.model;

import org.apache.commons.lang3.StringUtils;

/**
 * defines getters for resource properties that are exposed to the handlebars template
 */
public interface RhymePropertyDocs {

  String getJsonPointer();

  String getType();

  String getDescription();

  default String getPropertyNameWithPadding() {

    String jsonPointer = getJsonPointer();

    String propertyName = StringUtils.substringAfterLast(jsonPointer, "/");

    int paddingLevel = StringUtils.countMatches(jsonPointer, "/") - 1;

    if ("0".equals(propertyName)) {
      propertyName = "[n]";
    }
    else if (StringUtils.substringBeforeLast(jsonPointer, "/").endsWith("/0")) {
      paddingLevel--;
      propertyName = "[n]." + propertyName;
    }
    else if (paddingLevel > 0) {
      propertyName = "." + propertyName;
    }

    String padding = StringUtils.repeat("&nbsp;", paddingLevel * 3);

    return padding + propertyName;
  }

}

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
package io.wcm.caravan.maven.plugins.rhymedocs.interfaces;

import java.util.List;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariables;

@HalApiInterface
public interface RhymeDocTestResource {

  /**
   * @param collection Javadoc comment for collection
   * @param integer Javadoc comment for integer
   * @return
   */
  @Related(TestRelations.MULTI_VAR_TEMPLATE)
  RhymeDocTestResource multiVarTemplate(
      @TemplateVariable("integer") Integer integer,
      @TemplateVariable("collection") List<String> collection,
      @TemplateVariable("booleanWithoutJavadocs") Boolean booleanWithoutJavaDocs);

  @Related(TestRelations.INTERFACE_TEMPLATE)
  RhymeDocTestResource interfaceTemplate(@TemplateVariables VariableInterface variables);

  @Related(TestRelations.CLASS_TEMPLATE)
  RhymeDocTestResource classTemplate(@TemplateVariables VariableClass variables);
}

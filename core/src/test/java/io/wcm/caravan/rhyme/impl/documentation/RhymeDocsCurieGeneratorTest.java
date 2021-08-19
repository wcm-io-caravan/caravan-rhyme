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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.relations.StandardRelations;
import io.wcm.caravan.ryhme.testing.resources.TestResource;
import io.wcm.caravan.ryhme.testing.resources.TestResourceTree;


public class RhymeDocsCurieGeneratorTest {

  private static final String DOCS = "/docs/";

  private TestResource testResource = new TestResourceTree().getEntryPoint();

  private List<Link> addAndGetCuries(String baseUrl) {

    RhymeDocsCurieGenerator curies = new RhymeDocsCurieGenerator(baseUrl);

    curies.addCuriesTo(testResource.asHalResource(), RhymeDocsTestResource.class);

    return testResource.asHalResource().getLinks("curies");
  }

  @Test
  public void addCuriesTo_should_not_add_curies_for_standard_relation() throws Exception {

    testResource.createLinked(StandardRelations.ITEM);

    List<Link> curies = addAndGetCuries(DOCS);

    assertThat(curies).isEmpty();
  }

  @Test
  public void addCuriesTo_should_add_curies_for_custom_relations() throws Exception {

    testResource.createLinked("test:foo");

    List<Link> curies = addAndGetCuries(DOCS);

    assertThat(curies).extracting(Link::getName)
        .containsExactly("test");
  }

  @Test
  public void addCuriesTo_should_not_repeat_curies_for_multiple_relations_with_same_prefix() throws Exception {

    testResource.createLinked("test:foo");
    testResource.createLinked("test:bar");

    List<Link> curies = addAndGetCuries(DOCS);

    assertThat(curies).extracting(Link::getName)
        .containsExactly("test");
  }

  @Test
  public void addCuriesTo_should_prepend_file_name_to_base_url() throws Exception {

    testResource.createLinked("test:foo");

    List<Link> curies = addAndGetCuries(DOCS);

    assertThat(curies.get(0).getHref())
        .startsWith(DOCS + RhymeDocsTestResource.class.getName() + ".html");
  }

  @Test
  public void addCuriesTo_should_append_slash_to_base_url_if_required() throws Exception {

    testResource.createLinked("test:foo");

    List<Link> curies = addAndGetCuries("/api");

    String href = curies.get(0).getHref();

    assertThat(href)
        .startsWith("/api/" + RhymeDocsTestResource.class.getName() + ".html");
  }

  @Test
  public void addCuriesTo_should_add_relation_fragment_template() throws Exception {

    testResource.createLinked("test:foo");

    List<Link> curies = addAndGetCuries(DOCS);

    assertThat(curies.get(0).getHref())
        .endsWith("#test:{rel}");
  }

  @Test
  public void addCuriesTo_should_add_link_title() throws Exception {

    testResource.createLinked("test:foo");

    List<Link> curies = addAndGetCuries(DOCS);

    assertThat(curies.get(0).getTitle())
        .isEqualTo("Documentation for relations with test prefix");
  }

}
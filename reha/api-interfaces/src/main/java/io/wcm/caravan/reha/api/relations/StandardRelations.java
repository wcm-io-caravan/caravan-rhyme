/*-
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 - 2020 wcm.io Caravan
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

package io.wcm.caravan.reha.api.relations;

import io.wcm.caravan.reha.api.annotations.RelatedResource;

/**
 * Constants for standard link relations used in {@link RelatedResource} annotations.
 * @see <a href="http://www.iana.org/assignments/link-relations/link-relations.xhtml">IANA Link Relation Types</a>
 */
public final class StandardRelations {

  private StandardRelations() {
    // constants only
  }

  /**
   * Refers to a resource that is the subject of the link's context.
   * @see <a href="https://www.iana.org/go/rfc6903">RFC6903, section 2</a>
   */
  public static final String ABOUT = "about";

  /**
   * Refers to a substitute for this context
   * @see <a href="http://www.w3.org/TR/html5/links.html#link-type-alternate">HTML5 link-type-alternate</a>
   */
  public static final String ALTERNATE = "alternate";

  /**
   * Designates the preferred version of a resource (the IRI and its contents).
   * @see <a href="http://www.iana.org/go/rfc6596">RFC6596</a>
   */
  public static final String CANONICAL = "canonical";

  /**
   * The target IRI points to a resource which represents the collection resource for the context IRI.
   * @see <a href="http://www.iana.org/go/rfc6573">RFC6573</a>
   */
  public static final String COLLECTION = "collection";

  /**
   * Refers to a resource that is not part of the same site as the current context.
   * @see <a href="https://html.spec.whatwg.org/multipage/links.html#link-type-external">HTML Spec</a>
   */
  public static final String EXTERNAL = "external";

  /**
   * An IRI that refers to the furthest preceding resource in a series of resources.
   * @see <a href="http://www.iana.org/go/rfc5988">RFC5988</a>
   */
  public static final String FIRST = "first";

  /**
   * Refers to an index.
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224">HTML 4.01 Specification</a>
   */
  public static final String INDEX = "index";

  /**
   * The target IRI points to a resource that is a member of the collection represented by the context IRI.
   * @see <a href="http://www.iana.org/go/rfc6573">RFC6573</a>
   */
  public static final String ITEM = "item";

  /**
   * An IRI that refers to the furthest following resource in a series of resources.
   * @see <a href="http://www.iana.org/go/rfc5988">RFC5988</a>
   */
  public static final String LAST = "last";

  /**
   * The Target IRI points to a Memento, a fixed resource that will not change state anymore
   * @see <a href="https://www.iana.org/go/rfc7089">RFC7089</a>
   */
  public static final String MEMENTO = "memento";

  /**
   * Indicates that the link's context is a part of a series, and that the next in the series is the link target.
   * @see <a href="http://www.w3.org/TR/html5/links.html#link-type-next">HTML5 Recommendation</a>
   */
  public static final String NEXT = "next";

  /**
   * Indicates that the link's context is a part of a series, and that the previous in the series is the link target.
   * @see <a href="http://www.w3.org/TR/html5/links.html#link-type-next">HTML5 Recommendation</a>
   */
  public static final String PREV = "prev";

  /**
   * Identifies a related resource.
   * @see <a href="http://www.iana.org/go/rfc4287">RFC4287</a>
   */
  public static final String RELATED = "related";

  /**
   * Refers to a resource that can be used to search through the link's context and related resources.
   * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/1.1">OpenSearch</a>
   */
  public static final String SEARCH = "search";

  /**
   * Refers to a section in a collection of resources.
   * @see <a href="https://www.w3.org/TR/html401/">HTML 4.01 Specification</a>
   */
  public static final String SECTION = "section";

  /**
   * Conveys an identifier for the link's context.
   * @see <a href="http://www.iana.org/go/rfc4287">RFC4287</a>
   */
  public static final String SELF = "self";

  /**
   * Refers to a resource serving as a subsection in a collection of resources.
   * @see <a href="https://www.w3.org/TR/html401/">HTML 4.01 Specification</a>
   */
  public static final String SUBSECTION = "subsection";

  /**
   * Refers to a parent document in a hierarchy of documents.
   * @see <a href="http://www.iana.org/go/rfc5988">RFC5988</a>
   */
  public static final String UP = "up";

  /**
   * Identifies a resource that is the source of the information in the link's context.
   * @see <a href="http://www.iana.org/go/rfc4287">RFC4287</a>
   */
  public static final String VIA = "via";

}

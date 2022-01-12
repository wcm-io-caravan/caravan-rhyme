/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
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
package io.wcm.caravan.rhyme.impl.client.http;

import static io.wcm.caravan.rhyme.impl.client.http.HttpHeadersParser.parseMaxAge;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class HttpHeadersParserTest {

  @Test
  void parseMaxAge_should_handle_null_value() {

    assertThat(parseMaxAge(null))
        .isNull();
  }

  @Test
  void parseMaxAge_should_handle_simple_value() {

    assertThat(parseMaxAge("max-age=123"))
        .isEqualTo(123);
  }

  @Test
  void parseMaxAge_should_handle_immutable() {

    assertThat((long)parseMaxAge("public, max-age=604800, immutable"))
        .isEqualTo(Duration.ofDays(365).getSeconds());
  }

  @Test
  void parseMaxAge_should_handle_empty_value() {

    assertThat(parseMaxAge(""))
        .isNull();
  }

  @Test
  void parseMaxAge_should_handle_no_store_as_0() {

    assertThat(parseMaxAge("no-store"))
        .isZero();
  }

  @Test
  void parseMaxAge_should_handle_uppser_case() {

    assertThat(parseMaxAge("MAX-AGE=123"))
        .isEqualTo(123);
  }

  @Test
  void parseMaxAge_should_ignore_preceeding_directives() {

    assertThat(parseMaxAge("public, max-age=456"))
        .isEqualTo(456);
  }

  @Test
  void parseMaxAge_should_ignore_following_directives() {

    assertThat(parseMaxAge("max-age=604800, must-revalidate"))
        .isEqualTo(604800);
  }


  @Test
  void parseMaxAge_should_ignore_multiple_commas() {

    assertThat(parseMaxAge("max-age=604800,, must-revalidate"))
        .isEqualTo(604800);
  }

  @Test
  void parseMaxAge_should_handle_extra_blanks() {

    assertThat(parseMaxAge(" max-age=604800 , must-revalidate"))
        .isEqualTo(604800);
  }

  @Test
  void parseMaxAge_should_trim_number_larger_then_max_int() {

    Long longerThanMaxInt = 2l * Integer.MAX_VALUE;

    assertThat(parseMaxAge("max-age=" + longerThanMaxInt))
        .isEqualTo(Integer.MAX_VALUE);
  }
}

/*
 * Copyright 2017 the original author or authors.
 *
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
 */
package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Greg Turnquist
 */
@Data
@Entity
@EntityListeners(RepositoryModificationListener.class)
@NoArgsConstructor
public class Manager {

  /** a generated ID (to be used in all the link templates) */
  @Id
  @GeneratedValue
  private Long id;

  /** the first name */
  private String name;

  /**
   * To break the recursive, bi-directional interface, don't serialize
   * {@literal employees}.
   */
  @JsonIgnore //
  @OneToMany(mappedBy = "manager") //
  private List<Employee> employees = new ArrayList<>();

  Manager(String name) {
    this.name = name;
  }

}
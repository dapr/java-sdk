/*
 * Copyright 2024 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.it.spring.data;

import org.springframework.data.annotation.Id;

import java.util.Objects;

public class TestType {

  @Id
  private Integer id;
  private String content;

  public TestType() {
  }

  public TestType(Integer id, String content) {
    this.id = id;
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  public Integer getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TestType testType = (TestType) o;
    return Objects.equals(id, testType.id) && Objects.equals(content, testType.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, content);
  }
}

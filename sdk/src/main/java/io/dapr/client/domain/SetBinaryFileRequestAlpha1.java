/*
 * Copyright 2026 The Dapr Authors
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

package io.dapr.client.domain;

import reactor.core.publisher.Flux;

/**
 * Request to store a file in a Dapr binary store component.
 *
 * <p>This is an alpha API. Its name and behavior can change in a later release.
 */
public class SetBinaryFileRequestAlpha1 {

  private final String storeName;
  private final String fileName;
  private final Flux<byte[]> content;
  private boolean overwrite;

  /**
   * Constructor for SetBinaryFileRequestAlpha1.
   *
   * @param storeName Name of the binary store component. Required.
   * @param fileName  Name of the file to store. Required.
   * @param content   File content as a stream of chunks. Required. A chunk can have any size.
   */
  public SetBinaryFileRequestAlpha1(String storeName, String fileName, Flux<byte[]> content) {
    this.storeName = storeName;
    this.fileName = fileName;
    this.content = content;
  }

  /**
   * Gets the binary store component name.
   *
   * @return the store name
   */
  public String getStoreName() {
    return storeName;
  }

  /**
   * Gets the name of the file to store.
   *
   * @return the file name
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Gets the file content.
   *
   * @return the content as a Flux of byte arrays
   */
  public Flux<byte[]> getContent() {
    return content;
  }

  /**
   * Gets whether the request replaces an existing file.
   *
   * @return true to replace an existing file, false to fail when the file exists
   */
  public boolean isOverwrite() {
    return overwrite;
  }

  /**
   * Sets whether the request replaces an existing file. The default is false.
   *
   * @param overwrite true to replace an existing file, false to fail when the file exists
   * @return this request instance for method chaining
   */
  public SetBinaryFileRequestAlpha1 setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
    return this;
  }
}

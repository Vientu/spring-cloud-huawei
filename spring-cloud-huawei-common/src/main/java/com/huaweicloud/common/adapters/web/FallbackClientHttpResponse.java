/*

 * Copyright (C) 2020-2022 Huawei Technologies Co., Ltd. All rights reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huaweicloud.common.adapters.web;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.AbstractClientHttpResponse;

public class FallbackClientHttpResponse extends AbstractClientHttpResponse {
  private final int code;

  private final String message;

  public FallbackClientHttpResponse(int code, String message) {
    this.code = code;
    this.message = message;
  }

  @Override
  public int getRawStatusCode() {
    return this.code;
  }

  @Override
  public String getStatusText() {
    return this.message;
  }

  @Override
  public void close() {

  }

  @Override
  public InputStream getBody() {
    return new ByteArrayInputStream(this.message.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public HttpHeaders getHeaders() {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/text");
    return httpHeaders;
  }
}

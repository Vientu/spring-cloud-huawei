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

package com.huaweicloud.governance.adapters.webmvc;

import org.apache.servicecomb.governance.handler.BulkheadHandler;
import org.apache.servicecomb.governance.handler.CircuitBreakerHandler;
import org.apache.servicecomb.governance.handler.RateLimitingHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter")
public class WebMvcConfiguration {
  @Bean
  @ConditionalOnProperty(value = "spring.cloud.servicecomb.webmvc.governance.enabled",
      havingValue = "true", matchIfMissing = true)
  public GovernanceRequestMappingHandlerAdapter governanceRequestMappingHandlerAdapter(
      RateLimitingHandler rateLimitingHandler, CircuitBreakerHandler circuitBreakerHandler,
      BulkheadHandler bulkheadHandler
  ) {
    return new GovernanceRequestMappingHandlerAdapter(rateLimitingHandler, circuitBreakerHandler, bulkheadHandler);
  }
}

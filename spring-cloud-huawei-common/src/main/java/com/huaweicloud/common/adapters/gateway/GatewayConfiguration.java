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

package com.huaweicloud.common.adapters.gateway;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.huaweicloud.common.configration.dynamic.ContextProperties;
import com.huaweicloud.common.metrics.InvocationMetrics;

@Configuration
@ConditionalOnClass(name = {"org.springframework.cloud.gateway.filter.GlobalFilter"})
public class GatewayConfiguration {
  @Bean
  public DecorateGlobalFilter decorateGlobalFilter(
      @Autowired(required = false) List<PreGlobalFilter> preGlobalFilters,
      @Autowired(required = false) List<PostGlobalFilter> postGlobalFilters,
      InvocationMetrics invocationMetrics) {
    return new DecorateGlobalFilter(
        preGlobalFilters,
        postGlobalFilters,
        invocationMetrics);
  }

  @Bean
  @ConditionalOnBean(DecorateGlobalFilter.class)
  public PreGlobalFilter deserializeContextPreGlobalFilter() {
    return new DeserializeContextPreGlobalFilter();
  }

  @Bean
  @ConditionalOnBean(DecorateGlobalFilter.class)
  public PreGlobalFilter serializeContextPreGlobalFilter() {
    return new SerializeContextPreGlobalFilter();
  }

  @Bean
  @ConditionalOnBean(DecorateGlobalFilter.class)
  public PreGlobalFilter metricsPreGlobalFilter() {
    return new MetricsPreGlobalFilter();
  }

  @Bean
  @ConditionalOnBean(DecorateGlobalFilter.class)
  public PreGlobalFilter traceIdPreGlobalFilter(ContextProperties contextProperties) {
    return new TraceIdPreGlobalFilter(contextProperties);
  }
}

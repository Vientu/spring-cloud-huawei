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

package com.huaweicloud.common.adapters.webmvc;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.huaweicloud.common.configration.dynamic.ContextProperties;
import com.huaweicloud.common.metrics.InvocationMetrics;

@Configuration
@ConditionalOnClass(name = "org.springframework.web.servlet.config.annotation.WebMvcConfigurer")
public class WebMvcConfiguration {
  static class WebMvcConfigurerBean implements WebMvcConfigurer {
    private final List<PreHandlerInterceptor> preHandlerInterceptors;

    private final List<PostHandlerInterceptor> postHandlerInterceptors;

    private final InvocationMetrics invocationMetrics;

    WebMvcConfigurerBean(List<PreHandlerInterceptor> preHandlerInterceptors,
        List<PostHandlerInterceptor> postHandlerInterceptors, InvocationMetrics invocationMetrics) {
      this.preHandlerInterceptors = preHandlerInterceptors;
      this.postHandlerInterceptors = postHandlerInterceptors;
      this.invocationMetrics = invocationMetrics;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(
              new DecorateHandlerInterceptor(preHandlerInterceptors, postHandlerInterceptors, invocationMetrics))
          .addPathPatterns("/**");
    }
  }

  @Bean
  public WebMvcConfigurer webMvcConfigurer(
      @Autowired(required = false) List<PreHandlerInterceptor> preHandlerInterceptors,
      @Autowired(required = false) List<PostHandlerInterceptor> postHandlerInterceptors,
      InvocationMetrics invocationMetrics) {
    return new WebMvcConfigurerBean(preHandlerInterceptors, postHandlerInterceptors, invocationMetrics);
  }

  @Bean
  public PreHandlerInterceptor deserializeContextPreHandlerInterceptor() {
    return new DeserializeContextPreHandlerInterceptor();
  }

  @Bean
  public PreHandlerInterceptor metricsPreHandlerInterceptor() {
    return new MetricsPreHandlerInterceptor();
  }

  @Bean
  public PreHandlerInterceptor traceIdPreHandlerInterceptor(ContextProperties contextProperties) {
    return new TraceIdPreHandlerInterceptor(contextProperties);
  }
}

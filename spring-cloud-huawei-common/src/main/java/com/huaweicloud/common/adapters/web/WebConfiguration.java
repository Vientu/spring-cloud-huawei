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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import com.huaweicloud.common.configration.dynamic.ContextProperties;
import com.huaweicloud.common.event.ClosedEventListener;

@Configuration
@ConditionalOnClass(name = {"org.springframework.http.client.ClientHttpRequestInterceptor",
    "org.springframework.web.client.RestTemplate"})
public class WebConfiguration {
  @Bean
  public DecorateClientHttpRequestInterceptor decorateClientHttpRequestInterceptor(
      ContextProperties contextProperties,
      ClosedEventListener closedEventListener,
      @Autowired(required = false) List<PreClientHttpRequestInterceptor> preClientHttpRequestInterceptors,
      @Autowired(required = false) List<PostClientHttpRequestInterceptor> postClientHttpRequestInterceptors) {
    return new DecorateClientHttpRequestInterceptor(
        contextProperties,
        closedEventListener,
        preClientHttpRequestInterceptors,
        postClientHttpRequestInterceptors);
  }

  @Bean
  @ConditionalOnProperty(value = "spring.cloud.servicecomb.web.context.enabled",
      havingValue = "true", matchIfMissing = true)
  // sort ClientHttpRequestInterceptors.
  // If ClientHttpRequestInterceptor does not implement Ordered, executed first, and then ordered .
  // And make LoadBalancerInterceptor the first ordered ClientHttpRequestInterceptor.
  public RestTemplateCustomizer restTemplateCustomizer(List<ClientHttpRequestInterceptor> interceptors) {
    return restTemplate -> {
      List<ClientHttpRequestInterceptor> nonOrderedList = new ArrayList<>();
      List<ClientHttpRequestInterceptor> orderedList = new ArrayList<>();
      LoadBalancerInterceptor loadBalancerInterceptor = null;

      for (ClientHttpRequestInterceptor interceptor : interceptors) {
        if (interceptor instanceof LoadBalancerInterceptor) {
          loadBalancerInterceptor = (LoadBalancerInterceptor) interceptor;
          continue;
        }
        if (interceptor instanceof Ordered) {
          orderedList.add(interceptor);
        } else {
          nonOrderedList.add(interceptor);
        }
      }
      orderedList.sort(Comparator.comparingInt(a -> ((Ordered) a).getOrder()));
      if (loadBalancerInterceptor != null) {
        nonOrderedList.add(loadBalancerInterceptor);
      }
      nonOrderedList.addAll(orderedList);

      // avoid restTemplate sort himself
      restTemplate.setInterceptors(new ArrayList<>());
      restTemplate.getInterceptors().addAll(nonOrderedList);
    };
  }

  @Bean
  @ConditionalOnBean(DecorateClientHttpRequestInterceptor.class)
  public PreClientHttpRequestInterceptor addContextPreClientHttpRequestInterceptor() {
    return new SerializeContextPreClientHttpRequestInterceptor();
  }

  @Bean
  @ConditionalOnBean(DecorateClientHttpRequestInterceptor.class)
  public TraceIdPreClientHttpRequestInterceptor traceIdPreClientHttpRequestInterceptor(
      ContextProperties contextProperties) {
    return new TraceIdPreClientHttpRequestInterceptor(contextProperties);
  }
}

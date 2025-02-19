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
package com.huaweicloud.sample;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.huaweicloud.common.context.InvocationContext;
import com.huaweicloud.common.context.InvocationContextHolder;

@RestController
public class OrderController {

  private final DiscoveryClient discoveryClient;

  private final RestTemplate restTemplate;

  private final FeignService feignService;

  @Autowired
  public OrderController(DiscoveryClient discoveryClient, RestTemplate restTemplate, FeignService feignService) {
    this.discoveryClient = discoveryClient;
    this.restTemplate = restTemplate;
    this.feignService = feignService;
  }

  @RequestMapping("/instances")
  public Object instances() {
    return discoveryClient.getInstances("price");
  }

  @RequestMapping("/order")
  public String getOrder(@RequestParam("id") String id) {
    String callServiceResult = restTemplate.getForObject("http://price/price?id=" + id, String.class);
    return callServiceResult;
  }

  @RequestMapping("/configuration")
  public String getEnums() {
    return restTemplate.getForObject("http://price/configuration", String.class);
  }

  @RequestMapping("/invocationContext")
  public String invocationContext() {
    InvocationContext invocationContext = InvocationContextHolder.getOrCreateInvocationContext();
    if (!"test01".equals(invocationContext.getContext("test01"))) {
      return null;
    }
    invocationContext.putContext("test02", "test02");
    return restTemplate.getForObject("http://price/invocationContext", String.class);
  }

  @RequestMapping("/invocationContextGateway")
  public String invocationContextGateway() {
    InvocationContext invocationContext = InvocationContextHolder.getOrCreateInvocationContext();
    if (!"test01".equals(invocationContext.getContext("test01"))) {
      return null;
    }
    if (!"test03".equals(invocationContext.getContext("test03"))) {
      return null;
    }
    if (!"discovery-gateway".equals(invocationContext.getContext(InvocationContext.CONTEXT_MICROSERVICE_NAME))) {
      return null;
    }
    if (StringUtils.isEmpty(invocationContext.getContext(InvocationContext.CONTEXT_INSTANCE_ID))) {
      return null;
    }
    return "success";
  }

  @RequestMapping("/invocationContextFeign")
  public String invocationContextFeign() {
    InvocationContext invocationContext = InvocationContextHolder.getOrCreateInvocationContext();
    if (!"test01".equals(invocationContext.getContext("test01"))) {
      return null;
    }
    invocationContext.putContext("test02", "test02");
    return feignService.invocationContext();
  }

  @RequestMapping(value = "/services", method = RequestMethod.GET)
  public Object services() {
    return discoveryClient.getServices();
  }

  @RequestMapping("/crossappinstances")
  public Object crossAppInstances() {
    return discoveryClient.getInstances("account-app.account");
  }

  @RequestMapping("/crossapporder")
  public String getCrossAppOrder(@RequestParam("id") String id) {
    return restTemplate.getForObject("http://account-app.account/account?id=" + id, String.class);
  }
}

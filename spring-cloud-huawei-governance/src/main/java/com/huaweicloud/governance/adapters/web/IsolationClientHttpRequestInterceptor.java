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

package com.huaweicloud.governance.adapters.web;

import org.apache.servicecomb.governance.handler.InstanceIsolationHandler;
import org.apache.servicecomb.governance.handler.ext.ClientRecoverPolicy;
import org.apache.servicecomb.governance.marker.GovernanceRequest;
import org.apache.servicecomb.governance.policy.CircuitBreakerPolicy;
import org.apache.servicecomb.service.center.client.DiscoveryEvents.PullInstanceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import com.huaweicloud.common.adapters.loadbalancer.RetryContext;
import com.huaweicloud.common.adapters.web.FallbackClientHttpResponse;
import com.huaweicloud.common.context.InvocationContextHolder;
import com.huaweicloud.common.event.EventManager;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.decorators.Decorators.DecorateCheckedSupplier;
import io.vavr.CheckedFunction0;

public class IsolationClientHttpRequestInterceptor implements ClientHttpRequestInterceptor, Ordered {
  private static final Logger LOG = LoggerFactory.getLogger(IsolationClientHttpRequestInterceptor.class);


  private static final int ORDER = 100;

  private final InstanceIsolationHandler instanceIsolationHandler;

  private final ClientRecoverPolicy<ClientHttpResponse> clientRecoverPolicy;

  public IsolationClientHttpRequestInterceptor(InstanceIsolationHandler instanceIsolationHandler,
      ClientRecoverPolicy<ClientHttpResponse> clientRecoverPolicy) {
    this.instanceIsolationHandler = instanceIsolationHandler;
    this.clientRecoverPolicy = clientRecoverPolicy;
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
    CheckedFunction0<ClientHttpResponse> next = () -> execution.execute(request, body);

    DecorateCheckedSupplier<ClientHttpResponse> dcs = Decorators.ofCheckedSupplier(next);

    GovernanceRequest governanceRequest = convert(request);
    try {
      CircuitBreakerPolicy circuitBreakerPolicy = instanceIsolationHandler.matchPolicy(governanceRequest);
      if (circuitBreakerPolicy != null && circuitBreakerPolicy.isForceOpen()) {
        return new FallbackClientHttpResponse(503,
            "Policy " + circuitBreakerPolicy.getName() + " forced open and deny requests");
      }

      if (circuitBreakerPolicy != null && !circuitBreakerPolicy.isForceClosed()) {
        addInstanceIsolation(dcs, governanceRequest);
      }

      return dcs.get();
    } catch (Throwable e) {
      if (e instanceof CallNotPermittedException) {
        // when instance isolated, request to pull instances.
        LOG.warn("instance isolated [{}]", governanceRequest.getInstanceId());
        EventManager.post(new PullInstanceEvent());
      }
      if (clientRecoverPolicy != null) {
        return clientRecoverPolicy.apply(e);
      }
      LOG.error("instance isolation catch throwable", e);
      // return 503, so that we can retry
      return new FallbackClientHttpResponse(503, e.getMessage());
    }
  }

  private GovernanceRequest convert(HttpRequest request) {
    GovernanceRequest governanceRequest = new GovernanceRequest();
    governanceRequest.setUri(request.getURI().getPath());
    governanceRequest.setMethod(request.getMethod().name());
    governanceRequest.setHeaders(request.getHeaders().toSingleValueMap());

    RetryContext retryContext = InvocationContextHolder.getOrCreateInvocationContext()
        .getLocalContext(RetryContext.RETRY_CONTEXT);
    if (retryContext != null && retryContext.getLastServer() != null) {
      governanceRequest.setServiceName(retryContext.getLastServer().getServiceId());
      governanceRequest.setInstanceId(retryContext.getLastServer().getInstanceId());
    }
    return governanceRequest;
  }

  private void addInstanceIsolation(DecorateCheckedSupplier<ClientHttpResponse> dcs,
      GovernanceRequest governanceRequest) {
    CircuitBreaker circuitBreaker = instanceIsolationHandler.getActuator(governanceRequest);
    if (circuitBreaker != null) {
      dcs.withCircuitBreaker(circuitBreaker);
    }
  }

  @Override
  public int getOrder() {
    return ORDER;
  }
}

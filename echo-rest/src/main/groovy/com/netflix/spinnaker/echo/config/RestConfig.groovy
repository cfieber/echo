/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.echo.config

import com.netflix.spinnaker.retrofit.Slf4jRetrofitLogger
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties

import static retrofit.Endpoints.newFixedEndpoint

import com.netflix.spinnaker.echo.rest.RestService
import groovy.transform.CompileStatic
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import retrofit.RestAdapter
import retrofit.RestAdapter.LogLevel
import retrofit.client.Client
import retrofit.client.OkClient
import retrofit.converter.JacksonConverter

/**
 * Rest endpoint configuration
 */
@Configuration
@ConditionalOnProperty('rest.enabled')
@CompileStatic
@EnableConfigurationProperties(RestProperties)
class RestConfig {

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  Client retrofitClient() {
    new OkClient()
  }

  @Bean
  RestUrls restServices(RestProperties restProperties, Client retrofitClient, LogLevel retrofitLogLevel) {

    RestUrls restUrls = new RestUrls()

    restProperties

    restProperties.endpoints.each { endpoint ->
      restUrls.services.add(
        [
          client: new RestAdapter.Builder()
            .setEndpoint(newFixedEndpoint(endpoint.url as String))
            .setClient(retrofitClient)
            .setLogLevel(retrofitLogLevel)
            .setLog(new Slf4jRetrofitLogger(RestService))
            .setConverter(new JacksonConverter())
            .build()
            .create(RestService),
          config: endpoint
        ]
      )
    }

    restUrls
  }

}

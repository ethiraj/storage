// Copyright 2017-2021, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.storage.provider.azure.di.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "cache.provider", havingValue = "vm", matchIfMissing = true)
public class VmConfig {

  @Bean
  public VmCache<String, Groups> groupCache(@Value("${group.cache.expiration}") final int expiration,
      @Value("${vm.cache.maxSize}") final int maxSize) {
    return new VmCache<>(expiration * 60, maxSize);
  }

  @Bean("legalTagCache")
  public VmCache<String, String> legalTagCache(@Value("${legalTag.cache.expiration}") final int expiration,
      @Value("${vm.cache.maxSize}") final int maxSize) {
    return new VmCache<>(expiration * 60, maxSize);
  }

  @Bean
  public VmCache<String, Schema> schemaCache(@Value("${schema.cache.expiration}") final int expiration,
      @Value("${vm.cache.maxSize}") final int maxSize) {
    return new VmCache<>(expiration * 60, maxSize);
  }

}

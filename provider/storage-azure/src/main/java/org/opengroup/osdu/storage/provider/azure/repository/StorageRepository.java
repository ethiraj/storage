/*
 * Copyright 2020  Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.storage.provider.azure.repository;

import org.opengroup.osdu.azure.CosmosStore;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Repository;

@Repository
public class StorageRepository {

  @Autowired
  private CosmosStore cosmosStore;

  @Autowired
  private String cosmosContainer;

  @Autowired
  private String cosmosDBName;

  @Autowired
  private DpsHeaders headers;

  public Object save(Object entity) {
    if (entity == null) {
      throw new IllegalArgumentException("The given entity is null");
    }

    cosmosStore.upsertItem(headers.getPartitionId(), cosmosDBName, cosmosContainer, entity);
    return entity;
  }
}

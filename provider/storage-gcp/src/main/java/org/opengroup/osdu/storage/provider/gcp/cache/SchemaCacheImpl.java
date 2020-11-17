// Copyright 2017-2019, Schlumberger
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

package org.opengroup.osdu.storage.provider.gcp.cache;

import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.storage.cache.SchemaCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SchemaCacheImpl extends RedisCache<String, Schema> implements SchemaCache {

	public SchemaCacheImpl(@Value("${REDIS_STORAGE_HOST}") final String REDIS_STORAGE_HOST, @Value("${REDIS_STORAGE_PORT}") final String REDIS_STORAGE_PORT) {
		super(REDIS_STORAGE_HOST, Integer.parseInt(REDIS_STORAGE_PORT), 60 * 60, String.class,
				Schema.class);
	}
}
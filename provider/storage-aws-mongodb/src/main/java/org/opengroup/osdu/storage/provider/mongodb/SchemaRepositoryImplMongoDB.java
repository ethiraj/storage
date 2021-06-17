// Copyright © Amazon Web Services
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

package org.opengroup.osdu.storage.provider.mongodb;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.mongodb.core.storage.helper.SchemaHelper;
import org.opengroup.osdu.storage.provider.interfaces.ISchemaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class SchemaRepositoryImplMongoDB implements ISchemaRepository {

    private DpsHeaders headers;
    private final SchemaHelper schemaHelper;

    public SchemaRepositoryImplMongoDB(DpsHeaders headers, SchemaHelper schemaHelper) {
        this.headers = headers;
        this.schemaHelper = schemaHelper;
    }

    @Override
    public void add(Schema schema, String user) {
        schemaHelper.add(schema, user, headers.getPartitionId());
    }

    @Override
    public Schema get(String kind) {
        return schemaHelper.get(kind);
    }

    @Override
    public void delete(String kind) {
        schemaHelper.delete(kind);
    }

}

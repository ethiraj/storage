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

import org.opengroup.osdu.core.common.model.legal.LegalCompliance;
import org.opengroup.osdu.core.common.model.storage.RecordMetadata;
import org.opengroup.osdu.core.mongodb.helper.RecordHelper;
import org.opengroup.osdu.storage.provider.interfaces.IRecordsMetadataRepository;
import org.springframework.stereotype.Repository;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

@Repository
public class RecordsMetadataRepositoryImplMongoDB implements IRecordsMetadataRepository<String> {

    private final RecordHelper recordHelper;

    public RecordsMetadataRepositoryImplMongoDB(RecordHelper recordHelper) {
        this.recordHelper = recordHelper;
    }

    @Override
    public List<RecordMetadata> createOrUpdate(List<RecordMetadata> recordsMetadata) {
        recordHelper.save(recordsMetadata);
        return recordsMetadata;
    }

    @Override
    public void delete(String id) {
        recordHelper.delete(id);
    }

    @Override
    public RecordMetadata get(String id) {
        return recordHelper.get(id);
    }

    @Override
    public Map<String, RecordMetadata> get(List<String> ids) {
        return recordHelper.get(ids);
    }

    @Override
    public AbstractMap.SimpleEntry<String, List<RecordMetadata>> queryByLegalTagName(
            String legalTagName, int limit, String cursor) {
        return recordHelper.queryByLegalTagName(legalTagName, limit, cursor);
    }

    @Override
    public AbstractMap.SimpleEntry<String, List<RecordMetadata>> queryByLegal(String legalTagName, LegalCompliance status, int limit) {
        return null;
    }

}

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
import org.opengroup.osdu.core.common.model.storage.DatastoreQueryResult;
import org.opengroup.osdu.core.mongodb.helper.QueryHelper;
import org.opengroup.osdu.storage.provider.interfaces.IQueryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class QueryRepositoryImplMongoDB implements IQueryRepository {

    private DpsHeaders headers;
    private final QueryHelper queryHelper;

    @Autowired
    public QueryRepositoryImplMongoDB(DpsHeaders headers, QueryHelper queryHelper) {
        this.headers = headers;
        this.queryHelper = queryHelper;
    }

    @Override
    public DatastoreQueryResult getAllKinds(Integer limit, String cursor) {

        // Set the page size or use the default constant
        int numRecords = PAGE_SIZE;
        if (limit != null) {
            numRecords = limit > 0 ? limit : PAGE_SIZE;
        }

        DatastoreQueryResult response = queryHelper.getAllKinds(
                headers.getUserEmail(),
                headers.getPartitionId(),
                numRecords,
                cursor
        );

        return response;
    }

    @Override
    public DatastoreQueryResult getAllRecordIdsFromKind(String kind, Integer limit, String cursor) {
        int numRecords = PAGE_SIZE;
        if (limit != null) {
            numRecords = limit > 0 ? limit : PAGE_SIZE;
        }

        DatastoreQueryResult response = queryHelper.getAllRecordIdsFromKind(kind, numRecords, cursor);

        return response;
    }
}
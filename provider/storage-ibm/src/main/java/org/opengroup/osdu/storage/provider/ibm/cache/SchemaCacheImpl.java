/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/
package org.opengroup.osdu.storage.provider.ibm.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.storage.cache.SchemaCache;
import org.springframework.stereotype.Component;

@Component
public class SchemaCacheImpl extends VmCache<String, Schema> implements SchemaCache {
    public SchemaCacheImpl() {
        super(5 * 60, 1000);
    }
}

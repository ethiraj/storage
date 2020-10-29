/* Licensed Materials - Property of IBM              */
/* (c) Copyright IBM Corp. 2020. All Rights Reserved.*/

package org.opengroup.osdu.storage.provider.ibm.cache;

import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.storage.cache.GroupCache;
import org.springframework.stereotype.Component;

@Component
public class GroupCacheImpl extends VmCache<String, Groups> implements GroupCache {

    public GroupCacheImpl() {
        super(30, 1000);
    }
}


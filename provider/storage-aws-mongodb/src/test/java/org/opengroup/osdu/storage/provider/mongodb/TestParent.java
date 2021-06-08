package org.opengroup.osdu.storage.provider.mongodb;

import org.opengroup.osdu.core.aws.multitenancy.TenantFactory;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.legal.ServiceConfig;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.SocketUtils;

public class TestParent {

    @MockBean
    TenantFactory tenantFactory;
    @MockBean
    ServiceConfig serviceConfig;
    @MockBean
    CloudStorageImpl cloudStorage;
    @MockBean
    MessageBusImpl messageBus;
    @MockBean
    DpsHeaders dpsHeaders;

    static {
        System.getProperties().setProperty("AWS_REGION", "us-east-1");
        System.getProperties().setProperty("ENVIRONMENT", "test");
        System.getProperties().setProperty("APPLICATION_PORT", String.valueOf(SocketUtils.findAvailableTcpPort(10_000)));
        System.getProperties().setProperty("LOG_LEVEL", "DEBUG");
        System.getProperties().setProperty("SSM_ENABLED", "false");
    }

}

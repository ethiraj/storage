#### The following updates need to be made when code is merged from OSDU:
1. Please add the repo in `devops/azure/pipeline.yml` file:
```
resources:
  repositories:
  - repository: security-templates
    type: git
    name: security-infrastructure
```

2. Update the env. names in the `devops/azure/pipeline.yml` file:
```
  providers:
    -  name: Azure
       environments: ['dev', 'qa', 'prd', 'cvx']
```

3. remove following settings if they are present from 'devops/azure/chart/templates/deployment.yaml'
```
 - name: azure_activedirectory_session_stateless
   value: "true"
 - name: azure_activedirectory_AppIdUri
   value: "api://$(aad_client_id)" 
```

4. update replicaCount in the `devops/azure/chart/helm-config.yaml` file:
```
  replicaCount: 10
```

5. CRS integration tests are enabled. 
- make sure that not override tests in Azure TestPostFetchRecordsIntegration.java
- make sure that not @Ignore tests in PostFetchRecordsIntegrationTests.java
- updates CrsConversionService.customizeHeaderBeforeCallingCrsConversion() use 'dpsHeaders.getAuthorization()' for token
- updates DpsConversionService.isMetaBlockPresent()
```
if (record.get("meta") == null || record.get("meta").isJsonNull()) {
        return false;
    }
JsonArray metaBlock = record.getAsJsonArray("meta");
return metaBlock != null && metaBlock.size() != 0;
```
- Keep the property for crs API in the application.properties:
``` 
  CRS_API=${crs_service_endpoint}
```
- Keep the property for crs API in the /devops/azure/chart/templates/deployment.yaml
```
  - name: crs_service_endpoint
    value: http://crs-conversion-service/api/crs/converter/v2
```
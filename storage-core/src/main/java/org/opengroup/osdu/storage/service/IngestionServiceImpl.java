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

package org.opengroup.osdu.storage.service;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import jdk.nashorn.internal.parser.JSONParser;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsAndCacheService;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.core.common.model.legal.LegalCompliance;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.model.indexer.OperationType;
import org.opengroup.osdu.core.common.model.storage.*;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.storage.validation.KindValidator;
import org.opengroup.osdu.core.common.legal.ILegalService;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.storage.*;
import org.opengroup.osdu.storage.logging.StorageAuditLogger;
import org.opengroup.osdu.storage.provider.interfaces.ICloudStorage;
import org.opengroup.osdu.storage.provider.interfaces.IRecordsMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

@Service
public class IngestionServiceImpl implements IngestionService {

	@Autowired
	private IRecordsMetadataRepository recordRepository;

	@Autowired
	private ICloudStorage cloudStorage;

	@Autowired
	private IPersistenceService persistenceService;

	@Autowired
	private ILegalService legalService;

	@Autowired
	private StorageAuditLogger auditLogger;

	@Autowired
	private DpsHeaders headers;

	@Autowired
	private TenantInfo tenant;

	@Autowired
    private JaxRsDpsLog logger;

	@Autowired
	private IEntitlementsAndCacheService entitlementsAndCacheService;

	@Override
	public TransferInfo createUpdateRecords(boolean skipDupes, List<Record> inputRecords, String user) {
		this.validateRecordsWithSchema(inputRecords);
		this.validateRecordIds(inputRecords);
		this.validateAcl(inputRecords);

		TransferInfo transfer = new TransferInfo(user, inputRecords.size());

		List<RecordProcessing> recordsToProcess = this.getRecordsForProcessing(skipDupes, inputRecords, transfer);

		this.sendRecordsForProcessing(recordsToProcess, transfer);
		return transfer;
	}

	private void validateAcl(List<Record> inputRecords) {
		Set<String> acls = new HashSet<>();
		for (Record record : inputRecords) {
			String[] viewers = record.getAcl().getViewers();
			String[] owners = record.getAcl().getOwners();
			for (String viewer : viewers) {
				acls.add(viewer);
			}
			for (String owner : owners) {
				acls.add(owner);
			}
		}
		if (!this.entitlementsAndCacheService.isValidAcl(this.headers, acls)) {
			throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid ACL", "Acl not match with tenant or domain");
		}
	}

	private void validateKindFormat(List<Record> inputRecords) {
		String tenantName = tenant.getName();

		for (Record record : inputRecords) {
			if (!KindValidator.isKindFromTenantValid(record.getKind(), tenantName)) {
				String msg = String.format(
						"The kind '%s' does not follow the required naming convention: the first kind component must be '%s'",
						record.getKind(), tenantName);

				throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid kind", msg);
			}
		}
	}

	private void validateRecordsWithSchema(List<Record> inputRecords) {

		System.out.println("=========Validating the schemas========");

			String tenantName = tenant.getName();
			for (Record record : inputRecords) {
				String kind = record.getKind();
				String schema = "";
				try {
					schema = getSchema(kind);
				} catch (Exception ex) {
					String message = "error retrieving schema of kind: " + kind;
					throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid kind", message);
				}
				try {
					this.validateRecordWithSchema(schema, record);
				} catch (Exception e) {
					String msg = "The record with id" + record.getId() + " does not follow the schema";

					throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid record", msg);
				}
			}
	}

	private String  getSchema(String kind) throws IOException {

		try {
			String host = System.getProperty("SCHEMA_URL", System.getenv("SCHEMA_URL"));
			URL url = new URL(host + kind);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			String sp_id = "00476b2d-2f6a-4dd9-a186-19d2b03cc1b0";
			String sp_secret = "4A{AmawKNkuY(?+vy#hUXmQ0e$PBV34-7";
			String tenant_id = "72f988bf-86f1-41af-91ab-2d7cd011db47";
			String app_resource_id = "api://c7287283-4858-7b56-93fa-c5e9ba717582";

			String token = AzureServicePrincipal.getIdToken(sp_id, sp_secret, tenant_id, app_resource_id);

			System.out.println("token: " + token);
			conn.setRequestProperty(HttpHeaders.AUTHORIZATION,"Bearer " + token);

			conn.setRequestProperty(HttpHeaders.CONTENT_TYPE,"application/json");
			conn.setRequestProperty("Data-Partition-Id","opendes");
			conn.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String output;

			StringBuffer response = new StringBuffer();
			while ((output = in.readLine()) != null) {
				response.append(output);
			}

			in.close();

			if (conn.getResponseCode() != 200) {
				String msg = "schema" + kind + "Notfound" + conn.getResponseCode();

				throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid kind", msg);
			}
			return response.toString();
		} catch (Exception e) {
			String message = "error retrieving schema of kind: " + kind;
			throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid kind", message);
		}

	}

	private void validateRecordWithSchema(String schema, Record record)
	{
		// logic to validate that the record is actually following the schema

		JSONObject recordData = new JSONObject(record.getData());
		JSONObject schemaDef = new JSONObject(schema);

		String message = "Schema: " + schemaDef.toString() + "Obj: " + recordData.toString();
		System.out.println(message);


			if (!compareWithSchema(schemaDef ,recordData )) {
				throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid kind", message);
			}

	}

	private boolean compareWithSchema(JSONObject schema, JSONObject obj) {

		Set<String> keysOfSchema = schema.keySet();
		Set<String> keysOfObject = obj.keySet();

		if (!keysOfObject.equals(keysOfSchema)) {
			return false;
		}
		else {
			return true;
		}
	}

	private void validateRecordIds(List<Record> inputRecords) {
		String tenantName = tenant.getName();

		Set<String> ids = new HashSet<>();
		for (Record record : inputRecords) {
			String id = record.getId();
			if (!Strings.isNullOrEmpty(id)) {
				if (ids.contains(id)) {
					throw new AppException(HttpStatus.SC_BAD_REQUEST, "Bad request",
							"Cannot update the same record multiple times in the same request. Id: " + id);
				}

				if (!Record.isRecordIdValid(id, tenantName)) {
					String msg = String.format(
							"The record '%s' does not follow the naming convention: the first id component must be '%s'",
							id, tenantName);
					throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid record id", msg);
				}

				ids.add(id);
			} else {
				record.createNewRecordId(tenantName);
			}
		}
	}

	private List<RecordProcessing> getRecordsForProcessing(boolean skipDupes, List<Record> inputRecords,
			TransferInfo transfer) {
		Map<String, List<String>> recordParentMap = new HashMap<>();
		List<RecordProcessing> recordsToProcess = new ArrayList<>();

		List<String> ids = this.getRecordIds(inputRecords, recordParentMap);
		Map<String, RecordMetadata> existingRecords = this.recordRepository.get(ids);

		this.validateParentsExist(existingRecords, recordParentMap);
		this.validateUserHasAccessToAllRecords(existingRecords);
		this.validateLegalConstraints(inputRecords, existingRecords, recordParentMap);

		Map<RecordMetadata, RecordData> recordUpdatesMap = new HashMap<>();
        Map<RecordMetadata, RecordData> recordUpdateWithoutVersions = new HashMap<>();

		final long currentTimestamp = System.currentTimeMillis();

		inputRecords.forEach(record -> {
			RecordData recordData = new RecordData(record);

			if (!existingRecords.containsKey(record.getId())) {
				RecordMetadata recordMetadata = new RecordMetadata(record);
				recordMetadata.setUser(transfer.getUser());
				recordMetadata.setStatus(RecordState.active);
				recordMetadata.setCreateTime(currentTimestamp);
				recordMetadata.addGcsPath(transfer.getVersion());

				recordsToProcess.add(new RecordProcessing(recordData, recordMetadata, OperationType.create));
			} else {
				RecordMetadata existingRecordMetadata = existingRecords.get(record.getId());

				if (!this.entitlementsAndCacheService.hasOwnerAccess(this.headers, existingRecordMetadata.getAcl().getOwners())) {
					this.logger.warning(String.format("User does not have owner access to record %s", record.getId()));
					throw new AppException(HttpStatus.SC_FORBIDDEN, "User Unauthorized", "User is not authorized to update records.");
				}

				RecordMetadata updatedRecordMetadata = new RecordMetadata(record);

				List<String> versions = new ArrayList<>();
				versions.addAll(existingRecordMetadata.getGcsVersionPaths());

				updatedRecordMetadata.setUser(existingRecordMetadata.getUser());
				updatedRecordMetadata.setCreateTime(existingRecordMetadata.getCreateTime());
				updatedRecordMetadata.setGcsVersionPaths(versions);

                if (versions.isEmpty()) {
                    this.logger.warning(String.format("Record %s does not have versions available", updatedRecordMetadata.getId()));
                    recordUpdateWithoutVersions.put(updatedRecordMetadata, recordData);
                } else {
                    recordUpdatesMap.put(updatedRecordMetadata, recordData);
                }
			}
		});

		if (skipDupes && recordUpdatesMap.size() > 0) {
			this.removeDuplicatedRecords(recordUpdatesMap, transfer);
		}
		recordUpdatesMap.putAll(recordUpdateWithoutVersions);

		this.populateUpdatedRecords(recordUpdatesMap, recordsToProcess, transfer, currentTimestamp);
		return recordsToProcess;
	}

	private void validateParentsExist(Map<String, RecordMetadata> existingRecords,
			Map<String, List<String>> recordParentMap) {

		for (Entry<String, List<String>> entry : recordParentMap.entrySet()) {
			List<String> parents = entry.getValue();
			for (String parent : parents) {
				if (!existingRecords.containsKey(parent)) {
					throw new AppException(HttpStatus.SC_NOT_FOUND, "Record not found",
							String.format("The record '%s' was not found", parent));
				}
			}
		}
	}

	private void validateLegalConstraints(List<Record> inputRecords,
			Map<String, RecordMetadata> existingRecordsMetadata,
			Map<String, List<String>> recordParentMap) {

		Set<String> legalTags = this.getLegalTags(inputRecords);
		Set<String> ordc = this.getORDC(inputRecords);

		this.legalService.validateLegalTags(legalTags);
		this.legalService.validateOtherRelevantDataCountries(ordc);
		this.legalService.populateLegalInfoFromParents(inputRecords, existingRecordsMetadata, recordParentMap);

		for (Record record : inputRecords) {
			Legal legal = record.getLegal();
			legal.setStatus(LegalCompliance.compliant);
		}
	}

	private void validateUserHasAccessToAllRecords(Map<String, RecordMetadata> existingRecords) {
		RecordMetadata[] records = existingRecords.values().toArray(new RecordMetadata[existingRecords.size()]);
		if (!this.cloudStorage.hasAccess(records)) {
			throw new AppException(HttpStatus.SC_FORBIDDEN, "Access denied",
					"The user is not authorized to perform this action");
		}
	}

	private void removeDuplicatedRecords(Map<RecordMetadata, RecordData> recordUpdatesMap, TransferInfo transfer) {
		Collection<RecordMetadata> metadataList = recordUpdatesMap.keySet();
		Map<String, String> hashMap = this.cloudStorage.getHash(metadataList);
		recordUpdatesMap
				.entrySet()
				.removeIf(kv -> this.cloudStorage.isDuplicateRecord(transfer, hashMap, kv));
	}

	private void populateUpdatedRecords(Map<RecordMetadata, RecordData> recordUpdatesMap,
			List<RecordProcessing> recordsToProcess, TransferInfo transfer, long timestamp) {
		for (Map.Entry<RecordMetadata, RecordData> recordEntry : recordUpdatesMap.entrySet()) {
			RecordMetadata recordMetadata = recordEntry.getKey();
			recordMetadata.addGcsPath(transfer.getVersion());
			recordMetadata.setModifyUser(transfer.getUser());
			recordMetadata.setModifyTime(timestamp);
			recordMetadata.setStatus(RecordState.active);

			RecordData recordData = recordEntry.getValue();

			recordsToProcess.add(new RecordProcessing(recordData, recordMetadata, OperationType.update));
		}
	}

	private void sendRecordsForProcessing(List<RecordProcessing> records, TransferInfo transferInfo) {
		if (!records.isEmpty()) {
			this.persistenceService.persistRecordBatch(new TransferBatch(transferInfo, records));
			this.auditLogger.createOrUpdateRecordsSuccess(this.extractRecordIds(records));
		}
	}

	private List<String> extractRecordIds(List<RecordProcessing> records) {
		List<String> recordIds = new ArrayList<>();
		for (RecordProcessing processing : records) {
			recordIds.add(processing.getRecordMetadata().getId());
		}
		return recordIds;
	}

	private List<String> getRecordIds(List<Record> records, Map<String, List<String>> recordParentMap) {
		List<String> ids = new ArrayList<>();
		for (Record record : records) {
			if (record.getAncestry() != null && !record.getAncestry().getParents().isEmpty()) {

				List<String> parents = new ArrayList<>();

				for (String parent : record.getAncestry().getParents()) {
					String[] tokens = parent.split(":");
					String parentRecordId = String.join(":", tokens[0], tokens[1], tokens[2]);
					Long parentRecordVersion = Long.parseLong(tokens[3]);

					parents.add(parentRecordId);
					ids.add(parentRecordId);
				}

				recordParentMap.put(record.getId(), parents);
			}

			ids.add(record.getId());
		}

		return ids;
	}

	private Set<String> getLegalTags(List<Record> inputRecords) {
		Set<String> legalTags = new HashSet<>();

		for (Record record : inputRecords) {
			if (record.getLegal().hasLegaltags()) {
				legalTags.addAll(record.getLegal().getLegaltags());
			}
		}

		return legalTags;
	}

	private Set<String> getORDC(List<Record> inputRecords) {
		Set<String> ordc = new HashSet<>();

		for (Record record : inputRecords) {
			if (record.getLegal().getOtherRelevantDataCountries() != null
					&& !record.getLegal().getOtherRelevantDataCountries().isEmpty()) {
				ordc.addAll(record.getLegal().getOtherRelevantDataCountries());
			}
		}

		return ordc;
	}
}
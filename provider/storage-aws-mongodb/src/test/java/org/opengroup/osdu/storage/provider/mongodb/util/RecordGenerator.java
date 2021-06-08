package org.opengroup.osdu.storage.provider.mongodb.util;

import org.opengroup.osdu.core.common.model.entitlements.Acl;
import org.opengroup.osdu.core.common.model.legal.Legal;
import org.opengroup.osdu.core.common.model.legal.LegalCompliance;
import org.opengroup.osdu.core.common.model.storage.RecordMetadata;
import org.opengroup.osdu.core.common.model.storage.RecordState;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RecordGenerator {

    public static List<RecordMetadata> withIds(String kind, String user, int... ids) {
        return Arrays.stream(ids)
                .mapToObj(i -> one(kind, user, i))
                .collect(Collectors.toList());
    }

    public static RecordMetadata one(String kind, String user, int id) {
        RecordMetadata recordMetadata = new RecordMetadata();
        recordMetadata.setId("opendes:id:" + id);
        recordMetadata.setKind(kind);

        Acl recordAcl = new Acl();
        String[] owners = {"data.tenant@mongodb.local"};
        String[] viewers = {"data.tenant@mongodb.local"};
        recordAcl.setOwners(owners);
        recordAcl.setViewers(viewers);
        recordMetadata.setAcl(recordAcl);

        Set<String> legalTags = new HashSet<>();
        legalTags.add("first");
        legalTags.add("second");
        legalTags.add("third");

        Legal recordLegal = new Legal();
        recordLegal.setLegaltags(legalTags);
        LegalCompliance status = LegalCompliance.compliant;
        recordLegal.setStatus(status);
        Set<String> otherRelevantDataCountries = new HashSet<>(Collections.singletonList("BR"));
        recordLegal.setOtherRelevantDataCountries(otherRelevantDataCountries);
        recordMetadata.setLegal(recordLegal);

        RecordState recordStatus = RecordState.active;
        recordMetadata.setStatus(recordStatus);

        recordMetadata.setUser(user);
        return recordMetadata;
    }
}

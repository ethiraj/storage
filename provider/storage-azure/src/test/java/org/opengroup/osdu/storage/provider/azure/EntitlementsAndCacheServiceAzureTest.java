package org.opengroup.osdu.storage.provider.azure;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.GroupInfo;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class EntitlementsAndCacheServiceAzureTest {

    @Mock
    private JaxRsDpsLog logger;

    @Mock
    private DpsHeaders headers;

    @Mock
    private ICache<String, Groups> cache;

    @InjectMocks
    private EntitlementsAndCacheServiceAzure sut;

    @Before
    public void init() {
    }

    @Test(expected = AppException.class)
    public void testHasAccessToData_whenGroupsIsEmpty() {
        Groups groups = mock(Groups.class);

        List<GroupInfo> groupInfos = mock(List.class);

        doReturn(groups).when(cache).get(any());
        doReturn(groupInfos).when(groups).getGroups();
        doReturn(true).when(groupInfos).isEmpty();

        try {
            sut.hasAccessToData(headers, null);
        } catch (AppException e) {
            int errorCode = 500;
            String errorMessage = "Unknown error happened when validating ACL";
            validateAppException(e, errorCode, errorMessage);
            throw(e);
        }
    }

    @Test(expected = AppException.class)
    public void testHasAccessToData_whenGroupsIsNull() {
        Groups groups = mock(Groups.class);

        List<GroupInfo> groupInfos = null;

        doReturn(groups).when(cache).get(any());
        doReturn(groupInfos).when(groups).getGroups();

        try {
            sut.hasAccessToData(headers, null);
        } catch (AppException e) {
            int errorCode = 500;
            String errorMessage = "Unknown error happened when validating ACL";
            validateAppException(e, errorCode, errorMessage);
            throw(e);
        }
    }

    @Test
    public void testHasAccessToData() {
        String desId = "desId";
        String memberEmail = "member@email.com";
        Groups groups = getGroups(desId, memberEmail);

        String email = "group@domain.com";
        String description = "Group description";
        String groupName = "group";
        GroupInfo groupInfo = getGroupInfo(email, description, groupName);
        List<GroupInfo> groupInfoList = Arrays.asList(groupInfo);

        groups.setGroups(groupInfoList);

        List<String> emails = Arrays.asList("group@domain.com", "group2@domain.com");
        Set<String> acls = new HashSet<String>(emails);

        doReturn(groups).when(cache).get(any());

        boolean status = sut.hasAccessToData(headers, acls);

        assertTrue(status);
    }

    @Test
    public void testHasAccessToData_whenGroupsDoesNotMatch() {
        String desId = "desId";
        String memberEmail = "member@email.com";
        Groups groups = getGroups(desId, memberEmail);

        String email = "group@domain.com";
        String description = "Group description";
        String groupName = "non-matching-group";
        GroupInfo groupInfo = getGroupInfo(email, description, groupName);
        List<GroupInfo> groupInfoList = Arrays.asList(groupInfo);

        groups.setGroups(groupInfoList);

        List<String> emails = Arrays.asList("group@domain.com", "group2@domain.com");
        Set<String> acls = new HashSet<String>(emails);

        doReturn(groups).when(cache).get(any());

        boolean status = sut.hasAccessToData(headers, acls);

        assertFalse(status);
    }

    @Test
    public void testHasAccessToData_whenMismatchingDomainInAcls_thenReturnsFalse() {
        String desId = "desId";
        String memberEmail = "member@email.com";
        Groups groups = getGroups(desId, memberEmail);

        String email = "group@domain.com";
        String description = "Group description";
        String groupName = "non-matching-group";
        GroupInfo groupInfo = getGroupInfo(email, description, groupName);
        List<GroupInfo> groupInfoList = Arrays.asList(groupInfo);

        groups.setGroups(groupInfoList);

        List<String> emails = Arrays.asList("group@otherdomain.com", "group2@domain.com");
        Set<String> acls = new HashSet<String>(emails);

        doReturn(groups).when(cache).get(any());

        boolean status = sut.hasAccessToData(headers, acls);

        assertFalse(status);
    }

    @Test(expected = AppException.class)
    public void testHasAccessToData_whenInvalidEmailIdProvided_thenThrowsException() {
        String desId = "desId";
        String memberEmail = "member@email.com";
        Groups groups = getGroups(desId, memberEmail);

        String email = "invalidemailid";
        String description = "Group description";
        String groupName = "non-matching-group";
        GroupInfo groupInfo = getGroupInfo(email, description, groupName);
        List<GroupInfo> groupInfoList = Arrays.asList(groupInfo);

        groups.setGroups(groupInfoList);

        List<String> emails = Arrays.asList("group@domain.com", "group2@domain.com");
        Set<String> acls = new HashSet<String>(emails);

        doReturn(groups).when(cache).get(any());

        try {
            sut.hasAccessToData(headers, acls);
        } catch (AppException e) {
            int errorCode = 500;
            String errorMessage = "Unknown error happened when validating ACL";

            validateAppException(e, errorCode, errorMessage);

            throw(e);
        }
    }

    private void validateAppException(AppException e, int errorCode, String errorMessage) {
        AppError error = e.getError();
        assertEquals(error.getCode(), errorCode);
        assertThat(error.getMessage(), containsString(errorMessage));
    }

    private Groups getGroups(String desId, String memberEmail) {
        Groups groups = new Groups();
        groups.setDesId(desId);
        groups.setMemberEmail(memberEmail);
        return groups;
    }

    private GroupInfo getGroupInfo(String email, String description, String name) {
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setEmail(email);
        groupInfo.setDescription(description);
        groupInfo.setName(name);
        return groupInfo;
    }
}

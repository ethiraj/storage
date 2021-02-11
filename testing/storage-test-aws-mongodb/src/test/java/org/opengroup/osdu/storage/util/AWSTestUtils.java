// Copyright © 2020 Amazon Web Services
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

package org.opengroup.osdu.storage.util;

import com.google.common.base.Strings;

public class AWSTestUtils extends TestUtils {
    private static String token;
    private static String noDataAccesstoken;
    private static AWSCognitoEnvClient awsCognitoClient = null;

    @Override
    public synchronized String getToken() throws Exception {
        if (Strings.isNullOrEmpty(token)) {
			System.out.println("[error-logging-test] getting auth for user = " + System.getenv("AWS_COGNITO_AUTH_PARAMS_USER"));
			token = getAwsCognitoClient().getTokenForUserWithAccess();
			System.out.println("[error-logging-test] token = " + token);
		}
        return "Bearer " + token;
    }

    @Override
    public synchronized String getNoDataAccessToken() throws Exception {
        if (Strings.isNullOrEmpty(noDataAccesstoken)) {
			System.out.println("[error-logging-test] getting auth-no-access for user = " + System.getenv("AWS_COGNITO_AUTH_PARAMS_USER_NO_ACCESS"));
			noDataAccesstoken = getAwsCognitoClient().getTokenForUserWithNoAccess();
			System.out.println("[error-logging-test] no-access-token = " + noDataAccesstoken);
        }
        return "Bearer " + noDataAccesstoken;
    }

    private AWSCognitoEnvClient getAwsCognitoClient() {
        if (awsCognitoClient == null)
            awsCognitoClient = new AWSCognitoEnvClient();
        return awsCognitoClient;
    }
}

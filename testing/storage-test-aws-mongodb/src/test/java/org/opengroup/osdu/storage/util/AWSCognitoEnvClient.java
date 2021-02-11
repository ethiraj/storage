// Copyright © 2020 Amazon Web Services
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

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.InitiateAuthResult;
import org.opengroup.osdu.core.aws.cognito.CognitoBuilder;

import java.util.HashMap;
import java.util.Map;


public class AWSCognitoEnvClient {
    private static final String USERNAME_PARAM = "USERNAME";
    private static final String PASSWORD_PARAM = "PASSWORD";
    private static final String COGNITO_CLIENT_ID_PROPERTY = "AWS_COGNITO_CLIENT_ID";
    private static final String COGNITO_AUTH_FLOW_PROPERTY = "AWS_COGNITO_AUTH_FLOW";
    private static final String COGNITO_AUTH_PARAMS_USER_PROPERTY = "AWS_COGNITO_AUTH_PARAMS_USER";
    private static final String COGNITO_AUTH_PARAMS_PASSWORD_PROPERTY = "AWS_COGNITO_AUTH_PARAMS_PASSWORD";
    private static final String COGNITO_AUTH_PARAMS_NO_ACCESS_USER_PROPERTY = "AWS_COGNITO_AUTH_PARAMS_USER_NO_ACCESS";
    private static final String COGNITO_AUTH_PARAMS_NO_ACCESS_PASSWORD_PROPERTY = "AWS_COGNITO_AUTH_PARAMS_PASSWORD";
    String awsCognitoClientId;
    String awsCognitoAuthFlow;
    String awsCognitoAuthParamsUser;
    String awsCognitoAuthParamsPassword;
    AWSCognitoIdentityProvider provider;

    public AWSCognitoEnvClient() {
        this.awsCognitoClientId = System.getenv("AWS_COGNITO_CLIENT_ID");
        this.awsCognitoAuthFlow = System.getenv("AWS_COGNITO_AUTH_FLOW");
        this.awsCognitoAuthParamsUser = System.getenv("AWS_COGNITO_AUTH_PARAMS_USER");
        this.awsCognitoAuthParamsPassword = System.getenv("AWS_COGNITO_AUTH_PARAMS_PASSWORD");
        this.provider = CognitoBuilder.generateCognitoClient();
    }

    public AWSCognitoEnvClient(String awsCognitoClientId, String awsCognitoAuthFlow, String awsCognitoAuthParamsUser, String awsCognitoAuthParamsPassword) {
        this.awsCognitoClientId = awsCognitoClientId;
        this.awsCognitoAuthFlow = awsCognitoAuthFlow;
        this.awsCognitoAuthParamsUser = awsCognitoAuthParamsUser;
        this.awsCognitoAuthParamsPassword = awsCognitoAuthParamsPassword;
        this.provider = CognitoBuilder.generateCognitoClient();
    }

    public String getToken() {
        return this.generateToken(this.awsCognitoAuthParamsUser, this.awsCognitoAuthParamsPassword);
    }

    public String getTokenForUserWithAccess() {
        return this.generateToken(System.getenv("AWS_COGNITO_AUTH_PARAMS_USER"), System.getenv("AWS_COGNITO_AUTH_PARAMS_PASSWORD"));
    }

    public String getTokenForUserWithNoAccess() {
        return this.generateToken(System.getenv("AWS_COGNITO_AUTH_PARAMS_USER_NO_ACCESS"), System.getenv("AWS_COGNITO_AUTH_PARAMS_PASSWORD"));
    }

    private String generateToken(String username, String password) {
        Map<String, String> authParameters = new HashMap();
        authParameters.put("USERNAME", username);
        authParameters.put("PASSWORD", password);
        InitiateAuthRequest request = new InitiateAuthRequest();
        request.setClientId(this.awsCognitoClientId);
        request.setAuthFlow(this.awsCognitoAuthFlow);
        request.setAuthParameters(authParameters);
        InitiateAuthResult result = this.provider.initiateAuth(request);
        return result.getAuthenticationResult().getAccessToken();
    }
}


/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.swssf.policy.assertionStates;

import org.apache.ws.secpolicy.WSSPolicyException;
import org.apache.ws.secpolicy.model.AbstractSecurityAssertion;
import org.apache.ws.secpolicy.model.AbstractToken;
import org.apache.ws.secpolicy.model.SecureConversationToken;
import org.swssf.wss.securityEvent.SecureConversationTokenSecurityEvent;
import org.swssf.wss.securityEvent.SecurityEvent;
import org.swssf.wss.securityEvent.TokenSecurityEvent;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */

public class SecureConversationTokenAssertionState extends TokenAssertionState {

    public SecureConversationTokenAssertionState(AbstractSecurityAssertion assertion, boolean asserted) {
        super(assertion, asserted);
    }

    @Override
    public SecurityEvent.Event[] getSecurityEventType() {
        return new SecurityEvent.Event[]{
                SecurityEvent.Event.SecureConversationToken
        };
    }

    @Override
    public boolean assertToken(TokenSecurityEvent tokenSecurityEvent, AbstractToken abstractToken) throws WSSPolicyException {
        if (!(tokenSecurityEvent instanceof SecureConversationTokenSecurityEvent)) {
            throw new WSSPolicyException("Expected a SecureConversationSecurityEvent but got " + tokenSecurityEvent.getClass().getName());
        }
        SecureConversationTokenSecurityEvent secureConversationSecurityEvent = (SecureConversationTokenSecurityEvent) tokenSecurityEvent;
        SecureConversationToken secureConversationToken = (SecureConversationToken) abstractToken;

        setAsserted(true);
        //todo move to super-class?
        if (secureConversationToken.getIssuerName() != null && !secureConversationToken.getIssuerName().equals(secureConversationSecurityEvent.getIssuerName())) {
            setAsserted(false);
            setErrorMessage("IssuerName in Policy (" + secureConversationToken.getIssuerName() + ") didn't match with the one in the SecureConversationToken (" + secureConversationSecurityEvent.getIssuerName() + ")");
        }
        if (secureConversationToken.isRequireExternalUriReference() && !secureConversationSecurityEvent.isExternalUriRef()) {
            setAsserted(false);
            setErrorMessage("Policy enforces externalUriRef but we didn't got one");
        }
        //todo sp:SC13SecurityContextToken:
        //if (securityContextToken.isSc10SecurityContextToken() && )
        //todo MustNotSendCancel etc...
        return isAsserted();
    }
}

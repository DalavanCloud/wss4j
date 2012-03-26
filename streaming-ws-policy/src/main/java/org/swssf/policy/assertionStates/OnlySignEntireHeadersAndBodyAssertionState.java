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

import org.apache.ws.secpolicy.AssertionState;
import org.apache.ws.secpolicy.WSSPolicyException;
import org.apache.ws.secpolicy.model.AbstractSecurityAssertion;
import org.apache.ws.secpolicy.model.AbstractSymmetricAsymmetricBinding;
import org.swssf.policy.Assertable;
import org.swssf.wss.ext.WSSConstants;
import org.swssf.wss.ext.WSSUtils;
import org.swssf.wss.securityEvent.SecurityEvent;
import org.swssf.wss.securityEvent.SignedPartSecurityEvent;

/**
 * WSP1.3, 6.6 Entire Header and Body Signatures Property
 *
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class OnlySignEntireHeadersAndBodyAssertionState extends AssertionState implements Assertable {

    public OnlySignEntireHeadersAndBodyAssertionState(AbstractSecurityAssertion assertion, boolean asserted) {
        super(assertion, asserted);
    }

    @Override
    public SecurityEvent.Event[] getSecurityEventType() {
        return new SecurityEvent.Event[]{
                SecurityEvent.Event.SignedPart
        };
    }

    @Override
    public boolean assertEvent(SecurityEvent securityEvent) throws WSSPolicyException {
        SignedPartSecurityEvent signedPartSecurityEvent = (SignedPartSecurityEvent) securityEvent;
        AbstractSymmetricAsymmetricBinding abstractSymmetricAsymmetricBinding = (AbstractSymmetricAsymmetricBinding) getAssertion();
        if (abstractSymmetricAsymmetricBinding.isOnlySignEntireHeadersAndBody()
                && WSSUtils.pathMatches(signedPartSecurityEvent.getElementPath(), WSSConstants.SOAP_11_BODY_PATH, true, false)) {
            if (signedPartSecurityEvent.isSigned()) {
                setAsserted(true);
                return true;
            } else {
                setAsserted(false);
                setErrorMessage("Element " + WSSUtils.pathAsString(signedPartSecurityEvent.getElementPath()) + " must be signed");
                return false;
            }
        }
        //body processed above. so this must be a header element
        if (abstractSymmetricAsymmetricBinding.isOnlySignEntireHeadersAndBody()) {
            if (signedPartSecurityEvent.isSigned()
                    //todo revisit: the equality check for wsse_Security probably opens the door
                    //for a rewriting attack! If the Security Header is not signed then all child
                    //elements must be signed!
                    // @see http://docs.oasis-open.org/ws-sx/ws-securitypolicy/v1.3/os/ws-securitypolicy-1.3-spec-os.html#_Toc212617840
                    || WSSUtils.pathMatches(signedPartSecurityEvent.getElementPath(), WSSConstants.WSSE_SECURITY_HEADER_PATH, true, false)) {
                setAsserted(true);
                return true;
            } else {
                setAsserted(false);
                setErrorMessage("Element " + WSSUtils.pathAsString(signedPartSecurityEvent.getElementPath()) + " must be signed");
                return false;
            }
        }
        return true;
    }
}

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
import org.apache.ws.secpolicy.model.AbstractBinding;
import org.apache.ws.secpolicy.model.AbstractSecurityAssertion;
import org.swssf.policy.Assertable;
import org.swssf.wss.securityEvent.SecurityEvent;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class IncludeTimeStampAssertionState extends AssertionState implements Assertable {

    public IncludeTimeStampAssertionState(AbstractSecurityAssertion assertion, boolean asserted) {
        super(assertion, asserted);
    }

    @Override
    public SecurityEvent.Event[] getSecurityEventType() {
        return new SecurityEvent.Event[]{
                SecurityEvent.Event.Timestamp
        };
    }

    public boolean assertEvent(SecurityEvent securityEvent) {
        // TimestampSecurityEvent timestampSecurityEvent = (TimestampSecurityEvent) securityEvent;
        boolean isIncludeTimestamp = ((AbstractBinding) getAssertion()).isIncludeTimestamp();

        if (isIncludeTimestamp) {
            setAsserted(true);
        } else {
            setAsserted(false);
            setErrorMessage("Timestamp must not be present");
        }
        return isAsserted();
    }
}

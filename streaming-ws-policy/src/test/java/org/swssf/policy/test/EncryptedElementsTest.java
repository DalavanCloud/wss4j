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
package org.swssf.policy.test;

import org.swssf.policy.PolicyEnforcer;
import org.swssf.policy.PolicyViolationException;
import org.swssf.wss.ext.WSSConstants;
import org.swssf.wss.ext.WSSecurityException;
import org.swssf.wss.securityEvent.EncryptedElementSecurityEvent;
import org.swssf.wss.securityEvent.OperationSecurityEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class EncryptedElementsTest extends AbstractPolicyTestBase {

    @Test
    public void testPolicy() throws Exception {
        String policyString =
                "<sp:EncryptedElements xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\" xmlns:sp3=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200802\">\n" +
                        "<sp:XPath xmlns:b=\"http://example.org\">/b:a</sp:XPath>\n" +
                        "</sp:EncryptedElements>";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        policyEnforcer.registerSecurityEvent(operationSecurityEvent);

        EncryptedElementSecurityEvent encryptedElementSecurityEvent = new EncryptedElementSecurityEvent(null, true, false);
        encryptedElementSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(encryptedElementSecurityEvent);
        List<QName> headerPath = new ArrayList<QName>();
        headerPath.add(new QName("http://example.org", "a"));
        encryptedElementSecurityEvent.setElementPath(headerPath);
        policyEnforcer.registerSecurityEvent(encryptedElementSecurityEvent);
        //additional EncryptedElements are also allowed!
        headerPath = new ArrayList<QName>();
        headerPath.add(new QName("http://example.org", "b"));
        encryptedElementSecurityEvent.setElementPath(headerPath);
        policyEnforcer.registerSecurityEvent(encryptedElementSecurityEvent);
        policyEnforcer.doFinal();
    }

    @Test
    public void testPolicyMultipleAssertionEventsNegative() throws Exception {
        String policyString =
                "<sp:EncryptedElements xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\" xmlns:sp3=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200802\">\n" +
                        "<sp:XPath xmlns:b=\"http://example.org\">/b:a</sp:XPath>\n" +
                        "</sp:EncryptedElements>";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        policyEnforcer.registerSecurityEvent(operationSecurityEvent);

        EncryptedElementSecurityEvent encryptedElementSecurityEvent = new EncryptedElementSecurityEvent(null, true, false);
        encryptedElementSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(encryptedElementSecurityEvent);
        encryptedElementSecurityEvent = new EncryptedElementSecurityEvent(null, false, false);
        List<QName> headerPath = new ArrayList<QName>();
        headerPath.add(new QName("http://example.org", "a"));
        encryptedElementSecurityEvent.setElementPath(headerPath);
        try {
            policyEnforcer.registerSecurityEvent(encryptedElementSecurityEvent);
            Assert.fail("Exception expected");
        } catch (WSSecurityException e) {
            Assert.assertTrue(e.getCause() instanceof PolicyViolationException);
        }
    }
}

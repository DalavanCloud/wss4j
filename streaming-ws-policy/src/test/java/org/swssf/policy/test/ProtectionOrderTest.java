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
import org.swssf.wss.securityEvent.*;
import org.swssf.xmlsec.ext.XMLSecurityConstants;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.util.LinkedList;
import java.util.List;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class ProtectionOrderTest extends AbstractPolicyTestBase {

    @Test
    public void testPolicySignBeforeEncrypt() throws Exception {
        String policyString =
                "<sp:AsymmetricBinding  xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\">\n" +
                        "   <wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "   </wsp:Policy>\n" +
                        "</sp:AsymmetricBinding>\n";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        policyEnforcer.registerSecurityEvent(operationSecurityEvent);

        List<XMLSecurityConstants.ContentType> protectionOrder = new LinkedList<XMLSecurityConstants.ContentType>();
        protectionOrder.add(XMLSecurityConstants.ContentType.SIGNATURE);
        protectionOrder.add(XMLSecurityConstants.ContentType.ENCRYPTION);

        SignedPartSecurityEvent signedPartSecurityEvent = new SignedPartSecurityEvent(null, true, protectionOrder);
        signedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(signedPartSecurityEvent);

        SignedElementSecurityEvent signedElementSecurityEvent = new SignedElementSecurityEvent(null, true, protectionOrder);
        signedElementSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(signedElementSecurityEvent);

        EncryptedPartSecurityEvent encryptedPartSecurityEvent = new EncryptedPartSecurityEvent(null, true, protectionOrder);
        encryptedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(encryptedPartSecurityEvent);

        EncryptedElementSecurityEvent encryptedElementSecurityEvent = new EncryptedElementSecurityEvent(null, true, protectionOrder);
        encryptedElementSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(encryptedElementSecurityEvent);

        ContentEncryptedElementSecurityEvent contentEncryptedElementSecurityEvent = new ContentEncryptedElementSecurityEvent(null, true, protectionOrder);
        contentEncryptedElementSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(contentEncryptedElementSecurityEvent);

        policyEnforcer.doFinal();
    }

    @Test
    public void testPolicySignBeforeEncryptWithoutEncryption() throws Exception {
        String policyString =
                "<sp:AsymmetricBinding  xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\">\n" +
                        "   <wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "   </wsp:Policy>\n" +
                        "</sp:AsymmetricBinding>\n";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        policyEnforcer.registerSecurityEvent(operationSecurityEvent);

        List<XMLSecurityConstants.ContentType> protectionOrder = new LinkedList<XMLSecurityConstants.ContentType>();
        protectionOrder.add(XMLSecurityConstants.ContentType.SIGNATURE);
        SignedPartSecurityEvent signedPartSecurityEvent = new SignedPartSecurityEvent(null, true, protectionOrder);
        signedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(signedPartSecurityEvent);
        policyEnforcer.doFinal();
    }

    @Test
    public void testPolicySignBeforeEncryptNegative() throws Exception {
        String policyString =
                "<sp:AsymmetricBinding  xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\">\n" +
                        "   <wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "   </wsp:Policy>\n" +
                        "</sp:AsymmetricBinding>\n";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        policyEnforcer.registerSecurityEvent(operationSecurityEvent);

        List<XMLSecurityConstants.ContentType> protectionOrder = new LinkedList<XMLSecurityConstants.ContentType>();
        protectionOrder.add(XMLSecurityConstants.ContentType.ENCRYPTION);
        protectionOrder.add(XMLSecurityConstants.ContentType.SIGNATURE);
        SignedPartSecurityEvent signedPartSecurityEvent = new SignedPartSecurityEvent(null, true, protectionOrder);
        signedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        try {
            policyEnforcer.registerSecurityEvent(signedPartSecurityEvent);
            Assert.fail("Exception expected");
        } catch (WSSecurityException e) {
            Assert.assertTrue(e.getCause() instanceof PolicyViolationException);
        }
    }

    @Test
    public void testPolicyEncryptBeforeSign() throws Exception {
        String policyString =
                "<sp:AsymmetricBinding  xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\">\n" +
                        "   <wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "       <sp:EncryptBeforeSigning/>\n" +
                        "   </wsp:Policy>\n" +
                        "</sp:AsymmetricBinding>\n";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        policyEnforcer.registerSecurityEvent(operationSecurityEvent);

        List<XMLSecurityConstants.ContentType> protectionOrder = new LinkedList<XMLSecurityConstants.ContentType>();
        protectionOrder.add(XMLSecurityConstants.ContentType.ENCRYPTION);
        protectionOrder.add(XMLSecurityConstants.ContentType.SIGNATURE);

        SignedPartSecurityEvent signedPartSecurityEvent = new SignedPartSecurityEvent(null, true, protectionOrder);
        signedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(signedPartSecurityEvent);

        SignedElementSecurityEvent signedElementSecurityEvent = new SignedElementSecurityEvent(null, true, protectionOrder);
        signedElementSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(signedElementSecurityEvent);

        EncryptedPartSecurityEvent encryptedPartSecurityEvent = new EncryptedPartSecurityEvent(null, true, protectionOrder);
        encryptedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(encryptedPartSecurityEvent);

        EncryptedElementSecurityEvent encryptedElementSecurityEvent = new EncryptedElementSecurityEvent(null, true, protectionOrder);
        encryptedElementSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(encryptedElementSecurityEvent);

        ContentEncryptedElementSecurityEvent contentEncryptedElementSecurityEvent = new ContentEncryptedElementSecurityEvent(null, true, protectionOrder);
        contentEncryptedElementSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(contentEncryptedElementSecurityEvent);

        policyEnforcer.doFinal();
    }

    @Test
    public void testPolicyEncryptBeforeSignWithoutSignature() throws Exception {
        String policyString =
                "<sp:AsymmetricBinding  xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\">\n" +
                        "   <wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "       <sp:EncryptBeforeSigning/>\n" +
                        "   </wsp:Policy>\n" +
                        "</sp:AsymmetricBinding>\n";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        policyEnforcer.registerSecurityEvent(operationSecurityEvent);

        List<XMLSecurityConstants.ContentType> protectionOrder = new LinkedList<XMLSecurityConstants.ContentType>();
        protectionOrder.add(XMLSecurityConstants.ContentType.ENCRYPTION);
        EncryptedPartSecurityEvent encryptedPartSecurityEvent = new EncryptedPartSecurityEvent(null, true, protectionOrder);
        encryptedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(encryptedPartSecurityEvent);
        policyEnforcer.doFinal();
    }

    @Test
    public void testPolicyEncryptBeforeSignNegative() throws Exception {
        String policyString =
                "<sp:AsymmetricBinding  xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\">\n" +
                        "   <wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "       <sp:EncryptBeforeSigning/>\n" +
                        "   </wsp:Policy>\n" +
                        "</sp:AsymmetricBinding>\n";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        policyEnforcer.registerSecurityEvent(operationSecurityEvent);

        List<XMLSecurityConstants.ContentType> protectionOrder = new LinkedList<XMLSecurityConstants.ContentType>();
        protectionOrder.add(XMLSecurityConstants.ContentType.SIGNATURE);
        protectionOrder.add(XMLSecurityConstants.ContentType.ENCRYPTION);
        EncryptedPartSecurityEvent encryptedPartSecurityEvent = new EncryptedPartSecurityEvent(null, true, protectionOrder);
        encryptedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        try {
            policyEnforcer.registerSecurityEvent(encryptedPartSecurityEvent);
            Assert.fail("Exception expected");
        } catch (WSSecurityException e) {
            Assert.assertTrue(e.getCause() instanceof PolicyViolationException);
        }
    }
}

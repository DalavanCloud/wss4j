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
import org.swssf.wss.impl.securityToken.SecureConversationSecurityToken;
import org.swssf.wss.securityEvent.*;
import org.swssf.xmlsec.ext.SecurityToken;
import org.swssf.xmlsec.ext.XMLSecurityConstants;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class SymmetricBindingTest extends AbstractPolicyTestBase {

    @Test
    public void testPolicy() throws Exception {
        String policyString =
                "<sp:SymmetricBinding xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\" xmlns:sp3=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200802\">\n" +
                        "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "   <sp:AlgorithmSuite>\n" +
                        "       <wsp:Policy>\n" +
                        "           <sp:Basic256/>\n" +
                        "       </wsp:Policy>\n" +
                        "   </sp:AlgorithmSuite>\n" +
                        "<sp:IncludeTimestamp/>\n" +
                        "<sp:EncryptSignature/>\n" +
                        "<sp:OnlySignEntireHeadersAndBody/>\n" +
                        "</wsp:Policy>\n" +
                        "</sp:SymmetricBinding>";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);

        TimestampSecurityEvent timestampSecurityEvent = new TimestampSecurityEvent();
        policyEnforcer.registerSecurityEvent(timestampSecurityEvent);

        RequiredElementSecurityEvent requiredElementSecurityEvent = new RequiredElementSecurityEvent();
        List<QName> headerPath = new ArrayList<QName>();
        headerPath.addAll(WSSConstants.WSSE_SECURITY_HEADER_PATH);
        headerPath.add(WSSConstants.TAG_wsu_Timestamp);
        requiredElementSecurityEvent.setElementPath(headerPath);
        policyEnforcer.registerSecurityEvent(requiredElementSecurityEvent);

        SecureConversationTokenSecurityEvent initiatorTokenSecurityEvent = new SecureConversationTokenSecurityEvent();
        SecurityToken securityToken = new SecureConversationSecurityToken(null, null, null, "1", null);
        securityToken.addTokenUsage(SecurityToken.TokenUsage.MainSignature);
        initiatorTokenSecurityEvent.setSecurityToken(securityToken);
        policyEnforcer.registerSecurityEvent(initiatorTokenSecurityEvent);

        SecureConversationTokenSecurityEvent recipientTokenSecurityEvent = new SecureConversationTokenSecurityEvent();
        securityToken = new SecureConversationSecurityToken(null, null, null, "1", null);
        securityToken.addTokenUsage(SecurityToken.TokenUsage.MainEncryption);
        recipientTokenSecurityEvent.setSecurityToken(securityToken);
        policyEnforcer.registerSecurityEvent(recipientTokenSecurityEvent);

        List<XMLSecurityConstants.ContentType> protectionOrder = new LinkedList<XMLSecurityConstants.ContentType>();
        protectionOrder.add(XMLSecurityConstants.ContentType.SIGNATURE);
        protectionOrder.add(XMLSecurityConstants.ContentType.ENCRYPTION);
        EncryptedElementSecurityEvent encryptedElementSecurityEvent = new EncryptedElementSecurityEvent(null, true, protectionOrder);
        headerPath = new ArrayList<QName>();
        headerPath.addAll(WSSConstants.WSSE_SECURITY_HEADER_PATH);
        headerPath.add(WSSConstants.TAG_dsig_Signature);
        encryptedElementSecurityEvent.setElementPath(headerPath);
        policyEnforcer.registerSecurityEvent(encryptedElementSecurityEvent);

        encryptedElementSecurityEvent = new EncryptedElementSecurityEvent(null, true, protectionOrder);
        headerPath = new ArrayList<QName>();
        headerPath.addAll(WSSConstants.WSSE_SECURITY_HEADER_PATH);
        headerPath.add(WSSConstants.TAG_wsse11_SignatureConfirmation);
        encryptedElementSecurityEvent.setElementPath(headerPath);
        policyEnforcer.registerSecurityEvent(encryptedElementSecurityEvent);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        policyEnforcer.registerSecurityEvent(operationSecurityEvent);

        SignedPartSecurityEvent signedPartSecurityEvent = new SignedPartSecurityEvent(null, true, protectionOrder);
        signedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(signedPartSecurityEvent);
        policyEnforcer.doFinal();
    }

    @Test
    public void testPolicyNotIncludeTimestamp() throws Exception {
        String policyString =
                "<sp:SymmetricBinding xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\" xmlns:sp3=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200802\">\n" +
                        "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "   <sp:AlgorithmSuite>\n" +
                        "       <wsp:Policy>\n" +
                        "           <sp:Basic256/>\n" +
                        "       </wsp:Policy>\n" +
                        "   </sp:AlgorithmSuite>\n" +
                        "<sp:EncryptSignature/>\n" +
                        "<sp:ProtectTokens/>\n" +
                        "<sp:OnlySignEntireHeadersAndBody/>\n" +
                        "</wsp:Policy>\n" +
                        "</sp:SymmetricBinding>";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);

        SecureConversationTokenSecurityEvent initiatorTokenSecurityEvent = new SecureConversationTokenSecurityEvent();
        SecurityToken securityToken = new SecureConversationSecurityToken(null, null, null, "1", null);
        securityToken.addTokenUsage(SecurityToken.TokenUsage.MainSignature);
        initiatorTokenSecurityEvent.setSecurityToken(securityToken);
        policyEnforcer.registerSecurityEvent(initiatorTokenSecurityEvent);

        SecureConversationTokenSecurityEvent recipientTokenSecurityEvent = new SecureConversationTokenSecurityEvent();
        securityToken = new SecureConversationSecurityToken(null, null, null, "1", null);
        securityToken.addTokenUsage(SecurityToken.TokenUsage.MainEncryption);
        recipientTokenSecurityEvent.setSecurityToken(securityToken);
        policyEnforcer.registerSecurityEvent(recipientTokenSecurityEvent);

        TimestampSecurityEvent timestampSecurityEvent = new TimestampSecurityEvent();
        policyEnforcer.registerSecurityEvent(timestampSecurityEvent);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        try {
            policyEnforcer.registerSecurityEvent(operationSecurityEvent);
            Assert.fail("Exception expected");
        } catch (WSSecurityException e) {
            Assert.assertTrue(e.getCause() instanceof PolicyViolationException);
            Assert.assertEquals(e.getCause().getMessage(), "\n" +
                    "Timestamp must not be present");
        }
    }

    @Test
    public void testPolicyWrongProtectionOrder() throws Exception {
        String policyString =
                "<sp:SymmetricBinding xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\" xmlns:sp3=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200802\">\n" +
                        "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "   <sp:AlgorithmSuite>\n" +
                        "       <wsp:Policy>\n" +
                        "           <sp:Basic256/>\n" +
                        "       </wsp:Policy>\n" +
                        "   </sp:AlgorithmSuite>\n" +
                        "<sp:IncludeTimestamp/>\n" +
                        "<sp:EncryptBeforeSigning/>\n" +
                        "<sp:EncryptSignature/>\n" +
                        "<sp:ProtectTokens/>\n" +
                        "<sp:OnlySignEntireHeadersAndBody/>\n" +
                        "</wsp:Policy>\n" +
                        "</sp:SymmetricBinding>";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);
        SecureConversationTokenSecurityEvent secureConversationTokenSecurityEvent = new SecureConversationTokenSecurityEvent();
        SecurityToken securityToken = new SecureConversationSecurityToken(null, null, null, "1", null);
        securityToken.addTokenUsage(SecurityToken.TokenUsage.MainEncryption);
        secureConversationTokenSecurityEvent.setSecurityToken(securityToken);
        policyEnforcer.registerSecurityEvent(secureConversationTokenSecurityEvent);

        List<XMLSecurityConstants.ContentType> protectionOrder = new LinkedList<XMLSecurityConstants.ContentType>();
        protectionOrder.add(XMLSecurityConstants.ContentType.SIGNATURE);
        protectionOrder.add(XMLSecurityConstants.ContentType.ENCRYPTION);
        SignedPartSecurityEvent signedPartSecurityEvent = new SignedPartSecurityEvent(null, true, protectionOrder);
        signedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        policyEnforcer.registerSecurityEvent(signedPartSecurityEvent);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        try {
            policyEnforcer.registerSecurityEvent(operationSecurityEvent);
            Assert.fail("Exception expected");
        } catch (WSSecurityException e) {
            Assert.assertTrue(e.getCause() instanceof PolicyViolationException);
            Assert.assertEquals(e.getCause().getMessage(), "\n" +
                    "Policy enforces EncryptBeforeSigning but the /{http://schemas.xmlsoap.org/soap/envelope/}Envelope/{http://schemas.xmlsoap.org/soap/envelope/}Body was signed and then encrypted");
        }
    }

    @Test
    public void testPolicySignatureNotEncrypted() throws Exception {
        String policyString =
                "<sp:SymmetricBinding xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\" xmlns:sp3=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200802\">\n" +
                        "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "   <sp:AlgorithmSuite>\n" +
                        "       <wsp:Policy>\n" +
                        "           <sp:Basic256/>\n" +
                        "       </wsp:Policy>\n" +
                        "   </sp:AlgorithmSuite>\n" +
                        "<sp:IncludeTimestamp/>\n" +
                        "<sp:EncryptSignature/>\n" +
                        "<sp:ProtectTokens/>\n" +
                        "<sp:OnlySignEntireHeadersAndBody/>\n" +
                        "</wsp:Policy>\n" +
                        "</sp:SymmetricBinding>";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);
        TimestampSecurityEvent timestampSecurityEvent = new TimestampSecurityEvent();
        policyEnforcer.registerSecurityEvent(timestampSecurityEvent);

        SecureConversationTokenSecurityEvent initiatorTokenSecurityEvent = new SecureConversationTokenSecurityEvent();
        SecurityToken securityToken = new SecureConversationSecurityToken(null, null, null, "1", null);
        securityToken.addTokenUsage(SecurityToken.TokenUsage.MainSignature);
        initiatorTokenSecurityEvent.setSecurityToken(securityToken);
        policyEnforcer.registerSecurityEvent(initiatorTokenSecurityEvent);

        SecureConversationTokenSecurityEvent recipientTokenSecurityEvent = new SecureConversationTokenSecurityEvent();
        securityToken = new SecureConversationSecurityToken(null, null, null, "1", null);
        securityToken.addTokenUsage(SecurityToken.TokenUsage.MainEncryption);
        recipientTokenSecurityEvent.setSecurityToken(securityToken);
        policyEnforcer.registerSecurityEvent(recipientTokenSecurityEvent);

        List<XMLSecurityConstants.ContentType> protectionOrder = new LinkedList<XMLSecurityConstants.ContentType>();
        protectionOrder.add(XMLSecurityConstants.ContentType.SIGNATURE);
        protectionOrder.add(XMLSecurityConstants.ContentType.ENCRYPTION);
        EncryptedElementSecurityEvent encryptedElementSecurityEvent = new EncryptedElementSecurityEvent(null, false, protectionOrder);
        List<QName> headerPath = new ArrayList<QName>();
        headerPath.addAll(WSSConstants.WSSE_SECURITY_HEADER_PATH);
        headerPath.add(WSSConstants.TAG_dsig_Signature);
        encryptedElementSecurityEvent.setElementPath(headerPath);
        policyEnforcer.registerSecurityEvent(encryptedElementSecurityEvent);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        try {
            policyEnforcer.registerSecurityEvent(operationSecurityEvent);
            Assert.fail("Exception expected");
        } catch (WSSecurityException e) {
            Assert.assertTrue(e.getCause() instanceof PolicyViolationException);
            Assert.assertEquals(e.getCause().getMessage(), "\n" +
                    "Element /{http://schemas.xmlsoap.org/soap/envelope/}Envelope/{http://schemas.xmlsoap.org/soap/envelope/}Header/{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd}Security/{http://www.w3.org/2000/09/xmldsig#}Signature must be encrypted");
        }
    }

    @Test
    public void testPolicyNotWholeBodySigned() throws Exception {
        String policyString =
                "<sp:SymmetricBinding xmlns:sp=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702\" xmlns:sp3=\"http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200802\">\n" +
                        "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                        "   <sp:AlgorithmSuite>\n" +
                        "       <wsp:Policy>\n" +
                        "           <sp:Basic256/>\n" +
                        "       </wsp:Policy>\n" +
                        "   </sp:AlgorithmSuite>\n" +
                        "<sp:IncludeTimestamp/>\n" +
                        "<sp:EncryptSignature/>\n" +
                        "<sp:OnlySignEntireHeadersAndBody/>\n" +
                        "</wsp:Policy>\n" +
                        "</sp:SymmetricBinding>";
        PolicyEnforcer policyEnforcer = buildAndStartPolicyEngine(policyString);
        TimestampSecurityEvent timestampSecurityEvent = new TimestampSecurityEvent();
        policyEnforcer.registerSecurityEvent(timestampSecurityEvent);

        SecureConversationTokenSecurityEvent initiatorTokenSecurityEvent = new SecureConversationTokenSecurityEvent();
        SecurityToken securityToken = new SecureConversationSecurityToken(null, null, null, "1", null);
        securityToken.addTokenUsage(SecurityToken.TokenUsage.MainSignature);
        initiatorTokenSecurityEvent.setSecurityToken(securityToken);
        policyEnforcer.registerSecurityEvent(initiatorTokenSecurityEvent);

        SecureConversationTokenSecurityEvent recipientTokenSecurityEvent = new SecureConversationTokenSecurityEvent();
        securityToken = new SecureConversationSecurityToken(null, null, null, "1", null);
        securityToken.addTokenUsage(SecurityToken.TokenUsage.MainEncryption);
        recipientTokenSecurityEvent.setSecurityToken(securityToken);
        policyEnforcer.registerSecurityEvent(recipientTokenSecurityEvent);

        List<XMLSecurityConstants.ContentType> protectionOrder = new LinkedList<XMLSecurityConstants.ContentType>();
        protectionOrder.add(XMLSecurityConstants.ContentType.SIGNATURE);
        protectionOrder.add(XMLSecurityConstants.ContentType.ENCRYPTION);
        EncryptedElementSecurityEvent encryptedElementSecurityEvent = new EncryptedElementSecurityEvent(null, true, protectionOrder);
        List<QName> headerPath = new ArrayList<QName>();
        headerPath.addAll(WSSConstants.WSSE_SECURITY_HEADER_PATH);
        headerPath.add(WSSConstants.TAG_dsig_Signature);
        encryptedElementSecurityEvent.setElementPath(headerPath);
        policyEnforcer.registerSecurityEvent(encryptedElementSecurityEvent);

        encryptedElementSecurityEvent = new EncryptedElementSecurityEvent(null, true, protectionOrder);
        headerPath = new ArrayList<QName>();
        headerPath.addAll(WSSConstants.WSSE_SECURITY_HEADER_PATH);
        headerPath.add(WSSConstants.TAG_wsse11_SignatureConfirmation);
        encryptedElementSecurityEvent.setElementPath(headerPath);
        policyEnforcer.registerSecurityEvent(encryptedElementSecurityEvent);

        OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
        operationSecurityEvent.setOperation(new QName("definitions"));
        policyEnforcer.registerSecurityEvent(operationSecurityEvent);

        SignedPartSecurityEvent signedPartSecurityEvent = new SignedPartSecurityEvent(null, false, protectionOrder);
        signedPartSecurityEvent.setElementPath(WSSConstants.SOAP_11_BODY_PATH);
        try {
            policyEnforcer.registerSecurityEvent(signedPartSecurityEvent);
            Assert.fail("Exception expected");
        } catch (WSSecurityException e) {
            Assert.assertTrue(e.getCause() instanceof PolicyViolationException);
            Assert.assertEquals(e.getCause().getMessage(), "\n" +
                    "Element /{http://schemas.xmlsoap.org/soap/envelope/}Envelope/{http://schemas.xmlsoap.org/soap/envelope/}Body must be signed");
        }
    }
}

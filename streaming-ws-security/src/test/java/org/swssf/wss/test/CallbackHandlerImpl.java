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
package org.swssf.wss.test;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.message.WSSecEncryptedKey;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.opensaml.common.SAMLVersion;
import org.swssf.wss.ext.WSPasswordCallback;
import org.swssf.wss.impl.saml.SAMLCallback;
import org.swssf.wss.impl.saml.bean.*;
import org.swssf.wss.impl.saml.builder.SAML1Constants;
import org.swssf.xmlsec.crypto.CryptoBase;
import org.swssf.xmlsec.crypto.Merlin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collections;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class CallbackHandlerImpl implements CallbackHandler {

    private String username = "default";
    private byte[] secret;

    public enum Statement {
        AUTHN, ATTR, AUTHZ
    }

    private String subjectName = "uid=joe,ou=people,ou=saml-demo,o=example.com";
    private String subjectQualifier = "www.example.com";
    private String confirmationMethod = SAML1Constants.CONF_SENDER_VOUCHES;
    private X509Certificate[] certs;
    private Statement statement = Statement.AUTHN;
    private KeyInfoBean.CERT_IDENTIFIER certIdentifier = KeyInfoBean.CERT_IDENTIFIER.X509_CERT;
    private byte[] ephemeralKey = null;
    private String issuer = null;
    private SAMLVersion samlVersion = SAMLVersion.VERSION_11;

    private boolean signAssertion = true;

    public CallbackHandlerImpl() {
    }

    public CallbackHandlerImpl(String username) {
        if (username != null) {
            this.username = username;
        }
    }

    public CallbackHandlerImpl(byte[] secret) {
        this.secret = secret;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        if (callbacks[0] instanceof WSPasswordCallback) {
            WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

            if (pc.getUsage() == WSPasswordCallback.Usage.DECRYPT
                    || pc.getUsage() == WSPasswordCallback.Usage.SIGNATURE
                    || pc.getUsage() == WSPasswordCallback.Usage.USERNAME_TOKEN
                    || pc.getUsage() == WSPasswordCallback.Usage.USERNAME_TOKEN_UNKNOWN
                    ) {
                pc.setPassword(username);
            } else if (pc.getUsage() == WSPasswordCallback.Usage.SECRET_KEY
                    || pc.getUsage() == WSPasswordCallback.Usage.SECURITY_CONTEXT_TOKEN) {
                pc.setKey(secret);
            } else {
                throw new UnsupportedCallbackException(pc, "Unrecognized CallbackHandlerImpl");
            }
        } else if (callbacks[0] instanceof SAMLCallback) {
            try {
                SAMLCallback samlCallback = (SAMLCallback) callbacks[0];
                KeyStore keyStore = KeyStore.getInstance("jks");
                keyStore.load(this.getClass().getClassLoader().getResourceAsStream("saml/issuer.jks"), "default".toCharArray());
                CryptoBase crypto = new Merlin();
                crypto.setKeyStore(keyStore);
                samlCallback.setIssuerCrypto(crypto);
                samlCallback.setIssuerKeyName("samlissuer");
                samlCallback.setIssuerKeyPassword("default");
                samlCallback.setSignAssertion(this.signAssertion);

                if (getSamlVersion() == SAMLVersion.VERSION_11) {

                    samlCallback.setSamlVersion(SAMLVersion.VERSION_11);
                    samlCallback.setIssuer(issuer);
                    SubjectBean subjectBean =
                            new SubjectBean(
                                    subjectName, subjectQualifier, confirmationMethod
                            );
                    if (SAML1Constants.CONF_HOLDER_KEY.equals(confirmationMethod)) {
                        try {
                            KeyInfoBean keyInfo = createKeyInfo();
                            subjectBean.setKeyInfo(keyInfo);
                        } catch (Exception ex) {
                            throw new IOException("Problem creating KeyInfo: " + ex.getMessage());
                        }
                    }
                    samlCallback.setSubject(subjectBean);
                    createAndSetStatement(subjectBean, samlCallback);
                } else {
                    samlCallback.setSamlVersion(SAMLVersion.VERSION_20);
                    samlCallback.setIssuer(issuer);
                    SubjectBean subjectBean =
                            new SubjectBean(
                                    subjectName, subjectQualifier, confirmationMethod
                            );
                    if (SAML2Constants.CONF_HOLDER_KEY.equals(confirmationMethod)) {
                        try {
                            KeyInfoBean keyInfo = createKeyInfo();
                            subjectBean.setKeyInfo(keyInfo);
                        } catch (Exception ex) {
                            throw new IOException("Problem creating KeyInfo: " + ex.getMessage());
                        }
                    }
                    samlCallback.setSubject(subjectBean);
                    createAndSetStatement(null, samlCallback);
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }

    /**
     * Note that the SubjectBean parameter should be null for SAML2.0
     */
    protected void createAndSetStatement(SubjectBean subjectBean, SAMLCallback callback) {
        if (statement == Statement.AUTHN) {
            AuthenticationStatementBean authBean = new AuthenticationStatementBean();
            if (subjectBean != null) {
                authBean.setSubject(subjectBean);
            }
            authBean.setAuthenticationMethod("Password");
            callback.setAuthenticationStatementData(Collections.singletonList(authBean));
        } else if (statement == Statement.ATTR) {
            AttributeStatementBean attrBean = new AttributeStatementBean();
            if (subjectBean != null) {
                attrBean.setSubject(subjectBean);
            }
            AttributeBean attributeBean = new AttributeBean();
            attributeBean.setSimpleName("role");
            attributeBean.setAttributeValues(Collections.singletonList("user"));
            attrBean.setSamlAttributes(Collections.singletonList(attributeBean));
            callback.setAttributeStatementData(Collections.singletonList(attrBean));
        } else {
            AuthDecisionStatementBean authzBean = new AuthDecisionStatementBean();
            if (subjectBean != null) {
                authzBean.setSubject(subjectBean);
            }
            ActionBean actionBean = new ActionBean();
            actionBean.setContents("Read");
            authzBean.setActions(Collections.singletonList(actionBean));
            authzBean.setResource("endpoint");
            authzBean.setDecision(AuthDecisionStatementBean.Decision.PERMIT);
            callback.setAuthDecisionStatementData(Collections.singletonList(authzBean));
        }
    }

    protected KeyInfoBean createKeyInfo() throws Exception {
        KeyInfoBean keyInfo = new KeyInfoBean();
        if (statement == Statement.AUTHN) {
            keyInfo.setCertificate(certs[0]);
            keyInfo.setCertIdentifer(certIdentifier);
        } else if (statement == Statement.ATTR) {
            // Build a new Document
            DocumentBuilderFactory docBuilderFactory =
                    DocumentBuilderFactory.newInstance();
            docBuilderFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            // Create an Encrypted Key
            WSSecEncryptedKey encrKey = new WSSecEncryptedKey();
            encrKey.setKeyIdentifierType(WSConstants.X509_KEY_IDENTIFIER);
            encrKey.setUseThisCert(certs[0]);
            encrKey.prepare(doc, null);
            ephemeralKey = encrKey.getEphemeralKey();
            Element encryptedKeyElement = encrKey.getEncryptedKeyElement();

            // Append the EncryptedKey to a KeyInfo element
            Element keyInfoElement =
                    doc.createElementNS(
                            WSConstants.SIG_NS, WSConstants.SIG_PREFIX + ":" + WSConstants.KEYINFO_LN
                    );
            keyInfoElement.setAttributeNS(
                    WSConstants.XMLNS_NS, "xmlns:" + WSConstants.SIG_PREFIX, WSConstants.SIG_NS
            );
            keyInfoElement.appendChild(encryptedKeyElement);

            keyInfo.setElement(keyInfoElement);
        }
        return keyInfo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getSubjectQualifier() {
        return subjectQualifier;
    }

    public void setSubjectQualifier(String subjectQualifier) {
        this.subjectQualifier = subjectQualifier;
    }

    public String getConfirmationMethod() {
        return confirmationMethod;
    }

    public void setConfirmationMethod(String confirmationMethod) {
        this.confirmationMethod = confirmationMethod;
    }

    public X509Certificate[] getCerts() {
        return certs;
    }

    public void setCerts(X509Certificate[] certs) {
        this.certs = certs;
    }

    public Statement getStatement() {
        return statement;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public KeyInfoBean.CERT_IDENTIFIER getCertIdentifier() {
        return certIdentifier;
    }

    public void setCertIdentifier(KeyInfoBean.CERT_IDENTIFIER certIdentifier) {
        this.certIdentifier = certIdentifier;
    }

    public byte[] getEphemeralKey() {
        return ephemeralKey;
    }

    public void setEphemeralKey(byte[] ephemeralKey) {
        this.ephemeralKey = ephemeralKey;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public boolean isSignAssertion() {
        return signAssertion;
    }

    public void setSignAssertion(boolean signAssertion) {
        this.signAssertion = signAssertion;
    }

    public SAMLVersion getSamlVersion() {
        return samlVersion;
    }

    public void setSamlVersion(SAMLVersion samlVersion) {
        this.samlVersion = samlVersion;
    }

    public byte[] getSecret() {
        return secret;
    }

    public void setSecret(byte[] secret) {
        this.secret = secret;
    }
}

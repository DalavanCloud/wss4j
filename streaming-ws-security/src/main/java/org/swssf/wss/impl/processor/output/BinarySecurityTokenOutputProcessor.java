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
package org.swssf.wss.impl.processor.output;

import org.swssf.wss.ext.*;
import org.swssf.wss.impl.securityToken.AbstractSecurityToken;
import org.swssf.wss.securityEvent.SecurityEvent;
import org.swssf.wss.securityEvent.TokenSecurityEvent;
import org.swssf.xmlsec.crypto.CryptoType;
import org.swssf.xmlsec.ext.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class BinarySecurityTokenOutputProcessor extends AbstractOutputProcessor {

    public BinarySecurityTokenOutputProcessor(WSSSecurityProperties securityProperties, XMLSecurityConstants.Action action) throws XMLSecurityException {
        super(securityProperties, action);
    }

    @Override
    public void processEvent(XMLEvent xmlEvent, OutputProcessorChain outputProcessorChain) throws XMLStreamException, XMLSecurityException {
        try {
            final String bstId = "BST-" + UUID.randomUUID().toString();
            final X509Certificate[] x509Certificates;
            final Key key;

            XMLSecurityConstants.Action action = getAction();
            if (action.equals(WSSConstants.SIGNATURE)
                    || action.equals(WSSConstants.SAML_TOKEN_SIGNED)
                    || action.equals(WSSConstants.SIGNATURE_WITH_DERIVED_KEY)) {

                String alias = getSecurityProperties().getSignatureUser();
                WSPasswordCallback pwCb = new WSPasswordCallback(alias, WSPasswordCallback.Usage.SIGNATURE);
                WSSUtils.doPasswordCallback(getSecurityProperties().getCallbackHandler(), pwCb);
                String password = pwCb.getPassword();
                if (password == null) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_SIGNATURE, "noPassword", alias);
                }
                key = getSecurityProperties().getSignatureCrypto().getPrivateKey(alias, password);
                CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
                cryptoType.setAlias(getSecurityProperties().getSignatureUser());
                x509Certificates = getSecurityProperties().getSignatureCrypto().getX509Certificates(cryptoType);
                if (x509Certificates == null || x509Certificates.length == 0) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_SIGNATURE, "noUserCertsFound", alias);
                }
            } else if (action.equals(WSSConstants.ENCRYPT) ||
                    action.equals(WSSConstants.ENCRYPT_WITH_DERIVED_KEY)) {
                X509Certificate x509Certificate = getReqSigCert(outputProcessorChain.getSecurityContext());
                if (((WSSSecurityProperties) getSecurityProperties()).isUseReqSigCertForEncryption()) {
                    if (x509Certificate == null) {
                        throw new WSSecurityException("noCert");
                    }
                    x509Certificates = new X509Certificate[1];
                    x509Certificates[0] = x509Certificate;
                } else if (getSecurityProperties().getEncryptionUseThisCertificate() != null) {
                    x509Certificate = getSecurityProperties().getEncryptionUseThisCertificate();
                    x509Certificates = new X509Certificate[1];
                    x509Certificates[0] = x509Certificate;
                } else {
                    CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
                    cryptoType.setAlias(getSecurityProperties().getEncryptionUser());
                    x509Certificates = getSecurityProperties().getEncryptionCrypto().getX509Certificates(cryptoType);
                    if (x509Certificates == null || x509Certificates.length == 0) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_ENCRYPTION, "noUserCertsFound", getSecurityProperties().getEncryptionUser());
                    }
                }
                key = null;
            } else {
                x509Certificates = null;
                key = null;
            }

            //todo use the abstractSecurityToken class and make setProcessor method?
            final AbstractSecurityToken binarySecurityToken = new AbstractSecurityToken(bstId) {

                @Override
                public boolean isAsymmetric() {
                    return true;
                }

                @Override
                public Key getKey(String algorithmURI, XMLSecurityConstants.KeyUsage keyUsage) throws WSSecurityException {
                    return key;
                }

                @Override
                public PublicKey getPubKey(String algorithmURI, XMLSecurityConstants.KeyUsage keyUsage) throws WSSecurityException {
                    return x509Certificates[0].getPublicKey();
                }

                @Override
                public X509Certificate[] getX509Certificates() throws WSSecurityException {
                    return x509Certificates;
                }

                @Override
                public SecurityToken getKeyWrappingToken() {
                    return null;
                }

                @Override
                public WSSConstants.TokenType getTokenType() {
                    return null;
                }
            };

            final SecurityTokenProvider binarySecurityTokenProvider = new SecurityTokenProvider() {

                @Override
                public SecurityToken getSecurityToken() throws WSSecurityException {
                    return binarySecurityToken;
                }

                @Override
                public String getId() {
                    return bstId;
                }
            };

            if (action.equals(WSSConstants.SIGNATURE)
                    || action.equals(WSSConstants.SAML_TOKEN_SIGNED)) {
                outputProcessorChain.getSecurityContext().put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_SIGNATURE, bstId);
                if (((WSSSecurityProperties) getSecurityProperties()).getSignatureKeyIdentifierType() == WSSConstants.KeyIdentifierType.SECURITY_TOKEN_DIRECT_REFERENCE) {
                    outputProcessorChain.getSecurityContext().put(WSSConstants.PROP_APPEND_SIGNATURE_ON_THIS_ID, bstId);
                    FinalBinarySecurityTokenOutputProcessor finalBinarySecurityTokenOutputProcessor = new FinalBinarySecurityTokenOutputProcessor(getSecurityProperties(), getAction(), binarySecurityToken);
                    finalBinarySecurityTokenOutputProcessor.getBeforeProcessors().add(org.swssf.wss.impl.processor.output.SignatureOutputProcessor.class.getName());
                    outputProcessorChain.addProcessor(finalBinarySecurityTokenOutputProcessor);
                    binarySecurityToken.setProcessor(finalBinarySecurityTokenOutputProcessor);
                }
            } else if (action.equals(WSSConstants.ENCRYPT)) {
                outputProcessorChain.getSecurityContext().put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTED_KEY, bstId);
                if (((WSSSecurityProperties) getSecurityProperties()).getEncryptionKeyIdentifierType() == WSSConstants.KeyIdentifierType.SECURITY_TOKEN_DIRECT_REFERENCE) {
                    FinalBinarySecurityTokenOutputProcessor finalBinarySecurityTokenOutputProcessor = new FinalBinarySecurityTokenOutputProcessor(getSecurityProperties(), getAction(), binarySecurityToken);
                    finalBinarySecurityTokenOutputProcessor.getAfterProcessors().add(org.swssf.wss.impl.processor.output.EncryptEndingOutputProcessor.class.getName());
                    outputProcessorChain.addProcessor(finalBinarySecurityTokenOutputProcessor);
                    binarySecurityToken.setProcessor(finalBinarySecurityTokenOutputProcessor);
                }
            } else if (action.equals(WSSConstants.SIGNATURE_WITH_DERIVED_KEY)
                    || action.equals(WSSConstants.ENCRYPT_WITH_DERIVED_KEY)) {

                WSSConstants.DerivedKeyTokenReference derivedKeyTokenReference = ((WSSSecurityProperties) getSecurityProperties()).getDerivedKeyTokenReference();
                switch (derivedKeyTokenReference) {

                    case DirectReference:
                        outputProcessorChain.getSecurityContext().put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_DERIVED_KEY, bstId);
                        break;
                    case EncryptedKey:
                        outputProcessorChain.getSecurityContext().put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_ENCRYPTED_KEY, bstId);
                        break;
                    case SecurityContextToken:
                        outputProcessorChain.getSecurityContext().put(WSSConstants.PROP_USE_THIS_TOKEN_ID_FOR_SECURITYCONTEXTTOKEN, bstId);
                        break;
                }
            }

            outputProcessorChain.getSecurityContext().registerSecurityTokenProvider(bstId, binarySecurityTokenProvider);

        } finally {
            outputProcessorChain.removeProcessor(this);
        }
        outputProcessorChain.processEvent(xmlEvent);
    }

    private X509Certificate getReqSigCert(SecurityContext securityContext) throws XMLSecurityException {
        List<SecurityEvent> securityEventList = securityContext.getAsList(SecurityEvent.class);
        if (securityEventList != null) {
            for (int i = 0; i < securityEventList.size(); i++) {
                SecurityEvent securityEvent = securityEventList.get(i);
                if (securityEvent instanceof TokenSecurityEvent) {
                    TokenSecurityEvent tokenSecurityEvent = (TokenSecurityEvent) securityEvent;
                    if (!tokenSecurityEvent.getSecurityToken().getTokenUsages().contains(SecurityToken.TokenUsage.MainSignature)) {
                        continue;
                    }
                    X509Certificate[] x509Certificates = tokenSecurityEvent.getSecurityToken().getX509Certificates();
                    if (x509Certificates != null && x509Certificates.length > 0) {
                        return x509Certificates[0];
                    }
                }
            }
        }
        return null;
    }

    class FinalBinarySecurityTokenOutputProcessor extends AbstractOutputProcessor {

        private SecurityToken securityToken;

        FinalBinarySecurityTokenOutputProcessor(XMLSecurityProperties securityProperties, XMLSecurityConstants.Action action, SecurityToken securityToken) throws XMLSecurityException {
            super(securityProperties, action);
            this.getAfterProcessors().add(BinarySecurityTokenOutputProcessor.class.getName());
            this.securityToken = securityToken;
        }

        @Override
        public void processEvent(XMLEvent xmlEvent, OutputProcessorChain outputProcessorChain) throws XMLStreamException, XMLSecurityException {
            outputProcessorChain.processEvent(xmlEvent);
            if (xmlEvent.isStartElement()) {
                StartElement startElement = xmlEvent.asStartElement();
                if (((WSSDocumentContext) outputProcessorChain.getDocumentContext()).isInSecurityHeader() && startElement.getName().equals(WSSConstants.TAG_wsse_Security)) {
                    OutputProcessorChain subOutputProcessorChain = outputProcessorChain.createSubChain(this);

                    boolean useSingleCertificate = getSecurityProperties().isUseSingleCert();
                    WSSUtils.createBinarySecurityTokenStructure(this, subOutputProcessorChain, securityToken.getId(), securityToken.getX509Certificates(), useSingleCertificate);

                    outputProcessorChain.removeProcessor(this);
                }
            }
        }
    }
}

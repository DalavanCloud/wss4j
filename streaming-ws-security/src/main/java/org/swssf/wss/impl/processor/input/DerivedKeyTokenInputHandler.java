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
package org.swssf.wss.impl.processor.input;

import org.swssf.binding.wssc.AbstractDerivedKeyTokenType;
import org.swssf.wss.ext.WSSConstants;
import org.swssf.wss.ext.WSSecurityContext;
import org.swssf.wss.ext.WSSecurityException;
import org.swssf.wss.impl.derivedKey.DerivedKeyUtils;
import org.swssf.wss.impl.securityToken.AbstractSecurityToken;
import org.swssf.wss.impl.securityToken.SAMLSecurityToken;
import org.swssf.wss.impl.securityToken.SecurityTokenFactoryImpl;
import org.swssf.wss.impl.securityToken.UsernameSecurityToken;
import org.swssf.wss.securityEvent.DerivedKeyTokenSecurityEvent;
import org.swssf.xmlsec.config.JCEAlgorithmMapper;
import org.swssf.xmlsec.ext.*;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;
import java.security.Key;
import java.security.PublicKey;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Processor for the SecurityContextToken XML Structure
 *
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class DerivedKeyTokenInputHandler extends AbstractInputSecurityHeaderHandler {

    @Override
    public void handle(final InputProcessorChain inputProcessorChain, final XMLSecurityProperties securityProperties,
                       Deque<XMLEvent> eventQueue, Integer index) throws XMLSecurityException {

        @SuppressWarnings("unchecked")
        final AbstractDerivedKeyTokenType derivedKeyTokenType =
                ((JAXBElement<AbstractDerivedKeyTokenType>) parseStructure(eventQueue, index)).getValue();
        if (derivedKeyTokenType.getId() == null) {
            derivedKeyTokenType.setId(UUID.randomUUID().toString());
        }
        if (derivedKeyTokenType.getSecurityTokenReference() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, "noReference");
        }

        final List<QName> elementPath = getElementPath(inputProcessorChain.getDocumentContext(), eventQueue);
        final XMLEvent responsibleStartXMLEvent = getResponsibleStartXMLEvent(eventQueue, index);

        SecurityTokenProvider securityTokenProvider = new SecurityTokenProvider() {

            private AbstractSecurityToken derivedKeySecurityToken = null;

            public SecurityToken getSecurityToken() throws XMLSecurityException {

                if (this.derivedKeySecurityToken != null) {
                    return this.derivedKeySecurityToken;
                }

                this.derivedKeySecurityToken = new AbstractSecurityToken(
                        (WSSecurityContext) inputProcessorChain.getSecurityContext(), null, null,
                        derivedKeyTokenType.getId(), null) {

                    private SecurityToken referencedSecurityToken = null;

                    private SecurityToken getReferencedSecurityToken() throws XMLSecurityException {
                        if (this.referencedSecurityToken != null) {
                            return referencedSecurityToken;
                        }

                        this.referencedSecurityToken = SecurityTokenFactoryImpl.getSecurityToken(
                                derivedKeyTokenType.getSecurityTokenReference(),
                                securityProperties.getDecryptionCrypto(),
                                securityProperties.getCallbackHandler(),
                                inputProcessorChain.getSecurityContext()
                        );
                        this.referencedSecurityToken.addWrappedToken(this);
                        return this.referencedSecurityToken;
                    }

                    public boolean isAsymmetric() {
                        return false;
                    }

                    protected Key getKey(String algorithmURI, XMLSecurityConstants.KeyUsage keyUsage) throws XMLSecurityException {
                        byte[] secret;
                        SecurityToken referencedSecurityToken = getReferencedSecurityToken();
                        if (referencedSecurityToken != null) {
                            if (referencedSecurityToken instanceof UsernameSecurityToken) {
                                UsernameSecurityToken usernameSecurityToken = (UsernameSecurityToken) referencedSecurityToken;
                                secret = usernameSecurityToken.generateDerivedKey(
                                        usernameSecurityToken.getPassword(),
                                        usernameSecurityToken.getSalt(),
                                        usernameSecurityToken.getIteration()
                                );
                            } else if (referencedSecurityToken instanceof SAMLSecurityToken) {
                                SAMLSecurityToken samlSecurityToken = (SAMLSecurityToken) referencedSecurityToken;
                                secret = samlSecurityToken.getSamlKeyInfo().getSecret();
                            } else {
                                secret = referencedSecurityToken.getSecretKey(algorithmURI, keyUsage).getEncoded();
                            }
                        } else {
                            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_CHECK, "unsupportedKeyId");
                        }
                        byte[] nonce = derivedKeyTokenType.getNonce();
                        if (nonce == null || nonce.length == 0) {
                            throw new WSSecurityException("Missing wsc:Nonce value");
                        }
                        byte[] keyBytes = DerivedKeyUtils.deriveKey(
                                derivedKeyTokenType.getAlgorithm(),
                                derivedKeyTokenType.getLabel(),
                                derivedKeyTokenType.getLength().intValue(),
                                secret,
                                nonce,
                                derivedKeyTokenType.getOffset().intValue()
                        );
                        //todo algo sec event here!
                        String algo = JCEAlgorithmMapper.translateURItoJCEID(algorithmURI);
                        return new SecretKeySpec(keyBytes, algo);
                    }

                    @Override
                    protected PublicKey getPubKey(String algorithmURI, XMLSecurityConstants.KeyUsage keyUsage) throws XMLSecurityException {
                        return null;
                    }

                    public SecurityToken getKeyWrappingToken() throws XMLSecurityException {
                        return getReferencedSecurityToken();
                    }

                    public WSSConstants.TokenType getTokenType() {
                        return WSSConstants.DerivedKeyToken;
                    }
                };
                this.derivedKeySecurityToken.setElementPath(elementPath);
                this.derivedKeySecurityToken.setXMLEvent(responsibleStartXMLEvent);
                return this.derivedKeySecurityToken;
            }

            public String getId() {
                return derivedKeyTokenType.getId();
            }
        };
        inputProcessorChain.getSecurityContext().registerSecurityTokenProvider(derivedKeyTokenType.getId(), securityTokenProvider);

        //fire a tokenSecurityEvent
        DerivedKeyTokenSecurityEvent derivedKeyTokenSecurityEvent = new DerivedKeyTokenSecurityEvent();
        derivedKeyTokenSecurityEvent.setSecurityToken(securityTokenProvider.getSecurityToken());
        ((WSSecurityContext) inputProcessorChain.getSecurityContext()).registerSecurityEvent(derivedKeyTokenSecurityEvent);
    }
}

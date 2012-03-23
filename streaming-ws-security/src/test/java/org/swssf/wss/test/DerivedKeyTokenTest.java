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
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.components.crypto.CryptoType;
import org.apache.ws.security.conversation.ConversationConstants;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.message.*;
import org.apache.ws.security.message.token.SecurityTokenReference;
import org.swssf.wss.WSSec;
import org.swssf.wss.ext.InboundWSSec;
import org.swssf.wss.ext.OutboundWSSec;
import org.swssf.wss.ext.WSSConstants;
import org.swssf.wss.ext.WSSSecurityProperties;
import org.swssf.wss.securityEvent.SecurityEvent;
import org.swssf.wss.test.utils.SOAPUtil;
import org.swssf.xmlsec.test.utils.StAX2DOM;
import org.swssf.xmlsec.test.utils.XmlReaderToWriter;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class DerivedKeyTokenTest extends AbstractTestBase {

    @BeforeClass
    public void setUp() throws Exception {
        WSSConfig.init();
    }

    @DataProvider(name = "versionProvider")
    public Object[][] versionProvider() {
        return new Object[][]{
                {ConversationConstants.VERSION_05_02},
                {ConversationConstants.VERSION_05_12}
        };
    }

    @Test
    public void testEncryptionDecryptionTRIPLEDESOutbound() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.ENCRYPT_WITH_DERIVED_KEY};
            securityProperties.setOutAction(actions);
            byte[] secret = new byte[128 / 8];
            WSSConstants.secureRandom.nextBytes(secret);
            CallbackHandlerImpl callbackHandler = new CallbackHandlerImpl(secret);
            securityProperties.setCallbackHandler(callbackHandler);
            securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
            securityProperties.setEncryptionUser("receiver");
            securityProperties.setEncryptionSymAlgorithm("http://www.w3.org/2001/04/xmlenc#tripledes-cbc");
            securityProperties.setEncryptionKeyIdentifierType(WSSConstants.KeyIdentifierType.THUMBPRINT_IDENTIFIER);

            OutboundWSSec wsSecOut = WSSec.getOutboundWSSec(securityProperties);
            XMLStreamWriter xmlStreamWriter = wsSecOut.processOutMessage(baos, "UTF-8", new ArrayList<SecurityEvent>());
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml"));
            XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
            xmlStreamWriter.close();

            Document document = documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedData.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedData.getLocalPart());
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_soap11_Body.getLocalPart());
        }
        {
            String action = WSHandlerConstants.ENCRYPT;
            doInboundSecurityWithWSS4J(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action);
        }
    }

    @Test(dataProvider = "versionProvider")
    public void testEncryptionDecryptionTRIPLEDESInbound(int version) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            Document doc = SOAPUtil.toSOAPPart(SOAPUtil.SAMPLE_SOAP_MSG);
            WSSecHeader secHeader = new WSSecHeader();
            secHeader.insertSecurityHeader(doc);

            WSSecSecurityContextToken sctBuilder = new WSSecSecurityContextToken();
            Crypto crypto = CryptoFactory.getInstance("transmitter-crypto.properties");
            sctBuilder.setWscVersion(version);
            sctBuilder.prepare(doc, crypto);

            //EncryptedKey
            WSSecEncryptedKey encrKeyBuilder = new WSSecEncryptedKey();
            encrKeyBuilder.setUserInfo("receiver");
            encrKeyBuilder.setKeyIdentifierType(WSConstants.THUMBPRINT_IDENTIFIER);
            encrKeyBuilder.prepare(doc, crypto);

            //Key information from the EncryptedKey
            byte[] ek = encrKeyBuilder.getEphemeralKey();
            String tokenIdentifier = encrKeyBuilder.getId();

            //Derived key encryption
            WSSecDKEncrypt encrBuilder = new WSSecDKEncrypt();
            encrBuilder.setWscVersion(version);
            encrBuilder.setSymmetricEncAlgorithm(WSConstants.TRIPLE_DES);
            encrBuilder.setExternalKey(ek, tokenIdentifier);
            encrBuilder.build(doc, secHeader);

            encrKeyBuilder.prependToHeader(secHeader);
            encrKeyBuilder.prependBSTElementToHeader(secHeader);

            NodeList nodeList = doc.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedData.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedData.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);

            javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(baos));
        }

        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setCallbackHandler(new CallbackHandlerImpl());
            InboundWSSec wsSecIn = WSSec.getInboundWSSec(securityProperties);

            SecurityEvent.Event[] expectedSecurityEvents = new SecurityEvent.Event[]{
                    SecurityEvent.Event.X509Token,
                    SecurityEvent.Event.EncryptedPart,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.Operation,
            };
            final TestSecurityEventListener securityEventListener = new TestSecurityEventListener(expectedSecurityEvents);
            XMLStreamReader xmlStreamReader = wsSecIn.processInMessage(
                    xmlInputFactory.createXMLStreamReader(
                            new ByteArrayInputStream(baos.toByteArray())), null, securityEventListener);

            Document document = StAX2DOM.readDoc(documentBuilderFactory.newDocumentBuilder(), xmlStreamReader);

            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedData.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedData.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 0);

            securityEventListener.compare();
        }
    }

    @Test
    public void testEncryptionDecryptionAES128Outbound() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.ENCRYPT_WITH_DERIVED_KEY};
            securityProperties.setOutAction(actions);
            byte[] secret = new byte[128 / 8];
            WSSConstants.secureRandom.nextBytes(secret);
            CallbackHandlerImpl callbackHandler = new CallbackHandlerImpl(secret);
            securityProperties.setCallbackHandler(callbackHandler);
            securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
            securityProperties.setEncryptionUser("receiver");
            securityProperties.setEncryptionSymAlgorithm("http://www.w3.org/2001/04/xmlenc#aes128-cbc");
            securityProperties.setEncryptionKeyIdentifierType(WSSConstants.KeyIdentifierType.THUMBPRINT_IDENTIFIER);

            OutboundWSSec wsSecOut = WSSec.getOutboundWSSec(securityProperties);
            XMLStreamWriter xmlStreamWriter = wsSecOut.processOutMessage(baos, "UTF-8", new ArrayList<SecurityEvent>());
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml"));
            XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
            xmlStreamWriter.close();

            Document document = documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedData.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedData.getLocalPart());
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_soap11_Body.getLocalPart());
        }
        {
            String action = WSHandlerConstants.ENCRYPT;
            doInboundSecurityWithWSS4J(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action);
        }
    }

    @Test(dataProvider = "versionProvider")
    public void testEncryptionDecryptionAES128Inbound(int version) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            Document doc = SOAPUtil.toSOAPPart(SOAPUtil.SAMPLE_SOAP_MSG);
            WSSecHeader secHeader = new WSSecHeader();
            secHeader.insertSecurityHeader(doc);

            WSSecSecurityContextToken sctBuilder = new WSSecSecurityContextToken();
            sctBuilder.setWscVersion(version);
            Crypto crypto = CryptoFactory.getInstance("transmitter-crypto.properties");
            sctBuilder.prepare(doc, crypto);

            //EncryptedKey
            WSSecEncryptedKey encrKeyBuilder = new WSSecEncryptedKey();
            encrKeyBuilder.setUserInfo("receiver");
            encrKeyBuilder.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);
            encrKeyBuilder.prepare(doc, crypto);

            //Key information from the EncryptedKey
            byte[] ek = encrKeyBuilder.getEphemeralKey();
            String tokenIdentifier = encrKeyBuilder.getId();

            //Derived key encryption
            WSSecDKEncrypt encrBuilder = new WSSecDKEncrypt();
            encrBuilder.setWscVersion(version);
            encrBuilder.setSymmetricEncAlgorithm(WSConstants.AES_128);
            encrBuilder.setExternalKey(ek, tokenIdentifier);
            encrBuilder.build(doc, secHeader);

            encrKeyBuilder.prependToHeader(secHeader);
            encrKeyBuilder.prependBSTElementToHeader(secHeader);

            NodeList nodeList = doc.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedData.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedData.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);

            javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(baos));
        }
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setCallbackHandler(new CallbackHandlerImpl());
            InboundWSSec wsSecIn = WSSec.getInboundWSSec(securityProperties);
            XMLStreamReader xmlStreamReader = wsSecIn.processInMessage(xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())));

            Document document = StAX2DOM.readDoc(documentBuilderFactory.newDocumentBuilder(), xmlStreamReader);

            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedData.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedData.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 0);
        }
    }

    @Test
    public void testSignatureOutbound() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.SIGNATURE_WITH_DERIVED_KEY};
            securityProperties.setOutAction(actions);
            CallbackHandlerImpl callbackHandler = new CallbackHandlerImpl();
            securityProperties.setCallbackHandler(callbackHandler);
            securityProperties.setEncryptionKeyIdentifierType(WSSConstants.KeyIdentifierType.THUMBPRINT_IDENTIFIER);
            securityProperties.setSignatureAlgorithm("http://www.w3.org/2000/09/xmldsig#hmac-sha1");
            securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
            securityProperties.setEncryptionUser("receiver");
            securityProperties.setDerivedKeyTokenReference(WSSConstants.DerivedKeyTokenReference.EncryptedKey);
            securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setSignatureUser("receiver");

            OutboundWSSec wsSecOut = WSSec.getOutboundWSSec(securityProperties);
            XMLStreamWriter xmlStreamWriter = wsSecOut.processOutMessage(baos, "UTF-8", new ArrayList<SecurityEvent>());
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml"));
            XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
            xmlStreamWriter.close();

            Document document = documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_wsse_Security.getLocalPart());
        }
        {
            String action = WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
            doInboundSecurityWithWSS4J(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action);
        }
    }

    @Test(dataProvider = "versionProvider")
    public void testSignatureInbound(int version) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            Document doc = SOAPUtil.toSOAPPart(SOAPUtil.SAMPLE_SOAP_MSG);
            WSSecHeader secHeader = new WSSecHeader();
            secHeader.insertSecurityHeader(doc);

            //EncryptedKey
            WSSecEncryptedKey encrKeyBuilder = new WSSecEncryptedKey();
            encrKeyBuilder.setUserInfo("receiver");
            encrKeyBuilder.setKeyIdentifierType(WSConstants.THUMBPRINT_IDENTIFIER);
            Crypto crypto = CryptoFactory.getInstance("transmitter-crypto.properties");
            encrKeyBuilder.prepare(doc, crypto);

            //Key information from the EncryptedKey
            byte[] ek = encrKeyBuilder.getEphemeralKey();
            String tokenIdentifier = encrKeyBuilder.getId();

            //Derived key encryption
            WSSecDKSign sigBuilder = new WSSecDKSign();
            sigBuilder.setWscVersion(version);
            sigBuilder.setExternalKey(ek, tokenIdentifier);
            sigBuilder.setSignatureAlgorithm(WSConstants.HMAC_SHA1);
            sigBuilder.build(doc, secHeader);

            encrKeyBuilder.prependToHeader(secHeader);
            encrKeyBuilder.prependBSTElementToHeader(secHeader);

            NodeList nodeList = doc.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);

            javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(baos));
        }

        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setCallbackHandler(new CallbackHandlerImpl());
            InboundWSSec wsSecIn = WSSec.getInboundWSSec(securityProperties);
            XMLStreamReader xmlStreamReader = wsSecIn.processInMessage(xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())));

            Document document = StAX2DOM.readDoc(documentBuilderFactory.newDocumentBuilder(), xmlStreamReader);

            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
        }
    }

    @Test
    public void testSignatureThumbprintSHA1Outbound() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.SIGNATURE_WITH_DERIVED_KEY};
            securityProperties.setOutAction(actions);
            CallbackHandlerImpl callbackHandler = new CallbackHandlerImpl();
            securityProperties.setCallbackHandler(callbackHandler);
            securityProperties.setSignatureAlgorithm("http://www.w3.org/2000/09/xmldsig#hmac-sha1");
            securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setSignatureUser("receiver");
            securityProperties.setDerivedKeyTokenReference(WSSConstants.DerivedKeyTokenReference.DirectReference);
            securityProperties.setDerivedKeyKeyIdentifierType(WSSConstants.KeyIdentifierType.THUMBPRINT_IDENTIFIER);

            OutboundWSSec wsSecOut = WSSec.getOutboundWSSec(securityProperties);
            XMLStreamWriter xmlStreamWriter = wsSecOut.processOutMessage(baos, "UTF-8", new ArrayList<SecurityEvent>());
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml"));
            XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
            xmlStreamWriter.close();

            Document document = documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_wsse_Security.getLocalPart());

            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_wsc0502_SecurityContextToken.getNamespaceURI(), WSSConstants.TAG_wsc0502_SecurityContextToken.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 0);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedKey.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedKey.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 0);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_wsc0502_DerivedKeyToken.getNamespaceURI(), WSSConstants.TAG_wsc0502_DerivedKeyToken.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_wsse_KeyIdentifier.getNamespaceURI(), WSSConstants.TAG_wsse_KeyIdentifier.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
            Attr attr = (Attr) nodeList.item(0).getAttributes().getNamedItem(WSSConstants.ATT_NULL_ValueType.getLocalPart());
            Assert.assertEquals(attr.getValue(), WSSConstants.NS_THUMBPRINT);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
        }
        {
            String action = WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
            doInboundSecurityWithWSS4J(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action);
        }
    }

    @Test(dataProvider = "versionProvider")
    public void testSignatureThumbprintSHA1Inbound(int version) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            Document doc = SOAPUtil.toSOAPPart(SOAPUtil.SAMPLE_SOAP_MSG);
            WSSecHeader secHeader = new WSSecHeader();
            secHeader.insertSecurityHeader(doc);

            SecurityTokenReference secToken = new SecurityTokenReference(doc);
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias("transmitter");
            Crypto crypto = CryptoFactory.getInstance("transmitter-crypto.properties");
            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
            secToken.setKeyIdentifierThumb(certs[0]);

            WSSecDKSign sigBuilder = new WSSecDKSign();
            sigBuilder.setWscVersion(version);
            java.security.Key key = crypto.getPrivateKey("transmitter", "default");
            sigBuilder.setExternalKey(key.getEncoded(), secToken.getElement());
            sigBuilder.setSignatureAlgorithm(WSConstants.HMAC_SHA1);
            sigBuilder.build(doc, secHeader);

            sigBuilder.prependDKElementToHeader(secHeader);

            NodeList nodeList = doc.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);

            javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(baos));
        }

        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
            securityProperties.setCallbackHandler(new CallbackHandlerImpl());
            InboundWSSec wsSecIn = WSSec.getInboundWSSec(securityProperties);
            XMLStreamReader xmlStreamReader = wsSecIn.processInMessage(xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())));

            Document document = StAX2DOM.readDoc(documentBuilderFactory.newDocumentBuilder(), xmlStreamReader);

            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
        }
    }

    @Test
    public void testSignatureSKIOutbound() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.SIGNATURE_WITH_DERIVED_KEY};
            securityProperties.setOutAction(actions);
            CallbackHandlerImpl callbackHandler = new CallbackHandlerImpl();
            securityProperties.setCallbackHandler(callbackHandler);
            securityProperties.setSignatureAlgorithm("http://www.w3.org/2000/09/xmldsig#hmac-sha1");
            securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setSignatureUser("receiver");
            securityProperties.setDerivedKeyTokenReference(WSSConstants.DerivedKeyTokenReference.DirectReference);
            securityProperties.setDerivedKeyKeyIdentifierType(WSSConstants.KeyIdentifierType.SKI_KEY_IDENTIFIER);

            OutboundWSSec wsSecOut = WSSec.getOutboundWSSec(securityProperties);
            XMLStreamWriter xmlStreamWriter = wsSecOut.processOutMessage(baos, "UTF-8", new ArrayList<SecurityEvent>());
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml"));
            XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
            xmlStreamWriter.close();

            Document document = documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_wsse_Security.getLocalPart());

            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_wsc0502_SecurityContextToken.getNamespaceURI(), WSSConstants.TAG_wsc0502_SecurityContextToken.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 0);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedKey.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedKey.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 0);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_wsc0502_DerivedKeyToken.getNamespaceURI(), WSSConstants.TAG_wsc0502_DerivedKeyToken.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_wsse_KeyIdentifier.getNamespaceURI(), WSSConstants.TAG_wsse_KeyIdentifier.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
            Attr attr = (Attr) nodeList.item(0).getAttributes().getNamedItem(WSSConstants.ATT_NULL_ValueType.getLocalPart());
            Assert.assertEquals(attr.getValue(), WSSConstants.NS_X509SubjectKeyIdentifier);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
        }
        {
            String action = WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
            doInboundSecurityWithWSS4J(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action);
        }
    }

    @Test(dataProvider = "versionProvider")
    public void testSignatureSKIInbound(int version) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            Document doc = SOAPUtil.toSOAPPart(SOAPUtil.SAMPLE_SOAP_MSG);
            WSSecHeader secHeader = new WSSecHeader();
            secHeader.insertSecurityHeader(doc);

            SecurityTokenReference secToken = new SecurityTokenReference(doc);
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias("transmitter");
            Crypto crypto = CryptoFactory.getInstance("transmitter-crypto.properties");
            X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
            secToken.setKeyIdentifierSKI(certs[0], crypto);

            WSSecDKSign sigBuilder = new WSSecDKSign();
            sigBuilder.setWscVersion(version);
            java.security.Key key = crypto.getPrivateKey("transmitter", "default");
            sigBuilder.setExternalKey(key.getEncoded(), secToken.getElement());
            sigBuilder.setSignatureAlgorithm(WSConstants.HMAC_SHA1);
            sigBuilder.build(doc, secHeader);

            sigBuilder.prependDKElementToHeader(secHeader);

            NodeList nodeList = doc.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);

            javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(baos));
        }

        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
            securityProperties.setCallbackHandler(new CallbackHandlerImpl());
            InboundWSSec wsSecIn = WSSec.getInboundWSSec(securityProperties);
            XMLStreamReader xmlStreamReader = wsSecIn.processInMessage(xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())));

            Document document = StAX2DOM.readDoc(documentBuilderFactory.newDocumentBuilder(), xmlStreamReader);

            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
        }
    }

    @Test
    public void testSignatureEncryptOutbound() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.SIGNATURE_WITH_DERIVED_KEY, WSSConstants.ENCRYPT_WITH_DERIVED_KEY};
            securityProperties.setOutAction(actions);
            CallbackHandlerImpl callbackHandler = new CallbackHandlerImpl();
            securityProperties.setCallbackHandler(callbackHandler);
            securityProperties.setSignatureAlgorithm("http://www.w3.org/2000/09/xmldsig#hmac-sha1");
            securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setSignatureUser("receiver");
            securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setEncryptionUser("receiver");
            securityProperties.setEncryptionKeyIdentifierType(WSSConstants.KeyIdentifierType.THUMBPRINT_IDENTIFIER);
            securityProperties.setSignatureKeyIdentifierType(WSSConstants.KeyIdentifierType.THUMBPRINT_IDENTIFIER);
            securityProperties.setDerivedKeyTokenReference(WSSConstants.DerivedKeyTokenReference.EncryptedKey);

            OutboundWSSec wsSecOut = WSSec.getOutboundWSSec(securityProperties);
            XMLStreamWriter xmlStreamWriter = wsSecOut.processOutMessage(baos, "UTF-8", new ArrayList<SecurityEvent>());
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml"));
            XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
            xmlStreamWriter.close();

            Document document = documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_wsse_Security.getLocalPart());

            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_wsc0502_SecurityContextToken.getNamespaceURI(), WSSConstants.TAG_wsc0502_SecurityContextToken.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 0);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedKey.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedKey.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 2);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_wsc0502_DerivedKeyToken.getNamespaceURI(), WSSConstants.TAG_wsc0502_DerivedKeyToken.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 2);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_wsse_KeyIdentifier.getNamespaceURI(), WSSConstants.TAG_wsse_KeyIdentifier.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 2);
            Attr attr = (Attr) nodeList.item(0).getAttributes().getNamedItem(WSSConstants.ATT_NULL_ValueType.getLocalPart());
            Assert.assertEquals(attr.getValue(), WSSConstants.NS_THUMBPRINT);
            attr = (Attr) nodeList.item(1).getAttributes().getNamedItem(WSSConstants.ATT_NULL_ValueType.getLocalPart());
            Assert.assertEquals(attr.getValue(), WSSConstants.NS_THUMBPRINT);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
        }
        {
            String action = WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
            doInboundSecurityWithWSS4J(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action);
        }
    }

    @Test(dataProvider = "versionProvider")
    public void testSignatureEncryptInbound(int version) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            Document doc = SOAPUtil.toSOAPPart(SOAPUtil.SAMPLE_SOAP_MSG);
            WSSecHeader secHeader = new WSSecHeader();
            secHeader.insertSecurityHeader(doc);

            Crypto crypto = CryptoFactory.getInstance("transmitter-crypto.properties");

            //EncryptedKey
            WSSecEncryptedKey encrKeyBuilder = new WSSecEncryptedKey();
            encrKeyBuilder.setUserInfo("receiver");
            encrKeyBuilder.setKeyIdentifierType(WSConstants.THUMBPRINT_IDENTIFIER);
            encrKeyBuilder.prepare(doc, crypto);

            //Key information from the EncryptedKey
            byte[] ek = encrKeyBuilder.getEphemeralKey();
            String tokenIdentifier = encrKeyBuilder.getId();

            //Derived key encryption
            WSSecDKSign sigBuilder = new WSSecDKSign();
            sigBuilder.setWscVersion(version);
            sigBuilder.setExternalKey(ek, tokenIdentifier);
            sigBuilder.setSignatureAlgorithm(WSConstants.HMAC_SHA1);
            Document signedDoc = sigBuilder.build(doc, secHeader);

            //Derived key signature
            WSSecDKEncrypt encrBuilder = new WSSecDKEncrypt();
            encrBuilder.setWscVersion(version);
            encrBuilder.setSymmetricEncAlgorithm(WSConstants.AES_128);
            encrBuilder.setExternalKey(ek, tokenIdentifier);
            encrBuilder.build(signedDoc, secHeader);

            encrKeyBuilder.prependToHeader(secHeader);
            encrKeyBuilder.prependBSTElementToHeader(secHeader);

            NodeList nodeList = doc.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);

            nodeList = doc.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedData.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedData.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);

            javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(baos));
        }

        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setCallbackHandler(new CallbackHandlerImpl());
            InboundWSSec wsSecIn = WSSec.getInboundWSSec(securityProperties);

            SecurityEvent.Event[] expectedSecurityEvents = new SecurityEvent.Event[]{
                    SecurityEvent.Event.X509Token,
                    SecurityEvent.Event.SignatureValue,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.EncryptedPart,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.Operation,
            };
            final TestSecurityEventListener securityEventListener = new TestSecurityEventListener(expectedSecurityEvents);

            XMLStreamReader xmlStreamReader = wsSecIn.processInMessage(xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())), null, securityEventListener);

            Document document = StAX2DOM.readDoc(documentBuilderFactory.newDocumentBuilder(), xmlStreamReader);

            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);

            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedData.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedData.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 0);

            securityEventListener.compare();
        }
    }

    @Test
    public void testEncryptSignatureOutbound() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.ENCRYPT_WITH_DERIVED_KEY, WSSConstants.SIGNATURE_WITH_DERIVED_KEY};
            securityProperties.setOutAction(actions);
            CallbackHandlerImpl callbackHandler = new CallbackHandlerImpl();
            securityProperties.setCallbackHandler(callbackHandler);
            securityProperties.setSignatureAlgorithm("http://www.w3.org/2000/09/xmldsig#hmac-sha1");
            securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setSignatureUser("receiver");
            securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setEncryptionUser("receiver");
            securityProperties.setEncryptionKeyIdentifierType(WSSConstants.KeyIdentifierType.THUMBPRINT_IDENTIFIER);
            securityProperties.setDerivedKeyTokenReference(WSSConstants.DerivedKeyTokenReference.EncryptedKey);

            OutboundWSSec wsSecOut = WSSec.getOutboundWSSec(securityProperties);
            XMLStreamWriter xmlStreamWriter = wsSecOut.processOutMessage(baos, "UTF-8", new ArrayList<SecurityEvent>());
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml"));
            XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
            xmlStreamWriter.close();

            Document document = documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_wsse_Security.getLocalPart());

            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_wsc0502_SecurityContextToken.getNamespaceURI(), WSSConstants.TAG_wsc0502_SecurityContextToken.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 0);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedKey.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedKey.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 2);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_wsc0502_DerivedKeyToken.getNamespaceURI(), WSSConstants.TAG_wsc0502_DerivedKeyToken.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 2);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_wsse_KeyIdentifier.getNamespaceURI(), WSSConstants.TAG_wsse_KeyIdentifier.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 2);
            Attr attr = (Attr) nodeList.item(0).getAttributes().getNamedItem(WSSConstants.ATT_NULL_ValueType.getLocalPart());
            Assert.assertEquals(attr.getValue(), WSSConstants.NS_THUMBPRINT);
            attr = (Attr) nodeList.item(1).getAttributes().getNamedItem(WSSConstants.ATT_NULL_ValueType.getLocalPart());
            Assert.assertEquals(attr.getValue(), WSSConstants.NS_THUMBPRINT);
            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
        }
        {
            String action = WSHandlerConstants.ENCRYPT + " " + WSHandlerConstants.SIGNATURE;
            doInboundSecurityWithWSS4J(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action);
        }
    }

    @Test(dataProvider = "versionProvider")
    public void testEncryptSignatureInbound(int version) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            Document doc = SOAPUtil.toSOAPPart(SOAPUtil.SAMPLE_SOAP_MSG);
            WSSecHeader secHeader = new WSSecHeader();
            secHeader.insertSecurityHeader(doc);

            Crypto crypto = CryptoFactory.getInstance("transmitter-crypto.properties");

            //EncryptedKey
            WSSecEncryptedKey encrKeyBuilder = new WSSecEncryptedKey();
            encrKeyBuilder.setUserInfo("receiver");
            encrKeyBuilder.setKeyIdentifierType(WSConstants.THUMBPRINT_IDENTIFIER);
            encrKeyBuilder.prepare(doc, crypto);

            //Key information from the EncryptedKey
            byte[] ek = encrKeyBuilder.getEphemeralKey();
            String tokenIdentifier = encrKeyBuilder.getId();

            //Derived key signature
            WSSecDKEncrypt encrBuilder = new WSSecDKEncrypt();
            encrBuilder.setWscVersion(version);
            encrBuilder.setSymmetricEncAlgorithm(WSConstants.AES_128);
            encrBuilder.setExternalKey(ek, tokenIdentifier);
            encrBuilder.build(doc, secHeader);

            //Derived key encryption
            WSSecDKSign sigBuilder = new WSSecDKSign();
            sigBuilder.setWscVersion(version);
            sigBuilder.setExternalKey(ek, tokenIdentifier);
            sigBuilder.setSignatureAlgorithm(WSConstants.HMAC_SHA1);
            sigBuilder.build(doc, secHeader);

            encrKeyBuilder.prependToHeader(secHeader);
            encrKeyBuilder.prependBSTElementToHeader(secHeader);

            NodeList nodeList = doc.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);

            nodeList = doc.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedData.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedData.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);

            javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(baos));
        }

        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setCallbackHandler(new CallbackHandlerImpl());
            InboundWSSec wsSecIn = WSSec.getInboundWSSec(securityProperties);
            XMLStreamReader xmlStreamReader = wsSecIn.processInMessage(xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())));

            Document document = StAX2DOM.readDoc(documentBuilderFactory.newDocumentBuilder(), xmlStreamReader);

            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);

            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_xenc_EncryptedData.getNamespaceURI(), WSSConstants.TAG_xenc_EncryptedData.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 0);
        }
    }
}

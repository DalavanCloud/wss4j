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

import org.apache.ws.security.handler.WSHandlerConstants;
import org.swssf.wss.WSSec;
import org.swssf.wss.ext.*;
import org.swssf.wss.securityEvent.SecurityEvent;
import org.swssf.wss.securityEvent.SecurityEventListener;
import org.swssf.xmlsec.ext.SecurePart;
import org.swssf.xmlsec.test.utils.StAX2DOM;
import org.swssf.xmlsec.test.utils.XmlReaderToWriter;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class InteroperabilityTest extends AbstractTestBase {

    @Test(invocationCount = 1)
    public void testInteroperabilityInbound() throws Exception {

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.SIGNATURE_PARTS, "{Element}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{Element}{http://schemas.xmlsoap.org/soap/envelope/}Body;");
        Document securedDocument = doOutboundSecurityWithWSS4J(sourceDocument, action, properties);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(securedDocument), new StreamResult(baos));

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
        securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());

        SecurityEvent.Event[] expectedSecurityEvents = new SecurityEvent.Event[]{
                SecurityEvent.Event.Operation,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.SignatureValue,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.Timestamp,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.EncryptedPart,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
        };
        final TestSecurityEventListener securityEventListener = new TestSecurityEventListener(expectedSecurityEvents);
        Document document = doInboundSecurity(securityProperties,
                xmlInputFactory.createXMLStreamReader(
                        new ByteArrayInputStream(baos.toByteArray())), securityEventListener);

        //read the whole stream:
        transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(
                new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        // > /dev/null
                    }
                }
        ));
        securityEventListener.compare();
    }

    @Test(invocationCount = 1)
    public void testInteroperabilityInboundSOAP12() throws Exception {

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.2.xml");

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.SIGNATURE_PARTS, "{Element}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{Element}{http://www.w3.org/2003/05/soap-envelope}Body;");
        Document securedDocument = doOutboundSecurityWithWSS4J(sourceDocument, action, properties);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(securedDocument), new StreamResult(baos));

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
        securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());

        SecurityEvent.Event[] expectedSecurityEvents = new SecurityEvent.Event[]{
                SecurityEvent.Event.Operation,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.SignatureValue,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.Timestamp,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.EncryptedPart,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
        };
        final TestSecurityEventListener securityEventListener = new TestSecurityEventListener(expectedSecurityEvents);
        Document document = doInboundSecurity(
                securityProperties,
                xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())), securityEventListener);

        //read the whole stream:
        transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(
                new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        // > /dev/null
                    }
                }
        ));
        securityEventListener.compare();
    }

    @Test(invocationCount = 1)
    public void testInteroperabilityEncryptedSignatureInbound() throws Exception {

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.SIGNATURE_PARTS, "{Element}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{Element}{http://schemas.xmlsoap.org/soap/envelope/}Body;");
        properties.setProperty(WSHandlerConstants.ENCRYPTION_PARTS, "{Element}{http://www.w3.org/2000/09/xmldsig#}Signature;{Content}{http://schemas.xmlsoap.org/soap/envelope/}Body;");
        Document securedDocument = doOutboundSecurityWithWSS4J(sourceDocument, action, properties);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(securedDocument), new StreamResult(baos));

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
        securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());

        SecurityEvent.Event[] expectedSecurityEvents = new SecurityEvent.Event[]{
                SecurityEvent.Event.Operation,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.EncryptedElement,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.SignatureValue,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.Timestamp,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.EncryptedPart,
                SecurityEvent.Event.AlgorithmSuite,
        };
        final TestSecurityEventListener securityEventListener = new TestSecurityEventListener(expectedSecurityEvents);
        Document document = doInboundSecurity(
                securityProperties,
                xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())), securityEventListener);

        //read the whole stream:
        transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(
                new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        // > /dev/null
                    }
                }
        ));

        securityEventListener.compare();
    }

    //Not supported ATM: Timestamp encrypted and then Signed
    /*
    @Test(invocationCount = 1)
    public void testInteroperabilitySignedEncryptedTimestampInbound() throws Exception {

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.ENCRYPT + " " + WSHandlerConstants.SIGNATURE;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.SIGNATURE_PARTS, "{Element}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{Element}{http://schemas.xmlsoap.org/soap/envelope/}Body;");
        properties.setProperty(WSHandlerConstants.ENCRYPTION_PARTS, "{Content}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{Content}{http://schemas.xmlsoap.org/soap/envelope/}Body;");
        Document securedDocument = doOutboundSecurityWithWSS4J(sourceDocument, action, properties);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(securedDocument), new StreamResult(baos));

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
        securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());

        Document document = doInboundSecurity(securityProperties, xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())));

        //read the whole stream:
        transformer = TRANSFORMER_FACTORY.newTransformer();
        //transformer.transform(new DOMSource(document), new StreamResult(System.out));
        transformer.transform(new DOMSource(document), new StreamResult(
                new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        // > /dev/null
                    }
                }
        ));
    }
*/

    @Test(invocationCount = 1)
    public void testInteroperabilityInboundReverseOrder() throws Exception {

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");

        String action = WSHandlerConstants.ENCRYPT + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.TIMESTAMP;
        Document securedDocument = doOutboundSecurityWithWSS4J(sourceDocument, action, new Properties());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(securedDocument), new StreamResult(baos));

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
        securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());

        SecurityEvent.Event[] expectedSecurityEvents = new SecurityEvent.Event[]{
                SecurityEvent.Event.Operation,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.Timestamp,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.SignatureValue,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.EncryptedPart,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
        };
        final TestSecurityEventListener securityEventListener = new TestSecurityEventListener(expectedSecurityEvents);
        Document document = doInboundSecurity(
                securityProperties,
                xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())), securityEventListener);

        //read the whole stream:
        transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(
                new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        // > /dev/null
                    }
                }
        ));
        securityEventListener.compare();
    }

    @Test
    public void testInteroperabilityOutbound() throws Exception {

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.setEncryptionUser("receiver");
        securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        securityProperties.setSignatureUser("transmitter");
        securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.TIMESTAMP, WSSConstants.SIGNATURE, WSSConstants.ENCRYPT};
        securityProperties.setOutAction(actions);

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");
        ByteArrayOutputStream baos = doOutboundSecurity(securityProperties, sourceDocument);

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        doInboundSecurityWithWSS4J(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action);
    }

    @Test
    public void testInteroperabilityOutboundReverseOrder() throws Exception {

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.setEncryptionUser("receiver");
        securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        securityProperties.setSignatureUser("transmitter");
        securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.ENCRYPT, WSSConstants.SIGNATURE, WSSConstants.TIMESTAMP};
        securityProperties.setOutAction(actions);

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");
        ByteArrayOutputStream baos = doOutboundSecurity(securityProperties, sourceDocument);

        String action = WSHandlerConstants.ENCRYPT + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.TIMESTAMP;
        doInboundSecurityWithWSS4J(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action);
    }

    @Test
    public void testInteroperabilityOutboundSignature() throws Exception {

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.setEncryptionUser("receiver");
        securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        securityProperties.setSignatureUser("transmitter");
        securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.SIGNATURE};
        securityProperties.setOutAction(actions);

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");
        ByteArrayOutputStream baos = doOutboundSecurity(securityProperties, sourceDocument);

        String action = WSHandlerConstants.SIGNATURE;
        doInboundSecurityWithWSS4J(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action);
    }

    @Test(invocationCount = 1)
    public void testInteroperabilityInboundSecurityHeaderTimestampOrder() throws Exception {

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.SIGNATURE_PARTS, "{Element}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{Element}{http://schemas.xmlsoap.org/soap/envelope/}Body;");
        properties.setProperty(WSHandlerConstants.ENCRYPTION_PARTS, "{Element}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{Content}{http://schemas.xmlsoap.org/soap/envelope/}Body;");
        Document securedDocument = doOutboundSecurityWithWSS4J(sourceDocument, action, properties);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(securedDocument), new StreamResult(baos));

        XPathExpression xPathExpression = getXPath("/env:Envelope/env:Header/wsse:Security/xenc:EncryptedData");
        Element timeStamp = (Element) xPathExpression.evaluate(securedDocument, XPathConstants.NODE);
        Element securityHeaderNode = (Element) timeStamp.getParentNode();
        securityHeaderNode.removeChild(timeStamp);

        xPathExpression = getXPath("/env:Envelope/env:Header/wsse:Security/dsig:Signature");
        Element signature = (Element) xPathExpression.evaluate(securedDocument, XPathConstants.NODE);

        securityHeaderNode.insertBefore(timeStamp, signature);

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
        securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());

        SecurityEvent.Event[] expectedSecurityEvents = new SecurityEvent.Event[]{
                SecurityEvent.Event.Operation,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.SignatureValue,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.EncryptedElement,
                SecurityEvent.Event.Timestamp,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.EncryptedPart,
                SecurityEvent.Event.AlgorithmSuite,
        };
        final TestSecurityEventListener securityEventListener = new TestSecurityEventListener(expectedSecurityEvents);
        Document document = doInboundSecurity(
                securityProperties, xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())), securityEventListener);

        //read the whole stream:
        transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(
                new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        // > /dev/null
                    }
                }
        ));
        securityEventListener.compare();
    }

    @Test
    public void testEncDecryptionUseReqSigCert() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");
            String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
            Properties properties = new Properties();
            properties.setProperty(WSHandlerConstants.SIGNATURE_PARTS, "{Element}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{Element}{http://schemas.xmlsoap.org/soap/envelope/}Body;");
            Map<String, Object> messageContext = doOutboundSecurityWithWSS4J_1(sourceDocument, action, properties);
            Document securedDocument = (Document) messageContext.get(SECURED_DOCUMENT);

            //some test that we can really sure we get what we want from WSS4J
            NodeList nodeList = securedDocument.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_wsse_Security.getLocalPart());

            javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.transform(new DOMSource(securedDocument), new StreamResult(baos));
        }

        final List<SecurityEvent> securityEventList = new ArrayList<SecurityEvent>();
        //done signature; now test sig-verification:
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setCallbackHandler(new org.swssf.wss.test.CallbackHandlerImpl());
            InboundWSSec wsSecIn = WSSec.getInboundWSSec(securityProperties);

            SecurityEventListener securityEventListener = new SecurityEventListener() {
                public void registerSecurityEvent(SecurityEvent securityEvent) throws WSSecurityException {
                    securityEventList.add(securityEvent);
                }
            };

            XMLStreamReader xmlStreamReader = wsSecIn.processInMessage(xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())), new ArrayList<SecurityEvent>(), securityEventListener);

            Document document = StAX2DOM.readDoc(documentBuilderFactory.newDocumentBuilder(), xmlStreamReader);

            //header element must still be there
            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_wsse_Security.getLocalPart());
        }

        //so we have a request generated, now do the response:
        baos = new ByteArrayOutputStream();
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.TIMESTAMP, WSSConstants.SIGNATURE, WSSConstants.ENCRYPT};
            securityProperties.setOutAction(actions);
            securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setSignatureUser("receiver");
            securityProperties.setCallbackHandler(new CallbackHandlerImpl());
            securityProperties.setUseReqSigCertForEncryption(true);

            OutboundWSSec wsSecOut = WSSec.getOutboundWSSec(securityProperties);
            XMLStreamWriter xmlStreamWriter = wsSecOut.processOutMessage(baos, "UTF-8", securityEventList);
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml"));
            XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
            xmlStreamWriter.close();

            Document document = documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_wsse_Security.getLocalPart());

            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Reference.getNamespaceURI(), WSSConstants.TAG_dsig_Reference.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);

            nodeList = document.getElementsByTagNameNS(WSSConstants.NS_SOAP11, WSSConstants.TAG_soap_Body_LocalName);
            Assert.assertEquals(nodeList.getLength(), 1);
            String idAttrValue = ((Element) nodeList.item(0)).getAttributeNS(WSSConstants.ATT_wsu_Id.getNamespaceURI(), WSSConstants.ATT_wsu_Id.getLocalPart());
            Assert.assertNotNull(idAttrValue);
            Assert.assertTrue(idAttrValue.startsWith("id-"), "wsu:id Attribute doesn't start with id");
        }

        //verify SigConf response:
        {
            String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
            Properties properties = new Properties();
            doInboundSecurityWithWSS4J_1(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action, properties, true);
        }
    }

    @Test
    public void testEncryptedSignatureC14NInclusivePartsOutbound() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.SIGNATURE, WSSConstants.ENCRYPT};
            securityProperties.setOutAction(actions);
            securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
            securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
            securityProperties.setSignatureUser("transmitter");
            securityProperties.setEncryptionUser("receiver");
            securityProperties.addSignaturePart(new SecurePart("complexType", "http://www.w3.org/1999/XMLSchema", SecurePart.Modifier.Element));
            securityProperties.setSignatureCanonicalizationAlgorithm("http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments");
            securityProperties.setCallbackHandler(new CallbackHandlerImpl());

            OutboundWSSec wsSecOut = WSSec.getOutboundWSSec(securityProperties);
            XMLStreamWriter xmlStreamWriter = wsSecOut.processOutMessage(baos, "UTF-8", new ArrayList<SecurityEvent>());
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml"));
            XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
            xmlStreamWriter.close();

            Document document = documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_wsse_Security.getLocalPart());

            nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Reference.getNamespaceURI(), WSSConstants.TAG_dsig_Reference.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 25);

            nodeList = document.getElementsByTagNameNS("http://www.w3.org/1999/XMLSchema", "complexType");
            Assert.assertEquals(nodeList.getLength(), 0);
        }

        //done signature; now test sig-verification:
        {
            String action = WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
            Properties properties = new Properties();
            properties.setProperty(WSHandlerConstants.IS_BSP_COMPLIANT, "false");
            doInboundSecurityWithWSS4J_1(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action, properties, false);
        }
    }

    /**
     * Inclusive Canonicalization with Encryption is problematic because xenc namespace "leaks"
     * WSS4J sets the xenc ns on the soap envelope which is not included in the signature on the sending
     * side. swsssf sets the ns where it belongs and so we don't have this problem. But if we
     * get an xenc ns on the envelope we will get a signature error. This case can't be handled correctly.
     * This is also one of the reasons why a exclusive canonicalisation is preferred for SOAP
     *
     * @throws Exception
     */
    @Test(invocationCount = 100) //retest 100 times to make sure we don't have a threading issue
    public void testSignatureC14NInclusivePartsInbound() throws Exception {
        Document securedDocument;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.SIGNATURE, WSSConstants.ENCRYPT};
            securityProperties.setOutAction(actions);
            securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
            securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
            securityProperties.setSignatureUser("transmitter");
            securityProperties.setEncryptionUser("receiver");
            securityProperties.addSignaturePart(new SecurePart("complexType", "http://www.w3.org/1999/XMLSchema", SecurePart.Modifier.Element));
            securityProperties.setSignatureCanonicalizationAlgorithm("http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments");
            securityProperties.setCallbackHandler(new CallbackHandlerImpl());

            OutboundWSSec wsSecOut = WSSec.getOutboundWSSec(securityProperties);
            XMLStreamWriter xmlStreamWriter = wsSecOut.processOutMessage(baos, "UTF-8", new ArrayList<SecurityEvent>());
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml"));
            XmlReaderToWriter.writeAll(xmlStreamReader, xmlStreamWriter);
            xmlStreamWriter.close();

            securedDocument = documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
            NodeList nodeList = securedDocument.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_wsse_Security.getLocalPart());
        }

        {
            String action = WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
            Properties properties = new Properties();
            properties.setProperty(WSHandlerConstants.IS_BSP_COMPLIANT, "false");
            doInboundSecurityWithWSS4J_1(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action, properties, false);
        }

        //done signature; now test sig-verification:
        {
            WSSSecurityProperties securityProperties = new WSSSecurityProperties();
            securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
            securityProperties.setCallbackHandler(new CallbackHandlerImpl());
            InboundWSSec wsSecIn = WSSec.getInboundWSSec(securityProperties);

            SecurityEvent.Event[] expectedSecurityEvents = new SecurityEvent.Event[]{
                    SecurityEvent.Event.Operation,
                    SecurityEvent.Event.X509Token,
                    SecurityEvent.Event.X509Token,
                    SecurityEvent.Event.SignatureValue,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.EncryptedPart,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.AlgorithmSuite,
                    SecurityEvent.Event.SignedElement,
            };
            final TestSecurityEventListener securityEventListener = new TestSecurityEventListener(expectedSecurityEvents);
            XMLStreamReader xmlStreamReader = wsSecIn.processInMessage(
                    xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())), null, securityEventListener);

            Document document = StAX2DOM.readDoc(documentBuilderFactory.newDocumentBuilder(), xmlStreamReader);

            //header element must still be there
            NodeList nodeList = document.getElementsByTagNameNS(WSSConstants.TAG_dsig_Signature.getNamespaceURI(), WSSConstants.TAG_dsig_Signature.getLocalPart());
            Assert.assertEquals(nodeList.getLength(), 1);
            Assert.assertEquals(nodeList.item(0).getParentNode().getLocalName(), WSSConstants.TAG_wsse_Security.getLocalPart());

            securityEventListener.compare();
        }
    }

    @Test(invocationCount = 1)
    public void testInteroperabilitySOAPActionInbound() throws Exception {

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.SIGNATURE_PARTS, "{Element}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{Element}{http://schemas.xmlsoap.org/soap/envelope/}Body;");
        properties.setProperty(WSHandlerConstants.ACTOR, "test");
        Document securedDocument = doOutboundSecurityWithWSS4J(sourceDocument, action, properties);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(securedDocument), new StreamResult(baos));

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.setActor("test");
        securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
        securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());

        SecurityEvent.Event[] expectedSecurityEvents = new SecurityEvent.Event[]{
                SecurityEvent.Event.Operation,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.SignatureValue,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.Timestamp,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.EncryptedPart,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
        };
        final TestSecurityEventListener securityEventListener = new TestSecurityEventListener(expectedSecurityEvents);
        Document document = doInboundSecurity(
                securityProperties, xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())), securityEventListener);

        //read the whole stream:
        transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(
                new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        // > /dev/null
                    }
                }
        ));
        securityEventListener.compare();
    }

    @Test(invocationCount = 1)
    public void testInteroperabilityInvalidSOAPActionInbound() throws Exception {

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.SIGNATURE_PARTS, "{Element}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{Element}{http://schemas.xmlsoap.org/soap/envelope/}Body;");
        properties.setProperty(WSHandlerConstants.ACTOR, "test");
        Document securedDocument = doOutboundSecurityWithWSS4J(sourceDocument, action, properties);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(securedDocument), new StreamResult(baos));

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.setActor("anotherTest");
        securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
        securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());

        try {
            Document document = doInboundSecurity(securityProperties, xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())));

            //read the whole stream:
            transformer = TRANSFORMER_FACTORY.newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(
                    new OutputStream() {
                        @Override
                        public void write(int b) throws IOException {
                            // > /dev/null
                        }
                    }
            ));
            Assert.fail("XMLStreamException expected");
        } catch (XMLStreamException e) {
            Assert.assertEquals(e.getMessage(), "org.swssf.wss.ext.WSSecurityException: General security error (Security header is missing)");
        }
    }

    @Test(invocationCount = 1)
    public void testInteroperabilitySOAPRoleInbound() throws Exception {

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.2.xml");

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.ACTOR, "test");
        properties.setProperty(WSHandlerConstants.SIGNATURE_PARTS, "{Element}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{Element}{http://www.w3.org/2003/05/soap-envelope}Body;");
        Document securedDocument = doOutboundSecurityWithWSS4J(sourceDocument, action, properties);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(securedDocument), new StreamResult(baos));

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.setActor("test");
        securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
        securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());

        SecurityEvent.Event[] expectedSecurityEvents = new SecurityEvent.Event[]{
                SecurityEvent.Event.Operation,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.X509Token,
                SecurityEvent.Event.SignatureValue,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.Timestamp,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.SignedElement,
                SecurityEvent.Event.EncryptedPart,
                SecurityEvent.Event.AlgorithmSuite,
                SecurityEvent.Event.AlgorithmSuite,
        };
        final TestSecurityEventListener securityEventListener = new TestSecurityEventListener(expectedSecurityEvents);
        Document document = doInboundSecurity(
                securityProperties, xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())), securityEventListener);

        //read the whole stream:
        transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(
                new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        // > /dev/null
                    }
                }
        ));
        securityEventListener.compare();
    }

    @Test(invocationCount = 1)
    public void testInteroperabilityTwoSecurityHeadersSOAPRoleInbound() throws Exception {

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.2.xml");

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.SIGNATURE_PARTS, "{Element}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{Element}{http://www.w3.org/2003/05/soap-envelope}Body;");
        Document securedDocument = doOutboundSecurityWithWSS4J(sourceDocument, action, properties);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        javax.xml.transform.Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(securedDocument), new StreamResult(baos));

        properties.setProperty(WSHandlerConstants.ACTOR, "test");
        securedDocument = doOutboundSecurityWithWSS4J(new ByteArrayInputStream(baos.toByteArray()), action, properties);

        transformer = TRANSFORMER_FACTORY.newTransformer();
        baos.reset();
        transformer.transform(new DOMSource(securedDocument), new StreamResult(baos));

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.setActor("test");
        securityProperties.loadSignatureVerificationKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());
        securityProperties.loadDecryptionKeystore(this.getClass().getClassLoader().getResource("receiver.jks"), "default".toCharArray());

        Document document = doInboundSecurity(securityProperties, xmlInputFactory.createXMLStreamReader(new ByteArrayInputStream(baos.toByteArray())));

        //read the whole stream:
        transformer = TRANSFORMER_FACTORY.newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(
                new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        // > /dev/null
                    }
                }
        ));
    }

    @Test(invocationCount = 1)
    public void testInteroperabilitySOAPActionOutbound() throws Exception {

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.setActor("test");
        securityProperties.setEncryptionUser("receiver");
        securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        securityProperties.setSignatureUser("transmitter");
        securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.TIMESTAMP, WSSConstants.SIGNATURE, WSSConstants.ENCRYPT};
        securityProperties.setOutAction(actions);

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");
        ByteArrayOutputStream baos = doOutboundSecurity(securityProperties, sourceDocument);

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.ACTOR, "test");
        doInboundSecurityWithWSS4J_1(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action, properties, false);
    }

    @Test(invocationCount = 1)
    public void testInteroperabilityInvalidSOAPActionOutbound() throws Exception {

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.setActor("test");
        securityProperties.setEncryptionUser("receiver");
        securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        securityProperties.setSignatureUser("transmitter");
        securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.TIMESTAMP, WSSConstants.SIGNATURE, WSSConstants.ENCRYPT};
        securityProperties.setOutAction(actions);

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml");
        ByteArrayOutputStream baos = doOutboundSecurity(securityProperties, sourceDocument);

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.ACTOR, "anotherTest");
        try {
            doInboundSecurityWithWSS4J_1(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action, properties, false);
            Assert.fail("Expected WSSecurityException");
        } catch (org.apache.ws.security.WSSecurityException e) {
            Assert.assertEquals(e.getMessage(), "WSS4JHandler: Request does not contain required Security header");
        }
    }

    @Test(invocationCount = 1)
    public void testInteroperabilitySOAPRoleOutbound() throws Exception {

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.setActor("test");
        securityProperties.setEncryptionUser("receiver");
        securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        securityProperties.setSignatureUser("transmitter");
        securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.TIMESTAMP, WSSConstants.SIGNATURE, WSSConstants.ENCRYPT};
        securityProperties.setOutAction(actions);

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.2.xml");

        ByteArrayOutputStream baos = doOutboundSecurity(securityProperties, sourceDocument);

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.ACTOR, "test");
        doInboundSecurityWithWSS4J_1(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action, properties, false);
    }

    @Test(invocationCount = 1)
    public void testInteroperabilityTwoSecurityHeadersSOAPRoleOutbound() throws Exception {

        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        securityProperties.setCallbackHandler(new CallbackHandlerImpl());
        securityProperties.setEncryptionUser("receiver");
        securityProperties.loadEncryptionKeystore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        securityProperties.setSignatureUser("transmitter");
        securityProperties.loadSignatureKeyStore(this.getClass().getClassLoader().getResource("transmitter.jks"), "default".toCharArray());
        WSSConstants.Action[] actions = new WSSConstants.Action[]{WSSConstants.TIMESTAMP, WSSConstants.SIGNATURE, WSSConstants.ENCRYPT};
        securityProperties.setOutAction(actions);

        InputStream sourceDocument = this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.2.xml");

        ByteArrayOutputStream baos = doOutboundSecurity(securityProperties, sourceDocument);

        securityProperties.setActor("test");
        baos = doOutboundSecurity(securityProperties, new ByteArrayInputStream(baos.toByteArray()));

        String action = WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        Properties properties = new Properties();
        properties.setProperty(WSHandlerConstants.ACTOR, "test");
        doInboundSecurityWithWSS4J_1(documentBuilderFactory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray())), action, properties, false);
    }
}

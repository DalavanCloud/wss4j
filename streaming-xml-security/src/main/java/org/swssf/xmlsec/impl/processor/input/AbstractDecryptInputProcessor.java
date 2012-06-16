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
package org.swssf.xmlsec.impl.processor.input;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.swssf.binding.xmldsig.KeyInfoType;
import org.swssf.binding.xmldsig.TransformType;
import org.swssf.binding.xmldsig.TransformsType;
import org.swssf.binding.xmlenc.EncryptedDataType;
import org.swssf.binding.xmlenc.ReferenceList;
import org.swssf.binding.xmlenc.ReferenceType;
import org.swssf.xmlsec.config.JCEAlgorithmMapper;
import org.swssf.xmlsec.config.TransformerAlgorithmMapper;
import org.swssf.xmlsec.ext.*;
import org.swssf.xmlsec.ext.stax.XMLSecEvent;
import org.swssf.xmlsec.ext.stax.XMLSecEventFactory;
import org.swssf.xmlsec.ext.stax.XMLSecNamespace;
import org.swssf.xmlsec.ext.stax.XMLSecStartElement;
import org.swssf.xmlsec.impl.XMLSecurityEventReader;
import org.swssf.xmlsec.impl.securityToken.SecurityTokenFactory;
import org.swssf.xmlsec.impl.util.IDGenerator;
import org.swssf.xmlsec.impl.util.IVSplittingOutputStream;
import org.swssf.xmlsec.impl.util.MultiInputStream;
import org.swssf.xmlsec.impl.util.ReplaceableOuputStream;
import org.xmlsecurity.ns.configuration.AlgorithmType;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;

/**
 * Processor for decryption of EncryptedData XML structures
 *
 * @author $Author$
 * @version $Revision$ $Date$
 */
public abstract class AbstractDecryptInputProcessor extends AbstractInputProcessor {

    private static final transient Log logger = LogFactory.getLog(AbstractDecryptInputProcessor.class);

    private final KeyInfoType keyInfoType;
    private final Map<String, ReferenceType> references;
    private final List<ReferenceType> processedReferences;

    private final String uuid = IDGenerator.generateID(null);
    private final QName wrapperElementName = new QName("http://dummy", "dummy", uuid);

    private final ArrayDeque<XMLSecEvent> tmpXmlEventList = new ArrayDeque<XMLSecEvent>();

    public AbstractDecryptInputProcessor(KeyInfoType keyInfoType, ReferenceList referenceList,
                                         XMLSecurityProperties securityProperties) throws XMLSecurityException {
        super(securityProperties);
        this.keyInfoType = keyInfoType;

        final List<JAXBElement<ReferenceType>> dataReferenceOrKeyReference = referenceList.getDataReferenceOrKeyReference();
        references = new HashMap<String, ReferenceType>(dataReferenceOrKeyReference.size() + 1); //+1 because the HashMap will resize otherwise
        processedReferences = new ArrayList<ReferenceType>(dataReferenceOrKeyReference.size());

        Iterator<JAXBElement<ReferenceType>> referenceTypeIterator = dataReferenceOrKeyReference.iterator();
        while (referenceTypeIterator.hasNext()) {
            ReferenceType referenceType = referenceTypeIterator.next().getValue();
            if (referenceType.getURI() == null) {
                throw new XMLSecurityException(XMLSecurityException.ErrorCode.FAILED_CHECK);
            }
            references.put(XMLSecurityUtils.dropReferenceMarker(referenceType.getURI()), referenceType);
        }
    }

    /*
   <xenc:EncryptedData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#" Id="EncDataId-1612925417" Type="http://www.w3.org/2001/04/xmlenc#Content">
       <xenc:EncryptionMethod xmlns:xenc="http://www.w3.org/2001/04/xmlenc#" Algorithm="http://www.w3.org/2001/04/xmlenc#aes256-cbc" />
       <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
           <wsse:SecurityTokenReference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
               <wsse:Reference xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" URI="#EncKeyId-1483925398" />
           </wsse:SecurityTokenReference>
       </ds:KeyInfo>
       <xenc:CipherData xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
           <xenc:CipherValue xmlns:xenc="http://www.w3.org/2001/04/xmlenc#">
           ...
           </xenc:CipherValue>
       </xenc:CipherData>
   </xenc:EncryptedData>
    */

    @Override
    public XMLSecEvent processNextHeaderEvent(InputProcessorChain inputProcessorChain)
            throws XMLStreamException, XMLSecurityException {
        return processEvent(inputProcessorChain, true);
    }

    @Override
    public XMLSecEvent processNextEvent(InputProcessorChain inputProcessorChain)
            throws XMLStreamException, XMLSecurityException {
        return processEvent(inputProcessorChain, false);
    }

    private XMLSecEvent processEvent(InputProcessorChain inputProcessorChain, boolean isSecurityHeaderEvent)
            throws XMLStreamException, XMLSecurityException {

        if (!tmpXmlEventList.isEmpty()) {
            return tmpXmlEventList.pollLast();
        }

        XMLSecEvent xmlSecEvent = isSecurityHeaderEvent
                ? inputProcessorChain.processHeaderEvent()
                : inputProcessorChain.processEvent();

        boolean encryptedHeader = false;

        if (xmlSecEvent.getEventType() == XMLStreamConstants.START_ELEMENT) {
            XMLSecStartElement xmlSecStartElement = xmlSecEvent.asStartElement();

            //buffer the events until the EncryptedData Element appears and discard it if we found the reference inside it
            //otherwise replay it
            if (xmlSecStartElement.getName().equals(XMLSecurityConstants.TAG_wsse11_EncryptedHeader)) {
                xmlSecEvent = readAndBufferEncryptedHeader(inputProcessorChain, isSecurityHeaderEvent, xmlSecEvent);
                xmlSecStartElement = xmlSecEvent.asStartElement();
                encryptedHeader = true;
            }

            //check if the current start-element has the name EncryptedData and an Id attribute
            if (xmlSecStartElement.getName().equals(XMLSecurityConstants.TAG_xenc_EncryptedData)) {
                ReferenceType referenceType = matchesReferenceId(xmlSecStartElement);
                if (referenceType == null) {
                    //if the events were not for us (no matching reference-id the we have to replay the EncryptedHeader elements)
                    if (!tmpXmlEventList.isEmpty()) {
                        return tmpXmlEventList.pollLast();
                    }
                    return xmlSecEvent;
                }
                //duplicate id's are forbidden
                if (processedReferences.contains(referenceType)) {
                    throw new XMLSecurityException(XMLSecurityException.ErrorCode.FAILED_CHECK, "duplicateId");
                }

                tmpXmlEventList.clear();
                processedReferences.add(referenceType);

                //the following logic reads the encryptedData structure and doesn't pass them further
                //through the chain
                InputProcessorChain subInputProcessorChain = inputProcessorChain.createSubChain(this);

                EncryptedDataType encryptedDataType =
                        parseEncryptedDataStructure(isSecurityHeaderEvent, xmlSecEvent, subInputProcessorChain);

                SecurityToken securityToken = getSecurityToken(inputProcessorChain, encryptedDataType);
                handleSecurityToken(securityToken, inputProcessorChain.getSecurityContext(), encryptedDataType);

                //only fire here ContentEncryptedElementEvents
                //the other ones will be fired later, because we don't know the encrypted element name yet
                if (SecurePart.Modifier.Content.getModifier().equals(encryptedDataType.getType())) {
                    handleEncryptedContent(inputProcessorChain, xmlSecStartElement.getParentXMLSecStartElement(), securityToken);
                }

                final String algorithmURI = encryptedDataType.getEncryptionMethod().getAlgorithm();
                Cipher symCipher = getCipher(algorithmURI);

                //create a new Thread for streaming decryption
                DecryptionThread decryptionThread =
                        new DecryptionThread(subInputProcessorChain, isSecurityHeaderEvent);
                decryptionThread.setSecretKey(securityToken.getSecretKey(algorithmURI, XMLSecurityConstants.Enc));
                decryptionThread.setSymmetricCipher(symCipher);
                XMLSecStartElement parentXMLSecStartElement = xmlSecStartElement.getParentXMLSecStartElement();
                if (encryptedHeader) {
                    parentXMLSecStartElement = parentXMLSecStartElement.getParentXMLSecStartElement();
                }
                AbstractDecryptedEventReaderInputProcessor decryptedEventReaderInputProcessor =
                        newDecryptedEventReaderInputProccessor(
                                encryptedHeader, parentXMLSecStartElement, encryptedDataType, securityToken,
                                inputProcessorChain.getSecurityContext()
                        );

                //add the new created EventReader processor to the chain.
                inputProcessorChain.addProcessor(decryptedEventReaderInputProcessor);

                inputProcessorChain.getDocumentContext().setIsInEncryptedContent(
                        inputProcessorChain.getProcessors().indexOf(decryptedEventReaderInputProcessor),
                        decryptedEventReaderInputProcessor);

                Thread thread = new Thread(decryptionThread);
                thread.setName("decrypting thread");
                //when an exception in the decryption thread occurs, we want to forward them:
                thread.setUncaughtExceptionHandler(decryptedEventReaderInputProcessor);

                //we have to start the thread before we call decryptionThread.getPipedInputStream().
                //Otherwise we will end in a deadlock, because the StAX reader expects already data.
                //@See some lines below:
                logger.debug("Starting decryption thread");
                thread.start();

                InputStream prologInputStream;
                InputStream epilogInputStream;
                try {
                    prologInputStream = writeWrapperStartElement(xmlSecStartElement);
                    epilogInputStream = writeWrapperEndElement();
                } catch (UnsupportedEncodingException e) {
                    throw new XMLSecurityException(XMLSecurityException.ErrorCode.FAILURE, e);
                } catch (IOException e) {
                    throw new XMLSecurityException(XMLSecurityException.ErrorCode.FAILURE, e);
                }

                InputStream decryptInputStream = decryptionThread.getPipedInputStream();

                TransformsType transformsType =
                        XMLSecurityUtils.getQNameType(referenceType.getAny(), XMLSecurityConstants.TAG_dsig_Transforms);
                if (transformsType != null) {
                    List<TransformType> transformTypes = transformsType.getTransform();
                    if (transformTypes.size() > 1) {
                        throw new XMLSecurityException(XMLSecurityException.ErrorCode.INVALID_SECURITY);
                    }
                    TransformType transformType = transformTypes.get(0);
                    @SuppressWarnings("unchecked")
                    Class<InputStream> transformerClass =
                            (Class<InputStream>) TransformerAlgorithmMapper.getTransformerClass(
                                    transformType.getAlgorithm(), "IN");
                    try {
                        Constructor<InputStream> constructor = transformerClass.getConstructor(InputStream.class);
                        decryptInputStream = constructor.newInstance(decryptInputStream);
                    } catch (InvocationTargetException e) {
                        throw new XMLSecurityException(XMLSecurityException.ErrorCode.FAILURE, e);
                    } catch (NoSuchMethodException e) {
                        throw new XMLSecurityException(XMLSecurityException.ErrorCode.FAILURE, e);
                    } catch (InstantiationException e) {
                        throw new XMLSecurityException(XMLSecurityException.ErrorCode.FAILURE, e);
                    } catch (IllegalAccessException e) {
                        throw new XMLSecurityException(XMLSecurityException.ErrorCode.FAILURE, e);
                    }
                }

                //spec says (4.2): "The cleartext octet sequence obtained in step 3 is
                //interpreted as UTF-8 encoded character data."
                XMLStreamReader xmlStreamReader =
                        inputProcessorChain.getSecurityContext().<XMLInputFactory>get(
                                XMLSecurityConstants.XMLINPUTFACTORY).createXMLStreamReader(
                                new MultiInputStream(prologInputStream, decryptInputStream, epilogInputStream), "UTF-8");

                //forward to wrapper element
                forwardToWrapperElement(xmlStreamReader);

                decryptedEventReaderInputProcessor.setXmlStreamReader(xmlStreamReader);

                if (isSecurityHeaderEvent) {
                    return decryptedEventReaderInputProcessor.processNextHeaderEvent(inputProcessorChain);
                } else {
                    return decryptedEventReaderInputProcessor.processNextEvent(inputProcessorChain);
                }
            }
        }
        return xmlSecEvent;
    }

    private InputStream writeWrapperStartElement(XMLSecStartElement xmlSecStartElement) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //temporary writer to write the dummy wrapper element with all namespaces in the current scope
        //spec says (4.2): "The cleartext octet sequence obtained in step 3 is interpreted as UTF-8 encoded character data."
        Writer writer = new OutputStreamWriter(byteArrayOutputStream, "UTF-8");

        writer.write('<');
        writer.write(wrapperElementName.getPrefix());
        writer.write(':');
        writer.write(wrapperElementName.getLocalPart());
        writer.write(' ');
        writer.write("xmlns:");
        writer.write(wrapperElementName.getPrefix());
        writer.write("=\"");
        writer.write(wrapperElementName.getNamespaceURI());
        writer.write('\"');

        //apply all namespaces from current scope to get a valid documentfragment:
        List<XMLSecNamespace> comparableNamespacesToApply = new ArrayList<XMLSecNamespace>();
        List<XMLSecNamespace> comparableNamespaceList = new ArrayList<XMLSecNamespace>();
        xmlSecStartElement.getNamespacesFromCurrentScope(comparableNamespaceList);
        for (int i = 0; i < comparableNamespaceList.size(); i++) {
            XMLSecNamespace comparableNamespace = comparableNamespaceList.get(i);
            if (!comparableNamespacesToApply.contains(comparableNamespace)) {
                comparableNamespacesToApply.add(comparableNamespace);
                writer.write(' ');

                String prefix = comparableNamespace.getPrefix();
                String uri = comparableNamespace.getNamespaceURI();
                if (prefix == null || prefix.isEmpty()) {
                    writer.write("xmlns=\"");
                    writer.write(uri);
                    writer.write("\"");
                } else {
                    writer.write("xmlns:");
                    writer.write(prefix);
                    writer.write("=\"");
                    writer.write(uri);
                    writer.write("\"");
                }
            }
        }

        writer.write('>');
        writer.close();
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    private InputStream writeWrapperEndElement() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(byteArrayOutputStream, "UTF-8");

        //close the dummy wrapper element:
        writer.write("</");
        writer.write(wrapperElementName.getPrefix());
        writer.write(':');
        writer.write(wrapperElementName.getLocalPart());
        writer.write('>');
        writer.close();
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    private void forwardToWrapperElement(XMLStreamReader xmlStreamReader) throws XMLStreamException {
        do {
            if (xmlStreamReader.getEventType() == XMLStreamConstants.START_ELEMENT
                    && xmlStreamReader.getName().equals(wrapperElementName)) {
                xmlStreamReader.next();
                break;
            }
            xmlStreamReader.next();
        } while (xmlStreamReader.hasNext());
    }

    private Cipher getCipher(String algorithmURI) throws XMLSecurityException {
        Cipher symCipher;
        try {
            AlgorithmType symEncAlgo = JCEAlgorithmMapper.getAlgorithmMapping(algorithmURI);
            if (symEncAlgo.getJCEProvider() != null) {
                symCipher = Cipher.getInstance(symEncAlgo.getJCEName(), symEncAlgo.getJCEProvider());
            } else {
                symCipher = Cipher.getInstance(symEncAlgo.getJCEName());
            }
            //we have to defer the initialization of the cipher until we can extract the IV...
        } catch (NoSuchAlgorithmException e) {
            throw new XMLSecurityException(
                    XMLSecurityException.ErrorCode.UNSUPPORTED_ALGORITHM, "unsupportedKeyTransp",
                    e, "No such algorithm: " + algorithmURI
            );
        } catch (NoSuchPaddingException e) {
            throw new XMLSecurityException(
                    XMLSecurityException.ErrorCode.UNSUPPORTED_ALGORITHM, "unsupportedKeyTransp",
                    e, "No such padding: " + algorithmURI
            );
        } catch (NoSuchProviderException e) {
            throw new XMLSecurityException(XMLSecurityException.ErrorCode.FAILURE, "noSecProvider", e);
        }
        return symCipher;
    }

    private SecurityToken getSecurityToken(InputProcessorChain inputProcessorChain,
                                           EncryptedDataType encryptedDataType) throws XMLSecurityException {
        KeyInfoType keyInfoType;
        if (this.keyInfoType != null) {
            keyInfoType = this.keyInfoType;
        } else {
            keyInfoType = encryptedDataType.getKeyInfo();
        }

        //retrieve the securityToken which must be used for decryption
        return SecurityTokenFactory.getInstance().getSecurityToken(
                keyInfoType, getSecurityProperties().getDecryptionCrypto(),
                getSecurityProperties().getCallbackHandler(), inputProcessorChain.getSecurityContext());
    }

    private EncryptedDataType parseEncryptedDataStructure(
            boolean isSecurityHeaderEvent, XMLSecEvent xmlSecEvent, InputProcessorChain subInputProcessorChain)
            throws XMLStreamException, XMLSecurityException {

        Deque<XMLSecEvent> xmlSecEvents = new ArrayDeque<XMLSecEvent>();
        xmlSecEvents.push(xmlSecEvent);
        XMLSecEvent encryptedDataXMLSecEvent;
        int count = 0;
        do {
            subInputProcessorChain.reset();
            if (isSecurityHeaderEvent) {
                encryptedDataXMLSecEvent = subInputProcessorChain.processHeaderEvent();
            } else {
                encryptedDataXMLSecEvent = subInputProcessorChain.processEvent();
            }

            xmlSecEvents.push(encryptedDataXMLSecEvent);
            if (++count >= 50) {
                throw new XMLSecurityException(XMLSecurityException.ErrorCode.INVALID_SECURITY);
            }
        }
        while (!(encryptedDataXMLSecEvent.getEventType() == XMLStreamConstants.START_ELEMENT
                && encryptedDataXMLSecEvent.asStartElement().getName().equals(XMLSecurityConstants.TAG_xenc_CipherValue)));

        xmlSecEvents.push(XMLSecEventFactory.createXmlSecEndElement(XMLSecurityConstants.TAG_xenc_CipherValue));
        xmlSecEvents.push(XMLSecEventFactory.createXmlSecEndElement(XMLSecurityConstants.TAG_xenc_CipherData));
        xmlSecEvents.push(XMLSecEventFactory.createXmlSecEndElement(XMLSecurityConstants.TAG_xenc_EncryptedData));

        EncryptedDataType encryptedDataType;

        try {
            Unmarshaller unmarshaller =
                    XMLSecurityConstants.getJaxbUnmarshaller(getSecurityProperties().isDisableSchemaValidation());
            @SuppressWarnings("unchecked")
            JAXBElement<EncryptedDataType> encryptedDataTypeJAXBElement =
                    (JAXBElement<EncryptedDataType>) unmarshaller.unmarshal(new XMLSecurityEventReader(xmlSecEvents, 0));
            encryptedDataType = encryptedDataTypeJAXBElement.getValue();

        } catch (JAXBException e) {
            throw new XMLSecurityException(XMLSecurityException.ErrorCode.INVALID_SECURITY, e);
        }
        return encryptedDataType;
    }

    private XMLSecEvent readAndBufferEncryptedHeader(InputProcessorChain inputProcessorChain, boolean isSecurityHeaderEvent,
                                                     XMLSecEvent xmlSecEvent) throws XMLStreamException, XMLSecurityException {
        InputProcessorChain subInputProcessorChain = inputProcessorChain.createSubChain(this);
        do {
            tmpXmlEventList.push(xmlSecEvent);

            subInputProcessorChain.reset();
            if (isSecurityHeaderEvent) {
                xmlSecEvent = subInputProcessorChain.processHeaderEvent();
            } else {
                xmlSecEvent = subInputProcessorChain.processEvent();
            }
        }
        while (!(xmlSecEvent.getEventType() == XMLStreamConstants.START_ELEMENT
                && xmlSecEvent.asStartElement().getName().equals(XMLSecurityConstants.TAG_xenc_EncryptedData)));

        tmpXmlEventList.push(xmlSecEvent);
        return xmlSecEvent;
    }

    protected abstract AbstractDecryptedEventReaderInputProcessor newDecryptedEventReaderInputProccessor(
            boolean encryptedHeader, XMLSecStartElement xmlSecStartElement, EncryptedDataType currentEncryptedDataType,
            SecurityToken securityToken, SecurityContext securityContext) throws XMLSecurityException;

    protected abstract void handleSecurityToken(SecurityToken securityToken, SecurityContext securityContext,
                                                EncryptedDataType encryptedDataType) throws XMLSecurityException;

    protected abstract void handleEncryptedContent(InputProcessorChain inputProcessorChain,
                                                   XMLSecStartElement parentXMLSecStartElement,
                                                   SecurityToken securityToken) throws XMLSecurityException;

    protected ReferenceType matchesReferenceId(XMLSecStartElement xmlSecStartElement) {
        Attribute refId = getReferenceIDAttribute(xmlSecStartElement);
        if (refId != null) {
            //does the id exist in the referenceList?
            return this.references.get(refId.getValue());
        }
        return null;
    }

    @Override
    public void doFinal(InputProcessorChain inputProcessorChain) throws XMLStreamException, XMLSecurityException {
        //here we check if all references where processed.
        Iterator<Map.Entry<String, ReferenceType>> refEntryIterator = this.references.entrySet().iterator();
        while (refEntryIterator.hasNext()) {
            Map.Entry<String, ReferenceType> referenceTypeEntry = refEntryIterator.next();
            if (!processedReferences.contains(referenceTypeEntry.getValue())) {
                throw new XMLSecurityException(XMLSecurityException.ErrorCode.FAILED_CHECK, "unprocessedEncryptionReferences");
            }
        }
        inputProcessorChain.doFinal();
    }

    /**
     * The DecryptedEventReaderInputProcessor reads the decrypted stream with a StAX reader and
     * forwards the generated XMLEvents
     */
    public abstract class AbstractDecryptedEventReaderInputProcessor
            extends AbstractInputProcessor implements Thread.UncaughtExceptionHandler {

        private XMLStreamReader xmlStreamReader;
        private XMLSecStartElement parentXmlSecStartElement;
        private boolean encryptedHeader = false;
        private final SecurityToken securityToken;
        private boolean rootElementProcessed;

        public AbstractDecryptedEventReaderInputProcessor(
                XMLSecurityProperties securityProperties, SecurePart.Modifier encryptionModifier,
                boolean encryptedHeader, XMLSecStartElement xmlSecStartElement,
                AbstractDecryptInputProcessor abstractDecryptInputProcessor,
                SecurityToken securityToken
        ) {
            super(securityProperties);
            addAfterProcessor(abstractDecryptInputProcessor);
            this.rootElementProcessed = encryptionModifier != SecurePart.Modifier.Element;
            this.encryptedHeader = encryptedHeader;
            this.securityToken = securityToken;
            this.parentXmlSecStartElement = xmlSecStartElement;
        }

        public void setXmlStreamReader(XMLStreamReader xmlStreamReader) {
            this.xmlStreamReader = xmlStreamReader;
        }

        @Override
        public XMLSecEvent processNextHeaderEvent(InputProcessorChain inputProcessorChain)
                throws XMLStreamException, XMLSecurityException {
            return processEvent(inputProcessorChain, true);
        }

        @Override
        public XMLSecEvent processNextEvent(InputProcessorChain inputProcessorChain)
                throws XMLStreamException, XMLSecurityException {
            return processEvent(inputProcessorChain, false);
        }

        private XMLSecEvent processEvent(InputProcessorChain inputProcessorChain, boolean headerEvent)
                throws XMLStreamException, XMLSecurityException {
            //did a execption occur during decryption in the decryption thread?
            testAndThrowUncaughtException();

            XMLSecEvent xmlSecEvent = XMLSecEventFactory.allocate(xmlStreamReader, parentXmlSecStartElement);
            //here we request the next XMLEvent from the decryption thread
            //instead from the processor-chain as we normally would do
            switch (xmlSecEvent.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    parentXmlSecStartElement = xmlSecEvent.asStartElement();

                    if (!rootElementProcessed) {
                        handleEncryptedElement(inputProcessorChain, parentXmlSecStartElement, this.securityToken);
                        rootElementProcessed = true;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (parentXmlSecStartElement != null) {
                        parentXmlSecStartElement = parentXmlSecStartElement.getParentXMLSecStartElement();
                    }

                    if (xmlSecEvent.asEndElement().getName().equals(wrapperElementName)) {
                        InputProcessorChain subInputProcessorChain = inputProcessorChain.createSubChain(this);

                        //skip EncryptedHeader Element when we processed it.
                        QName endElement;
                        if (encryptedHeader) {
                            endElement = XMLSecurityConstants.TAG_wsse11_EncryptedHeader;
                        } else {
                            endElement = XMLSecurityConstants.TAG_xenc_EncryptedData;
                        }

                        //read and discard XMLEvents until the EncryptedData structure
                        XMLSecEvent endEvent;
                        do {
                            subInputProcessorChain.reset();
                            if (headerEvent) {
                                endEvent = subInputProcessorChain.processHeaderEvent();
                            } else {
                                endEvent = subInputProcessorChain.processEvent();
                            }
                        }
                        while (!(endEvent.getEventType() == XMLStreamConstants.END_ELEMENT
                                && endEvent.asEndElement().getName().equals(endElement)));

                        inputProcessorChain.getDocumentContext().unsetIsInEncryptedContent(this);

                        //...fetch the next (unencrypted) event
                        if (headerEvent) {
                            xmlSecEvent = inputProcessorChain.processHeaderEvent();
                        } else {
                            xmlSecEvent = inputProcessorChain.processEvent();
                        }

                        inputProcessorChain.removeProcessor(this);
                    }
                    break;
            }
            xmlStreamReader.next();
            return xmlSecEvent;
        }

        protected abstract void handleEncryptedElement(
                InputProcessorChain inputProcessorChain,
                XMLSecStartElement xmlSecStartElement, SecurityToken securityToken) throws XMLSecurityException;

        private volatile Throwable thrownException;

        public void uncaughtException(Thread t, Throwable e) {
            this.thrownException = e;
        }

        private void testAndThrowUncaughtException() throws XMLStreamException {
            if (this.thrownException != null) {
                if (this.thrownException instanceof UncheckedXMLSecurityException) {
                    UncheckedXMLSecurityException uxse = (UncheckedXMLSecurityException) this.thrownException;
                    throw new XMLStreamException(uxse.getCause());
                } else {
                    throw new XMLStreamException(this.thrownException.getCause());
                }
            }
        }
    }

    /**
     * The DecryptionThread handles encrypted XML-Parts
     */
    class DecryptionThread implements Runnable {

        private final InputProcessorChain inputProcessorChain;
        private final boolean header;
        private final PipedOutputStream pipedOutputStream;
        private final PipedInputStream pipedInputStream;
        private Cipher symmetricCipher;
        private Key secretKey;

        protected DecryptionThread(InputProcessorChain inputProcessorChain,
                                   boolean header) throws XMLStreamException, XMLSecurityException {

            this.inputProcessorChain = inputProcessorChain;
            this.header = header;

            //prepare the piped streams and connect them:
            this.pipedInputStream = new PipedInputStream(8192 * 5);
            try {
                this.pipedOutputStream = new PipedOutputStream(pipedInputStream);
            } catch (IOException e) {
                throw new XMLStreamException(e);
            }
        }

        public PipedInputStream getPipedInputStream() {
            return pipedInputStream;
        }

        private XMLSecEvent processNextEvent() throws XMLSecurityException, XMLStreamException {
            inputProcessorChain.reset();
            if (header) {
                return inputProcessorChain.processHeaderEvent();
            } else {
                return inputProcessorChain.processEvent();
            }
        }

        public void run() {

            try {
                IVSplittingOutputStream ivSplittingOutputStream = new IVSplittingOutputStream(
                        new CipherOutputStream(pipedOutputStream, getSymmetricCipher()),
                        getSymmetricCipher(), getSecretKey());
                //buffering seems not to help
                //bufferedOutputStream = new BufferedOutputStream(new Base64OutputStream(ivSplittingOutputStream, false), 8192 * 5);
                ReplaceableOuputStream replaceableOuputStream = new ReplaceableOuputStream(ivSplittingOutputStream);
                OutputStream base64OutputStream = new Base64OutputStream(replaceableOuputStream, false);
                ivSplittingOutputStream.setParentOutputStream(replaceableOuputStream);
                OutputStreamWriter outputStreamWriter =
                        new OutputStreamWriter(base64OutputStream, inputProcessorChain.getDocumentContext().getEncoding());

                //read the encrypted data from the stream until an end-element occurs and write then
                //to the decrypter-stream
                exitLoop:
                while (true) {
                    XMLSecEvent xmlSecEvent = processNextEvent();

                    switch (xmlSecEvent.getEventType()) {
                        case XMLStreamConstants.END_ELEMENT:
                            //this must be the CipherValue EndElement.                            
                            break exitLoop;
                        case XMLStreamConstants.CHARACTERS:
                            final String data = xmlSecEvent.asCharacters().getData();
                            outputStreamWriter.write(data, 0, data.length());
                            break;
                        default:
                            throw new XMLSecurityException(XMLSecurityException.ErrorCode.FAILED_CHECK,
                                    "unexpectedXMLEvent", XMLSecurityUtils.getXMLEventAsString(xmlSecEvent));
                    }
                }

                //close to get Cipher.doFinal() called
                outputStreamWriter.close();
                logger.debug("Decryption thread finished");

            } catch (Exception e) {
                try {
                    //we have to close the pipe when an exception occurs. Otherwise we can run into a deadlock when an exception occurs
                    //before we have written any byte to the pipe.
                    this.pipedOutputStream.close();
                } catch (IOException e1) {
                    //ignore since we will throw the original exception below
                }
                throw new UncheckedXMLSecurityException(e);
            }
        }

        protected Cipher getSymmetricCipher() {
            return symmetricCipher;
        }

        protected void setSymmetricCipher(Cipher symmetricCipher) {
            this.symmetricCipher = symmetricCipher;
        }

        protected Key getSecretKey() {
            return secretKey;
        }

        protected void setSecretKey(Key secretKey) {
            this.secretKey = secretKey;
        }
    }
}

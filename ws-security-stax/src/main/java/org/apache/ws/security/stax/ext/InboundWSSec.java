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
package org.apache.ws.security.stax.ext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.stax.impl.InboundWSSecurityContextImpl;
import org.apache.ws.security.stax.impl.processor.input.OperationInputProcessor;
import org.apache.ws.security.stax.impl.processor.input.SecurityHeaderInputProcessor;
import org.apache.ws.security.stax.impl.processor.input.SignatureConfirmationInputProcessor;
import org.apache.ws.security.stax.securityEvent.WSSecurityEventConstants;
import org.apache.xml.security.stax.ext.InputProcessor;
import org.apache.xml.security.stax.ext.XMLSecurityException;
import org.apache.xml.security.stax.impl.DocumentContextImpl;
import org.apache.xml.security.stax.impl.InputProcessorChainImpl;
import org.apache.xml.security.stax.impl.XMLSecurityStreamReader;
import org.apache.xml.security.stax.impl.processor.input.LogInputProcessor;
import org.apache.xml.security.stax.impl.processor.input.XMLEventReaderInputProcessor;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventListener;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Inbound Streaming-WebService-Security
 * An instance of this class can be retrieved over the WSSec class
 *
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class InboundWSSec {

    protected static final transient Log log = LogFactory.getLog(InboundWSSec.class);

    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    static {
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try {
            xmlInputFactory.setProperty("org.codehaus.stax2.internNames", true);
            xmlInputFactory.setProperty("org.codehaus.stax2.internNsUris", true);
            xmlInputFactory.setProperty("org.codehaus.stax2.preserveLocation", false);
        } catch (IllegalArgumentException e) {
            log.debug(e.getMessage(), e);
            //ignore
        }
    }

    private final WSSSecurityProperties securityProperties;

    public InboundWSSec(WSSSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * Warning:
     * configure your xmlStreamReader correctly. Otherwise you can create a security hole.
     * At minimum configure the following properties:
     * xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
     * xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
     * xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, false);
     * xmlInputFactory.setProperty(WstxInputProperties.P_MIN_TEXT_SEGMENT, new Integer(8192));
     * <p/>
     * This method is the entry point for the incoming security-engine.
     * Hand over the original XMLStreamReader and use the returned one for further processing
     *
     * @param xmlStreamReader The original XMLStreamReader
     * @return A new XMLStreamReader which does transparently the security processing.
     * @throws XMLStreamException  thrown when a streaming error occurs
     * @throws XMLSecurityException 
     */
    public XMLStreamReader processInMessage(XMLStreamReader xmlStreamReader) throws XMLStreamException, XMLSecurityException {
        return this.processInMessage(xmlStreamReader, null, null);
    }

    /**
     * Warning:
     * configure your xmlStreamReader correctly. Otherwise you can create a security hole.
     * At minimum configure the following properties:
     * xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
     * xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
     * xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, false);
     * xmlInputFactory.setProperty(WstxInputProperties.P_MIN_TEXT_SEGMENT, new Integer(8192));
     * <p/>
     * This method is the entry point for the incoming security-engine.
     * Hand over the original XMLStreamReader and use the returned one for further processing
     *
     * @param xmlStreamReader       The original XMLStreamReader
     * @param securityEventListener A SecurityEventListener to receive security-relevant events.
     * @return A new XMLStreamReader which does transparently the security processing.
     * @throws XMLStreamException  thrown when a streaming error occurs
     * @throws XMLSecurityException 
     */
    public XMLStreamReader processInMessage(XMLStreamReader xmlStreamReader, List<SecurityEvent> requestSecurityEvents, SecurityEventListener securityEventListener) throws XMLStreamException, XMLSecurityException {

        if (requestSecurityEvents == null) {
            requestSecurityEvents = Collections.emptyList();
        }

        final InboundWSSecurityContextImpl securityContextImpl = new InboundWSSecurityContextImpl();
        securityContextImpl.putList(SecurityEvent.class, requestSecurityEvents);
        securityContextImpl.addSecurityEventListener(securityEventListener);
        securityContextImpl.ignoredBSPRules(this.securityProperties.getIgnoredBSPRules());

        if (!requestSecurityEvents.isEmpty()) {
            Iterator<SecurityEvent> securityEventIterator = requestSecurityEvents.iterator();
            while (securityEventIterator.hasNext()) {
                SecurityEvent securityEvent = securityEventIterator.next();
                if (securityEvent.getSecurityEventType() == WSSecurityEventConstants.HttpsToken) {
                    securityContextImpl.registerSecurityEvent(securityEvent);
                    securityContextImpl.put(WSSConstants.TRANSPORT_SECURITY_ACTIVE, Boolean.TRUE);
                    break;
                }
            }
        }

        securityContextImpl.put(WSSConstants.XMLINPUTFACTORY, xmlInputFactory);

        DocumentContextImpl documentContext = new DocumentContextImpl();
        documentContext.setEncoding(xmlStreamReader.getEncoding() != null ? xmlStreamReader.getEncoding() : "UTF-8");
        InputProcessorChainImpl inputProcessorChain = new InputProcessorChainImpl(securityContextImpl, documentContext);
        inputProcessorChain.addProcessor(new XMLEventReaderInputProcessor(securityProperties, xmlStreamReader));
        inputProcessorChain.addProcessor(new SecurityHeaderInputProcessor(securityProperties));
        inputProcessorChain.addProcessor(new OperationInputProcessor(securityProperties));

        if (securityProperties.isEnableSignatureConfirmationVerification()) {
            inputProcessorChain.addProcessor(new SignatureConfirmationInputProcessor(securityProperties));
        }

        if (log.isTraceEnabled()) {
            LogInputProcessor logInputProcessor = new LogInputProcessor(securityProperties);
            logInputProcessor.addAfterProcessor(SecurityHeaderInputProcessor.class.getName());
            inputProcessorChain.addProcessor(logInputProcessor);
        }

        List<InputProcessor> additionalInputProcessors = securityProperties.getInputProcessorList();
        if (!additionalInputProcessors.isEmpty()) {
            Iterator<InputProcessor> inputProcessorIterator = additionalInputProcessors.iterator();
            while (inputProcessorIterator.hasNext()) {
                InputProcessor inputProcessor = inputProcessorIterator.next();
                inputProcessorChain.addProcessor(inputProcessor);
            }
        }

        return new XMLSecurityStreamReader(inputProcessorChain, securityProperties);
    }
}

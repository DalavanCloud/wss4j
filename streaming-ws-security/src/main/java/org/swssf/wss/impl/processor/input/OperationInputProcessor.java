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

import org.swssf.wss.ext.WSSConstants;
import org.swssf.wss.ext.WSSDocumentContext;
import org.swssf.wss.ext.WSSSecurityProperties;
import org.swssf.wss.ext.WSSecurityContext;
import org.swssf.wss.securityEvent.OperationSecurityEvent;
import org.swssf.xmlsec.ext.AbstractInputProcessor;
import org.swssf.xmlsec.ext.InputProcessorChain;
import org.swssf.xmlsec.ext.XMLSecurityException;
import org.swssf.xmlsec.ext.XMLSecurityProperties;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Processor whiich emits the Operation-Security-Event
 *
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class OperationInputProcessor extends AbstractInputProcessor {

    public OperationInputProcessor(XMLSecurityProperties securityProperties) {
        super(securityProperties);
        this.setPhase(WSSConstants.Phase.POSTPROCESSING);
        this.getBeforeProcessors().add(SecurityHeaderInputProcessor.class.getName());
    }

    @Override
    public XMLEvent processNextHeaderEvent(InputProcessorChain inputProcessorChain) throws XMLStreamException, XMLSecurityException {
        return inputProcessorChain.processHeaderEvent();
    }

    @Override
    public XMLEvent processNextEvent(InputProcessorChain inputProcessorChain) throws XMLStreamException, XMLSecurityException {
        XMLEvent xmlEvent = inputProcessorChain.processEvent();
        if (xmlEvent.isStartElement()) {
            if (inputProcessorChain.getDocumentContext().getDocumentLevel() == 3 && ((WSSDocumentContext) inputProcessorChain.getDocumentContext()).isInSOAPBody()) {
                OperationSecurityEvent operationSecurityEvent = new OperationSecurityEvent();
                operationSecurityEvent.setWsSecurityContext((WSSecurityContext) inputProcessorChain.getSecurityContext());
                operationSecurityEvent.setWssSecurityProperties((WSSSecurityProperties) getSecurityProperties());
                operationSecurityEvent.setOperation(xmlEvent.asStartElement().getName());
                ((WSSecurityContext) inputProcessorChain.getSecurityContext()).registerSecurityEvent(operationSecurityEvent);
                inputProcessorChain.removeProcessor(this);
            }
        }
        return xmlEvent;
    }
}

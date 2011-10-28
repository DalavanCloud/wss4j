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

import org.swssf.binding.xmlenc.ReferenceList;
import org.swssf.wss.ext.WSSSecurityProperties;
import org.swssf.xmlsec.ext.AbstractInputSecurityHeaderHandler;
import org.swssf.xmlsec.ext.InputProcessorChain;
import org.swssf.xmlsec.ext.XMLSecurityException;

import javax.xml.stream.events.XMLEvent;
import java.util.Deque;

/**
 * Processor for the ReferenceList XML Structure
 *
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class ReferenceListInputHandler extends AbstractInputSecurityHeaderHandler {

    public ReferenceListInputHandler(InputProcessorChain inputProcessorChain,
                                     final WSSSecurityProperties securityProperties,
                                     Deque<XMLEvent> eventQueue, Integer index) throws XMLSecurityException {

        final ReferenceList referenceList = (ReferenceList) parseStructure(eventQueue, index);

        //instantiate a new DecryptInputProcessor and add it to the chain
        inputProcessorChain.addProcessor(new org.swssf.wss.impl.processor.input.DecryptInputProcessor(referenceList, securityProperties));
    }
}

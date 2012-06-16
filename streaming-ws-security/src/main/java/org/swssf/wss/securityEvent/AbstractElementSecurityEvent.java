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
package org.swssf.wss.securityEvent;

import org.swssf.xmlsec.ext.stax.XMLSecEvent;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author $Author: giger $
 * @version $Revision: 1296293 $ $Date: 2012-03-02 17:33:09 +0100 (Fri, 02 Mar 2012) $
 */
public abstract class AbstractElementSecurityEvent extends SecurityEvent {

    private List<QName> elementPath;
    private XMLSecEvent xmlSecEvent;

    public AbstractElementSecurityEvent(Event securityEventType) {
        super(securityEventType);
    }

    public List<QName> getElementPath() {
        return elementPath;
    }

    public void setElementPath(List<QName> elementPath) {
        this.elementPath = new ArrayList<QName>(elementPath);
    }

    public XMLSecEvent getXmlSecEvent() {
        return xmlSecEvent;
    }

    public void setXmlSecEvent(XMLSecEvent xmlSecEvent) {
        this.xmlSecEvent = xmlSecEvent;
    }
}

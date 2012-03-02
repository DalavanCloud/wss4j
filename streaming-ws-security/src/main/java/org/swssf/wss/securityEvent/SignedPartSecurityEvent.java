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

import org.swssf.xmlsec.ext.SecurityToken;
import org.swssf.xmlsec.ext.XMLSecurityConstants;

import javax.xml.namespace.QName;
import java.util.LinkedList;
import java.util.List;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class SignedPartSecurityEvent extends SecurityEvent {

    private List<QName> elementPath;
    private boolean signed;
    private SecurityToken securityToken;
    private List<XMLSecurityConstants.ContentType> protectionOrder;

    public SignedPartSecurityEvent(SecurityToken securityToken, boolean signed, List<XMLSecurityConstants.ContentType> protectionOrder) {
        super(Event.SignedPart);
        this.signed = signed;
        this.securityToken = securityToken;
        this.protectionOrder = protectionOrder;
    }

    public List<QName> getElementPath() {
        return elementPath;
    }

    public void setElementPath(List<QName> elementPath) {
        this.elementPath = new LinkedList<QName>(elementPath);
    }

    public List<XMLSecurityConstants.ContentType> getProtectionOrder() {
        return protectionOrder;
    }

    public void setProtectionOrder(List<XMLSecurityConstants.ContentType> protectionOrder) {
        this.protectionOrder = protectionOrder;
    }

    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public SecurityToken getSecurityToken() {
        return securityToken;
    }

    public void setSecurityToken(SecurityToken securityToken) {
        this.securityToken = securityToken;
    }
}

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

import javax.xml.namespace.QName;
import java.util.LinkedList;
import java.util.List;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class ContentEncryptedElementSecurityEvent extends SecurityEvent {

    private List<QName> pathElements; //parent element
    private boolean encrypted;
    private SecurityToken securityToken;
    private boolean signedContent;

    public ContentEncryptedElementSecurityEvent(SecurityToken securityToken, boolean encrypted, boolean signedContent) {
        super(Event.ContentEncrypted);
        this.securityToken = securityToken;
        this.encrypted = encrypted;
        this.signedContent = signedContent;
    }

    public List<QName> getElementPath() {
        return pathElements;
    }

    public void setElementPath(List<QName> elementPath) {
        this.pathElements = new LinkedList<QName>(elementPath);
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public SecurityToken getSecurityToken() {
        return securityToken;
    }

    public void setSecurityToken(SecurityToken securityToken) {
        this.securityToken = securityToken;
    }

    public boolean isSignedContent() {
        return signedContent;
    }

    public void setSignedContent(boolean signedContent) {
        this.signedContent = signedContent;
    }
}

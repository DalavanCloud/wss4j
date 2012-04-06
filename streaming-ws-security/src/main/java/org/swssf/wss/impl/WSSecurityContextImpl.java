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
package org.swssf.wss.impl;

import org.swssf.wss.ext.WSSConstants;
import org.swssf.wss.ext.WSSecurityContext;
import org.swssf.wss.ext.WSSecurityException;
import org.swssf.wss.securityEvent.SecurityEvent;
import org.swssf.wss.securityEvent.SecurityEventListener;
import org.swssf.xmlsec.impl.SecurityContextImpl;

import java.util.LinkedList;
import java.util.List;

/**
 * Concrete security context implementation
 *
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class WSSecurityContextImpl extends SecurityContextImpl implements WSSecurityContext {

    private List<SecurityEventListener> securityEventListeners = new LinkedList<SecurityEventListener>();

    public void addSecurityEventListener(SecurityEventListener securityEventListener) {
        if (securityEventListener != null) {
            this.securityEventListeners.add(securityEventListener);
        }
    }

    public synchronized void registerSecurityEvent(SecurityEvent securityEvent) throws WSSecurityException {
        forwardSecurityEvent(securityEvent);
    }

    protected void forwardSecurityEvent(SecurityEvent securityEvent) throws WSSecurityException {
        for (int i = 0; i < securityEventListeners.size(); i++) {
            SecurityEventListener securityEventListener = securityEventListeners.get(i);
            securityEventListener.registerSecurityEvent(securityEvent);
        }
    }

    public void handleBSPRule(WSSConstants.BSPRule bspRule) throws WSSecurityException {
    }

    public void ignoredBSPRules(List<WSSConstants.BSPRule> bspRules) {
    }
}

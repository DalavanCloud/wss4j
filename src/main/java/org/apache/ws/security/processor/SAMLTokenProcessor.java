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

package org.apache.ws.security.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.util.DOM2Writer;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.Validator;

import org.w3c.dom.Element;

import java.util.List;
import javax.xml.namespace.QName;

public class SAMLTokenProcessor implements Processor {
    private static Log log = LogFactory.getLog(SAMLTokenProcessor.class.getName());
    
    public List<WSSecurityEngineResult> handleToken(
        Element elem, 
        RequestData data, 
        WSDocInfo wsDocInfo 
    ) throws WSSecurityException {
        if (log.isDebugEnabled()) {
            log.debug("Found SAML Assertion element");
        }
        
        AssertionWrapper assertion = handleSAMLToken(elem, data, wsDocInfo);
        wsDocInfo.addTokenElement(elem);
        WSSecurityEngineResult result = null;
        if (assertion.isSigned()) {
            result = new WSSecurityEngineResult(WSConstants.ST_SIGNED, assertion);
        } else {
            result = new WSSecurityEngineResult(WSConstants.ST_UNSIGNED, assertion);
        }
        String id = assertion.getId();
        result.put(WSSecurityEngineResult.TAG_ID, id);
        wsDocInfo.addResult(result);
        return java.util.Collections.singletonList(result);
    }

    public AssertionWrapper handleSAMLToken(
        Element token, 
        RequestData data,
        WSDocInfo docInfo
    ) throws WSSecurityException {
        AssertionWrapper assertion = new AssertionWrapper(token);
        if (assertion.isSigned()) {
            assertion.verifySignature(data, docInfo);
        }
        // Parse the HOK subject if it exists
        assertion.parseHOKSubject(data, docInfo);
            
        // Now delegate the rest of the verification to the Validator
        Validator validator = data.getValidator(new QName(token.getNamespaceURI(),
                                                          token.getLocalName()));
        Credential credential = new Credential();
        credential.setAssertion(assertion);
        validator.validate(credential, data);
        
        if (log.isDebugEnabled()) {
            log.debug("SAML Assertion issuer " + assertion.getIssuerString());
            log.debug(DOM2Writer.nodeToString(token));
        }
        
        return assertion;
    }

}

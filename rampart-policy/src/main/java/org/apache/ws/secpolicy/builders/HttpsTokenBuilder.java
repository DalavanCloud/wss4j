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
package org.apache.ws.secpolicy.builders;

import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.ws.secpolicy.SP11Constants;
import org.apache.ws.secpolicy.SP13Constants;
import org.apache.ws.secpolicy.SPConstants;
import org.apache.ws.secpolicy.SPUtils;
import org.apache.ws.secpolicy.model.HttpsToken;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class HttpsTokenBuilder implements AssertionBuilder<Element> {

    public Assertion build(Element element, AssertionBuilderFactory factory) throws IllegalArgumentException {

        final SPConstants.SPVersion spVersion = SPConstants.SPVersion.getSPVersion(element.getNamespaceURI());
        final String includeTokenValue = SPUtils.getAttribute(element, spVersion.getSPConstants().getIncludeToken());
        final Element issuer = SPUtils.getFirstChildElement(element, spVersion.getSPConstants().getIssuer());
        if (spVersion == SPConstants.SPVersion.SP11 && issuer != null) {
            throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
        }
        final String issuerName = SPUtils.getFirstChildElementText(element, spVersion.getSPConstants().getIssuerName());
        if (spVersion == SPConstants.SPVersion.SP11 && issuerName != null) {
            throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
        }
        final Element claims = SPUtils.getFirstChildElement(element, spVersion.getSPConstants().getClaims());
        final Element nestedPolicyElement = SPUtils.getFirstPolicyChildElement(element);
        final Policy nestedPolicy = nestedPolicyElement != null ? factory.getPolicyEngine().getPolicy(nestedPolicyElement) : new Policy();
        HttpsToken httpsToken = new HttpsToken(
                spVersion,
                spVersion.getSPConstants().getInclusionFromAttributeValue(includeTokenValue),
                issuer,
                issuerName,
                claims,
                nestedPolicy
        );
        httpsToken.setOptional(SPUtils.isOptional(element));
        httpsToken.setIgnorable(SPUtils.isIgnorable(element));
        return httpsToken;
    }

    public QName[] getKnownElements() {
        return new QName[]{SP13Constants.HTTPS_TOKEN, SP11Constants.HTTPS_TOKEN};
    }
}

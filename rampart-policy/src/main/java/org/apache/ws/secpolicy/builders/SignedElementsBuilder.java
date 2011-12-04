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

import org.apache.commons.lang.StringUtils;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.ws.secpolicy.SP11Constants;
import org.apache.ws.secpolicy.SP13Constants;
import org.apache.ws.secpolicy.SPConstants;
import org.apache.ws.secpolicy.SPUtils;
import org.apache.ws.secpolicy.model.SignedElements;
import org.apache.ws.secpolicy.model.XPath;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class SignedElementsBuilder implements AssertionBuilder<Element> {

    public Assertion build(Element element, AssertionBuilderFactory factory) throws IllegalArgumentException {

        final SPConstants.SPVersion spVersion = SPConstants.SPVersion.getSPVersion(element.getNamespaceURI());
        final String xPathVersion = getXPathVersion(element);
        final List<XPath> xPaths = getXPathExpressions(element, spVersion);
        final List<XPath> xPaths2 = getXPath2Expressions(element, spVersion);
        xPaths.addAll(xPaths2);
        SignedElements signedElements = new SignedElements(spVersion, xPathVersion, xPaths);
        signedElements.setOptional(SPUtils.isOptional(element));
        signedElements.setIgnorable(SPUtils.isIgnorable(element));
        return signedElements;
    }

    protected List<XPath> getXPathExpressions(Element element, SPConstants.SPVersion spVersion) {
        List<XPath> xPaths = new ArrayList<XPath>();

        Element child = SPUtils.getFirstChildElement(element);
        while (child != null) {
            if (SPConstants.XPATH_EXPR.equals(child.getLocalName())
                    && spVersion.getSPConstants().getXPathExpression().getNamespaceURI().equals(child.getNamespaceURI())) {
                Map<String, String> declaredNamespaces = new HashMap<String, String>();
                addDeclaredNamespaces(child, declaredNamespaces);
                xPaths.add(new XPath(child.getTextContent(), XPath.Version.V1, null, declaredNamespaces));
            }
            child = SPUtils.getNextSiblingElement(child);
        }
        return xPaths;
    }

    protected List<XPath> getXPath2Expressions(Element element, SPConstants.SPVersion spVersion) {
        List<XPath> xPaths = new ArrayList<XPath>();

        Element child = SPUtils.getFirstChildElement(element);
        while (child != null) {
            if (SPConstants.XPATH2_EXPR.equals(child.getLocalName())
                    && spVersion.getSPConstants().getXPath2Expression().getNamespaceURI().equals(child.getNamespaceURI())) {
                Map<String, String> declaredNamespaces = new HashMap<String, String>();
                addDeclaredNamespaces(child, declaredNamespaces);
                String filter = child.getAttribute(SPConstants.FILTER);
                if (StringUtils.isEmpty(filter)) {
                    throw new IllegalArgumentException(SPConstants.ERR_INVALID_POLICY);
                }
                xPaths.add(new XPath(child.getTextContent(), XPath.Version.V2, filter, declaredNamespaces));
            }
            child = SPUtils.getNextSiblingElement(child);
        }
        return xPaths;
    }

    protected String getXPathVersion(Element element) {
        String xPathVersion = element.getAttribute(SPConstants.XPATH_VERSION);
        if (StringUtils.isEmpty(xPathVersion)) {
            xPathVersion = "1.0";
        }
        return xPathVersion;
    }

    protected void addDeclaredNamespaces(Element element, Map<String, String> declaredNamespaces) {
        if (element.getParentNode() != null && element.getParentNode() instanceof Element) {
            addDeclaredNamespaces((Element) element.getParentNode(), declaredNamespaces);
        }
        NamedNodeMap map = element.getAttributes();
        for (int x = 0; x < map.getLength(); x++) {
            Attr attr = (Attr) map.item(x);
            if ("xmlns".equals(attr.getPrefix())) {
                declaredNamespaces.put(attr.getLocalName(), attr.getValue());
            }
        }
    }

    public QName[] getKnownElements() {
        return new QName[]{SP13Constants.SIGNED_ELEMENTS, SP11Constants.SIGNED_ELEMENTS};
    }
}

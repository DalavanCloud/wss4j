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
package org.swssf.xmlsec.impl.stax;

import org.swssf.xmlsec.ext.stax.XMLSecDTD;
import org.swssf.xmlsec.ext.stax.XMLSecStartElement;

import javax.xml.stream.XMLStreamConstants;
import java.util.Collections;
import java.util.List;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class XMLSecDTDImpl extends XMLSecEventBaseImpl implements XMLSecDTD {

    private final String dtd;

    public XMLSecDTDImpl(String dtd, XMLSecStartElement parentXmlSecStartElement) {
        this.dtd = dtd;
        setParentXMLSecStartElement(parentXmlSecStartElement);
    }

    @Override
    public String getDocumentTypeDeclaration() {
        return dtd;
    }

    @Override
    public Object getProcessedDTD() {
        return null;
    }

    @Override
    public List getNotations() {
        return Collections.emptyList();
    }

    @Override
    public List getEntities() {
        return Collections.emptyList();
    }

    @Override
    public int getEventType() {
        return XMLStreamConstants.DTD;
    }
}

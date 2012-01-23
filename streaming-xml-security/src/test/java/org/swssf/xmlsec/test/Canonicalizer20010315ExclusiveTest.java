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
package org.swssf.xmlsec.test;

import org.swssf.xmlsec.impl.transformer.canonicalizer.Canonicalizer20010315_ExclOmitCommentsTransformer;
import org.swssf.xmlsec.impl.transformer.canonicalizer.Canonicalizer20010315_ExclWithCommentsTransformer;
import org.swssf.xmlsec.test.utils.XMLEventNSAllocator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class Canonicalizer20010315ExclusiveTest {

    private XMLInputFactory xmlInputFactory;

    @BeforeMethod
    public void setUp() throws Exception {
        this.xmlInputFactory = XMLInputFactory.newFactory();
        this.xmlInputFactory.setEventAllocator(new XMLEventNSAllocator());
    }

    @Test
    public void test221excl() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Canonicalizer20010315_ExclWithCommentsTransformer c = new Canonicalizer20010315_ExclWithCommentsTransformer(null, baos);
        XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(
                this.getClass().getClassLoader().getResourceAsStream("testdata/c14n/inExcl/example2_2_1.xml")
        );

        XMLEvent xmlEvent = null;
        while (xmlEventReader.hasNext()) {
            xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement() && xmlEvent.asStartElement().getName().equals(new QName("http://example.net", "elem2"))) {
                break;
            }
        }
        while (xmlEventReader.hasNext()) {

            c.transform(xmlEvent);

            if (xmlEvent.isEndElement() && xmlEvent.asEndElement().getName().equals(new QName("http://example.net", "elem2"))) {
                break;
            }
            xmlEvent = xmlEventReader.nextEvent();
        }

        byte[] reference = getBytesFromResource(this.getClass().getClassLoader().getResource("testdata/c14n/inExcl/example2_2_c14nized_exclusive.xml"));
        boolean equals = java.security.MessageDigest.isEqual(reference, baos.toByteArray());

        if (!equals) {
            System.out.println("Expected:\n" + new String(reference, "UTF-8"));
            System.out.println("");
            System.out.println("Got:\n" + new String(baos.toByteArray(), "UTF-8"));
        }

        assertTrue(equals);
    }

    @Test
    public void test222excl() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Canonicalizer20010315_ExclWithCommentsTransformer c = new Canonicalizer20010315_ExclWithCommentsTransformer(null, baos);
        XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(
                this.getClass().getClassLoader().getResourceAsStream("testdata/c14n/inExcl/example2_2_2.xml")
        );

        XMLEvent xmlEvent = null;
        while (xmlEventReader.hasNext()) {
            xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement() && xmlEvent.asStartElement().getName().equals(new QName("http://example.net", "elem2"))) {
                break;
            }
        }
        while (xmlEventReader.hasNext()) {

            c.transform(xmlEvent);

            if (xmlEvent.isEndElement() && xmlEvent.asEndElement().getName().equals(new QName("http://example.net", "elem2"))) {
                break;
            }
            xmlEvent = xmlEventReader.nextEvent();
        }

        byte[] reference = getBytesFromResource(this.getClass().getClassLoader().getResource("testdata/c14n/inExcl/example2_2_c14nized_exclusive.xml"));
        boolean equals = java.security.MessageDigest.isEqual(reference, baos.toByteArray());

        if (!equals) {
            System.out.println("Expected:\n" + new String(reference, "UTF-8"));
            System.out.println("");
            System.out.println("Got:\n" + new String(baos.toByteArray(), "UTF-8"));
        }

        assertTrue(equals);
    }
    
    @Test
    public void test24excl() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Canonicalizer20010315_ExclWithCommentsTransformer c = new Canonicalizer20010315_ExclWithCommentsTransformer(null, baos);
        XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(
                this.getClass().getClassLoader().getResourceAsStream("testdata/c14n/inExcl/example2_4.xml")
        );

        XMLEvent xmlEvent = null;
        while (xmlEventReader.hasNext()) {
            xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement() && xmlEvent.asStartElement().getName().equals(new QName("http://example.net", "elem2"))) {
                break;
            }
        }
        while (xmlEventReader.hasNext()) {

            c.transform(xmlEvent);

            if (xmlEvent.isEndElement() && xmlEvent.asEndElement().getName().equals(new QName("http://example.net", "elem2"))) {
                break;
            }
            xmlEvent = xmlEventReader.nextEvent();
        }

        byte[] reference = getBytesFromResource(this.getClass().getClassLoader().getResource("testdata/c14n/inExcl/example2_4_c14nized.xml"));
        boolean equals = java.security.MessageDigest.isEqual(reference, baos.toByteArray());

        if (!equals) {
            System.out.println("Expected:\n" + new String(reference, "UTF-8"));
            System.out.println("");
            System.out.println("Got:\n" + new String(baos.toByteArray(), "UTF-8"));
        }

        assertTrue(equals);
    }

    @Test
    public void testComplexDocexcl() throws Exception {

        QName TAG_soap11_Body = new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body", "env");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Canonicalizer20010315_ExclWithCommentsTransformer c = new Canonicalizer20010315_ExclWithCommentsTransformer(null, baos);
        XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(
                this.getClass().getClassLoader().getResourceAsStream("testdata/plain-soap-1.1.xml")
        );

        XMLEvent xmlEvent = null;
        while (xmlEventReader.hasNext()) {
            xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement() && xmlEvent.asStartElement().getName().equals(TAG_soap11_Body)) {
                break;
            }
        }
        while (xmlEventReader.hasNext()) {

            c.transform(xmlEvent);

            if (xmlEvent.isEndElement() && xmlEvent.asEndElement().getName().equals(TAG_soap11_Body)) {
                break;
            }
            xmlEvent = xmlEventReader.nextEvent();
        }

        byte[] reference = getBytesFromResource(this.getClass().getClassLoader().getResource("testdata/c14n/inExcl/plain-soap-c14nized.xml"));
        boolean equals = java.security.MessageDigest.isEqual(reference, baos.toByteArray());

        if (!equals) {
            System.out.println("Expected:\n" + new String(reference, "UTF-8"));
            System.out.println("");
            System.out.println("Got:\n" + new String(baos.toByteArray(), "UTF-8"));
        }
/*
        for (int i = 0; i < reference.length; i++) {
            if (reference[i] != baos.toByteArray()[i]) {
                System.out.println("Expected diff: " + new String(reference, i - 10, 20));
                System.out.println("Got diff: " + new String(baos.toByteArray(), i - 10, 20));
                return;
            }
        }
*/
        assertTrue(equals);
    }

    @Test
    public void testNodeSet() throws Exception {

        final String XML =
                "<env:Envelope"
                        + " xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\""
                        + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " xmlns:ns0=\"http://xmlsoap.org/Ping\""
                        + " xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">"
                        + "<env:Body wsu:Id=\"body\">"
                        + "<ns0:Ping xsi:type=\"ns0:ping\">"
                        + "<ns0:text xsi:type=\"xsd:string\">hello</ns0:text>"
                        + "</ns0:Ping>"
                        + "</env:Body>"
                        + "</env:Envelope>";

        final String c14nXML =
                "<env:Body"
                        + " xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\""
                        + " xmlns:ns0=\"http://xmlsoap.org/Ping\""
                        + " xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\""
                        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " wsu:Id=\"body\">"
                        + "<ns0:Ping xsi:type=\"ns0:ping\">"
                        + "<ns0:text xsi:type=\"xsd:string\">hello</ns0:text>"
                        + "</ns0:Ping>"
                        + "</env:Body>";

/*        Set nodeSet = new HashSet();
        XMLUtils.getSet
	    (doc.getDocumentElement().getFirstChild(), nodeSet, null, false);
        XMLSignatureInput input = new XMLSignatureInput(nodeSet);
        byte[] bytes = c14n.engineCanonicalize(input, "env ns0 xsi wsu");

*/
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<String> inclusiveNamespaces = new ArrayList<String>();
        inclusiveNamespaces.add("env");
        inclusiveNamespaces.add("ns0");
        inclusiveNamespaces.add("xsi");
        inclusiveNamespaces.add("wsu");
        Canonicalizer20010315_ExclOmitCommentsTransformer c = new Canonicalizer20010315_ExclOmitCommentsTransformer(inclusiveNamespaces, baos);
        XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(
                new StringReader(XML)
        );

        XMLEvent xmlEvent = null;
        while (xmlEventReader.hasNext()) {
            xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement() && xmlEvent.asStartElement().getName().equals(new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body"))) {
                break;
            }
        }

        while (xmlEventReader.hasNext()) {
            c.transform(xmlEvent);
            if (xmlEvent.isEndElement() && xmlEvent.asEndElement().getName().equals(new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body"))) {
                break;
            }
            xmlEvent = xmlEventReader.nextEvent();
        }

        assertEquals(new String(baos.toByteArray()), c14nXML);
    }
    /*
     private String getAbsolutePath(String path)
     {
           String basedir = System.getProperty("basedir");
           if(basedir != null && !"".equals(basedir)) {
             path = basedir + "/" + path;
           }
           return path;
     }
    */

    public static byte[] getBytesFromResource(URL resource) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream inputStream = resource.openStream();
        try {
            byte buf[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }

            return baos.toByteArray();
        } finally {
            inputStream.close();
        }
    }
}
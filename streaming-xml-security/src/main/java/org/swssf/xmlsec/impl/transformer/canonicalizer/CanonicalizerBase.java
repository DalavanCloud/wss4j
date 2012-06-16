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
package org.swssf.xmlsec.impl.transformer.canonicalizer;

import org.swssf.xmlsec.ext.Transformer;
import org.swssf.xmlsec.ext.XMLSecurityException;
import org.swssf.xmlsec.ext.XMLSecurityUtils;
import org.swssf.xmlsec.ext.stax.*;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.*;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public abstract class CanonicalizerBase implements Transformer {

    protected static final byte[] _END_PI = {'?', '>'};
    protected static final byte[] _BEGIN_PI = {'<', '?'};
    protected static final byte[] _END_COMM = {'-', '-', '>'};
    protected static final byte[] _BEGIN_COMM = {'<', '!', '-', '-'};
    protected static final byte[] __XA_ = {'&', '#', 'x', 'A', ';'};
    protected static final byte[] __X9_ = {'&', '#', 'x', '9', ';'};
    protected static final byte[] _QUOT_ = {'&', 'q', 'u', 'o', 't', ';'};
    protected static final byte[] __XD_ = {'&', '#', 'x', 'D', ';'};
    protected static final byte[] _GT_ = {'&', 'g', 't', ';'};
    protected static final byte[] _LT_ = {'&', 'l', 't', ';'};
    protected static final byte[] _END_TAG = {'<', '/'};
    protected static final byte[] _AMP_ = {'&', 'a', 'm', 'p', ';'};
    protected static final byte[] EQUAL_STRING = {'=', '\"'};
    protected static final byte[] NEWLINE = {'\n'};

    protected static final String XML = "xml";
    protected static final String XMLNS = "xmlns";
    protected static final char DOUBLEPOINT = ':';
    protected static final String XMLNS_DOUBLEPOINT = XMLNS + DOUBLEPOINT;

    private enum DocumentLevel {
        NODE_BEFORE_DOCUMENT_ELEMENT,
        NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT,
        NODE_AFTER_DOCUMENT_ELEMENT
    }

    private OutputStream outputStream;

    private static final Map<String, byte[]> cache = new WeakHashMap<String, byte[]>();
    private final C14NStack outputStack = new C14NStack();
    private boolean includeComments = false;
    private DocumentLevel currentDocumentLevel = DocumentLevel.NODE_BEFORE_DOCUMENT_ELEMENT;
    private boolean firstCall = true;
    private SortedSet<String> inclusiveNamespaces = null;

    public CanonicalizerBase(boolean includeComments) {
        this.includeComments = includeComments;
    }

    @Override
    public void setOutputStream(OutputStream outputStream) throws XMLSecurityException {
        this.outputStream = outputStream;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setList(List list) throws XMLSecurityException {
        this.inclusiveNamespaces = prefixList2Set(list);
    }

    @Override
    public void setTransformer(Transformer transformer) throws XMLSecurityException {
        throw new UnsupportedOperationException("Transformer not supported");
    }

    public static SortedSet<String> prefixList2Set(List<String> inclusiveNamespaces) {

        if ((inclusiveNamespaces == null) || (inclusiveNamespaces.isEmpty())) {
            return null;
        }

        final SortedSet<String> prefixes = new TreeSet<String>();

        for (int i = 0; i < inclusiveNamespaces.size(); i++) {
            final String s = inclusiveNamespaces.get(i).intern();
            if ("#default".equals(s)) {
                prefixes.add("");
            } else {
                prefixes.add(s);
            }
        }
        return prefixes;
    }

    protected SortedSet<XMLSecNamespace> getCurrentUtilizedNamespaces(final XMLSecStartElement xmlSecStartElement,
                                                                      final C14NStack outputStack) {

        SortedSet<XMLSecNamespace> utilizedNamespaces = emptySortedSet();

        XMLSecNamespace elementNamespace = xmlSecStartElement.getElementNamespace();
        final XMLSecNamespace found = (XMLSecNamespace) outputStack.containsOnStack(elementNamespace);
        //found means the prefix matched. so check the ns further
        if (found == null || found.getNamespaceURI() == null
                || !found.getNamespaceURI().equals(elementNamespace.getNamespaceURI())) {

            utilizedNamespaces = new TreeSet<XMLSecNamespace>();
            utilizedNamespaces.add(elementNamespace);
            outputStack.peek().add(elementNamespace);
        }

        List<XMLSecNamespace> declaredNamespaces = xmlSecStartElement.getOnElementDeclaredNamespaces();
        for (int i = 0; i < declaredNamespaces.size(); i++) {
            XMLSecNamespace comparableNamespace = declaredNamespaces.get(i);
            final XMLSecNamespace resultNamespace = (XMLSecNamespace) outputStack.containsOnStack(comparableNamespace);
            //resultNamespace means the prefix matched. so check the ns further
            if (resultNamespace != null && resultNamespace.getNamespaceURI() != null
                    && resultNamespace.getNamespaceURI().equals(comparableNamespace.getNamespaceURI())) {
                continue;
            }

            if (utilizedNamespaces == (Object) emptySortedSet()) {
                utilizedNamespaces = new TreeSet<XMLSecNamespace>();
            }
            utilizedNamespaces.add(comparableNamespace);
            outputStack.peek().add(comparableNamespace);
        }

        List<XMLSecAttribute> comparableAttributes = xmlSecStartElement.getOnElementDeclaredAttributes();
        for (int i = 0; i < comparableAttributes.size(); i++) {
            XMLSecAttribute xmlSecAttribute = comparableAttributes.get(i);
            XMLSecNamespace attributeNamespace = xmlSecAttribute.getAttributeNamespace();
            if ("xml".equals(attributeNamespace.getPrefix())) {
                continue;
            }
            if (attributeNamespace.getNamespaceURI() == null || attributeNamespace.getNamespaceURI().isEmpty()) {
                continue;
            }
            final XMLSecNamespace resultNamespace = (XMLSecNamespace) outputStack.containsOnStack(attributeNamespace);
            //resultNamespace means the prefix matched. so check the ns further
            if (resultNamespace == null || resultNamespace.getNamespaceURI() == null
                    || !resultNamespace.getNamespaceURI().equals(attributeNamespace.getNamespaceURI())) {

                if (utilizedNamespaces == (Object) emptySortedSet()) {
                    utilizedNamespaces = new TreeSet<XMLSecNamespace>();
                }
                utilizedNamespaces.add(attributeNamespace);
                outputStack.peek().add(attributeNamespace);
            }
        }

        return utilizedNamespaces;
    }

    protected SortedSet<XMLSecAttribute> getCurrentUtilizedAttributes(final XMLSecStartElement xmlSecStartElement,
                                                                      final C14NStack outputStack) {
        List<XMLSecAttribute> comparableAttributes = xmlSecStartElement.getOnElementDeclaredAttributes();
        if (comparableAttributes.isEmpty()) {
            return emptySortedSet();
        }

        return new TreeSet<XMLSecAttribute>(comparableAttributes);
    }

    protected SortedSet<XMLSecNamespace> getInitialUtilizedNamespaces(final XMLSecStartElement xmlSecStartElement,
                                                                      final C14NStack outputStack) {

        final SortedSet<XMLSecNamespace> utilizedNamespaces = new TreeSet<XMLSecNamespace>();
        List<XMLSecNamespace> visibleNamespaces = new ArrayList<XMLSecNamespace>();
        xmlSecStartElement.getNamespacesFromCurrentScope(visibleNamespaces);
        for (int i = 0; i < visibleNamespaces.size(); i++) {
            XMLSecNamespace comparableNamespace = visibleNamespaces.get(i);

            final XMLSecNamespace found = (XMLSecNamespace) outputStack.containsOnStack(comparableNamespace);
            //found means the prefix matched. so check the ns further
            if (found != null && found.getNamespaceURI() != null && found.getNamespaceURI().equals(comparableNamespace.getNamespaceURI())) {
                continue;
            }
            utilizedNamespaces.add(comparableNamespace);
            outputStack.peek().add(comparableNamespace);
        }

        return utilizedNamespaces;
    }

    protected SortedSet<XMLSecAttribute> getInitialUtilizedAttributes(final XMLSecStartElement xmlSecStartElement,
                                                                      final C14NStack outputStack) {

        SortedSet<XMLSecAttribute> utilizedAttributes = emptySortedSet();

        List<XMLSecAttribute> comparableAttributes = new ArrayList<XMLSecAttribute>();
        xmlSecStartElement.getAttributesFromCurrentScope(comparableAttributes);
        for (int i = 0; i < comparableAttributes.size(); i++) {
            XMLSecAttribute comparableAttribute = comparableAttributes.get(i);
            if (!XML.equals(comparableAttribute.getName().getPrefix())) {
                continue;
            }
            if (outputStack.containsOnStack(comparableAttribute) != null) {
                continue;
            }
            if (utilizedAttributes == (Object) emptySortedSet()) {
                utilizedAttributes = new TreeSet<XMLSecAttribute>();
            }
            utilizedAttributes.add(comparableAttribute);
            outputStack.peek().add(comparableAttribute);
        }

        List<XMLSecAttribute> elementAttributes = xmlSecStartElement.getOnElementDeclaredAttributes();
        for (int i = 0; i < elementAttributes.size(); i++) {
            XMLSecAttribute comparableAttribute = elementAttributes.get(i);
            //attributes with xml prefix are already processed in the for loop above
            //xml:id attributes must be handled like other attributes: emit but dont inherit
            final QName attributeName = comparableAttribute.getName();
            if (XML.equals(attributeName.getPrefix())) {
                continue;
            }
            if (utilizedAttributes == (Object) emptySortedSet()) {
                utilizedAttributes = new TreeSet<XMLSecAttribute>();
            }
            utilizedAttributes.add(comparableAttribute);
        }
        return utilizedAttributes;
    }

    public void transform(final XMLSecEvent xmlSecEvent) throws XMLStreamException {
        try {
            switch (xmlSecEvent.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:

                    final XMLSecStartElement xmlSecStartElement = xmlSecEvent.asStartElement();

                    currentDocumentLevel = DocumentLevel.NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT;
                    outputStack.push(Collections.<Comparable>emptyList());

                    final SortedSet<XMLSecNamespace> utilizedNamespaces;
                    final SortedSet<XMLSecAttribute> utilizedAttributes;

                    if (firstCall) {
                        utilizedNamespaces = new TreeSet<XMLSecNamespace>();
                        utilizedAttributes = new TreeSet<XMLSecAttribute>();
                        outputStack.peek().add(XMLSecEventFactory.createXMLSecNamespace(null, ""));
                        outputStack.push(Collections.<Comparable>emptyList());
                        firstCall = false;

                        if (this.inclusiveNamespaces != null) {
                            final Iterator<String> iterator = this.inclusiveNamespaces.iterator();
                            while (iterator.hasNext()) {
                                final String prefix = iterator.next();
                                final String ns = xmlSecStartElement.getNamespaceURI(prefix);
                                //add default ns:
                                if (ns == null && prefix != null && prefix.isEmpty()) {
                                    final XMLSecNamespace comparableNamespace = XMLSecEventFactory.createXMLSecNamespace(prefix, "");
                                    utilizedNamespaces.add(comparableNamespace);
                                    outputStack.peek().add(comparableNamespace);
                                } else if (ns != null) {
                                    final XMLSecNamespace comparableNamespace = XMLSecEventFactory.createXMLSecNamespace(prefix, ns);
                                    utilizedNamespaces.add(comparableNamespace);
                                    outputStack.peek().add(comparableNamespace);
                                }
                            }
                        }

                        utilizedNamespaces.addAll(getInitialUtilizedNamespaces(xmlSecStartElement, outputStack));
                        utilizedAttributes.addAll(getInitialUtilizedAttributes(xmlSecStartElement, outputStack));
                    } else {
                        utilizedNamespaces = getCurrentUtilizedNamespaces(xmlSecStartElement, outputStack);
                        utilizedAttributes = getCurrentUtilizedAttributes(xmlSecStartElement, outputStack);
                    }

                    outputStream.write('<');
                    final String prefix = xmlSecStartElement.getName().getPrefix();
                    if (prefix != null && !prefix.isEmpty()) {
                        UtfHelpper.writeByte(prefix, outputStream, cache);
                        outputStream.write(DOUBLEPOINT);
                    }
                    final String name = xmlSecStartElement.getName().getLocalPart();
                    UtfHelpper.writeByte(name, outputStream, cache);

                    if (!utilizedNamespaces.isEmpty()) {
                        final Iterator<XMLSecNamespace> namespaceIterator = utilizedNamespaces.iterator();
                        while (namespaceIterator.hasNext()) {
                            final XMLSecNamespace namespace = namespaceIterator.next();

                            if (!namespaceIsAbsolute(namespace.getNamespaceURI())) {
                                throw new XMLStreamException("namespace is relative encountered: " + namespace.getNamespaceURI());
                            }

                            if (namespace.isDefaultNamespaceDeclaration()) {
                                outputAttrToWriter(XMLNS, namespace.getNamespaceURI(), outputStream, cache);
                            } else {
                                outputAttrToWriter(XMLNS_DOUBLEPOINT + namespace.getPrefix(), namespace.getNamespaceURI(), outputStream, cache);
                            }
                        }
                    }

                    if (!utilizedAttributes.isEmpty()) {
                        final Iterator<XMLSecAttribute> attributeIterator = utilizedAttributes.iterator();
                        while (attributeIterator.hasNext()) {
                            final XMLSecAttribute attribute = attributeIterator.next();

                            final QName attributeName = attribute.getName();
                            final String attributeNamePrefix = attributeName.getPrefix();
                            if (attributeNamePrefix != null && !attributeNamePrefix.isEmpty()) {
                                final String localPart = attributeNamePrefix + DOUBLEPOINT + attributeName.getLocalPart();
                                outputAttrToWriter(localPart, attribute.getValue(), outputStream, cache);
                            } else {
                                outputAttrToWriter(attributeName.getLocalPart(), attribute.getValue(), outputStream, cache);
                            }
                        }
                    }

                    outputStream.write('>');
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    final XMLSecEndElement xmlSecEndElement = xmlSecEvent.asEndElement();
                    final String localPrefix = xmlSecEndElement.getName().getPrefix();
                    outputStream.write(_END_TAG);
                    if (localPrefix != null && !localPrefix.isEmpty()) {
                        UtfHelpper.writeByte(localPrefix, outputStream, cache);
                        outputStream.write(DOUBLEPOINT);
                    }
                    UtfHelpper.writeStringToUtf8(xmlSecEndElement.getName().getLocalPart(), outputStream);
                    outputStream.write('>');

                    //We finished with this level, pop to the previous definitions.
                    outputStack.pop();
                    if (outputStack.size() == 1) {
                        currentDocumentLevel = DocumentLevel.NODE_AFTER_DOCUMENT_ELEMENT;
                    }

                    break;
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    outputPItoWriter(((XMLSecProcessingInstruction) xmlSecEvent), this.outputStream, currentDocumentLevel);
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if (currentDocumentLevel == DocumentLevel.NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT) {
                        outputTextToWriter(xmlSecEvent.asCharacters().getData(), this.outputStream);
                    }
                    break;
                case XMLStreamConstants.COMMENT:
                    if (includeComments) {
                        outputCommentToWriter(((XMLSecComment) xmlSecEvent), this.outputStream, currentDocumentLevel);
                    }
                    break;
                case XMLStreamConstants.SPACE:
                    if (currentDocumentLevel == DocumentLevel.NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT) {
                        outputTextToWriter(xmlSecEvent.asCharacters().getData(), this.outputStream);
                    }
                    break;
                case XMLStreamConstants.START_DOCUMENT:
                    currentDocumentLevel = DocumentLevel.NODE_BEFORE_DOCUMENT_ELEMENT;
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    throw new XMLStreamException("illegal event :" + XMLSecurityUtils.getXMLEventAsString(xmlSecEvent));
                case XMLStreamConstants.ATTRIBUTE:
                    throw new XMLStreamException("illegal event :" + XMLSecurityUtils.getXMLEventAsString(xmlSecEvent));
                case XMLStreamConstants.DTD:
                    break;
                case XMLStreamConstants.CDATA:
                    outputTextToWriter(xmlSecEvent.asCharacters().getData(), this.outputStream);
                    break;
                case XMLStreamConstants.NAMESPACE:
                    throw new XMLStreamException("illegal event :" + XMLSecurityUtils.getXMLEventAsString(xmlSecEvent));
                case XMLStreamConstants.NOTATION_DECLARATION:
                    throw new XMLStreamException("illegal event :" + XMLSecurityUtils.getXMLEventAsString(xmlSecEvent));
                case XMLStreamConstants.ENTITY_DECLARATION:
                    throw new XMLStreamException("illegal event :" + XMLSecurityUtils.getXMLEventAsString(xmlSecEvent));
            }
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    protected static void outputAttrToWriter(final String name, final String value, final OutputStream writer,
                                             final Map<String, byte[]> cache) throws IOException {
        writer.write(' ');
        UtfHelpper.writeByte(name, writer, cache);
        writer.write(EQUAL_STRING);
        byte[] toWrite;
        final int length = value.length();
        int i = 0;
        while (i < length) {
            final char c = value.charAt(i++);

            switch (c) {

                case '&':
                    toWrite = _AMP_;
                    break;

                case '<':
                    toWrite = _LT_;
                    break;

                case '"':
                    toWrite = _QUOT_;
                    break;

                case 0x09:    // '\t'
                    toWrite = __X9_;
                    break;

                case 0x0A:    // '\n'
                    toWrite = __XA_;
                    break;

                case 0x0D:    // '\r'
                    toWrite = __XD_;
                    break;

                default:
                    if (c < 0x80) {
                        writer.write(c);
                    } else {
                        UtfHelpper.writeCharToUtf8(c, writer);
                    }
                    continue;
            }
            writer.write(toWrite);
        }

        writer.write('\"');
    }

    /**
     * Outputs a Text of CDATA section to the internal Writer.
     *
     * @param text
     * @param writer writer where to write the things
     * @throws IOException
     */
    protected static void outputTextToWriter(final String text, final OutputStream writer) throws IOException {
        final int length = text.length();
        byte[] toWrite;
        for (int i = 0; i < length; i++) {
            final char c = text.charAt(i);

            switch (c) {

                case '&':
                    toWrite = _AMP_;
                    break;

                case '<':
                    toWrite = _LT_;
                    break;

                case '>':
                    toWrite = _GT_;
                    break;

                case 0xD:
                    toWrite = __XD_;
                    break;

                default:
                    if (c < 0x80) {
                        writer.write(c);
                    } else {
                        UtfHelpper.writeCharToUtf8(c, writer);
                    }
                    continue;
            }
            writer.write(toWrite);
        }
    }

    /**
     * Outputs a PI to the internal Writer.
     *
     * @param currentPI
     * @param writer    where to write the things
     * @throws IOException
     */
    protected static void outputPItoWriter(XMLSecProcessingInstruction currentPI, OutputStream writer, DocumentLevel position) throws IOException {
        if (position == DocumentLevel.NODE_AFTER_DOCUMENT_ELEMENT) {
            writer.write(NEWLINE);
        }
        writer.write(_BEGIN_PI);

        final String target = currentPI.getTarget();
        int length = target.length();

        for (int i = 0; i < length; i++) {
            final char c = target.charAt(i);
            if (c == 0x0D) {
                writer.write(__XD_);
            } else {
                if (c < 0x80) {
                    writer.write(c);
                } else {
                    UtfHelpper.writeCharToUtf8(c, writer);
                }
            }
        }

        final String data = currentPI.getData();

        length = data.length();

        if (length > 0) {
            writer.write(' ');

            for (int i = 0; i < length; i++) {
                char c = data.charAt(i);
                if (c == 0x0D) {
                    writer.write(__XD_);
                } else {
                    UtfHelpper.writeCharToUtf8(c, writer);
                }
            }
        }

        writer.write(_END_PI);
        if (position == DocumentLevel.NODE_BEFORE_DOCUMENT_ELEMENT) {
            writer.write(NEWLINE);
        }
    }

    /**
     * Method outputCommentToWriter
     *
     * @param currentComment
     * @param writer         writer where to write the things
     * @throws IOException
     */
    protected static void outputCommentToWriter(XMLSecComment currentComment, OutputStream writer, DocumentLevel position) throws IOException {
        if (position == DocumentLevel.NODE_AFTER_DOCUMENT_ELEMENT) {
            writer.write(NEWLINE);
        }
        writer.write(_BEGIN_COMM);

        final String data = currentComment.getText();
        final int length = data.length();

        for (int i = 0; i < length; i++) {
            final char c = data.charAt(i);
            if (c == 0x0D) {
                writer.write(__XD_);
            } else {
                if (c < 0x80) {
                    writer.write(c);
                } else {
                    UtfHelpper.writeCharToUtf8(c, writer);
                }
            }
        }

        writer.write(_END_COMM);
        if (position == DocumentLevel.NODE_BEFORE_DOCUMENT_ELEMENT) {
            writer.write(NEWLINE);
        }
    }

    private boolean namespaceIsAbsolute(final String namespaceValue) {
        // assume empty namespaces are absolute
        if (namespaceValue.isEmpty()) {
            return true;
        }
        return namespaceValue.indexOf(DOUBLEPOINT) > 0;
    }


    public static class C14NStack<E> extends ArrayDeque<List<Comparable>> {

        public Object containsOnStack(final Object o) {
            //Important: iteration order from head to tail!
            final Iterator<List<Comparable>> elementIterator = super.iterator();
            while (elementIterator.hasNext()) {
                final List list = elementIterator.next();
                if (list.isEmpty()) {
                    continue;
                }
                final int idx = list.indexOf(o);
                if (idx != -1) {
                    return list.get(idx);
                }
            }
            return null;
        }

        @Override
        public List<Comparable> peek() {
            List<Comparable> list = super.peekFirst();
            if (list == Collections.<Comparable>emptyList()) {
                super.removeFirst();
                list = new ArrayList<Comparable>();
                super.addFirst(list);
            }
            return list;
        }

        @Override
        public List<Comparable> peekFirst() {
            throw new UnsupportedOperationException("Use peek()");
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> SortedSet<T> emptySortedSet() {
        return (SortedSet<T>) EMPTY_SORTEDSET;
    }

    private static final SortedSet EMPTY_SORTEDSET = new EmptySortedSet();

    private static class EmptySortedSet extends AbstractSet<Object> implements SortedSet<Object>, Serializable {

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Object next() {
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean contains(Object obj) {
            return false;
        }

        @Override
        public Comparator<? super Object> comparator() {
            return null;
        }

        @Override
        public SortedSet<Object> subSet(Object fromElement, Object toElement) {
            return this;
        }

        @Override
        public SortedSet<Object> headSet(Object toElement) {
            return this;
        }

        @Override
        public SortedSet<Object> tailSet(Object fromElement) {
            return this;
        }

        @Override
        public Object first() {
            throw new NoSuchElementException();
        }

        @Override
        public Object last() {
            throw new NoSuchElementException();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            return (T[]) new Object[0];
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }
    }
}

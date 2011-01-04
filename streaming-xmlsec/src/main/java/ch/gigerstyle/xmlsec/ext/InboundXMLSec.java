/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.gigerstyle.xmlsec.ext;

import ch.gigerstyle.xmlsec.impl.DocumentContextImpl;
import ch.gigerstyle.xmlsec.impl.InputProcessorChainImpl;
import ch.gigerstyle.xmlsec.impl.XMLEventNSAllocator;
import ch.gigerstyle.xmlsec.impl.XMLSecurityStreamReader;
import ch.gigerstyle.xmlsec.impl.processor.input.LogInputProcessor;
import ch.gigerstyle.xmlsec.impl.processor.input.SecurityHeaderInputProcessor;
import ch.gigerstyle.xmlsec.impl.processor.input.XMLEventReaderInputProcessor;
import ch.gigerstyle.xmlsec.securityEvent.SecurityEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Iterator;
import java.util.List;

/**
 * Inbound Streaming-WebService-Security
 * An instance of this class can be retrieved over the XMLSec class 
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class InboundXMLSec {

    protected static final transient Log log = LogFactory.getLog(InboundXMLSec.class);

    private SecurityProperties securityProperties;

    public InboundXMLSec(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * Warning:
     * configure your xmlStreamReader correctly. Otherwise you can create a security hole.
     * At minimum configure the following properties:
     * xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
     * xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
     * xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, false);
     * xmlInputFactory.setProperty(WstxInputProperties.P_MIN_TEXT_SEGMENT, new Integer(8192));
     *
     * This method is the entry point for the incoming security-engine.
     * Hand over the original XMLStreamReader and use the returned one for further processing
     * @param xmlStreamReader The original XMLStreamReader
     * @return A new XMLStreamReader which does transparently the security processing.
     * @throws XMLStreamException thrown when a streaming error occurs
     * @throws XMLSecurityException thrown when a Security failure occurs
     */
    public XMLStreamReader processInMessage(XMLStreamReader xmlStreamReader) throws XMLStreamException, XMLSecurityException {
        return this.processInMessage(xmlStreamReader, null);
    }

    /**
     * Warning:
     * configure your xmlStreamReader correctly. Otherwise you can create a security hole.
     * At minimum configure the following properties:
     * xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
     * xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
     * xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, false);
     * xmlInputFactory.setProperty(WstxInputProperties.P_MIN_TEXT_SEGMENT, new Integer(8192));
     *
     * This method is the entry point for the incoming security-engine.
     * Hand over the original XMLStreamReader and use the returned one for further processing
     * @param xmlStreamReader The original XMLStreamReader
     * @param securityEventListener A SecurityEventListener to receive security-relevant events.
     * @return A new XMLStreamReader which does transparently the security processing.
     * @throws XMLStreamException thrown when a streaming error occurs
     * @throws XMLSecurityException thrown when a Security failure occurs
     */
    public XMLStreamReader processInMessage(XMLStreamReader xmlStreamReader, SecurityEventListener securityEventListener) throws XMLStreamException, XMLSecurityException {

        final SecurityContextImpl securityContextImpl = new SecurityContextImpl();
        securityContextImpl.setSecurityEventListener(securityEventListener);

        final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlInputFactory.setEventAllocator(new XMLEventNSAllocator());
        securityContextImpl.put(Constants.XMLINPUTFACTORY, xmlInputFactory);
        final XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(xmlStreamReader);

        DocumentContextImpl documentContext = new DocumentContextImpl();
        documentContext.setEncoding(xmlStreamReader.getEncoding() != null ? xmlStreamReader.getEncoding() : "UTF-8");
        InputProcessorChainImpl inputProcessorChain = new InputProcessorChainImpl(securityContextImpl, documentContext);
        inputProcessorChain.addProcessor(new XMLEventReaderInputProcessor(securityProperties, xmlEventReader));
        inputProcessorChain.addProcessor(new SecurityHeaderInputProcessor(securityProperties));

        if (log.isTraceEnabled()) {
            inputProcessorChain.addProcessor(new LogInputProcessor(securityProperties));
        }

        List<InputProcessor> additionalInputProcessors = securityProperties.getInputProcessorList();
        Iterator<InputProcessor> inputProcessorIterator = additionalInputProcessors.iterator();
        while (inputProcessorIterator.hasNext()) {
            InputProcessor inputProcessor = inputProcessorIterator.next();
            inputProcessorChain.addProcessor(inputProcessor);
        }

        return new XMLSecurityStreamReader(inputProcessorChain, securityProperties);
    }
}

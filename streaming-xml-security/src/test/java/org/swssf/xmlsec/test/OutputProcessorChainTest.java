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

import org.swssf.xmlsec.ext.*;
import org.swssf.xmlsec.impl.OutputProcessorChainImpl;
import org.swssf.xmlsec.impl.SecurityContextImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class OutputProcessorChainTest {

    abstract class AbstractOutputProcessor implements OutputProcessor {

        private XMLSecurityConstants.Phase phase = XMLSecurityConstants.Phase.PROCESSING;
        private Set<Object> beforeProcessors = new HashSet<Object>();
        private Set<Object> afterProcessors = new HashSet<Object>();

        public void setXMLSecurityProperties(XMLSecurityProperties xmlSecurityProperties) {
        }

        public void setAction(XMLSecurityConstants.Action action) {
        }

        public void init(OutputProcessorChain outputProcessorChain) throws XMLSecurityException {
        }

        public Set<Object> getBeforeProcessors() {
            return beforeProcessors;
        }

        public Set<Object> getAfterProcessors() {
            return afterProcessors;
        }

        public XMLSecurityConstants.Phase getPhase() {
            return phase;
        }

        public void setPhase(XMLSecurityConstants.Phase phase) {
            this.phase = phase;
        }

        public void processNextEvent(XMLEvent xmlEvent, OutputProcessorChain outputProcessorChain) throws XMLStreamException, XMLSecurityException {
        }

        public void doFinal(OutputProcessorChain outputProcessorChain) throws XMLStreamException, XMLSecurityException {
        }
    }

    @Test
    public void testAddProcessorPhase1() {
        OutputProcessorChainImpl outputProcessorChain = new OutputProcessorChainImpl(new SecurityContextImpl());

        AbstractOutputProcessor outputProcessor1 = new AbstractOutputProcessor() {
        };
        outputProcessorChain.addProcessor(outputProcessor1);

        AbstractOutputProcessor outputProcessor2 = new AbstractOutputProcessor() {
        };
        outputProcessorChain.addProcessor(outputProcessor2);

        AbstractOutputProcessor outputProcessor3 = new AbstractOutputProcessor() {
        };
        outputProcessorChain.addProcessor(outputProcessor3);

        Assert.assertEquals(outputProcessorChain.getProcessors().get(0), outputProcessor1);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(1), outputProcessor2);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(2), outputProcessor3);
    }

    @Test
    public void testAddProcessorPhase2() {
        OutputProcessorChainImpl outputProcessorChain = new OutputProcessorChainImpl(new SecurityContextImpl());

        AbstractOutputProcessor outputProcessor1 = new AbstractOutputProcessor() {
        };
        outputProcessorChain.addProcessor(outputProcessor1);

        AbstractOutputProcessor outputProcessor2 = new AbstractOutputProcessor() {
        };
        outputProcessor2.setPhase(XMLSecurityConstants.Phase.PREPROCESSING);
        outputProcessorChain.addProcessor(outputProcessor2);

        AbstractOutputProcessor outputProcessor3 = new AbstractOutputProcessor() {
        };
        outputProcessor3.setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        outputProcessorChain.addProcessor(outputProcessor3);

        AbstractOutputProcessor outputProcessor4 = new AbstractOutputProcessor() {
        };
        outputProcessor4.setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        outputProcessorChain.addProcessor(outputProcessor4);

        AbstractOutputProcessor outputProcessor5 = new AbstractOutputProcessor() {
        };
        outputProcessor5.setPhase(XMLSecurityConstants.Phase.PREPROCESSING);
        outputProcessorChain.addProcessor(outputProcessor5);

        AbstractOutputProcessor outputProcessor6 = new AbstractOutputProcessor() {
        };
        outputProcessorChain.addProcessor(outputProcessor6);

        Assert.assertEquals(outputProcessorChain.getProcessors().get(0), outputProcessor2);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(1), outputProcessor5);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(2), outputProcessor1);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(3), outputProcessor6);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(4), outputProcessor3);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(5), outputProcessor4);
    }

    @Test
    public void testAddProcessorBefore1() {
        OutputProcessorChainImpl outputProcessorChain = new OutputProcessorChainImpl(new SecurityContextImpl());

        AbstractOutputProcessor outputProcessor1 = new AbstractOutputProcessor() {
        };
        outputProcessorChain.addProcessor(outputProcessor1);

        AbstractOutputProcessor outputProcessor2 = new AbstractOutputProcessor() {
        };
        outputProcessor2.setPhase(XMLSecurityConstants.Phase.PREPROCESSING);
        outputProcessorChain.addProcessor(outputProcessor2);

        AbstractOutputProcessor outputProcessor3 = new AbstractOutputProcessor() {
        };
        outputProcessor3.setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        outputProcessorChain.addProcessor(outputProcessor3);

        AbstractOutputProcessor outputProcessor4 = new AbstractOutputProcessor() {
        };
        outputProcessor4.setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        outputProcessor4.getBeforeProcessors().add(outputProcessor3.getClass().getName());
        outputProcessorChain.addProcessor(outputProcessor4);

        AbstractOutputProcessor outputProcessor5 = new AbstractOutputProcessor() {
        };
        outputProcessor5.setPhase(XMLSecurityConstants.Phase.PREPROCESSING);
        outputProcessor5.getBeforeProcessors().add(outputProcessor2.getClass().getName());
        outputProcessorChain.addProcessor(outputProcessor5);

        AbstractOutputProcessor outputProcessor6 = new AbstractOutputProcessor() {
        };
        outputProcessor6.getBeforeProcessors().add(outputProcessor1.getClass().getName());
        outputProcessorChain.addProcessor(outputProcessor6);

        Assert.assertEquals(outputProcessorChain.getProcessors().get(0), outputProcessor5);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(1), outputProcessor2);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(2), outputProcessor6);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(3), outputProcessor1);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(4), outputProcessor4);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(5), outputProcessor3);
    }

    @Test
    public void testAddProcessorAfter1() {
        OutputProcessorChainImpl outputProcessorChain = new OutputProcessorChainImpl(new SecurityContextImpl());

        AbstractOutputProcessor outputProcessor1 = new AbstractOutputProcessor() {
        };
        outputProcessorChain.addProcessor(outputProcessor1);

        AbstractOutputProcessor outputProcessor2 = new AbstractOutputProcessor() {
        };
        outputProcessor2.setPhase(XMLSecurityConstants.Phase.PREPROCESSING);
        outputProcessorChain.addProcessor(outputProcessor2);

        AbstractOutputProcessor outputProcessor3 = new AbstractOutputProcessor() {
        };
        outputProcessor3.setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        outputProcessorChain.addProcessor(outputProcessor3);

        AbstractOutputProcessor outputProcessor4 = new AbstractOutputProcessor() {
        };
        outputProcessor4.setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        outputProcessor4.getAfterProcessors().add(outputProcessor3.getClass().getName());
        outputProcessorChain.addProcessor(outputProcessor4);

        AbstractOutputProcessor outputProcessor5 = new AbstractOutputProcessor() {
        };
        outputProcessor5.setPhase(XMLSecurityConstants.Phase.PREPROCESSING);
        outputProcessor5.getAfterProcessors().add(outputProcessor2.getClass().getName());
        outputProcessorChain.addProcessor(outputProcessor5);

        AbstractOutputProcessor outputProcessor6 = new AbstractOutputProcessor() {
        };
        outputProcessor6.getAfterProcessors().add(outputProcessor1.getClass().getName());
        outputProcessorChain.addProcessor(outputProcessor6);

        Assert.assertEquals(outputProcessorChain.getProcessors().get(0), outputProcessor2);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(1), outputProcessor5);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(2), outputProcessor1);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(3), outputProcessor6);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(4), outputProcessor3);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(5), outputProcessor4);
    }

    @Test
    public void testAddProcessorBeforeAndAfter1() {
        OutputProcessorChainImpl outputProcessorChain = new OutputProcessorChainImpl(new SecurityContextImpl());

        AbstractOutputProcessor outputProcessor1 = new AbstractOutputProcessor() {
        };
        outputProcessorChain.addProcessor(outputProcessor1);

        AbstractOutputProcessor outputProcessor2 = new AbstractOutputProcessor() {
        };
        outputProcessorChain.addProcessor(outputProcessor2);

        AbstractOutputProcessor outputProcessor3 = new AbstractOutputProcessor() {
        };
        outputProcessorChain.addProcessor(outputProcessor3);

        AbstractOutputProcessor outputProcessor4 = new AbstractOutputProcessor() {
        };
        outputProcessorChain.addProcessor(outputProcessor4);

        AbstractOutputProcessor outputProcessor5 = new AbstractOutputProcessor() {
        };
        outputProcessor5.getBeforeProcessors().add("");
        outputProcessor5.getAfterProcessors().add(outputProcessor3.getClass().getName());
        outputProcessorChain.addProcessor(outputProcessor5);

        AbstractOutputProcessor outputProcessor6 = new AbstractOutputProcessor() {
        };
        outputProcessor6.getBeforeProcessors().add(outputProcessor5.getClass().getName());
        outputProcessor6.getAfterProcessors().add("");
        outputProcessorChain.addProcessor(outputProcessor6);

        Assert.assertEquals(outputProcessorChain.getProcessors().get(0), outputProcessor1);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(1), outputProcessor2);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(2), outputProcessor3);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(3), outputProcessor6);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(4), outputProcessor5);
        Assert.assertEquals(outputProcessorChain.getProcessors().get(5), outputProcessor4);
    }
}

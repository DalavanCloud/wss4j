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

import org.swssf.xmlsec.ext.InputProcessor;
import org.swssf.xmlsec.ext.InputProcessorChain;
import org.swssf.xmlsec.ext.XMLSecurityConstants;
import org.swssf.xmlsec.ext.XMLSecurityException;
import org.swssf.xmlsec.ext.stax.XMLSecEvent;
import org.swssf.xmlsec.impl.InputProcessorChainImpl;
import org.swssf.xmlsec.impl.SecurityContextImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.stream.XMLStreamException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author $Author$
 * @version $Revision$ $Date$
 */
public class InputProcessorChainTest {

    abstract class AbstractInputProcessor implements InputProcessor {

        private XMLSecurityConstants.Phase phase = XMLSecurityConstants.Phase.PROCESSING;
        private Set<Object> beforeProcessors = new HashSet<Object>();
        private Set<Object> afterProcessors = new HashSet<Object>();

        public void addBeforeProcessor(Object processor) {
            this.beforeProcessors.add(processor);
        }

        public Set<Object> getBeforeProcessors() {
            return beforeProcessors;
        }

        public void addAfterProcessor(Object processor) {
            this.afterProcessors.add(processor);
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

        public XMLSecEvent processNextHeaderEvent(InputProcessorChain inputProcessorChain) throws XMLStreamException, XMLSecurityException {
            return null;
        }

        public XMLSecEvent processNextEvent(InputProcessorChain inputProcessorChain) throws XMLStreamException, XMLSecurityException {
            return null;
        }

        public void doFinal(InputProcessorChain inputProcessorChain) throws XMLStreamException, XMLSecurityException {
        }
    }

    @Test
    public void testAddProcessorPhase1() {
        InputProcessorChainImpl inputProcessorChain = new InputProcessorChainImpl(new SecurityContextImpl());

        AbstractInputProcessor inputProcessor1 = new AbstractInputProcessor() {
        };
        inputProcessorChain.addProcessor(inputProcessor1);

        AbstractInputProcessor inputProcessor2 = new AbstractInputProcessor() {
        };
        inputProcessorChain.addProcessor(inputProcessor2);

        AbstractInputProcessor inputProcessor3 = new AbstractInputProcessor() {
        };
        inputProcessorChain.addProcessor(inputProcessor3);

        Assert.assertEquals(inputProcessorChain.getProcessors().get(0), inputProcessor3);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(1), inputProcessor2);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(2), inputProcessor1);
    }

    @Test
    public void testAddProcessorPhase2() {
        InputProcessorChainImpl inputProcessorChain = new InputProcessorChainImpl(new SecurityContextImpl());

        AbstractInputProcessor inputProcessor1 = new AbstractInputProcessor() {
        };
        inputProcessorChain.addProcessor(inputProcessor1);

        AbstractInputProcessor inputProcessor2 = new AbstractInputProcessor() {
        };
        inputProcessor2.setPhase(XMLSecurityConstants.Phase.PREPROCESSING);
        inputProcessorChain.addProcessor(inputProcessor2);

        AbstractInputProcessor inputProcessor3 = new AbstractInputProcessor() {
        };
        inputProcessor3.setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        inputProcessorChain.addProcessor(inputProcessor3);

        AbstractInputProcessor inputProcessor4 = new AbstractInputProcessor() {
        };
        inputProcessor4.setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        inputProcessorChain.addProcessor(inputProcessor4);

        AbstractInputProcessor inputProcessor5 = new AbstractInputProcessor() {
        };
        inputProcessor5.setPhase(XMLSecurityConstants.Phase.PREPROCESSING);
        inputProcessorChain.addProcessor(inputProcessor5);

        AbstractInputProcessor inputProcessor6 = new AbstractInputProcessor() {
        };
        inputProcessorChain.addProcessor(inputProcessor6);

        Assert.assertEquals(inputProcessorChain.getProcessors().get(0), inputProcessor4);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(1), inputProcessor3);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(2), inputProcessor6);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(3), inputProcessor1);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(4), inputProcessor5);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(5), inputProcessor2);
    }

    @Test
    public void testAddProcessorBefore1() {
        InputProcessorChainImpl inputProcessorChain = new InputProcessorChainImpl(new SecurityContextImpl());

        AbstractInputProcessor inputProcessor1 = new AbstractInputProcessor() {
        };
        inputProcessorChain.addProcessor(inputProcessor1);

        AbstractInputProcessor inputProcessor2 = new AbstractInputProcessor() {
        };
        inputProcessor2.setPhase(XMLSecurityConstants.Phase.PREPROCESSING);
        inputProcessorChain.addProcessor(inputProcessor2);

        AbstractInputProcessor inputProcessor3 = new AbstractInputProcessor() {
        };
        inputProcessor3.setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        inputProcessorChain.addProcessor(inputProcessor3);

        AbstractInputProcessor inputProcessor4 = new AbstractInputProcessor() {
        };
        inputProcessor4.setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        inputProcessor4.addBeforeProcessor(inputProcessor3.getClass().getName());
        inputProcessorChain.addProcessor(inputProcessor4);

        AbstractInputProcessor inputProcessor5 = new AbstractInputProcessor() {
        };
        inputProcessor5.setPhase(XMLSecurityConstants.Phase.PREPROCESSING);
        inputProcessor5.addBeforeProcessor(inputProcessor2.getClass().getName());
        inputProcessorChain.addProcessor(inputProcessor5);

        AbstractInputProcessor inputProcessor6 = new AbstractInputProcessor() {
        };
        inputProcessor6.addBeforeProcessor(inputProcessor1.getClass().getName());
        inputProcessorChain.addProcessor(inputProcessor6);

        Assert.assertEquals(inputProcessorChain.getProcessors().get(0), inputProcessor3);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(1), inputProcessor4);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(2), inputProcessor1);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(3), inputProcessor6);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(4), inputProcessor2);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(5), inputProcessor5);
    }

    @Test
    public void testAddProcessorAfter1() {
        InputProcessorChainImpl inputProcessorChain = new InputProcessorChainImpl(new SecurityContextImpl());

        AbstractInputProcessor inputProcessor1 = new AbstractInputProcessor() {
        };
        inputProcessorChain.addProcessor(inputProcessor1);

        AbstractInputProcessor inputProcessor2 = new AbstractInputProcessor() {
        };
        inputProcessor2.setPhase(XMLSecurityConstants.Phase.PREPROCESSING);
        inputProcessorChain.addProcessor(inputProcessor2);

        AbstractInputProcessor inputProcessor3 = new AbstractInputProcessor() {
        };
        inputProcessor3.setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        inputProcessorChain.addProcessor(inputProcessor3);

        AbstractInputProcessor inputProcessor4 = new AbstractInputProcessor() {
        };
        inputProcessor4.setPhase(XMLSecurityConstants.Phase.POSTPROCESSING);
        inputProcessor4.addAfterProcessor(inputProcessor3.getClass().getName());
        inputProcessorChain.addProcessor(inputProcessor4);

        AbstractInputProcessor inputProcessor5 = new AbstractInputProcessor() {
        };
        inputProcessor5.setPhase(XMLSecurityConstants.Phase.PREPROCESSING);
        inputProcessor5.addAfterProcessor(inputProcessor2.getClass().getName());
        inputProcessorChain.addProcessor(inputProcessor5);

        AbstractInputProcessor inputProcessor6 = new AbstractInputProcessor() {
        };
        inputProcessor6.addAfterProcessor(inputProcessor1.getClass().getName());
        inputProcessorChain.addProcessor(inputProcessor6);

        Assert.assertEquals(inputProcessorChain.getProcessors().get(0), inputProcessor4);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(1), inputProcessor3);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(2), inputProcessor6);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(3), inputProcessor1);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(4), inputProcessor5);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(5), inputProcessor2);
    }

    @Test
    public void testAddProcessorBeforeAndAfter1() {
        InputProcessorChainImpl inputProcessorChain = new InputProcessorChainImpl(new SecurityContextImpl());

        AbstractInputProcessor inputProcessor1 = new AbstractInputProcessor() {
        };
        inputProcessorChain.addProcessor(inputProcessor1);

        AbstractInputProcessor inputProcessor2 = new AbstractInputProcessor() {
        };
        inputProcessorChain.addProcessor(inputProcessor2);

        AbstractInputProcessor inputProcessor3 = new AbstractInputProcessor() {
        };
        inputProcessorChain.addProcessor(inputProcessor3);

        AbstractInputProcessor inputProcessor4 = new AbstractInputProcessor() {
        };
        inputProcessorChain.addProcessor(inputProcessor4);

        AbstractInputProcessor inputProcessor5 = new AbstractInputProcessor() {
        };
        inputProcessor5.addBeforeProcessor("");
        inputProcessor5.addAfterProcessor(inputProcessor3.getClass().getName());
        inputProcessorChain.addProcessor(inputProcessor5);

        AbstractInputProcessor inputProcessor6 = new AbstractInputProcessor() {
        };
        inputProcessor6.addBeforeProcessor(inputProcessor5.getClass().getName());
        inputProcessor6.addAfterProcessor("");
        inputProcessorChain.addProcessor(inputProcessor6);

        Assert.assertEquals(inputProcessorChain.getProcessors().get(0), inputProcessor4);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(1), inputProcessor5);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(2), inputProcessor6);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(3), inputProcessor3);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(4), inputProcessor2);
        Assert.assertEquals(inputProcessorChain.getProcessors().get(5), inputProcessor1);
    }
}

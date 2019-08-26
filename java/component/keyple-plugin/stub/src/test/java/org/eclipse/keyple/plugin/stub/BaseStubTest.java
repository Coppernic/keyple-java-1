/********************************************************************************
 * Copyright (c) 2019 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.plugin.stub;


import org.eclipse.keyple.core.seproxy.ReaderPlugin;
import org.eclipse.keyple.core.seproxy.event.ObservablePlugin;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseStubTest {

    StubPluginFactory stubPluginFactory;
    ReaderPlugin stubPlugin;

    private static final Logger logger = LoggerFactory.getLogger(BaseStubTest.class);


    @Rule
    public TestName name = new TestName();

    public void setupStub() throws Exception {
        logger.info("------------------------------");
        logger.info("Test {}", name.getMethodName());
        logger.info("------------------------------");

        logger.info("setupStub, assert stubplugin is empty");
        stubPluginFactory = StubPluginFactory.getInstance();
        stubPlugin = stubPluginFactory.getPluginInstance(); // singleton

        logger.info("Stubplugin readers size {}", stubPlugin.getReaders().size());
        Assert.assertEquals(0, stubPlugin.getReaders().size());

        logger.info("Stubplugin observers size {}",
                ((ObservablePlugin) stubPlugin).countObservers());
        Assert.assertEquals(0, ((ObservablePlugin) stubPlugin).countObservers());

        // add a sleep to play with thread monitor timeout
        Thread.sleep(100);

    }

    public void clearStub() throws InterruptedException, KeypleReaderException {
        logger.info("---------");
        logger.info("TearDown ");
        logger.info("---------");

        stubPlugin = StubPluginFactory.getInstance().getPluginInstance(); // singleton

        stubPluginFactory.unplugStubReaders(stubPlugin.getReaderNames(), true);
        /*
         * for (AbstractObservableReader reader : stubPlugin.getReaders()) {
         * logger.info("Stubplugin unplugStubReader {}", reader.getName());
         * stubPlugin.unplugStubReader(reader.getName(), true); Thread.sleep(100); //
         * logger.debug("Stubplugin readers size {}", stubPlugin.getReaders().size()); }
         */
        ((ObservablePlugin) stubPlugin).clearObservers();

    }

}

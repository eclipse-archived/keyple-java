/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.reader;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.keyple.core.plugin.AbstractPlugin;
import org.eclipse.keyple.core.plugin.MockAbstractThreadedPlugin;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.PluginFactory;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.exception.KeyplePluginInstantiationException;
import org.eclipse.keyple.core.service.exception.KeyplePluginNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.SignatureDeclareThrowsException")
@RunWith(MockitoJUnitRunner.class)
public class SmartCardServiceTest {

  private static final Logger logger = LoggerFactory.getLogger(SmartCardServiceTest.class);

  // class to test
  SmartCardService proxyService;

  AbstractPlugin plugin1 = new MockAbstractThreadedPlugin(PLUGIN_NAME_1);

  AbstractPlugin plugin2 = new MockAbstractThreadedPlugin(PLUGIN_NAME_2);

  @Mock PluginFactory factory1;

  @Mock PluginFactory factory2;

  static String PLUGIN_NAME_1 = "plugin1";
  static String PLUGIN_NAME_2 = "plugin2";

  public SmartCardServiceTest() {}

  @Before
  public void setupBeforeEach() {

    // init class to test
    proxyService = SmartCardService.getInstance();

    Assert.assertEquals(0, proxyService.getPlugins().size());

    when(factory1.getPlugin()).thenReturn(plugin1);
    when(factory2.getPlugin()).thenReturn(plugin2);

    when(factory1.getPluginName()).thenReturn(PLUGIN_NAME_1);
    when(factory2.getPluginName()).thenReturn(PLUGIN_NAME_2);
  }

  @Test
  public void testGetVersion() {
    // test that version follows semver guidelines
    String regex =
        "^([0-9]+)\\.([0-9]+)\\.([0-9]+)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+)?$";
    String version = SmartCardService.getInstance().getVersion();
    logger.info("Version of SmartCardService {}", version);
    System.out.println("Version of SmartCardService " + version);
    assertTrue(version.matches(regex));
  }

  @Test(expected = KeyplePluginInstantiationException.class)
  public void testFailingPlugin() {

    doThrow(new KeyplePluginInstantiationException("")).when(factory1).getPlugin();

    proxyService.registerPlugin(factory1);
  }

  @Test
  public void testRegisterPlugin() {

    // register plugin1 by its factory
    Plugin testPlugin = proxyService.registerPlugin(factory1);

    // results
    Map<String, Plugin> testPlugins = proxyService.getPlugins();

    Assert.assertNotNull(testPlugin);
    Assert.assertEquals(PLUGIN_NAME_1, testPlugin.getName());
    Assert.assertEquals(1, testPlugins.size());

    // unregister
    proxyService.unregisterPlugin(PLUGIN_NAME_1);
  }

  @Test
  public void testRegisterTwicePlugin() {
    boolean isExceptionRaised = false;
    // register plugin1 by its factory
    proxyService.registerPlugin(factory1);
    try {
      proxyService.registerPlugin(factory1);
    } catch (IllegalStateException e) {
      isExceptionRaised = true;
    }

    Assert.assertTrue(isExceptionRaised);

    // should not be added twice
    Map<String, Plugin> testPlugins = proxyService.getPlugins();
    Assert.assertEquals(1, testPlugins.size());

    // unregister
    proxyService.unregisterPlugin(PLUGIN_NAME_1);
  }

  @Test
  public void testRegisterTwoPlugins() {

    // register plugin1 by its factory
    proxyService.registerPlugin(factory1);
    proxyService.registerPlugin(factory2);

    // should not be added twice
    Map<String, Plugin> testPlugins = proxyService.getPlugins();
    Assert.assertEquals(2, testPlugins.size());

    // unregister
    proxyService.unregisterPlugin(PLUGIN_NAME_1);
    proxyService.unregisterPlugin(PLUGIN_NAME_2);
  }

  @Test(expected = KeyplePluginNotFoundException.class)
  public void testGetPluginFail() throws Exception {
    proxyService.getPlugin("unknown"); // Throw exception
  }

  /**
   * Test that a plugin can not be added twice with multi thread
   *
   * @throws Exception
   */
  @Test
  public void testRegister_MultiThread() throws Exception {

    final MockObservablePluginFactory factory = new MockObservablePluginFactory(PLUGIN_NAME_1);
    final CountDownLatch latch = new CountDownLatch(1);

    final AtomicBoolean running = new AtomicBoolean();
    final AtomicInteger overlaps = new AtomicInteger();

    int threads = 10;
    ExecutorService service = Executors.newFixedThreadPool(threads);
    Collection<Future> futures = new ArrayList(threads);

    for (int t = 0; t < threads; ++t) {
      futures.add(
          service.submit(
              new Runnable() {
                @Override
                public void run() {
                  try {
                    /*
                     * All thread wait for the countdown
                     */
                    latch.await();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                  if (running.get()) {
                    overlaps.incrementAndGet();
                  }
                  running.set(true);
                  try {
                    proxyService.registerPlugin(factory);
                  } catch (KeyplePluginInstantiationException e) {
                    e.printStackTrace();
                  }
                  running.set(false);
                }
              }));
    }
    /*
     * Release all thread at once
     */
    latch.countDown();
    /*
     * wait for execution
     */
    Thread.sleep(500);
    logger.info("Overlap {}", overlaps);
    assertEquals(1, proxyService.getPlugins().size());

    // unregister
    proxyService.unregisterPlugin(PLUGIN_NAME_1);
  }

  /**
   * Test that a plugin can not be removed twice with multi thread
   *
   * @throws IllegalStateException
   */
  @Test(expected = IllegalStateException.class)
  public void unregisterMultiThread() throws Exception {

    final MockObservablePluginFactory factory = new MockObservablePluginFactory(PLUGIN_NAME_1);

    // add a plugin
    proxyService.registerPlugin(factory);

    final CountDownLatch latch = new CountDownLatch(1);

    final AtomicBoolean running = new AtomicBoolean();
    final AtomicInteger overlaps = new AtomicInteger();

    int threads = 10;
    ExecutorService service = Executors.newFixedThreadPool(threads);
    Collection<Future> futures = new ArrayList(threads);

    for (int t = 0; t < threads; ++t) {
      futures.add(
          service.submit(
              new Runnable() {
                @Override
                public void run() {
                  try {
                    /*
                     * All thread wait for the countdown
                     */
                    latch.await();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                  if (running.get()) {
                    overlaps.incrementAndGet();
                  }
                  running.set(true);
                  proxyService.unregisterPlugin(factory.getPluginName());
                  running.set(false);
                }
              }));
    }
    /*
     * Release all thread at once
     */
    latch.countDown();
    Thread.sleep(500);
    logger.info("Overlap {}", overlaps);
    assertEquals(0, proxyService.getPlugins().size());
    // unregister
    proxyService.unregisterPlugin(PLUGIN_NAME_1);
  }
}

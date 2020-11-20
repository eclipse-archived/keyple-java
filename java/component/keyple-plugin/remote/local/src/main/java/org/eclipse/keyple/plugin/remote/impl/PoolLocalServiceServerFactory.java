/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.plugin.remote.impl;

import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.ReaderPoolPlugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.plugin.remote.PoolLocalServiceServer;
import org.eclipse.keyple.plugin.remote.spi.AsyncEndpointServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class of the {@link PoolLocalServiceServer}.
 *
 * @since 1.0
 */
public final class PoolLocalServiceServerFactory {

  private static final Logger logger = LoggerFactory.getLogger(PoolLocalServiceServerFactory.class);

  /**
   * (private)<br>
   * Constructor
   */
  private PoolLocalServiceServerFactory() {}

  /**
   * Init the builder
   *
   * @return next configuration step
   * @since 1.0
   */
  public static NodeStep builder() {
    return new PoolLocalServiceServerFactory.Step();
  }

  public interface BuilderStep {
    /**
     * Builds and gets the service.
     *
     * @return singleton instance of the service
     * @since 1.0
     */
    PoolLocalServiceServer getService();
  }

  public interface NodeStep {
    /**
     * Configures the service with a {@link org.eclipse.keyple.plugin.remote.AsyncNodeServer} node.
     *
     * @param endpoint The {@link AsyncEndpointServer} network endpoint to use.
     * @return next configuration step
     * @since 1.0
     */
    PluginStep withAsyncNode(AsyncEndpointServer endpoint);

    /**
     * Configures the service with a {@link org.eclipse.keyple.plugin.remote.SyncNodeServer} node.
     *
     * @return next configuration step
     * @since 1.0
     */
    PluginStep withSyncNode();
  }

  public interface PluginStep {
    /**
     * Configures the service with one or more {@link ReaderPoolPlugin} plugin(s).
     *
     * @param poolPluginNames One or more plugin names of PoolPlugin
     * @return next configuration step
     */
    BuilderStep withPoolPlugins(String... poolPluginNames);
  }

  private static class Step implements NodeStep, BuilderStep, PluginStep {

    private AsyncEndpointServer asyncEndpoint;
    private String[] poolPluginNames;

    private Step() {}

    /** {@inheritDoc} */
    @Override
    public PluginStep withAsyncNode(AsyncEndpointServer endpoint) {
      Assert.getInstance().notNull(endpoint, "endpoint");
      this.asyncEndpoint = endpoint;
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public PluginStep withSyncNode() {
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public BuilderStep withPoolPlugins(String... poolPluginNames) {
      Assert.getInstance().notNull(poolPluginNames, "poolPluginNames");
      // verify that each plugin is instance of ReaderPoolPlugin
      for (String poolPluginName : poolPluginNames) {
        Plugin plugin = SmartCardService.getInstance().getPlugin(poolPluginName);
        if (!(plugin instanceof ReaderPoolPlugin)) {
          throw new IllegalArgumentException(
              "Invalid plugin type for plugin "
                  + poolPluginName
                  + ", only ReaderPoolPlugin are valid");
        }
      }
      this.poolPluginNames = poolPluginNames;
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public PoolLocalServiceServer getService() {
      PoolLocalServiceServerImpl poolLocalServiceServerImpl =
          PoolLocalServiceServerImpl.createInstance(poolPluginNames);
      if (asyncEndpoint != null) {
        poolLocalServiceServerImpl.bindAsyncNodeServer(asyncEndpoint);
        logger.info("Create a new PoolLocalServiceServer with a AsyncNodeServer");
      } else {
        poolLocalServiceServerImpl.bindSyncNodeServer();
        logger.info("Create a new PoolLocalServiceServer with a SyncNodeServer");
      }

      return poolLocalServiceServerImpl;
    }
  }
}

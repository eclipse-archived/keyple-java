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
package org.eclipse.keyple.distributed.impl;

import com.google.gson.JsonObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.card.message.ProxyReader;
import org.eclipse.keyple.core.service.PoolPlugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.exception.KeypleAllocationReaderException;
import org.eclipse.keyple.core.service.exception.KeypleException;
import org.eclipse.keyple.core.service.exception.KeypleReaderNotFoundException;
import org.eclipse.keyple.core.util.json.BodyError;
import org.eclipse.keyple.core.util.json.KeypleGsonParser;
import org.eclipse.keyple.distributed.MessageDto;
import org.eclipse.keyple.distributed.PoolLocalServiceServer;

/**
 * (package-private)<br>
 * Implementation of the {@link PoolLocalServiceServer}.
 *
 * @since 1.0
 */
final class PoolLocalServiceServerImpl extends AbstractLocalService
    implements PoolLocalServiceServer {

  private static Map<String, PoolLocalServiceServerImpl> serviceByName;
  private final String[] poolPluginNames;

  private PoolLocalServiceServerImpl(String[] poolPluginNames) {
    this.poolPluginNames = poolPluginNames;
  }

  /**
   * (package-private)<br>
   * Creates an instance of the service.
   *
   * @param serviceName The identifier of the local service.
   * @param poolPluginNames The name(s) of the pool plugin(s) associated with this service.
   * @return A not null instance.
   * @throws IllegalArgumentException If a service already exists with the provided serviceName.
   * @since 1.0
   */
  static PoolLocalServiceServerImpl createInstance(String serviceName, String[] poolPluginNames) {
    if (serviceByName == null) {
      serviceByName = new ConcurrentHashMap<String, PoolLocalServiceServerImpl>();
    }
    if (serviceByName.containsKey(serviceName)) {
      throw new IllegalArgumentException(
          "A PoolLocalServiceServer already exists with the same name : " + serviceName);
    }
    PoolLocalServiceServerImpl instance = new PoolLocalServiceServerImpl(poolPluginNames);
    serviceByName.put(serviceName, instance);
    return instance;
  }

  /**
   * (package-private)<br>
   * Retrieves the service having the provided service name.
   *
   * @param serviceName The identifier of the local service.
   * @return A not null reference.
   * @throws IllegalStateException If there's no service having the provided name.
   * @since 1.0
   */
  static PoolLocalServiceServerImpl getInstance(String serviceName) {
    if (!serviceByName.containsKey(serviceName)) {
      throw new IllegalStateException(
          "No PoolLocalServiceServer could be found with the provided name : " + serviceName);
    }
    return serviceByName.get(serviceName);
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  void onMessage(MessageDto msg) {
    MessageDto response;
    ProxyReader reader;
    PoolPlugin poolPlugin;
    try {
      switch (MessageDto.Action.valueOf(msg.getAction())) {
        case ALLOCATE_READER:
          String groupReference =
              KeypleGsonParser.getParser()
                  .fromJson(msg.getBody(), JsonObject.class)
                  .get("groupReference")
                  .getAsString();
          poolPlugin = getAPoolPlugin(groupReference);
          reader = (ProxyReader) poolPlugin.allocateReader(groupReference);
          response = new MessageDto(msg).setLocalReaderName(reader.getName()).setBody(null);
          break;
        case RELEASE_READER:
          releaseReader(msg.getLocalReaderName());
          response = new MessageDto(msg).setBody(null);
          break;
        case GET_READER_GROUP_REFERENCES:
          SortedSet<String> groupReferences = getAllGroupReferences();
          JsonObject body = new JsonObject();
          body.add(
              "readerGroupReferences", KeypleGsonParser.getParser().toJsonTree(groupReferences));
          response = new MessageDto(msg).setBody(body.toString());
          break;
        default:
          reader = findReader(msg.getLocalReaderName());
          response = executeLocally(reader, msg);
          break;
      }
    } catch (KeypleException e) {
      response =
          new MessageDto(msg) //
              .setAction(MessageDto.Action.ERROR.name()) //
              .setBody(KeypleGsonParser.getParser().toJson(new BodyError(e)));
    }
    node.sendMessage(response);
  }

  /**
   * (private)<br>
   * Retrieve a pool plugin that contains a specific groupReference
   *
   * @param groupReference non nullable instance of a group instance
   * @return non nullable instance of a pool plugin
   * @throws KeypleAllocationReaderException if no pool plugin containing group reference is found
   */
  private PoolPlugin getAPoolPlugin(String groupReference) {
    for (String poolPluginName : poolPluginNames) {
      PoolPlugin poolPlugin = (PoolPlugin) SmartCardService.getInstance().getPlugin(poolPluginName);
      if (poolPlugin.getReaderGroupReferences().contains(groupReference)) {
        return poolPlugin;
      }
    }
    throw new KeypleAllocationReaderException(
        "No reader pool plugin containing group reference '"
            + groupReference
            + "' is registered in this service");
  }

  /**
   * (private)<br>
   * Concatenate group references of all registered pool plugins.
   *
   * @return non nullable instance of a group references, can be empty
   */
  private SortedSet<String> getAllGroupReferences() {
    SortedSet<String> allGroupReferences = new TreeSet<String>();
    for (String poolPluginName : poolPluginNames) {
      PoolPlugin poolPlugin = (PoolPlugin) SmartCardService.getInstance().getPlugin(poolPluginName);
      allGroupReferences.addAll(poolPlugin.getReaderGroupReferences());
    }
    return allGroupReferences;
  }

  /**
   * (private)<br>
   * Release reader with given reader name
   *
   * @param readerName non nullable value of a reader name
   * @throws KeypleReaderNotFoundException if no reader is found with given reader name
   */
  private void releaseReader(String readerName) {
    for (String poolPluginName : poolPluginNames) {
      PoolPlugin poolPlugin = (PoolPlugin) SmartCardService.getInstance().getPlugin(poolPluginName);
      if (poolPlugin.getReaderNames().contains(readerName)) {
        poolPlugin.releaseReader(poolPlugin.getReader(readerName));
        return;
      }
    }
    throw new KeypleReaderNotFoundException(readerName);
  }

  /**
   * (private)<br>
   * Find a reader among all pool plugins associated to this service
   *
   * @param localReaderName name of the reader to be found
   * @return a not null instance of a reader
   * @throws KeypleReaderNotFoundException if no reader is found with this name
   */
  private ProxyReader findReader(String localReaderName) {
    for (String poolPluginName : poolPluginNames) {
      PoolPlugin plugin = (PoolPlugin) SmartCardService.getInstance().getPlugin(poolPluginName);
      try {
        return (ProxyReader) plugin.getReader(localReaderName);
      } catch (KeypleReaderNotFoundException e) {
        // reader has not been found in this plugin, continue
      }
    }
    throw new KeypleReaderNotFoundException(localReaderName);
  }
}

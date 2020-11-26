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
package org.eclipse.keyple.core.card.message;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;
import org.eclipse.keyple.core.util.ByteArrayUtil;

/**
 * This POJO wraps a data set related to an ISO-7816 APDU.
 *
 * <ul>
 *   <li>A byte array containing the raw APDU data.
 *   <li>A flag indicating if the APDU is of type 4 (ingoing and outgoing data).
 *   <li>A set of integers corresponding to valid status codes in addition to the standard 9000h
 *       status word.
 * </ul>
 *
 * @since 0.9
 */
public final class ApduRequest implements Serializable {

  /** Buffer of the APDU Request */
  private final byte[] bytes;

  private final boolean case4;

  private final Set<Integer> successfulStatusCodes;

  /** Name of the request being sent */
  private String name;

  /**
   * Constructor called by a card specific library in order to build an APDU command requests sent
   * to the card through the ProxyReader.
   *
   * <p>The buffer contains the bytes to be sent.<br>
   * The case4 flag is required to manage cards that presents a behaviour not compliant with ISO
   * 7816-3 in contacts mode (not returning the 61XYh status).<br>
   * The successfulStatusCodes list indicates which status words should be considered successful
   * even though they are different from 9000h.
   *
   * @param buffer A not empty byte array.
   * @param case4 True if the APDU is in case 4, false if not.
   * @param successfulStatusCodes the list of status codes to be considered as successful although
   *     different from 9000h
   * @since 0.9
   */
  public ApduRequest(byte[] buffer, boolean case4, Set<Integer> successfulStatusCodes) {
    this.bytes = buffer;
    this.case4 = case4;
    this.successfulStatusCodes = successfulStatusCodes;
  }

  /**
   * Constructor called by a card specific library in order to build an named APDU command requests
   * sent to the card through the ProxyReader.
   *
   * <p>The buffer contains the APDU bytes.<br>
   * The name is intended to be printed in logs.<br>
   * The case4 flag is required to manage cards that presents a behaviour not compliant with ISO
   * 7816-3 in contacts mode (not returning the 61XYh status).<br>
   * The successfulStatusCodes list indicates which status words should be considered successful
   * even though they are different from 9000h.
   *
   * @param name A not empty string.
   * @param buffer A not empty byte array.
   * @param case4 True if the APDU is in case 4, false if not.
   * @param successfulStatusCodes the list of status codes to be considered as successful although
   *     different from 9000h
   * @since 0.9
   */
  public ApduRequest(
      String name, byte[] buffer, boolean case4, Set<Integer> successfulStatusCodes) {
    this(buffer, case4, successfulStatusCodes);
    this.name = name;
  }

  /**
   * Constructor called by a card specific library in order to build an APDU command requests sent
   * to the card through the ProxyReader.
   *
   * <p>The buffer contains the APDU bytes.<br>
   * The case4 flag is required to manage cards that presents a behaviour not compliant with ISO
   * 7816-3 in contacts mode (not returning the 61XYh status).<br>
   *
   * @param buffer A not empty byte array.
   * @param case4 True if the APDU is in case 4, false if not.
   * @since 0.9
   */
  public ApduRequest(byte[] buffer, boolean case4) {
    this(buffer, case4, null);
  }

  /**
   * Constructor called by a card specific library in order to build an named APDU command requests
   * sent to the card through the ProxyReader.
   *
   * <p>The buffer contains the APDU bytes.<br>
   * The name is intended to be printed in logs.<br>
   * The case4 flag is required to manage cards that presents a behaviour not compliant with ISO
   * 7816-3 in contacts mode (not returning the 61XYh status).<br>
   *
   * @param name A not empty string.
   * @param buffer A not empty byte array.
   * @param case4 True if the APDU is in case 4, false if not.
   * @since 0.9
   */
  public ApduRequest(String name, byte[] buffer, boolean case4) {
    this(buffer, case4, null);
    this.name = name;
  }

  /**
   * Indicates if the APDU is type 4.
   *
   * @return True if the APDU is type 4, false if not.
   * @since 0.9
   */
  public boolean isCase4() {
    return case4;
  }

  /**
   * Name this APDU request
   *
   * @param name A not null String.
   * @since 0.9
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Get the list of valid status codes for the request.
   *
   * @return A Set of Integer (can be null).
   * @since 0.9
   */
  public Set<Integer> getSuccessfulStatusCodes() {
    return successfulStatusCodes;
  }

  /**
   * Get the name of this APDU request
   *
   * @return A not null String.
   * @since 0.9
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the APDU buffer byte array.
   *
   * @return A not null byte array.
   * @since 0.9
   */
  public byte[] getBytes() {
    return this.bytes;
  }

  @Override
  public String toString() {
    StringBuilder string;
    string =
        new StringBuilder(
            "ApduRequest: NAME = \""
                + this.getName()
                + "\", RAWDATA = "
                + ByteArrayUtil.toHex(bytes));
    if (isCase4()) {
      string.append(", case4");
    }
    if (successfulStatusCodes != null) {
      string.append(", additional successful status codes = ");
      Iterator<Integer> iterator = successfulStatusCodes.iterator();
      while (iterator.hasNext()) {
        string.append(String.format("%04X", iterator.next()));
        if (iterator.hasNext()) {
          string.append(", ");
        }
      }
    }
    return string.toString();
  }
}

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
package org.eclipse.keyple.plugin.pcsc;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javax.smartcardio.*;
import org.eclipse.keyple.core.seproxy.SeProxyService;
import org.eclipse.keyple.core.seproxy.exception.*;
import org.eclipse.keyple.core.seproxy.plugin.reader.AbstractObservableLocalReader;
import org.eclipse.keyple.core.seproxy.plugin.reader.AbstractObservableState;
import org.eclipse.keyple.core.seproxy.plugin.reader.CardPresentMonitoringJob;
import org.eclipse.keyple.core.seproxy.plugin.reader.ObservableReaderStateService;
import org.eclipse.keyple.core.seproxy.plugin.reader.SmartInsertionMonitoringJob;
import org.eclipse.keyple.core.seproxy.plugin.reader.SmartInsertionReader;
import org.eclipse.keyple.core.seproxy.plugin.reader.SmartRemovalMonitoringJob;
import org.eclipse.keyple.core.seproxy.plugin.reader.SmartRemovalReader;
import org.eclipse.keyple.core.seproxy.plugin.reader.WaitForSeInsertion;
import org.eclipse.keyple.core.seproxy.plugin.reader.WaitForSeProcessing;
import org.eclipse.keyple.core.seproxy.plugin.reader.WaitForSeRemoval;
import org.eclipse.keyple.core.seproxy.plugin.reader.WaitForStartDetect;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Package private class implementing the {@link org.eclipse.keyple.core.seproxy.SeReader} interface
 * for PC/SC based readers.
 *
 * <p>A PC/SC reader is observable ({@link AbstractObservableLocalReader}), autonomous to detect the
 * insertion of secure elements ({@link SmartInsertionReader}, able to detect the removal of a
 * secure element prior an attempt to communicate with it ({@link SmartRemovalReader} and has
 * specific settings ({@link PcscReader}.
 *
 * @since 0.9
 */
final class PcscReaderImpl extends AbstractObservableLocalReader
    implements PcscReader, SmartInsertionReader, SmartRemovalReader {

  private static final Logger logger = LoggerFactory.getLogger(PcscReaderImpl.class);

  private final CardTerminal terminal;
  private String parameterCardProtocol;
  private boolean cardExclusiveMode;
  private boolean cardReset;
  private String currentProtocol;
  private Boolean isContactless;

  private Card card;
  private CardChannel channel;

  // the latency delay value (in ms) determines the maximum time during which the
  // waitForCardPresent and waitForCardPresent blocking functions will execute.
  // This will correspond to the capacity to react to the interrupt signal of
  // the thread (see cancel method of the Future object)
  private static final long INSERT_LATENCY = 500;
  private static final long REMOVAL_LATENCY = 500;

  private static final long INSERT_WAIT_TIMEOUT = 200;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  private final AtomicBoolean loopWaitSe = new AtomicBoolean();
  private final AtomicBoolean loopWaitSeRemoval = new AtomicBoolean();

  private final boolean usePingPresence;
  private final Map<String, ProtocolPair> protocolsMap;

  /**
   * This constructor should only be called by PcscPlugin PCSC reader parameters are initialized
   * with their default values as defined in setParameter.
   *
   * @param pluginName the name of the plugin
   * @param terminal the PC/SC terminal
   * @since 0.9
   */
  protected PcscReaderImpl(String pluginName, CardTerminal terminal) {

    super(pluginName, terminal.getName());

    this.terminal = terminal;
    this.card = null;
    this.channel = null;
    this.parameterCardProtocol = IsoProtocol.ANY.getValue();
    this.cardExclusiveMode = true;
    this.cardReset = false;
    this.isContactless = null;
    this.currentProtocol = null;
    this.protocolsMap = new HashMap<String, ProtocolPair>();

    String os = System.getProperty("os.name").toLowerCase();
    usePingPresence = os.contains("mac");
    logger.info(
        "System detected : {}, is macOs checkPresence ping activated {}", os, usePingPresence);

    this.stateService = initStateService();

    logger.debug("[{}] constructor => using terminal ", terminal);
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.9
   */
  @Override
  public ObservableReaderStateService initStateService() {

    Map<AbstractObservableState.MonitoringState, AbstractObservableState> states =
        new EnumMap<AbstractObservableState.MonitoringState, AbstractObservableState>(
            AbstractObservableState.MonitoringState.class);
    states.put(
        AbstractObservableState.MonitoringState.WAIT_FOR_START_DETECTION,
        new WaitForStartDetect(this));

    // should the SmartInsertionMonitoringJob be used?
    if (!usePingPresence) {
      // use the SmartInsertionMonitoringJob
      states.put(
          AbstractObservableState.MonitoringState.WAIT_FOR_SE_INSERTION,
          new WaitForSeInsertion(this, new SmartInsertionMonitoringJob(this), executorService));
    } else {
      // use the CardPresentMonitoring job (only on Mac due to jvm crash)
      // https://github.com/eclipse/keyple-java/issues/153
      states.put(
          AbstractObservableState.MonitoringState.WAIT_FOR_SE_INSERTION,
          new WaitForSeInsertion(
              this,
              new CardPresentMonitoringJob(this, INSERT_WAIT_TIMEOUT, true),
              executorService));
    }

    states.put(
        AbstractObservableState.MonitoringState.WAIT_FOR_SE_PROCESSING,
        new WaitForSeProcessing(this, new SmartRemovalMonitoringJob(this), executorService));

    states.put(
        AbstractObservableState.MonitoringState.WAIT_FOR_SE_REMOVAL,
        new WaitForSeRemoval(this, new SmartRemovalMonitoringJob(this), executorService));

    return new ObservableReaderStateService(
        this, states, AbstractObservableState.MonitoringState.WAIT_FOR_START_DETECTION);
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.9
   */
  @Override
  protected void closePhysicalChannel() {

    try {
      if (card != null) {
        logger.debug("[{}] closePhysicalChannel => closing the channel.", this.getName());

        channel = null;
        card.disconnect(cardReset);
        card = null;
        currentProtocol = null;
      } else {
        logger.debug("[{}] closePhysicalChannel => card object is null.", this.getName());
      }
    } catch (CardException e) {
      throw new KeypleReaderIOException("Error while closing physical channel", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.9
   */
  @Override
  protected boolean checkSePresence() {
    try {
      return terminal.isCardPresent();
    } catch (CardException e) {
      throw new KeypleReaderIOException("Exception occurred in isSePresent", e);
    }
  }

  /**
   * Implements from SmartInsertionReader<br>
   * {@inheritDoc}
   *
   * @since 0.9
   */
  @Override
  public boolean waitForCardPresent() {

    logger.debug(
        "[{}] waitForCardPresent => loop with latency of {} ms.", this.getName(), INSERT_LATENCY);

    // activate loop
    loopWaitSe.set(true);

    try {
      while (loopWaitSe.get()) {
        if (logger.isTraceEnabled()) {
          logger.trace("[{}] waitForCardPresent => looping", this.getName());
        }
        if (terminal.waitForCardPresent(INSERT_LATENCY)) {
          // card inserted
          return true;
        } else {
          if (Thread.interrupted()) {
            logger.debug("[{}] waitForCardPresent => task has been cancelled", this.getName());
            // task has been cancelled
            return false;
          }
        }
      }
      // if loop was stopped
      return false;
    } catch (CardException e) {
      throw new KeypleReaderIOException(
          "["
              + this.getName()
              + "] Exception occurred in waitForCardPresent. "
              + "Message: "
              + e.getMessage());
    } catch (Throwable t) {
      // can or can not happen depending on terminal.waitForCardPresent
      logger.debug("[{}] waitForCardPresent => Throwable caught.", this.getName(), t);
      return false;
    }
  }

  /**
   * Implements from SmartInsertionReader<br>
   * {@inheritDoc}
   *
   * @since 0.9
   */
  @Override
  public void stopWaitForCard() {
    loopWaitSe.set(false);
  }

  /**
   * Wait for the card absent event from smartcard.io<br>
   * {@inheritDoc}
   *
   * @since 0.9
   */
  @Override
  public boolean waitForCardAbsentNative() {

    logger.debug(
        "[{}] waitForCardAbsentNative => loop with latency of {} ms.",
        this.getName(),
        REMOVAL_LATENCY);

    loopWaitSeRemoval.set(true);

    try {
      while (loopWaitSeRemoval.get()) {
        if (logger.isTraceEnabled()) {
          logger.trace("[{}] waitForCardAbsentNative => looping", this.getName());
        }
        if (terminal.waitForCardAbsent(REMOVAL_LATENCY)) {
          // card removed
          return true;
        } else {
          if (Thread.interrupted()) {
            logger.debug("[{}] waitForCardAbsentNative => task has been cancelled", this.getName());
            // task has been cancelled
            return false;
          }
        }
      }
      return false;
    } catch (CardException e) {
      throw new KeypleReaderIOException(
          "["
              + this.getName()
              + "] Exception occurred in waitForCardAbsentNative. "
              + "Message: "
              + e.getMessage());
    } catch (Throwable t) {
      // can or can not happen depending on terminal.waitForCardAbsent
      logger.debug("[{}] waitForCardAbsentNative => Throwable caught.", this.getName(), t);
      return false;
    }
  }

  /**
   * Implements from SmartRemovalReader<br>
   * {@inheritDoc}
   *
   * @since 0.9
   */
  @Override
  public void stopWaitForCardRemoval() {
    loopWaitSeRemoval.set(false);
  }

  /**
   * {@inheritDoc}
   *
   * @since 0.9
   */
  @Override
  protected byte[] transmitApdu(byte[] apduIn) {

    ResponseAPDU apduResponseData;

    if (channel != null) {
      try {
        apduResponseData = channel.transmit(new CommandAPDU(apduIn));
      } catch (CardException e) {
        throw new KeypleReaderIOException(this.getName() + ":" + e.getMessage());
      } catch (IllegalArgumentException e) {
        // card could have been removed prematurely
        throw new KeypleReaderIOException(this.getName() + ":" + e.getMessage());
      }
    } else {
      // could occur if the SE was removed
      throw new KeypleReaderIOException(this.getName() + ": null channel.");
    }
    return apduResponseData.getBytes();
  }

  /** Utility class to associate an external protocol name and the corresponding rule. */
  class ProtocolPair {
    public ProtocolPair(String applicationProtocolName, String readerProtocolRule) {
      this.applicationProtocolName = applicationProtocolName;
      this.readerProtocolRule = readerProtocolRule;
    }

    public String getApplicationProtocolName() {
      return applicationProtocolName;
    }

    public String getReaderProtocolRule() {
      return readerProtocolRule;
    }

    private final String applicationProtocolName;
    private final String readerProtocolRule;
  }

  /**
   * {@inheritDoc}
   *
   * <p>In the PC/SC case, this method fills the internal protocols map with the provided protocol
   * and its associated rule (regular expression).
   *
   * <p>The full ISO protocols (ISO7816-3 and ISO14443-4) are activated by default (see {@link
   * PcscReaderImpl#PcscReaderImpl(String, CardTerminal)}.
   *
   * @throws IllegalArgumentException if seProtocol is null.
   * @since 1.0
   */
  @Override
  public void activateProtocol(String readerProtocolName, String applicationProtocolName) {

    Assert.getInstance()
        .notEmpty(readerProtocolName, "readerProtocolName")
        .notEmpty(applicationProtocolName, "applicationProtocolName");

    String protocolRule = PcscProtocolSetting.getSettings().get(readerProtocolName);

    if (protocolRule == null || protocolRule.isEmpty()) {
      throw new KeypleReaderProtocolNotSupportedException(readerProtocolName);
    }

    if (logger.isInfoEnabled()) {
      logger.info(
          "{}: Activate protocol {} with rule \"{}\".",
          getName(),
          readerProtocolName,
          protocolRule);
    }
    protocolsMap.put(readerProtocolName, new ProtocolPair(applicationProtocolName, protocolRule));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Remove the protocol from the active protocols list.<br>
   * Does nothing if the provided protocol is not active.
   *
   * @throws IllegalArgumentException if seProtocol is null.
   * @since 1.0
   */
  @Override
  public void deactivateProtocol(String readerProtocolName) {

    Assert.getInstance().notNull(readerProtocolName, "seProtocol");

    if (logger.isInfoEnabled()) {
      logger.info("{}: Deactivate protocol {}.", getName(), readerProtocolName);
    }
    if (protocolsMap.remove(readerProtocolName) == null && logger.isInfoEnabled()) {
      logger.info("{}: Protocol {} was not active", getName(), readerProtocolName);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>The standard interface of the PC/SC readers does not allow to know directly the type of
   * protocol used by an SE.
   *
   * <p>This is especially true in contactless mode. Moreover, in this mode, the Answer To Reset
   * (ATR) returned by the reader is not produced by the SE but reconstructed by the reader from low
   * level internal data and with elements defined in the standard (see <b>Interoperability
   * Specification for ICCs and Personal Computer Systems</b>, Part 3).
   *
   * <p>We therefore use ATR (real or reconstructed) to identify the SE protocol using regular
   * expressions. These regular expressions are defined in {@link PcscProtocolSetting}.
   *
   * <p>The protocol is determined once per SE communication session. If this method is called
   * several times, the last determined protocol is returned.
   *
   * @return A not empty String.
   * @throws KeypleReaderProtocolNotFoundException if none of the defined regular expressions match
   *     the ATR of the SE.
   * @since 1.0
   */
  @Override
  protected String getCurrentProtocol() {

    if (currentProtocol == null) {
      // open physical channel if needed
      if (!isPhysicalChannelOpen()) {
        openPhysicalChannel();
      }
      String atr = ByteArrayUtil.toHex(card.getATR().getBytes());
      for (Map.Entry<String, ProtocolPair> entry : protocolsMap.entrySet()) {
        Pattern p = Pattern.compile(entry.getValue().getReaderProtocolRule());
        if (p.matcher(atr).matches()) {
          currentProtocol = entry.getValue().getApplicationProtocolName();
          break;
        }
      }
      if (currentProtocol == null) {
        // none of the entries in the map correspond to the current ATR
        throw new KeypleReaderProtocolNotFoundException(atr);
      }
    }
    return currentProtocol;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public PcscReader setSharingMode(SharingMode sharingMode) {

    Assert.getInstance().notNull(sharingMode, "sharingMode");

    if (sharingMode == SharingMode.SHARED) {
      // if an SE is present, change the mode immediately
      if (card != null) {
        try {
          card.endExclusive();
        } catch (CardException e) {
          throw new KeypleReaderIOException("Couldn't disable exclusive mode", e);
        }
      }
      cardExclusiveMode = false;
    } else if (sharingMode == SharingMode.EXCLUSIVE) {
      cardExclusiveMode = true;
    }
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public PcscReader setContaclessMode(boolean isContactless) {

    this.isContactless = isContactless;
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public PcscReader setIsoProtocol(IsoProtocol isoProtocol) {

    Assert.getInstance().notNull(isoProtocol, "isoProtocol");

    parameterCardProtocol = isoProtocol.getValue();
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public PcscReader setDisconnectionMode(DisconnectionMode disconnectionMode) {

    Assert.getInstance().notNull(disconnectionMode, "disconnectionMode");

    cardReset = disconnectionMode == DisconnectionMode.RESET;
    return this;
  }

  @Override
  protected byte[] getATR() {
    return card.getATR().getBytes();
  }

  /**
   * Tells if a physical channel is open
   *
   * <p>This status may be wrong if the card has been removed.
   *
   * <p>The caller should test the card presence with isSePresent before calling this method.
   *
   * @return true if the physical channel is open
   * @since 0.9
   */
  @Override
  protected boolean isPhysicalChannelOpen() {
    return card != null;
  }

  /**
   * Opens a physical channel
   *
   * <p>The card access may be set to 'Exclusive' through the reader's settings.
   *
   * <p>In this case be aware that on some platforms (ex. Windows 8+), the exclusivity is granted
   * for a limited time (ex. 5 seconds). After this delay, the card is automatically resetted.
   *
   * @throws KeypleReaderIOException if the communication with the reader or the SE has failed
   * @since 0.9
   */
  @Override
  protected void openPhysicalChannel() {

    /* init of the physical SE channel: if not yet established, opening of a new physical channel */
    try {
      if (card == null) {
        this.card = this.terminal.connect(parameterCardProtocol);
        if (cardExclusiveMode) {
          card.beginExclusive();
          logger.debug("[{}] Opening of a physical SE channel in exclusive mode.", this.getName());

        } else {
          logger.debug("[{}] Opening of a physical SE channel in shared mode.", this.getName());
        }
      }
      this.channel = card.getBasicChannel();
    } catch (CardException e) {
      throw new KeypleReaderIOException("Error while opening Physical Channel", e);
    }
  }

  /**
   * Return the mode of transmission used to communicate with the SEs<br>
   * The transmission mode can set explicitly with setParameter(SETTING_KEY_TRANSMISSION_MODE,
   * MODE). In this case, this parameter has priority.
   *
   * <p>When the transmission mode has not explicitly set, we try to determine it from the name of
   * the reader and a parameter defined at the plugin level.
   *
   * @return the current transmission mode
   * @throws IllegalStateException if the transmission mode could not be determined
   * @since 0.9
   */
  @Override
  public boolean isContactless() {

    if (isContactless == null) {
      /* First time initialisation, the transmission mode has not yet been determined or fixed explicitly, let's ask the plugin to determine it (only once) */
      isContactless =
          ((PcscPluginImpl) SeProxyService.getInstance().getPlugin(getPluginName()))
              .isContactless(getName());
    }
    return isContactless;
  }
}

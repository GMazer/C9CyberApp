package com.c9cyber.app.domain.smartcard;

import javax.smartcardio.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SmartCardServiceImpl implements SmartCardService {
    private Card activeCard = null;
    private String connectedTerminalName = null;

    public List<String> listReaders() {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            if (terminals == null || terminals.isEmpty()) return Collections.emptyList();
            return terminals.stream().map(CardTerminal::getName).collect(Collectors.toList());
        } catch (CardException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public boolean isCardPresent(String terminalName) {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminal terminal = factory.terminals().getTerminal(terminalName);
            return terminal != null && terminal.isCardPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean connect(String terminalName) {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            CardTerminal terminal = factory.terminals().getTerminal(terminalName);

            // Kết nối và giữ session
            this.activeCard = terminal.connect("*");
            this.connectedTerminalName = terminalName;
            return true;
        } catch (Exception e) {
            this.activeCard = null;
            return false;
        }
    }

    @Override
    public byte[] transmit(byte[] apduBytes) {
        if (this.activeCard == null) {
            System.err.println("Error: Card not connected");
            return null;
        }

        try {
            CardChannel channel = this.activeCard.getBasicChannel();
            ResponseAPDU response = channel.transmit(new CommandAPDU(apduBytes));
            return response.getBytes();
        } catch (CardException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void disconnect() {
        if (this.activeCard != null) {
            try {
                this.activeCard.disconnect(false);
            } catch (CardException e) {
                e.printStackTrace();
            } finally {
                this.activeCard = null;
                this.connectedTerminalName = null;
            }
        }
    }
    public boolean isConnected() {
        return activeCard != null;
    }
}
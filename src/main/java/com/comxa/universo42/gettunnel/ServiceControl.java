package com.comxa.universo42.gettunnel;

import com.comxa.universo42.gettunnel.modelo.listener.ByteCounter;
import com.comxa.universo42.gettunnel.modelo.ClientServer;
import com.comxa.universo42.gettunnel.modelo.listener.LogBox;

public interface ServiceControl {
    public LogBox getLogBox();
    public void setLogBox(LogBox logbox);
    public ByteCounter getByteCounter();
    public void setByteCounter(ByteCounter counter);
    public ClientServer getClientServer();
    public void setClientServer(String listeningAddr, int listeningPort, String target, String serverAddr, int serverPort);
}

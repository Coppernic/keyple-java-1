/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package org.eclise.keyple.example.remote.websocket.demoPO;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.keyple.plugin.remote_se.transport.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WskServer extends WebSocketServer implements DtoSender, TransportNode {

    private static final Logger logger = LoggerFactory.getLogger(WskServer.class);
    private DtoReceiver dtoReceiver;
    private ConnectionCb connectionCb;

    public WskServer(InetSocketAddress address, ConnectionCb connectionCb) {
        super(address);
        this.connectionCb = connectionCb;
    }

    /*
        WebSocketServer
     */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.debug("Web socket onOpen {} {}", conn, handshake);
        connectionCb.onConnection(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.debug("Web socket onClose {} {} {} {}", conn, code, reason, remote);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        logger.debug("Web socket onMessage {} {}", conn, message);
        KeypleDTO keypleDTO = KeypleDTOHelper.fromJson(message);
        if (dtoReceiver != null) {
            KeypleDTO response = dtoReceiver.onDTO(keypleDTO,this,conn);
            if(response.getSessionId()!=null){
                sessionId_Connection.put(response.getSessionId(), conn);
            }else{
                logger.warn("No session defined in response {}", response);
            }
            sendDTO(response);
        } else {
            logger.warn("Received a message but no DtoReceiver");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.debug("Web socket onError {} {}", conn, ex);

    }

    @Override
    public void onStart() {
        logger.info("Web socket server started");
    }


    /*
        TransportNode
    */

    Map<String, Object> sessionId_Connection = new HashMap<String, Object>();

    @Override
    public Object getConnection(String sessionId) {
        return sessionId_Connection.get(sessionId);
    }

    public void setDtoReceiver(DtoReceiver dtoReceiver) {
        this.dtoReceiver = dtoReceiver;
    }


    /*
        DTO Sender
    */
    @Override
    public void sendDTO(KeypleDTO message) {

        if (!message.getAction().isEmpty()) {
            if(message.getSessionId()==null){
                logger.warn("No sessionId defined in message");
            }else{
                //retrieve connection object from the transport
                Object conn = getConnection(message.getSessionId());
                logger.debug("send DTO {} {}", KeypleDTOHelper.toJson(message), conn);
                ((WebSocket) conn).send(KeypleDTOHelper.toJson(message));
            }
        }
    }



}

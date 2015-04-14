/**
 * Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu 
 * Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 * Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL), Fraunhofer-Institut für 
 * Produktionstechnologie (IPT), Telcaria Ideas SL (TELCA) )
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors:
 *     	Rafael Leon Miranda
 *		Andres Beato Ollero
 */

package com.telefonica.pyretic.backendchannel;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import java.io.*;

/*
Integrates rabbitmq functions in odl_shim
*/


public class RabbitLogic {

  private final static String QUEUE_NAME = "qRabbitMQ";

/*
Creates a connection for use rabbit
input: none
return a Connection objet
*/
  public Connection connection ()
  throws Exception
  {
    Connection conn = null;

      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost("localhost");
      conn = factory.newConnection();
    return conn;
  }

/*
Creates a communication channel using an existing connection
input: Connection conn
return: a Channel object
*/

  public Channel chann (Connection conn)
  throws Exception
  {
    Channel channel = null;

      channel = conn.createChannel();

    return channel;
  }

/*
Close both of connection and channel
input: Connection conn, Channel chan
return: none
*/

  void closeAll (Connection conn, Channel chann)
  throws Exception
  {

      chann.close();
      conn.close();

  }

/*
Using previous function, creates a queue specified by a channel
chann and publish a message there.
input:  Channel chann
        String msg (Message to publish)
        Connection conn
return: none
*/
  public void SendToRabbit (Channel chann, String msg, Connection conn)
  {
    try{
      chann.queueDeclare(QUEUE_NAME, false,false,false,null);
      chann.basicPublish("",QUEUE_NAME,null,msg.getBytes());
      closeAll(conn,chann);
    }
    catch (Exception e)
    {
      System.err.println("An Error happens sending to RabbitMQ\n");
      System.out.print("trace: ");
      e.printStackTrace();
    }

  }


}

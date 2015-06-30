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

  private static final String EXCHANGE_NAME = "direct_logs";

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
      channel.exchangeDeclare(EXCHANGE_NAME, "direct");

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
  public void SendToRabbit (Channel chann, String msg, Connection conn, String severity)
  {
    try{
      chann.exchangeDeclare(EXCHANGE_NAME, "direct");
      chann.basicPublish(EXCHANGE_NAME, severity, null, msg.getBytes("UTF-8"));
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

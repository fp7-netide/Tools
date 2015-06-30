import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class logger
{
  private static final String EXCHANGE_NAME = "direct_logs";
  public static void main(String[] argv) throws Exception
  {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    channel.exchangeDeclare(EXCHANGE_NAME, "direct");
    String queueName = channel.queueDeclare().getQueue();

    /*
    if (argv.length < 1)
    {
      System.err.println("Usage: ReceiveLogsDirect [info] [warning] [error]");
      System.exit(1);
    }
    */
    String options[]={"1","0"};
    for(String severity : options)
    {
      channel.queueBind(queueName, EXCHANGE_NAME, severity);
    }

    System.out.println(" [*] Waiting for logs. To exit press CTRL+C");
    QueueingConsumer consumer = new QueueingConsumer(channel);
    channel.basicConsume(queueName, true, consumer);
    //System.out.println("Listening in :"+queueName);
    while (true)
    {
      QueueingConsumer.Delivery delivery = consumer.nextDelivery();
      String message = new String(delivery.getBody(),"UTF-8");
      String routingKey = delivery.getEnvelope().getRoutingKey();
      Date date= new Date();
      SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

      switch (routingKey)
      {
        case "1":
          System.out.print("\033[32m ['"+format.format(date)+"']");
          System.out.println("\033[32m ['" + routingKey + "'] " + message);
          break;

        case "0":
          System.out.print("\033[33m ['"+format.format(date)+"']");
          System.out.println("\033[33m ['" + routingKey + "'] " + message);
          break;

        default:
          System.out.println(" [x] Received '" + routingKey + "':'" + message + "'");
          break;
      }
    }
  }
}

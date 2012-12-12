package agagame

import java.io._
import java.net._
import variable.Proposition
import parsing._
import planning._
import bestfirst._
import structures._
import logging._
import action._
import scala.collection.mutable.ListBuffer
import GameMain._

class GameServer {

  var providerSocket: ServerSocket = null
  var connection: Socket = null
  var out: ObjectOutputStream = null
  var in: ObjectInputStream = null
  var message: String = "";

  def run() {

    val (prob, actions) = try {
      TotalParser.parse("./planfiles/aga/aga.prob", "./planfiles/aga/aga.act")
    } catch {
      case e: RuntimeException =>
        println("cannot parse file: " + e.getMessage())
        e.printStackTrace()
        return
    }
    //DebugInfo.setDebug()
    var initState = prob.init

    try {
      //1. creating a server socket
      providerSocket = new ServerSocket(2013, 10);
      //2. Wait for connection
      System.out.println("Waiting for connection");
      connection = providerSocket.accept();
      System.out.println("Connection received from " + connection.getInetAddress().getHostName());
      //3. get Input and Output streams
      out = new ObjectOutputStream(connection.getOutputStream());
      out.flush();
      in = new ObjectInputStream(connection.getInputStream());
      sendMessage("Connection successful");
      //4. The two parts communicate via the input and output streams
      do {
        try {
          message = in.readObject().asInstanceOf[String];
          System.out.println("client>" + message);
          if (message.equals("bye"))
            sendMessage("bye");
          else {
            val props = string2WorldState(message)
            initState = updateInitState(initState, props)
            val numbers = plan(prob, actions, initState)
            val rtnMsg = numbers.mkString(",")
            sendMessage(rtnMsg)
          }
        } catch {
          case err: ClassNotFoundException =>
            System.err.println("Data received in unknown format");
        }
      } while (!message.equals("bye"));
    } catch {
      case ioException: IOException =>
        ioException.printStackTrace();
    } finally {
      //4: Closing connection
      try {
        in.close();
        out.close();
        providerSocket.close();
      } catch {
        case ioException: IOException =>
          ioException.printStackTrace();
      }
    }
  }

  def sendMessage(msg: String) {
    try {
      out.writeObject(msg);
      out.flush();
      System.out.println("server>" + msg);
    } catch {
      case ioException: IOException =>
        ioException.printStackTrace();
    }
  }
}

object GameServer {

  def main(args: Array[String]) {
    val server = new GameServer();
    while (true) {
      server.run();
    }
  }
}
  
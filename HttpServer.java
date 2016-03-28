package ex01.pyrmont;

import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.util.*;
import java.io.PrintWriter;

public class HttpServer{

  /** WEB_ROOT is the directory where our HTML and other files reside.
   *  For this package, WEB_ROOT is the "webroot" directory under the working
   *  directory.
   *  The working directory is the location in the file system
   *  from where the java command was invoked.
   */
  public static final String WEB_ROOT =
    System.getProperty("user.dir") + File.separator  + "webroot";

  // shutdown command
  private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

  // the shutdown command received
  private boolean shutdown = false;

  public static void main(String[] args) {
    HttpServer server = new HttpServer();
    server.await();
  }

  public void await() {
    ServerSocket serverSocket = null;
    int port = 8080;
    try {
      serverSocket =  new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
    }
    catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    // Loop waiting for a request
    while (!shutdown) {
      Socket socket = null;
      InputStream input = null;
      OutputStream output = null;
      try {
        socket = serverSocket.accept();
        input = socket.getInputStream();
        output = socket.getOutputStream();

        // create Request object and parse
        Request request = new Request(input);
        request.parse();

        //------------------------This block of code is written/changed by me (between the lines)--------------------------//
        
        String uri = request.getUri();
        if(uri.matches(".*\\bspy\\b.*")){
          System.out.println("Waiting...");
          //multithreading right here
          threadSocket ts = new threadSocket(socket); //creating new object that has socket member 
          Thread spyThread = new Thread(ts);  //creating new thread on the threadSocket object
          spyThread.start(); //This should call to the "run" method in the threadSocket class but it doesn't 
        }
        else
        {        
            //this block handles the '/index.html' call and works so long as the directory tructure 
          if(uri.matches(".*\\bindex\\b.*"))
          {
            try
            {
              File f = new File("../../../webroot/index.html");
              Scanner scan = new Scanner(f);
              String indexText = "";
              while(scan.hasNextLine())
              {
                indexText += scan.nextLine();
              }
              System.out.println(indexText);

              OutputStream outFile = socket.getOutputStream();
              String msg = "<html><h1>Index should be here</h1></html>";
              String sendableMsg = "HTTP/1.1 200 OK\r\n Content-Type: text/html\r\n Content-Length: " + indexText.length() + "\r\n\r\n" + indexText; 
              outFile.write(sendableMsg.getBytes());
              socket.close();
            }
            catch(Exception e)
            {
              System.out.println("Could not locate \"index.html\"");
            }
          }
          //-----------------------------------------------------------------------------------------------------//


          // create Response object
          Response response = new Response(output);
          response.setRequest(request);
          response.sendStaticResource();

          // Close the socket
          socket.close();
        }

        //check if the previous URI is a shutdown command
        shutdown = request.getUri().equals(SHUTDOWN_COMMAND);
      }
      catch (Exception e) {
        e.printStackTrace();
        continue;
      }
    }
  }



  //---------------------Custom class written by me below-------------------------------//
  public class threadSocket implements Runnable
  {
    Socket s;
    public threadSocket(Socket newS)
    {
      this.s = newS;
    }

    public void run()
    {
      try
      {
        Socket numberSocket = new Socket("127.0.0.1", 9090);
        BufferedReader buffer = new BufferedReader(new InputStreamReader(numberSocket.getInputStream()));
        String number = buffer.readLine();
        OutputStream outFile = s.getOutputStream();

        //This is modeled after the request in Request.java
        String spyMessage = "<html><h2><title>*spy</title></h2><body>Secret : <b><i>" + number + "</b></i><br></body></html>";
             
        String responseMsg = "HTTP/1.1 200 OK\r\n Content-Type: text/html\r\n Content-Length: " + spyMessage.length() + "\r\n\r\n" + spyMessage; 

          // System.out.println("Number is: " + number);   
          outFile.write(responseMsg.getBytes());    

          // Close the socket
          s.close();

      }catch(Exception e)
      {
        System.out.println("Something has gone wrong.");
      }
    }
  }
}

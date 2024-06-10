import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class server {
    public static void main(String [] args){
		int port = 8001;
		
		ServerSocket welcomeSocket=null;
		try {
			welcomeSocket = new ServerSocket(port);
			System.out.println("Server started on port: "+port);
		} catch (IOException e1) {
			System.out.println("Sorry can not run server on port "+port);
			e1.printStackTrace();
		}

		while(true){
			
			Socket ClientSocket=null;
			try {
				ClientSocket = welcomeSocket.accept();
				System.out.println("Connection with client established");
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			Implementation request = new Implementation(ClientSocket);
			
			Thread thread = new Thread(request);
			
			thread.start();

		}

	}
}


final class Implementation implements Runnable{
    static Socket clientSocket;

    public Implementation(Socket Socket){
        this.clientSocket = Socket;
    }


    @Override
    public void run(){
        try{
            processRequest();
            clientSocket.close();
        }
        catch(Exception e){
            System.out.println("cannot process request");
        }
    }

    public static void processRequest(){
        String option = "";
        try(
            DataInputStream inFromClient = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());
        ){
            option = inFromClient.readUTF();
            System.out.println(option);
        }
        catch(Exception e){
        }
    }
}
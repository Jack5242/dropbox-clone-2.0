import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class client{
    public static Scanner KeyboardInput = new Scanner(System.in);

    int portPrimaryServer = 8000;
    int portSecondaryServer = 8001;
    public static void main(String [] args){

        boolean syncMode = false;
        
        //1: login screen
        //2: syncing screen
        int uiMode = 1;
        
        String username = "";
        String pass = "";

        // initiate connection to server
        while (true){
            

            if (uiMode == 1){
                System.out.println("choose your option: \n1. register\n2. login\n3. exit");
                int userInput = KeyboardInput.nextInt();
                KeyboardInput.nextLine();

                switch (userInput) {
                    case 1:
                        registerNewAccount();
                        break;
                    case 2:
                        System.out.println("enter username:");
                        username = KeyboardInput.nextLine();

                        System.out.println("enter pass:");
                        pass = KeyboardInput.nextLine();

                        if (loginAccount(username, pass) == true){
                            uiMode = 2;
                        }
                        break;
                    case 3:
                        break;
                
                    default:
                        System.out.println("Please enter again");
                        break;
                }
            }

            if (uiMode == 2){
                System.out.println("choose your option: \n1. sync file: " + syncMode + "\n2. download\n3. logout");
                int userInput = KeyboardInput.nextInt();
                KeyboardInput.nextLine();
                switch (userInput) {
                    case 1:
                        syncMode = !syncMode;
                        break;
                    case 2: 
                        downloadFile();
                        break;
                    case 3: 
                        syncMode = false;
                        uiMode = 1;
                        break;
                    default:
                        break;
                }
            }

            if (syncMode == true){
                syncFile();
            }
                
        }

    }

    public static int serverPing(){
        try (Socket s = new Socket("localhost", 8000)) {
            s.close();
            return 8000;
        } catch (IOException ex) {
            /* ignore */
        }
        try (Socket s = new Socket("localhost", 8001)) {
            s.close();
            return 8001;
        } catch (IOException ex) {
            /* ignore */
        }
        return -1;
    }

    public static void registerNewAccount(){
        int port = serverPing();
        if (port == -1){
            System.out.println("no server available");
        }
        try(
            Socket socket = new Socket("localhost", port);
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
        ){
            outToServer.writeUTF("register");
            outToServer.flush();
            outToServer.close();
            socket.close();
        }
        catch (Exception e){
        }

    }
   
    public static boolean loginAccount(String username, String pass){
        int port = serverPing();
        if (port == -1){
            System.out.println("no server available");
        }
        try(
            Socket socket = new Socket("localhost", port);
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
        ){
            outToServer.writeUTF("login");
            outToServer.flush();
            outToServer.close();
            socket.close();
        }
        catch (Exception e){
        }
        return false;
    }

    public static void syncFile(){
        int port = serverPing();
        if (port == -1){
            System.out.println("no server available");
        }
        try(
            Socket socket = new Socket("localhost", port);
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
        ){
            outToServer.writeUTF("sync");
            outToServer.flush();
            outToServer.close();
            socket.close();
        }
        catch (Exception e){
        }

    };

    public static void downloadFile(){
        int port = serverPing();
        if (port == -1){
            System.out.println("no server available");
        }
        try(
            Socket socket = new Socket("localhost", port);
            DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
        ){
            outToServer.writeUTF("download");
            outToServer.flush();
            outToServer.close();
            socket.close();
        }
        catch (Exception e){
        }
    }

}


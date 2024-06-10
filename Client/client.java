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

    public static void registerNewAccount(){

    }
   
    public static boolean loginAccount(String username, String pass){
        System.out.println(username + " " + pass);
        if (username.equals("jack")){
            return true;
        }
        return false;
    }

    public static void syncFile(){

    };

    public static void downloadFile(){

    }

}


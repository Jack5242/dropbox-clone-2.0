import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class client{
    public static Scanner KeyboardInput = new Scanner(System.in);

    int portPrimaryServer = 8000;
    int portSecondaryServer = 8001;
    static boolean syncMode = false;
    static String username = "";
    static String pass = "";
    public static void main(String [] args){
        Thread syncThread = new Thread(new Runnable() {
            @Override
            public void run() {
                syncFunction();
            }
        });

        syncThread.start();
        
        //1: login screen
        //2: syncing screen
        int uiMode = 1;
        
        

        // initiate connection to server
        while (true){
            

            if (uiMode == 1){
                System.out.println("choose your option: \n1. register\n2. login\n3. exit");
                int userInput = KeyboardInput.nextInt();
                KeyboardInput.nextLine();

                switch (userInput) {
                    case 1:
                        System.out.println("enter username:");
                        username = KeyboardInput.nextLine();

                        System.out.println("enter pass:");
                        pass = KeyboardInput.nextLine();
                        registerNewAccount(username, pass);
                        break;
                    case 2:
                        System.out.println("enter username:");
                        username = KeyboardInput.nextLine();

                        System.out.println("enter pass:");
                        pass = KeyboardInput.nextLine();

                        if (loginAccount(username, pass) == true){
                            File userFolder = new File(username);
                            if (!userFolder.exists()){
                                try{
                                    userFolder.mkdir();
                                }
                                catch(Exception e){
                                    System.out.println("fail to create folder");
                                }
                            }
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
        }

    }

    public static int serverPing(){
        try (Socket s = new Socket("localhost", 8000)) {
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF("ping");
            dos.flush();
            dos.close();
            s.close();
            return 8000;
        } catch (IOException ex) {
            /* ignore */
        }
        try (Socket s = new Socket("localhost", 8001)) {
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            dos.writeUTF("ping");
            dos.flush();
            dos.close();
            s.close();
            return 8001;
        } catch (IOException ex) {
            /* ignore */
        }
        return -1;
    }

    public static void registerNewAccount(String username, String pass){
        int port = serverPing();
        if (port == -1){
            System.out.println("no server available");
            return;
        }
        try(
            Socket socket = new Socket("localhost", 8000);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        ){
            dos.writeUTF("register");
            dos.flush();
            dos.writeUTF(username);
            dos.flush();
            dos.writeUTF(pass);
            dos.flush();
            dos.close();
            socket.close();
        }
        catch (Exception e){
        }
    }
   
    public static boolean loginAccount(String username, String pass){
        int port = serverPing();
        boolean result = false;
        if (port == -1){
            System.out.println("no server available");
            return false;
        }
        try(
            Socket socket = new Socket("localhost", port);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
        ){
            dos.writeUTF("login");
            dos.flush();
            dos.flush();
            dos.writeUTF(username);
            dos.flush();
            dos.writeUTF(pass);
            dos.flush();
            result = dis.readBoolean();
            dis.close();
            dos.close();
            socket.close();
            return result;
        }
        catch (Exception e){
            return result;
        }
    }

    public static void syncFunction(){
        while(true){
            if(syncMode == true){
                syncFile();
            }
            
            try{
                Thread.sleep(1000);
            }
            catch(InterruptedException e){
            }
        }
    }

    private static void sendDirectory(File folder, DataOutputStream dos, int rootPathLength) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                sendDirectory(file, dos, rootPathLength);
            } else {
                sendFile(file, dos, rootPathLength);
            }
        }
    }

    private static int countFiles(File folder, int rootPathLength) {
        File[] files = folder.listFiles();
        if (files == null) {
            return 0;
        }

        int count = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                count += countFiles(file, rootPathLength);
            } else {
                count++;
            }
        }
        return count;
    }

    private static void sendFile(File file, DataOutputStream dos, int rootPathLength) throws IOException {
        String relativePath = file.getAbsolutePath().substring(rootPathLength);
        dos.writeUTF(relativePath);
        dos.writeLong(file.length());

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }

        System.out.println("Sent file: " + file.getAbsolutePath());
    }


    public static void syncFile(){
        int port = serverPing();
        File folder = new File(username);

        if (port == -1){
            System.out.println("no server available");
            return;
        }
        try(
            Socket socket = new Socket("localhost", port);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        ){
            dos.writeUTF("sync");
            dos.flush();
            dos.writeUTF(folder.getName());
            dos.flush();
            dos.writeInt(countFiles(folder, folder.getAbsolutePath().length() + 1));
            dos.flush();
            sendDirectory(folder, dos, folder.getAbsolutePath().length() + 1);
            dos.flush();
            dos.close();
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
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        ){
            dos.writeUTF("download");
            dos.flush();
            dos.close();
            socket.close();
        }
        catch (Exception e){
        }
    }

}


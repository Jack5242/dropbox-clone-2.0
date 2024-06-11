import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.List;
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
                    //register new account
                    case 1:
                        System.out.println("enter username:");
                        username = KeyboardInput.nextLine();

                        System.out.println("enter pass:");
                        pass = KeyboardInput.nextLine();
                        registerNewAccount(username, pass);
                        break;
                        //login to account
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
                        else {
                            System.out.println("login failed");
                        }
                        break;
                        //close client
                    case 3:
                    System.out.println("exit program");
                        System.exit(0);
                        break;
                
                    default:
                        System.out.println("Please enter again");
                        break;
                }
            }

            if (uiMode == 2){
                System.out.println("choose your option: \n1. sync file: " + syncMode + "\n2. download\n3. see server files\n4. delete file\n5. logout");
                int userInput = KeyboardInput.nextInt();
                KeyboardInput.nextLine();
                switch (userInput) {
                    //turn on/off sync mode
                    case 1:
                        syncMode = !syncMode;
                        break;
                        //download file from server
                    case 2: 
                        downloadFile();
                        break;
                        //logout
                    
                    case 3:
                        seeFile();
                        break;

                    case 4: 
                        deleteFile();
                        break;
                    case 5: 
                        syncMode = false;
                        uiMode = 1;
                        break;

                    default:
                        System.out.println("Please enter again");
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
            Socket socket = new Socket("localhost", port);
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
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        ){
            syncMode = false;

            dos.writeUTF("download");
            dos.flush();
            dos.writeUTF(username);
            dos.flush();
            dos.writeUTF(pass);
            dos.flush();

            receiveDirectory(dis);

            syncMode = true;
            dos.close();
            dis.close();
            socket.close();
        }
        catch (Exception e){
        }
    }

    public static void receiveDirectory(DataInputStream dis) throws IOException {
        String rootFolderName = dis.readUTF(); // Read the root folder name
        int numberOfFiles = dis.readInt(); // Read the number of files

        for (int i = 0; i < numberOfFiles; i++) {
            String relativeFilePath = dis.readUTF();
            long fileSize = dis.readLong();

            File file = new File(rootFolderName, relativeFilePath);
            file.getParentFile().mkdirs(); // Create parent directories if they don't exist

            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    fileSize -= bytesRead;
                }
            }

            System.out.println("Received file: " + file.getAbsolutePath());
        }
    }

    @SuppressWarnings("unchecked")
    public static void seeFile(){
        int port = serverPing();
        if (port == -1){
            System.out.println("no server available");
            return;
        }
        try(
            Socket socket = new Socket("localhost", port);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
        ){
            dos.writeUTF("seefile");
            dos.flush();
            dos.writeUTF(username);
            dos.flush();

            int length = dis.readInt();
            byte[] dirBytes = new byte[length];

            // Read the byte array
            dis.readFully(dirBytes);

            // Deserialize the byte array to an Account object
            ByteArrayInputStream bis = new ByteArrayInputStream(dirBytes);
            ObjectInputStream ois = new ObjectInputStream(bis);

            List<String> directoryStructure = (List<String>) ois.readObject();
            
            for (String line : directoryStructure) {
                System.out.println(line);
            }

            dos.close();
            ois.close();
            socket.close();
        }
        catch (Exception e){
            System.out.println("see file error");
        }
    }

    public static void deleteFile(){
        int port = serverPing();
        if (port == -1){
            System.out.println("no server available");
        }
        try(
            Socket socket = new Socket("localhost", port);
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        ){
            Scanner keyboard = new Scanner (System.in);
            dos.writeUTF("delete");
            dos.flush();
            dos.writeUTF(username);
            dos.flush();
            
            System.out.print("enter file dir you want to delete: ");
            String path = keyboard.nextLine();
            dos.writeUTF(path);
            dos.flush();

            
            dos.close();
            dis.close();
            socket.close();
        }
        catch (Exception e){
        }
    }
}


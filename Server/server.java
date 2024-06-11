import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.File;
import java.util.Scanner;


public class server {
    public static void main(String [] args){
        Scanner scanner = new Scanner(System.in);
        System.out.print("Server operate on port? : ");
        int port = scanner.nextInt();
		
        ServerSocket welcomeSocket = null;
        try{
            welcomeSocket = new ServerSocket(port);
        }
        catch (IOException e){}
        
		while(true){
			
			Implementation request = new Implementation(welcomeSocket);
			
			Thread thread = new Thread(request);
			
			thread.start();
		}

	}
}


final class Implementation implements Runnable{
    static ServerSocket welcomeSocket;
    static Hashtable<String,String> accountInfo = new Hashtable<String, String>();
    static String accountInfoFileName = "acc.ser";

    public Implementation(ServerSocket welcomeSocket){
        this.welcomeSocket = welcomeSocket;
        //open account information file
        File accountInfoFile = new File(accountInfoFileName);
		if (!accountInfoFile.exists())
		{
			saveHashtableToFile(accountInfo, accountInfoFileName);
		}
        accountInfo = loadHashtableFromFile(accountInfoFileName);
    }

    @Override
    public void run(){
        try{
            processRequest();
        }
        catch(Exception e){
            System.out.println("cannot process request");
        }
    }

    public static void processRequest(){
        String option = "";
        try{
            Socket ClientSocket = welcomeSocket.accept();
            DataInputStream dis = new DataInputStream(ClientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(ClientSocket.getOutputStream());

            String username = "";
            String pass = "";

            option = dis.readUTF();
            System.out.println(option);
            
            if (option.equals("register")){
                System.out.print("register function");
                username = dis.readUTF();
                pass = dis.readUTF();
                System.out.print(username+pass);
                registerAccount(username, pass);
            }

            if (option.equals("login")){
                username = dis.readUTF();
                pass = dis.readUTF();
                System.out.print(username+pass);
                dos.writeBoolean(loginAccount(username, pass));
                dos.flush();
            }

            if (option.equals("sync")){
                syncFile(dis);
            }

            if (option.equals("download")){
                username = dis.readUTF();
                pass = dis.readUTF();

                if(loginAccount(username, pass)){
                    File folder = new File(username);
                    if (!folder.exists())
                    {
                        folder.mkdirs();
                    }
                    dos.writeUTF(folder.getName());
                    dos.writeInt(countFiles(folder, folder.getAbsolutePath().length() + 1));

                    sendDirectory(folder, dos, folder.getAbsolutePath().length() + 1);
                }
                
            }


            //close connection
            dis.close();
            dos.close();
            ClientSocket.close();
        }
        catch(Exception e){
            System.out.println("somethings wrong");
        }
    }
	
    public static void saveHashtableToFile(Hashtable<String, String> hashtable, String filename) {
        try (FileOutputStream fileOut = new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(hashtable);
            out.flush();
            out.close();
            fileOut.close();
            // System.out.println("Serialized data is saved in " + filename);
        } catch (EOFException e) {
            //this is normal
        } catch(IOException i){
            // i.printStackTrace();
        }
    }

	@SuppressWarnings("unchecked")
	public static Hashtable<String, String> loadHashtableFromFile(String filename) {
        Hashtable<String, String> hashtable = null;
        try (FileInputStream fileIn = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(fileIn)) {
            hashtable = (Hashtable<String, String>) in.readObject();
            fileIn.close();
            in.close();
            // System.out.println("Serialized data is loaded from " + filename);
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
        }
        return hashtable;
    }

    public static void registerAccount(String username, String pass){
        accountInfo.put(username, pass);
        saveHashtableToFile(accountInfo, accountInfoFileName);
        

        File userFolder = new File(username);
        if (!userFolder.exists()){
            try{
                userFolder.mkdir();
            }
            catch(Exception e){
                System.out.println("fail to create folder");
            }
        }

    }

    public static boolean loginAccount(String username, String pass){
        String value = "";
        if (!accountInfo.contains(username)){
            value = accountInfo.get(username);
            if (value.equals(pass)){
                return true;
            }
        }
        return false;
    }

    public static void syncFile(DataInputStream dis) throws IOException {
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

        // System.out.println("Sent file: " + file.getAbsolutePath());
    }
    
}
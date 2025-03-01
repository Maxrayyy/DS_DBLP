import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class serverInit {
    private static String DBLP_Path = "/hzd/dblpXmls";
    private static String DBLP_Backup_Path = "/hzd/dblpBackupXmls";

    public static void receiveXml(int portSelected) throws Exception{
        try {
            System.out.println("正在启动文件接收服务器，端口: " + portSelected);
            // 创建ServerSocket对象
            ServerSocket serverSocket = new ServerSocket(portSelected);
            System.out.println("文件接收服务器启动成功，端口: " + portSelected);

            while (true) {
                Socket socket = null;
                DataInputStream inputStream = null;
                FileOutputStream fileOutputStream = null;
                
                try {
                    System.out.println("\n等待客户端连接...");
                    socket = serverSocket.accept();
                    System.out.println("收到客户端连接: " + socket.getInetAddress() + ":" + socket.getPort());

                    inputStream = new DataInputStream(socket.getInputStream());

                    // 设置读取超时
                    socket.setSoTimeout(30000); // 30秒超时

                    // 接收文件名信息
                    String fileName = inputStream.readUTF();
                    System.out.println("接收到文件名: " + fileName);

                    // 接收备份标识
                    String backupTag = inputStream.readUTF();
                    System.out.println("接收到备份标识: " + backupTag);

                    // 确定文件路径
                    String filePath = backupTag.equals("isBackup") 
                        ? DBLP_Backup_Path + "/" + portSelected + "/" + fileName
                        : DBLP_Path + "/" + portSelected + "/" + fileName;
                    
                    System.out.println("准备写入文件: " + filePath);
                    
                    // 确保目录存在
                    new File(filePath).getParentFile().mkdirs();

                    fileOutputStream = new FileOutputStream(filePath);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    long totalBytes = 0;
                    
                    System.out.println("开始接收文件内容...");
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                    }
                    System.out.println("文件接收完成，总大小: " + totalBytes + " bytes");

                } catch (EOFException e) {
                    System.out.println("客户端连接断开: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("IO错误: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // 关闭所有资源
                    try {
                        if (fileOutputStream != null) fileOutputStream.close();
                        if (inputStream != null) inputStream.close();
                        if (socket != null) socket.close();
                        System.out.println("连接已关闭");
                    } catch (IOException e) {
                        System.out.println("关闭资源时发生错误: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("服务器启动失败: " + e.getMessage());
            throw e;
        }
    }
}
import java.io.*;
import java.net.Socket;

import config.ServerConfig;

// 客户端访问服务端
public class accessServer {
    private final static ServerConfig config = new ServerConfig();

    // 向指定服务端发送查询请求，参数为姓名与年份
    public static int sendQuery(String name, String beginYear, String endYear,int ipSelected,int portSelected,boolean isBackup,boolean useIndex){
        int num;
        String serverAddress = config.getServerIps()[ipSelected];
        int queryPort = config.getServerPorts()[portSelected] + 100;
        
        try {
            System.out.println("\n尝试连接服务器: " + serverAddress + ":" + queryPort);
            //创建Socket链接
            Socket socket = new Socket(serverAddress, queryPort);
            System.out.println("成功连接到服务器");
            
            DataInputStream is = new DataInputStream(socket.getInputStream());
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());

            try {
                //向Server传递isBackup是否要查询备份文件
                System.out.println("发送查询参数 - isBackup: " + isBackup);
                os.writeUTF(isBackup ? "true" : "false");
                os.flush();

                // 发送姓名
                System.out.println("发送查询参数 - name: " + name);
                os.writeUTF(name);
                os.flush();

                // 发送起始年份
                System.out.println("发送查询参数 - beginYear: " + beginYear);
                os.writeUTF(beginYear);
                os.flush();

                // 发送截止年份
                System.out.println("发送查询参数 - endYear: " + endYear);
                os.writeUTF(endYear);
                os.flush();

                // 是否使用索引
                System.out.println("发送查询参数 - useIndex: " + useIndex);
                os.writeUTF(useIndex ? "true" : "false");
                os.flush();

                //接收服务端的查询信息
                System.out.println("等待服务器响应...");
                String queryResult = is.readUTF();
                System.out.println("收到查询结果: " + queryResult);
                
                num = Integer.parseInt(queryResult);

            } catch (IOException e) {
                System.out.println("与服务器通信时发生错误: " + e.getMessage());
                return -1;
            } finally {
                System.out.println("关闭连接...");
                is.close();
                os.close();
                socket.close();
            }

        } catch (IOException e) {
            System.out.println("连接服务器失败: " + serverAddress + ":" + queryPort);
            System.out.println("错误信息: " + e.getMessage());
            return -1;
        }
        return num;
    }
}
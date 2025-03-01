import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class queryServer {
    private int port;
    // 每台服务器应该存放的xml文件数量
    private final int xmlProperNum=4;
    private static final String LOG_FILE = "2251760-hw3-q1.log";  // 修改为你的学号
    private FileWriter logWriter;
    
    public queryServer(int portID){
        this.port=portID+100;
        try {
            // 创建或追加到日志文件
            logWriter = new FileWriter(LOG_FILE, true);
        } catch (IOException e) {
            System.out.println("创建日志文件失败: " + e.getMessage());
        }
        // 构建索引
        String xmlPath = "/hzd/dblpXmls/" + (port-100) + "/";
        
        // 初始化索引路径
        IndexQuery.init(port-100);
        
        File dir = new File(xmlPath);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".xml")) {
                    System.out.println("正在为文件构建索引: " + file.getName());
                    IndexQuery.buildIndex(file.getAbsolutePath());
                }
            }
        }
    }
    
    private void logQueryTime(long queryTime) {
        try {
            logWriter.write(String.format("查询耗时: %d ms\n", queryTime));
            logWriter.flush();
        } catch (IOException e) {
            System.out.println("写入日志失败: " + e.getMessage());
        }
    }
    
    public void receiveQuery() {
        try {
            System.out.println("正在启动查询服务器，端口: " + port);
            ServerSocket server = new ServerSocket(port);
            System.out.println("查询服务器启动成功，等待连接...");

            while (true) {
                System.out.println("等待查询请求...");
                Socket socket = server.accept();
                System.out.println("收到查询请求，来自: " + socket.getInetAddress() + ":" + socket.getPort());

                DataInputStream is = new DataInputStream(socket.getInputStream());
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());

                //接收来自客户端的是否查询备份的信息
                String isBackup = is.readUTF();
                System.out.println("接收到查询参数 - isBackup: " + isBackup);
                
                String name = is.readUTF();
                System.out.println("接收到查询参数 - name: " + name);
                
                String beginYear = is.readUTF();
                System.out.println("接收到查询参数 - beginYear: " + beginYear);
                
                String endYear = is.readUTF();
                System.out.println("接收到查询参数 - endYear: " + endYear);
                
                String useIndex = is.readUTF();
                System.out.println("接收到查询参数 - useIndex: " + useIndex);

                query query=new query(port);
                if (name.length()>0) {
                    if(useIndex.equals("true")){
                        long startTime = System.currentTimeMillis();
                        String queryResult = IndexQuery.query(name, beginYear, endYear);
                        long endTime = System.currentTimeMillis();
                        long queryTime = endTime - startTime;
                        System.out.println("索引查询耗时: " + queryTime + "ms");
                        logQueryTime(queryTime);  // 记录到日志
                        os.writeUTF(queryResult);
                    }
                    else {
                        //向客户端发送查询结果信息
                        long startTime = System.currentTimeMillis();
                        String queryResult = query.queryByNameAndYear(name, beginYear, endYear, isBackup);
                        long endTime = System.currentTimeMillis();
                        long queryTime = endTime - startTime;
                        System.out.println("全文查询耗时: " + queryTime + "ms");
                        logQueryTime(queryTime);  // 记录到日志
                        os.writeUTF(queryResult);
                        os.flush();
                    }
                }

                System.out.println("查询处理完成，关闭连接\n");
                is.close();
                os.close();
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("查询服务器发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                logWriter.close();
            } catch (IOException e) {
                System.out.println("关闭日志文件失败: " + e.getMessage());
            }
        }
    }
}
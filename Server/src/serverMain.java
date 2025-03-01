import java.util.Scanner;
public class serverMain {
//    private final static String[] ipList = new String[]
//            { "139.196.149.61", "106.14.15.207","110.40.201.71"};
    private final static int[] portList = new int[]
            {8200,8201};

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int portSelected = -1;

        while (portSelected == -1) {
            System.out.println("请输入0/1选择虚拟机端口：0--8200, 1--8201");
            String portStr = sc.nextLine();
            portSelected = Integer.parseInt(portStr);
            if ((portSelected != 0) && (portSelected != 1)) {
                portSelected = -1;
                System.out.println("请重新输入");
            }
        }

        int basePort = portList[portSelected];
        
        // 创建查询虚拟机线程: 使用basePort + 100作为查询端口
        queryServer vs = new queryServer(basePort);  // queryServer内部会加100
        Thread queryThread = new Thread(() -> vs.receiveQuery());
        queryThread.start();
        System.out.println("查询服务已启动，监听端口: " + (basePort + 100));

        // 创建文件接收线程: 使用basePort作为文件接收端口
        serverInit in = new serverInit();
        Thread receiveThread = new Thread(() -> {
            try {
                in.receiveXml(basePort);
            } catch (Exception e) {
                System.out.println("文件接收服务出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
        receiveThread.start();
        System.out.println("文件接收服务已启动，监听端口: " + basePort);
    }
}
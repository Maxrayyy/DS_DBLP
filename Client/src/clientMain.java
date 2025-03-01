import config.ServerConfig;

import java.util.Scanner;

public class clientMain {
    private final static ServerConfig config = new ServerConfig();

    // 多线程向服务器发送查询请求（主文件）
    public static void sendQuerys(int[] numWithYear,String name,String beginYear,String endYear,boolean useIndex){
        System.out.println("\n开始向主节点发送查询请求...");
        
        // 每个服务器两个端口，总共3*2=6个节点
        Thread[] threads = new Thread[6];
        threads[0] = new Thread(() -> numWithYear[0] = accessServer.sendQuery(name, beginYear, endYear,0,0,false,useIndex));
        threads[1] = new Thread(() -> numWithYear[1] = accessServer.sendQuery(name, beginYear, endYear,0,1,false,useIndex));
        threads[2] = new Thread(() -> numWithYear[2] = accessServer.sendQuery(name, beginYear, endYear,1,0,false,useIndex));
        threads[3] = new Thread(() -> numWithYear[3] = accessServer.sendQuery(name, beginYear, endYear,1,1,false,useIndex));
        threads[4] = new Thread(() -> numWithYear[4] = accessServer.sendQuery(name, beginYear, endYear,2,0,false,useIndex));
        threads[5] = new Thread(() -> numWithYear[5] = accessServer.sendQuery(name, beginYear, endYear,2,1,false,useIndex));

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("线程执行被中断: " + e.getMessage());
            }
        }
    }

    // 多线程向服务器发送查询请求（备份文件）
    public static void sendBackupQuerys(int[] numWithYear,String name,String beginYear,String endYear,boolean useIndex){
        System.out.println("\n开始查询备份节点...");
        
        // 创建查询备份的线程
        Thread thread1 = new Thread(new Runnable() {
            public void run() {
                numWithYear[0] = accessServer.sendQuery(name, beginYear, endYear,0,1,true,useIndex);
            }
        });
        Thread thread2 = new Thread(new Runnable() {
            public void run() {
                numWithYear[1] = accessServer.sendQuery(name, beginYear, endYear,1,0,true,useIndex);
            }
        });
        Thread thread3 = new Thread(new Runnable() {
            public void run() {
                numWithYear[2] = accessServer.sendQuery(name, beginYear, endYear,1,1,true,useIndex);
            }
        });
        Thread thread4 = new Thread(new Runnable() {
            public void run() {
                numWithYear[3] = accessServer.sendQuery(name, beginYear, endYear,2,0,true,useIndex);
            }
        });
        Thread thread5 = new Thread(new Runnable() {
            public void run() {
                numWithYear[4] = accessServer.sendQuery(name, beginYear, endYear,2,1,true,useIndex);
            }
        });
        Thread thread6 = new Thread(new Runnable() {
            public void run() {
                numWithYear[5] = accessServer.sendQuery(name, beginYear, endYear,0,0,true,useIndex);
            }
        });

        boolean hasFailedNodes = false;

        // 检查并启动备份查询线程
        if(numWithYear[0]==-1) {
            System.out.println("\n节点1 (IP:" + config.getServerIps()[0] + ", Port:" + config.getServerPorts()[0] + ") 故障");
            System.out.println("正在查询节点1的备份数据...");
            thread1.start();
            hasFailedNodes = true;
        }
        if(numWithYear[1]==-1){
            System.out.println("\n节点2 (IP:" + config.getServerIps()[0] + ", Port:" + config.getServerPorts()[1] + ") 故障");
            System.out.println("正在查询节点2的备份数据...");
            thread2.start();
            hasFailedNodes = true;
        }
        if(numWithYear[2]==-1) {
            System.out.println("\n节点3 (IP:" + config.getServerIps()[1] + ", Port:" + config.getServerPorts()[0] + ") 故障");
            System.out.println("正在查询节点3的备份数据...");
            thread3.start();
            hasFailedNodes = true;
        }
        if(numWithYear[3]==-1){
            System.out.println("\n节点4 (IP:" + config.getServerIps()[1] + ", Port:" + config.getServerPorts()[1] + ") 故障");
            System.out.println("正在查询节点4的备份数据...");
            thread4.start();
            hasFailedNodes = true;
        }
        if(numWithYear[4]==-1) {
            System.out.println("\n节点5 (IP:" + config.getServerIps()[2] + ", Port:" + config.getServerPorts()[0] + ") 故障");
            System.out.println("正在查询节点5的备份数据...");
            thread5.start();
            hasFailedNodes = true;
        }
        if(numWithYear[5]==-1) {
            System.out.println("\n节点6 (IP:" + config.getServerIps()[2] + ", Port:" + config.getServerPorts()[1] + ") 故障");
            System.out.println("正在查询节点6的备份数据...");
            thread6.start();
            hasFailedNodes = true;
        }

        if (!hasFailedNodes) {
            System.out.println("所有主节点正常，无需查询备份");
            return;
        }

        // 等待备份查询完成
        try {
            if(thread1.isAlive()) thread1.join();
            if(thread2.isAlive()) thread2.join();
            if(thread3.isAlive()) thread3.join();
            if(thread4.isAlive()) thread4.join();
            if(thread5.isAlive()) thread5.join();
            if(thread6.isAlive()) thread6.join();
            System.out.println("备份查询完成");
        } catch (Exception e) {
            System.out.println("备份查询线程执行出错: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while(true) {
            System.out.println("\n======== DBLP分布式查询系统 ========");
            System.out.println("是否要初始化DBLP分布式存储: 输入yes/no");
            String str=sc.nextLine();
            if(str.equals("yes")){
                try {
                    // 执行初始化流程
                    xmlProcess.initDBLP();
                    System.out.println("DBLP分布式存储初始化完成");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            }
            else if(str.equals(("no"))){
                break;
            }
            else{
                System.out.println("输入不合法，请重新输入");
            }
        }
        while(true) {
            System.out.println("请输入作者姓名 (输入exit退出):");
            String name = sc.nextLine();
            if (name.equals("exit")) {
                System.out.println("程序退出");
                return;
            }

            System.out.println("\n请依次输入起始年份和截至年份,若不指定起始/截至年份，则输入*");
            System.out.println("请输入起始年份:");
            String beginYear = sc.nextLine();
            System.out.println("请输入截至年份:");
            String endYear = sc.nextLine();

            // 1. 不使用索引的查询
            System.out.println("\n开始全文查询...");
            long startTime = System.currentTimeMillis();
            int[] numWithYear = {-2, -2, -2, -2, -2, -2};
            int numAll = 0;

            sendQuerys(numWithYear,name,beginYear,endYear,false);

            // 检查节点状态
            boolean hasFailedNodes = false;
            for(int i = 0; i < 6; i++) {
                if(numWithYear[i] == -1) {
                    hasFailedNodes = true;
                    break;
                }
            }

            if(hasFailedNodes) {
                System.out.println("\n检测到节点故障，启动容错机制...");
                sendBackupQuerys(numWithYear,name,beginYear,endYear,false);
            }

            // 统计结果
            for(int i = 0; i < 6; i++) {
                if(numWithYear[i] > 0) {
                    numAll += numWithYear[i];
                    System.out.println("节点" + (i+1) + "查询结果: " + numWithYear[i]);
                }
            }

            long endTime = System.currentTimeMillis();
            System.out.println("\n=== 全文查询完成 ===");
            System.out.println("查询用时：" + (double)(endTime - startTime)/1000 + "s");
            System.out.println("作者 " + name + " 在" + beginYear + "到" + endYear + "年间的论文总数: " + numAll);

            // 2. 使用索引的查询
            System.out.println("\n开始索引查询...");
            startTime = System.currentTimeMillis();
            numWithYear = new int[]{-2, -2, -2, -2, -2, -2};
            numAll = 0;

            sendQuerys(numWithYear,name,beginYear,endYear,true);

            // 检查节点状态
            hasFailedNodes = false;
            for(int i = 0; i < 6; i++) {
                if(numWithYear[i] == -1) {
                    hasFailedNodes = true;
                    break;
                }
            }

            if(hasFailedNodes) {
                System.out.println("\n检测到节点故障，启动容错机制...");
                sendBackupQuerys(numWithYear,name,beginYear,endYear,true);
            }

            // 统计结果
            for(int i = 0; i < 6; i++) {
                if(numWithYear[i] > 0) {
                    numAll += numWithYear[i];
                    System.out.println("节点" + (i+1) + "查询结果: " + numWithYear[i]);
                }
            }

            endTime = System.currentTimeMillis();
            System.out.println("\n=== 索引查询完成 ===");
            System.out.println("查询用时：" + (double)(endTime - startTime)/1000 + "s");
            System.out.println("作者 " + name + " 在" + beginYear + "到" + endYear + "年间的论文总数: " + numAll);
            
            System.out.println("\n----------------------------------------");
        }
    }
}
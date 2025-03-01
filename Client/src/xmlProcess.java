import javax.xml.stream.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import config.ServerConfig;
import java.net.InetSocketAddress;

public class xmlProcess {
    private final static ServerConfig config = new ServerConfig();

//    public xmlProcess() {
//        this.config = new ServerConfig(); // 使用默认配置
//    }
//
//    public xmlProcess(ServerConfig config) {
//        this.config = config;
//    }

    public static void SplitXml(String inputPath, String outputDir) throws Exception {
        // 按块拆分的大类标签
        Set<String> set = new HashSet<>();
        set.add("article");
        set.add("book");
        set.add("inproceedings");
        set.add("proceedings");
        set.add("incollection");
        set.add("phdthesis");
        set.add("mastersthesis");
        set.add("www");
        set.add("data");

        // 确保输出目录存在
        new File(outputDir).mkdirs();

        // 创建XMLInputFactory和XMLStreamReader
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = inputFactory.createXMLStreamReader(new FileReader(inputPath));

        XMLStreamWriter currentWriter = null;
        List<XMLStreamWriter> writers = new ArrayList<>();
        Random random = new Random(10);

        // 逐行读取XML文件
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    String startElement = reader.getLocalName();
                    if ("dblp".equals(startElement)) {
                        System.out.println("开始切分dblp标签");
                        for (int i = 0; i < 24; i++) {
                            String currentFile = outputDir + "/dblp" + i + ".xml";
                            FileWriter fileWriter = new FileWriter(currentFile);
                            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(fileWriter);
                            writers.add(writer);
                            writer.writeStartDocument();
                            writer.writeStartElement("dblp");
                        }
                        currentWriter = writers.get(0);
                    } else if (set.contains(startElement)) {
                        currentWriter = writers.get(random.nextInt(24));
                        currentWriter.writeStartElement(startElement);
                    } else {
                        currentWriter.writeStartElement(startElement);
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            currentWriter.writeAttribute(
                                    reader.getAttributeLocalName(i),
                                    reader.getAttributeValue(i)
                            );
                        }
                    }
                    break;

                case XMLStreamConstants.CHARACTERS:
                    String text = reader.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        currentWriter.writeCharacters(text);
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    String endElement = reader.getLocalName();
                    if ("dblp".equals(endElement)) {
                        System.out.println("完成切分dblp标签");
                        for (XMLStreamWriter writer : writers) {
                            writer.writeEndElement();
                            writer.writeEndDocument();
                            writer.close();
                        }
                        currentWriter = null;
                    } else {
                        currentWriter.writeEndElement();
                    }
                    break;
            }
        }
        reader.close();
        System.out.println("DBLP.xml文件切分完成");
    }

    public static void sendXml(String fileName, String ipSelected, int portSelected, boolean isBackup) throws Exception {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // 创建Socket对象
                Socket socket = new Socket(ipSelected, portSelected);
                // 创建输出流
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

                // 向Server传递文件名称
                outputStream.writeUTF(fileName);
                outputStream.flush();
                System.out.println("Send:"+fileName);

                // 向Server传递是否为备份文件的信息
                String backupTag;
                if (isBackup == true) {
                    backupTag = "isBackup";
                } else {
                    backupTag = "notBackup";
                }
                outputStream.writeUTF(backupTag);
                outputStream.flush();
                System.out.println("Send:"+backupTag);

                // 文件路径
                String filePath = "data/splitedXmls/" + fileName;

                // 读取文件并将文件内容写入输出流
                File file = new File(filePath);
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int bytesRead=0;
                System.out.println("Send:发送文件....");
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                // 关闭文件输入流和Socket输出流
                fileInputStream.close();
                outputStream.close();
                socket.close();
                return; // 成功发送后返回
            } catch (IOException e) {
                retryCount++;
                if (retryCount == maxRetries) {
                    throw e;
                }
                Thread.sleep(1000); // 等待1秒后重试
            }
        }
    }

    public static boolean testConnection(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 3000); // 3秒超时
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void initDBLP() {
        try {
            xmlProcess in = new xmlProcess();
            
            // 首先测试所有服务器连接
            System.out.println("测试服务器连接...");
            for (int i = 0; i < config.getServerIps().length; i++) {
                String ip = config.getServerIps()[i];
                int port = config.getServerPorts()[i];
                if (!testConnection(ip, port)) {
                    throw new RuntimeException("无法连接到服务器: " + ip + ":" + port + 
                        "\n请确保:\n1. 服务器程序已启动\n2. 防火墙已开放端口\n3. IP和端口配置正确");
                }
            }
            
            // 继续执行原有逻辑
            SplitXml(config.getInputXmlPath(), config.getOutputDir());
            
            for(int i=0; i<24; i++) {
                String fileName="dblp"+i+".xml";
                System.out.println("发送"+fileName+"文件");
                // 虚拟机分配顺序：服务器0-端口0，服务器0-端口1，服务器1-端口0....
                // ip序号
                int ipSelect=(i/2)%3;
                // port序号
                int portSelect=i%2;
                // 备份文件的ip序号
                int ipBackupSelect=-1;
                // 备份文件的port序号
                int portBackupSelect=-1;
                if(portSelect==0){
                    ipBackupSelect=ipSelect;
                    portBackupSelect=1;
                }
                else{
                    ipBackupSelect=(ipSelect+1)%3;
                    portBackupSelect=0;
                }
                // 发送xml文件（正式版本）
                System.out.println(fileName+"发送至："+config.getServerIps()[ipSelect]+":"+config.getServerPorts()[portSelect]);
                in.sendXml(fileName, config.getServerIps()[ipSelect], config.getServerPorts()[portSelect], false);
                System.out.println(fileName+"发送至："+config.getServerIps()[ipSelect]+":"+config.getServerPorts()[portSelect]);
                // 发送xml文件（备份版本）：规则为虚拟机序号+1
                in.sendXml(fileName, config.getServerIps()[ipBackupSelect], config.getServerPorts()[portBackupSelect], true);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

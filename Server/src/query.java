import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import java.io.File;

public class query {
    // 当前虚拟机端口
    private static int port;
    // dblp.xml正式块路径
    private static String DBLP_Path;
    // dblp.xml备份块路径
    private static String DBLP_Backup_Path;
    // 当前虚拟机下存储的文件块
    private static ArrayList<String> dblpNames=new ArrayList<String>();
    private static ArrayList<String> dblpBackupNames=new ArrayList<String>();

    query(int portSelected){
        port=portSelected-100;
        DBLP_Path = "/hzd/dblpXmls/"+port;
        DBLP_Backup_Path = "/hzd/dblpBackupXmls/"+port;

        dblpNames.clear();
        dblpBackupNames.clear();

        // 获取正式dblp文件块的名称
        File dir = new File(DBLP_Path);

        File[] xmlFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });

        // 获取xml文件的名称
        for (File xmlFile : xmlFiles) {
            dblpNames.add(xmlFile.getName());
        }

        // 获取备份dblp文件块的名称
        dir = new File(DBLP_Backup_Path);

        File[] xmlFilesBackup = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });

        // 获取xml文件的名称
        for (File xmlFile : xmlFilesBackup) {
            dblpBackupNames.add(xmlFile.getName());
        }
    }

    public static String exeCmd(String commandStr) {
        //执行Linux的Cmd命令
        String result = null;
        try {
            String[] cmd = new String[]{"/bin/sh", "-c", commandStr};
            Process ps = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                //执行结果加上回车
                sb.append(line);
            }
            result = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String queryByName(String name,String isBackup) {
        // 记录频次
        int num=0;
        for(int i=0;i<dblpNames.size();i++) {
            //根据姓名进行查询
            String command="";
            //非备份的情况
            if(isBackup.equals("false")){
                command = "grep -wo \"" + name + "\" " + DBLP_Path +"/"+ dblpNames.get(i) +" |wc -l"; //按作者名查询，非模糊搜索
            }
            else{
                command = "grep -wo \"" + name + "\" " + DBLP_Backup_Path +"/"+ dblpBackupNames.get(i) +" |wc -l"; //按作者名查询，非模糊搜索
            }
            String result = exeCmd(command);//命令执行结果
            num+=Integer.parseInt(result);
        }
        return String.valueOf(num);
    }

    public static boolean checkYearInRange(String year, String beginYear, String endYear) {
        if(year==null){
            return true;
        }
        if(beginYear.equals("*") && endYear.equals("*")) {
            return true;
        } else if(beginYear.equals("*")) {
            if(Integer.parseInt(year) <= Integer.parseInt(endYear)) {
                return true;
            } else {
                return false;
            }
        } else if(endYear.equals("*")) {
            if(Integer.parseInt(year) >= Integer.parseInt(beginYear)) {
                return true;
            } else {
                return false;
            }
        } else {
            if(Integer.parseInt(year) >= Integer.parseInt(beginYear)
                    && Integer.parseInt(year) <= Integer.parseInt(endYear)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static String queryBlockByNameAndYear(String name,String beginYear,String endYear,String dblpBlockPath) throws FileNotFoundException {
        try {
            // 创建一个 XMLInputFactory
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            // 创建一个 XMLStreamReader
            System.out.println("Reading file"+dblpBlockPath);
            XMLStreamReader reader = inputFactory.createXMLStreamReader(new FileReader(dblpBlockPath));
            //创建一个字符串集合，包含DBLP数据库中所有可能的文章类型
            Set<String> typeSet = new HashSet<>(Arrays.asList(
                    "article",
                    "inproceedings",
                    "proceedings",
                    "book",
                    "incollection",
                    "phdthesis",
                    "mastersthesis",
                    "www",
                    "person",
                    "data"));
            // 用于记录匹配的块的计数器
            int matchedCounter = 0;
            // 用于记录当前读取的块的信息
            String currentAuthor = null;
            String currentYear = null;
            boolean hasAuthor = false;
            // 创建一个栈，用于记录当前读取到的所有元素的名称
            Stack<String> elementStack = new Stack<String>();
            // 开始读取 XML 文档
            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        // 如果是某个块的开头，则重置块信息
                        if (typeSet.contains(reader.getLocalName())) {
                            currentAuthor = null;
                            currentYear = null;
                            hasAuthor = false;
                        }
                        // 将元素名称压入栈
                        elementStack.push(reader.getLocalName());
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        // 如果是某个块的结尾，则检查块信息
                        if (typeSet.contains(reader.getLocalName())) {
                            if (hasAuthor && checkYearInRange(currentYear, beginYear, endYear)) {
                                // 如果块信息满足条件，则更新匹配计数器
                                matchedCounter++;
                            }
                            // 重置块信息
                            currentAuthor = null;
                            currentYear = null;
                            hasAuthor = false;
                        }
                        // 将元素名称弹出栈
                        elementStack.pop();
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        // 如果是某个块的文本内容，则更新块
                        if ("author".equals(elementStack.peek())) {
                            currentAuthor = reader.getText();
                            if (name.equals(currentAuthor)) {
                                hasAuthor = true;
                            }
                        } else if ("year".equals(elementStack.peek())) {
                            currentYear = reader.getText();
                        }
                        break;
                }
            }
            // 关闭 XMLStreamReader
            reader.close();
            // 输出匹配的块的数量
            //System.out.println(matchedCounter);
            //次数转为字符串
            String result = String.valueOf(matchedCounter);
            //System.out.println("Finished file"+dblpBlockPath);
            return result;
        }
        catch (FileNotFoundException | XMLStreamException ex)
        {
            return null;
        }
    }

    public static String queryByNameAndYear(String name,String beginYear,String endYear,String isBackup){
        // 记录频次
        int num=0;
        for(int i=0;i<dblpNames.size();i++) {
            String result = null;
            try {
                if(isBackup.equals("false")) {
                    // 得到某一块的查询结果:非备份
                    result = queryBlockByNameAndYear(name, beginYear, endYear, DBLP_Path + "/" + dblpNames.get(i));
                }
                else {
                    result = queryBlockByNameAndYear(name, beginYear, endYear, DBLP_Backup_Path + "/" + dblpBackupNames.get(i));
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            // 累加频次
            num+=Integer.parseInt(result);
        }
        return String.valueOf(num);
    }

}
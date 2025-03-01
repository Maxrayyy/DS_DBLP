import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class IndexQuery {
    // 索引结构: author -> year -> count
    private final static Map<String, Map<String, Integer>> authorYearIndex = new HashMap<>();
    private final static Set<String> PUBLICATION_TYPES = new HashSet<>(Arrays.asList(
        "article",
        "inproceedings",
        "proceedings",
        "book",
        "incollection",
        "phdthesis",
        "mastersthesis",
        "www",
        "person",
        "data"
    ));
    
    private static String indexPath;  // 索引文件路径
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final int AUTHORS_PER_FILE = 1000;  // 每个文件存储的作者数量
    
    // 初始化索引路径
    public static void init(int port) {
        indexPath = "/hzd/dblpXmls/" + port + "/index/";
        new File(indexPath).mkdirs();
    }
    
    // 保存索引到JSON文件
    private static void saveIndex() {
        try {
            List<String> authors = new ArrayList<>(authorYearIndex.keySet());
            Collections.sort(authors);  // 排序以保持一致性
            
            // 按字母分组保存
            for (char c = 'A'; c <= 'Z'; c++) {
                final char currentChar = c;
                Map<String, Map<String, Integer>> authorGroup = new HashMap<>();
                
                // 收集以该字母开头的作者
                authors.stream()
                    .filter(author -> author.length() > 0 && 
                            Character.toUpperCase(author.charAt(0)) == currentChar)
                    .forEach(author -> authorGroup.put(author, authorYearIndex.get(author)));
                
                if (!authorGroup.isEmpty()) {
                    String fileName = indexPath + "index_" + c + ".json";
                    try (Writer writer = new BufferedWriter(new FileWriter(fileName))) {
                        gson.toJson(authorGroup, writer);
                    }
                }
            }
            
            System.out.println("索引已保存到目录: " + indexPath);
            
        } catch (IOException e) {
            System.out.println("保存索引失败: " + e.getMessage());
        }
    }
    
    // 按需加载特定作者的数据
    private static Map<String, Integer> loadAuthorData(String author) {
        if (author == null || author.isEmpty()) {
            return null;
        }
        
        char firstChar = Character.toUpperCase(author.charAt(0));
        String fileName = indexPath + "index_" + firstChar + ".json";
        File indexFile = new File(fileName);
        
        if (indexFile.exists()) {
            try (Reader reader = new BufferedReader(new FileReader(fileName))) {
                Type type = new TypeToken<Map<String, Map<String, Integer>>>(){}.getType();
                Map<String, Map<String, Integer>> group = gson.fromJson(reader, type);
                return group.get(author);
            } catch (Exception e) {
                System.out.println("加载作者数据失败: " + e.getMessage());
            }
        }
        return null;
    }

    // 查询方法
    public static String query(String author, String beginYear, String endYear) {
        // 先从内存中查找
        Map<String, Integer> yearCounts = authorYearIndex.get(author);
        
        // 如果内存中没有，尝试从文件加载
        if (yearCounts == null) {
            yearCounts = loadAuthorData(author);
        if (yearCounts == null) {
            return "0";
            }
        }

        int totalCount = 0;
        int begin = beginYear.equals("*") ? 0 : Integer.parseInt(beginYear);
        int end = endYear.equals("*") ? 9999 : Integer.parseInt(endYear);

        for (Map.Entry<String, Integer> entry : yearCounts.entrySet()) {
            int year = Integer.parseInt(entry.getKey());
            if (year >= begin && year <= end) {
                totalCount += entry.getValue();
            }
        }

        return String.valueOf(totalCount);
    }
    
    private static String getTextContent(NodeList nodeList) {
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    // 构建索引
    public static void buildIndex(String xmlPath) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            
            DefaultHandler handler = new DefaultHandler() {
                private StringBuilder currentValue = new StringBuilder();
                private String currentYear = null;
                private List<String> currentAuthors = new ArrayList<>();
                private boolean inPublication = false;
                private String currentType = null;
                
                @Override
                public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
                    currentValue.setLength(0);
                    if (PUBLICATION_TYPES.contains(qName)) {
                        inPublication = true;
                        currentType = qName;
                        currentAuthors.clear();
                        currentYear = null;
                    }
                }
                
                @Override
                public void characters(char[] ch, int start, int length) {
                    if (inPublication) {
                        currentValue.append(ch, start, length);
                    }
                }
                
                @Override
                public void endElement(String uri, String localName, String qName) {
                    if (inPublication) {
                        String content = currentValue.toString().trim();
                        if ("author".equals(qName) && !content.isEmpty()) {
                            currentAuthors.add(content);
                        } else if ("year".equals(qName) && !content.isEmpty()) {
                            currentYear = content;
                        } else if (qName.equals(currentType)) {
                            if (currentYear != null && !currentAuthors.isEmpty()) {
                                for (String author : currentAuthors) {
                                    authorYearIndex
                                        .computeIfAbsent(author, k -> new HashMap<>())
                                        .merge(currentYear, 1, Integer::sum);
                                }
                            }
                            inPublication = false;
                            currentType = null;
                        }
                    }
                }
            };
            
            saxParser.parse(new InputSource(new InputStreamReader(
                new FileInputStream(xmlPath), "ISO-8859-1")), handler);
            saveIndex();  // 构建完成后保存到JSON文件
            
        } catch (Exception e) {
            System.out.println("构建索引时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 
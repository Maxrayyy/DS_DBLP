package config;

public class ServerConfig {
    private final String[] serverIps;
    private final int[] serverPorts;
    private final String inputXmlPath;
    private final String outputDir;

    public ServerConfig() {
//        this.serverIps = new String[]{
//                "localhost",
//                "localhost",
//                "localhost"
//        };
        this.serverIps = new String[]{
            "139.196.149.61",
            "118.178.238.242",
            "110.40.201.71"
        };
        this.serverPorts = new int[]{8200, 8201};
        this.inputXmlPath = "data/dblp.xml";
        this.outputDir = "data/splitedXmls";
    }
    
    public ServerConfig(String[] ips, int[] ports, String inputPath, String outDir) {
        this.serverIps = ips;
        this.serverPorts = ports;
        this.inputXmlPath = inputPath;
        this.outputDir = outDir;
    }

    public String[] getServerIps() { return serverIps; }
    public int[] getServerPorts() { return serverPorts; }
    public String getInputXmlPath() { return inputXmlPath; }
    public String getOutputDir() { return outputDir; }
} 
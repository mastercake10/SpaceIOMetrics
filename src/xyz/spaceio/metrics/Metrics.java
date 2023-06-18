package xyz.spaceio.metrics;

import com.google.gson.Gson;
import org.bukkit.plugin.Plugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;


/*
 * SpaceIOMetrics main class by Linus122
 * version: 0.08
 * 
 */

public class Metrics {
	private Plugin plugin;
	private final Gson gson = new Gson();
	
	private String URL = "https://spaceio.xyz/update/%s";
	private final String VERSION = "0.08";
	private int REFRESH_INTERVAL = 600000;
	
	public Metrics(Plugin plugin){
		this.plugin = plugin;

		// check if Metrics are disabled (checks if file "disablemetrics" is added to the plugins's folder
		try (Stream<Path> pathStream = Files.list(plugin.getDataFolder().getParentFile().toPath())) {
			boolean disabledMetrics = pathStream.filter(Files::isRegularFile)
					.anyMatch(file -> file.getFileName().toString().equalsIgnoreCase("disablemetrics"));

			if (disabledMetrics) {
				return;
			}
		} catch (IOException ignored) {
			return;
		}

		this.URL = String.format(this.URL, plugin.getName());
		
		// fetching refresh interval first
		plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
			String dataJson = collectData();
			try{
				this.REFRESH_INTERVAL = sendData(dataJson);
			} catch(Exception ignored){}
		}, 20L * 5);
		
		// executing repeating task, our main metrics updater
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			String dataJson = collectData();
			try{
				sendData(dataJson);
			}catch(Exception e){}
			
		}, 20L * (this.REFRESH_INTERVAL / 1000), 20L * (this.REFRESH_INTERVAL / 1000));
	}
	private String collectData() {
		Data data = new Data();
		
		// collect plugin list
		for(Plugin plug : plugin.getServer().getPluginManager().getPlugins()) {
			data.plugs.put(plug.getName(), plug.getDescription().getVersion());
		}

		// fetch online players
		data.onlinePlayers = plugin.getServer().getOnlinePlayers().size();
		
		// server version
		data.serverVersion = getVersion();
		
		// plugin version
		data.pluginVersion = plugin.getDescription().getVersion();
		
		// plugin author
		data.pluginAuthors = plugin.getDescription().getAuthors();
		
		// core count
		data.coreCnt = Runtime.getRuntime().availableProcessors();
		
		// java version
		data.javaRuntime = System.getProperty("java.runtime.version");
		
		// online mode
		data.onlineMode = plugin.getServer().getOnlineMode();

		// software information
		data.osName = System.getProperty("os.name");
		data.osArch = System.getProperty("os.arch");
		data.osVersion = System.getProperty("os.version");

		data.executableName = new java.io.File(Metrics.class.getProtectionDomain()
				  .getCodeSource()
				  .getLocation()
				  .getPath())
				.getName();
		
		if(data.osName.equals("Linux")){
			data.linuxDistro = getDistro();
		}
		
		return gson.toJson(data);
	}
	private int sendData(String dataJson) throws Exception{
		java.net.URL obj = new java.net.URL(URL);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Java/Bukkit");
		con.setRequestProperty("Metrics-Version", this.VERSION);

		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(dataJson);
		wr.flush();
		wr.close();
		
		return Integer.parseInt(con.getHeaderField("interval-millis"));
	}
	private String getVersion(){
        String packageName = plugin.getServer().getClass().getPackage().getName();
        return  packageName.substring(packageName.lastIndexOf('.') + 1);
	}
	// method source: http://www.jcgonzalez.com/linux-get-distro-from-java-examples
	private String getDistro(){
		
		// lists all the files ending with -release in the etc folder
        File dir = new File("/etc/");
        File[] fileList = new File[0];
        if(dir.exists()){
            fileList =  dir.listFiles((dir1, filename) -> filename.endsWith("-release"));
        }
        try {
			if(fileList == null) {
				return "unknown";
			}

	        // looks for the version file (not all linux distros)
	        File fileVersion = new File("/proc/version");
	        if(fileVersion.exists() && fileList.length > 0){
	            fileList = Arrays.copyOf(fileList,fileList.length+1);
	            fileList[fileList.length-1] = fileVersion;
	        }
	        
	        // prints first version-related file
	        for (File f : fileList) {
	                BufferedReader br = new BufferedReader(new FileReader(f));
	                String strLine = null;
	                while ((strLine = br.readLine()) != null) {
	                    return strLine;
	                }
	                br.close();
	        }
        } catch (Exception ignored) {
        	// Exception is thrown when something went wrong while obtaining the distribution name.
        }
		return "unknown";    
	}
}
class Data {
	HashMap<String, String> plugs = new HashMap<String, String>();
	int onlinePlayers;
	String pluginVersion;
	public List<String> pluginAuthors;
	String serverVersion;
	
	int coreCnt;
	String javaRuntime;
	
	String executableName;
	boolean onlineMode;
	
	String osName;
	String osArch;
	String osVersion;
	String linuxDistro;
}

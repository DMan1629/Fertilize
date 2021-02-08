package me.DMan16.Fertilize;

import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	public static Main main;
	public static final String pluginName = "Fertilize";
	public static final String pluginNameColors = "&a&lFertilize";

	public void onEnable() {
		main = this;
		String versionMC = Bukkit.getServer().getVersion().split("\\(MC:")[1].split("\\)")[0].trim().split(" ")[0].trim();
		if (Integer.parseInt(versionMC.split("\\.")[0]) < 1 || Integer.parseInt(versionMC.split("\\.")[1]) < 16) {
			General.chatColorsLogPlugin("&cunsupported version! Please use version 1.16+");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		try {
			Bukkit.getPluginManager().registerEvents(new General(),this);
		} catch (Exception e) {
			General.chatColorsLogPlugin("&ccouldn't initialize plugin. &c&lError:");
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		General.chatColorsLogPlugin("&aLoaded, running on version: &f" + versionMC + "&a, Java version: &f" + javaVersion());
	}
	
	private String javaVersion() {
		String javaVersion = "";
		Iterator<Entry<Object,Object>> systemProperties = System.getProperties().entrySet().iterator();
		while (systemProperties.hasNext() && javaVersion.isEmpty()) {
			Entry<Object,Object> property = (Entry<Object,Object>) systemProperties.next();
			if (property.getKey().toString().equalsIgnoreCase("java.version")) javaVersion = property.getValue().toString();
		}
		return javaVersion;
	}

	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
		General.chatColorsLogPlugin("&aDisabed successfully!");
	}
}
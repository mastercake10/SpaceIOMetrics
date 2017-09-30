package de.Linus122.testing;

import org.bukkit.plugin.java.JavaPlugin;

public class BukkitMain extends JavaPlugin{
	@Override
	public void onEnable(){
		// Initializing metrics
		new Metrics(this);
	}
}

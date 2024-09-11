package com.gamerforea.eventhelper;

import com.gamerforea.eventhelper.command.CommandReloadAllConfigs;
import com.gamerforea.eventhelper.config.ConfigUtils;
import com.gamerforea.eventhelper.inject.InjectionManager;
import com.google.common.collect.Lists;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;

import java.io.File;
import java.util.List;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

@Mod(modid = "EventHelper", name = "EventHelper", version = "@VERSION@", acceptableRemoteVersions = "*")
public final class EventHelper
{
	public static final Logger LOGGER = LogManager.getLogger("EventHelper");
	public static final File cfgDir = new File(Loader.instance().getConfigDir(), "Events");
	//Специально без дженерика, чтобы можно было запустить в dev без бакита
	public static final List listeners = Lists.newArrayList();
	public static String craftPackage = "org.bukkit.craftbukkit.v1_7_R4";
	public static boolean explosions = true;
	public static boolean debug = true;

	@EventHandler
	public void onServerStart(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CommandReloadAllConfigs());
	}

	@EventHandler
	public void serverStarted(FMLServerStartedEvent event)
	{
		Configuration cfg = ConfigUtils.getConfig("EventHelper");
		String c = CATEGORY_GENERAL;
		String[] plugins = cfg.getStringList("plugins", c, new String[] { "WorldGuard", "GriefPreventionPlus" }, "Plugins for sending events");
		boolean pluginHooking = cfg.getBoolean("pluginHooking", c, true, "Hooking plugins (allow checking regions)");
		explosions = cfg.getBoolean("explosions", c, explosions, "Explosions enabled");
		debug = cfg.getBoolean("debug", c, debug, "Debugging enabled");
		cfg.save();

		if ((boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
			if (debug)
				LOGGER.info("Skip injectors initialization because running in dev mode");
			return;
		}

		craftPackage = "org.bukkit.craftbukkit." + detectCraftVersion();

		PluginManager plManager = Bukkit.getPluginManager();
		for (String plName : plugins)
		{
			Plugin plugin = plManager.getPlugin(plName);
			if (plugin == null)
				LOGGER.warn("Plugin {} not found!", plName);
			else
				listeners.addAll(HandlerList.getRegisteredListeners(plugin));
		}
		if (pluginHooking)
			InjectionManager.init();
	}

	public static void callEvent(Event event)
	{
		for (Object listener : listeners)
		{
			try
			{
				((RegisteredListener)listener).callEvent(event);
			}
			catch (Throwable throwable)
			{
				if (debug)
					LOGGER.error("Failed event call", throwable);
			}
		}
	}

	public static void error(Throwable throwable, String message, Object... args)
	{
		if (debug)
			LOGGER.error(new FormattedMessage(message, args), throwable);
		else
			LOGGER.error(message, args);
	}

	private static String detectCraftVersion() {
		String clazz = Bukkit.getServer().getClass().getName();
		if (!clazz.startsWith("org.bukkit.craftbukkit.") || !clazz.endsWith(".CraftServer"))
			throw new IllegalStateException("Cant detect craftbukkit version, bad server class name: " + clazz);
		String craftBukkitPackage = clazz.substring("org.bukkit.craftbukkit.".length());
		craftBukkitPackage = craftBukkitPackage.substring(0, craftBukkitPackage.length() - ".CraftServer".length());
		if (debug)
        	LOGGER.info("Runtime craft bukkit version: {}", craftBukkitPackage);
		return craftBukkitPackage;
	}
}

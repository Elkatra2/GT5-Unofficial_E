package miscutil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import miscutil.core.commands.CommandMath;
import miscutil.core.common.CommonProxy;
import miscutil.core.creative.AddToCreativeTab;
import miscutil.core.handler.events.PickaxeBlockBreakEventHandler;
import miscutil.core.lib.CORE;
import miscutil.core.lib.LoadedMods;
import miscutil.core.util.PlayerCache;
import miscutil.core.util.Utils;
import miscutil.core.util.debug.DEBUG_ScreenOverlay;
import miscutil.core.util.uptime.Uptime;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;

@Mod(modid=CORE.MODID, name="Misc. Utils", version=CORE.VERSION, dependencies="required-after:gregtech;")
public class MiscUtils
implements ActionListener
{ 
	
	@Mod.Instance(CORE.MODID)
	public static MiscUtils instance;
	public static Uptime Uptime = new Uptime();

	@SidedProxy(clientSide="miscutil.core.proxy.ClientProxy", serverSide="miscutil.core.proxy.ServerProxy")
	public static CommonProxy proxy;


	//Pre-Init
	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		LoadedMods.checkLoaded();
		Utils.LOG_INFO("Doing some house cleaning.");
		AddToCreativeTab.initialiseTabs();
		proxy.registerTileEntities();
		proxy.registerRenderThings();
		proxy.preInit(event);
		Uptime.preInit(event); //Integration of Uptime.
		//FMLInterModComms.sendMessage("Waila", "register", "miscutil.core.waila.WailaCompat.load");
	}

	//Init
	@Mod.EventHandler
	public void init(FMLInitializationEvent event)
	{
		proxy.init(event);
		proxy.registerOreDict();
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(new PickaxeBlockBreakEventHandler());
		if (CORE.DEBUG){
			MinecraftForge.EVENT_BUS.register(new DEBUG_ScreenOverlay());	
		}
		FMLCommonHandler.instance().bus().register(this);
		proxy.registerNetworkStuff();
		Uptime.init(event); //Integration of Uptime.
	}

	//Post-Init
	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		Utils.LOG_INFO("Cleaning up, doing postInit.");
		PlayerCache.initCache();
		proxy.postInit(event);
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CommandMath());
		Uptime.serverStarting(event); //Integration of Uptime.

	}

	@Mod.EventHandler
	public void serverStopping(FMLServerStoppingEvent event)
	{
		Uptime.serverStopping(event); //Integration of Uptime.

	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		Uptime.actionPerformed(arg0); //Integration of Uptime.

	}

}

package deathtags.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import deathtags.api.PartyHelper;
import deathtags.commands.PartyCommand;
import deathtags.config.ConfigHolder;
import deathtags.core.events.EventClient;
import deathtags.core.events.EventCommon;
import deathtags.core.events.EventServer;
import deathtags.gui.HealthBar;
import deathtags.networking.MessageGUIInvitePlayer;
import deathtags.networking.MessageOpenUI;
import deathtags.networking.MessageSendMemberData;
import deathtags.networking.MessageUpdateParty;
import deathtags.stats.Party;
import deathtags.stats.PlayerStats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;
import org.lwjgl.glfw.GLFW;

@Mod(value = MMOParties.MODID)
public class MMOParties {

	public static final String MODID = "mmoparties";
	public static Party localParty;
	public static Map<PlayerEntity, PlayerStats> PlayerStats = new HashMap<>();
	private static final String PROTOCOL_VERSION = "1";
	public static KeyBinding OPEN_GUI_KEY;
	
	public static final SimpleChannel network = NetworkRegistry.ChannelBuilder
			.named(new ResourceLocation(MODID, "sync"))
			.clientAcceptedVersions(s -> true)
			.serverAcceptedVersions(s -> true)
			.networkProtocolVersion(() -> PROTOCOL_VERSION)
			.simpleChannel();
	
	public MMOParties ()
	{
		// Construct configuration
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHolder.COMMON_SPEC);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigHolder.CLIENT_SPEC);

		// Construct game events.
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientInit);

		MinecraftForge.EVENT_BUS.addListener(this::serverInit);
		MinecraftForge.EVENT_BUS.addListener(this::serverInitEvent);

		MinecraftForge.EVENT_BUS.register(this);
	}
	
	public void preInit(FMLCommonSetupEvent event) 
	{
		System.out.println(MODID + " is pre-loading!");

		network.registerMessage(1, MessageUpdateParty.class, MessageUpdateParty::encode, MessageUpdateParty::decode, MessageUpdateParty.Handler::handle );
		network.registerMessage(2, MessageSendMemberData.class, MessageSendMemberData::encode, MessageSendMemberData::decode, MessageSendMemberData.Handler::handle);
		network.registerMessage(3, MessageGUIInvitePlayer.class, MessageGUIInvitePlayer::encode, MessageGUIInvitePlayer::decode, MessageGUIInvitePlayer.Handler::handle);

		// Register event handlers
		MinecraftForge.EVENT_BUS.register(new EventCommon());
		MinecraftForge.EVENT_BUS.register(new EventClient());
		MinecraftForge.EVENT_BUS.register(new EventServer());
	}

	public void serverInitEvent(FMLServerStartingEvent event) {
		PartyHelper.Server.server = event.getServer(); // Set server instance
		network.registerMessage(4, MessageOpenUI.class, MessageOpenUI::encode, MessageOpenUI::decode, MessageOpenUI.Handler::handleServer);
	}

	public void clientInit(FMLClientSetupEvent event)
	{
		HealthBar.init();
		network.registerMessage(4, MessageOpenUI.class, MessageOpenUI::encode, MessageOpenUI::decode, MessageOpenUI.Handler::handle);

		OPEN_GUI_KEY = new KeyBinding("key.opengui.desc", KeyConflictContext.UNIVERSAL, InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_P, "key.mmoparties.category"); // Open GUI on G.
		ClientRegistry.registerKeyBinding(OPEN_GUI_KEY);
	}
	
	public void serverInit(RegisterCommandsEvent event)
	{	
		event.getDispatcher().register(PartyCommand.register());
		PermissionAPI.registerNode("rpgparties.*", DefaultPermissionLevel.ALL, "The base permission");
	}

	/**
	 * Get the stat value of a player by their name.
	 * @param name
	 * @return
	 */
	public static PlayerStats GetStatsByName(String name)
	{
		for (Entry<PlayerEntity, deathtags.stats.PlayerStats> plr : PlayerStats.entrySet()) {
			if ( plr.getKey().getName().getContents().equals(name) )
				return plr.getValue();
		}

		return null;
	}

	/**
	 * Get the stat value of a player.
	 * @param player
	 * @return
	 */
	public static PlayerStats GetStats(PlayerEntity player)
	{
		return GetStatsByName(player.getName().getString());
	}

}

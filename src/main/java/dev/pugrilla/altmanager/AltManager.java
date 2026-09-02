package dev.pugrilla.altmanager;

import dev.pugrilla.altmanager.client.ClientEventListener;
import dev.pugrilla.altmanager.gui.AltManagerScreen;
import dev.pugrilla.altmanager.storage.FileStorageManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = AltManager.MOD_ID, version = AltManager.VERSION, acceptedMinecraftVersions = AltManager.MINECRAFT_VERSION_RANGE)
public class AltManager {
   public static final String MOD_ID = "pugaltmanager";
   public static final String MOD_NAME = "Pug Alt Manager";
   public static final String VERSION = "1.0";
   public static final String MINECRAFT_VERSION_RANGE = "[1.8.9]";

   private final Logger logger = LogManager.getLogger(MOD_NAME);
   private final FileStorageManager storageManager = new FileStorageManager(this);
   private final AltManagerScreen mainScreen = new AltManagerScreen(this.storageManager, this);
   private final ClientEventListener eventListener = new ClientEventListener(this);
   private final ExecutorService threadPool = Executors.newFixedThreadPool(4);

   @EventHandler
   public void init(FMLInitializationEvent fmlinitializationevent) {
      if (fmlinitializationevent.getSide() == Side.CLIENT) {
         MinecraftForge.EVENT_BUS.register(this.eventListener);
      }
   }

   public ExecutorService getThreadPool() {
      return this.threadPool;
   }

   public ClientEventListener getEventListener() {
      return this.eventListener;
   }

   public AltManagerScreen getMainScreen() {
      return this.mainScreen;
   }

   public FileStorageManager getStorageManager() {
      return this.storageManager;
   }

   public Logger getLogger() {
      return this.logger;
   }
}

package dev.layjmod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.layjmod.api.LayjApiServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class LayjMod implements ModInitializer {
	public static final String MOD_ID = "layjmod";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private LayjApiServer apiServer;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			try {
				apiServer = new LayjApiServer(server);
				apiServer.start();
			} catch (Exception e) {
				LOGGER.error("LayjMod: API server failed to start", e);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (apiServer != null) {
				apiServer.stop();
				apiServer = null;
			}
		});
		LOGGER.info("LayjMod loaded");
	}
}

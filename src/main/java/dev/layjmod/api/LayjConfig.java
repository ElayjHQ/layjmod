package dev.layjmod.api;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

import dev.layjmod.LayjMod;
import net.fabricmc.loader.api.FabricLoader;

public final class LayjConfig {
	public int port;
	public String bindAddress;
	public String token;
	public String allowOrigin;

	private static Path configFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("layjmod.properties");
	}

	public static LayjConfig load() {
		Path file = configFile();
		Properties props = new Properties();
		if (Files.isRegularFile(file)) {
			try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				props.load(r);
			} catch (IOException e) {
				LayjMod.LOGGER.error("layjmod: failed reading config, defaults apply", e);
			}
		}

		LayjConfig cfg = new LayjConfig();
		cfg.port = intOf(props.getProperty("port"), 8081);
		cfg.bindAddress = norm(props.getProperty("bind-address"), "0.0.0.0");
		cfg.allowOrigin = norm(props.getProperty("allow-origin"), "*");
		cfg.token = norm(props.getProperty("token"), "");

		boolean generated = false;
		if (cfg.token.isEmpty()) {
			cfg.token = UUID.randomUUID().toString();
			generated = true;
		}

		try {
			Files.createDirectories(file.getParent());
			try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				w.write("# LayjMod HTTP API config. Website must send: Authorization: Bearer <token>");
				w.write(System.lineSeparator());
				w.write("port=" + cfg.port + System.lineSeparator());
				w.write("bind-address=" + cfg.bindAddress + System.lineSeparator());
				w.write("token=" + cfg.token + System.lineSeparator());
				w.write("allow-origin=" + cfg.allowOrigin + System.lineSeparator());
			}
		} catch (IOException e) {
			LayjMod.LOGGER.error("layjmod: could not write config file {}", file, e);
		}

		if (generated) {
			LayjMod.LOGGER.info("layjmod: generated new API token and saved to {}", file);
			LayjMod.LOGGER.info("layjmod: API token = {} (also stored in {})", cfg.token, file);
			LayjMod.LOGGER.info("layjmod: listening on port {} (set 'port' and 'token' in {})", cfg.port, file);
		}

		return cfg;
	}

	private static String norm(String s, String def) {
		if (s == null) {
			return def;
		}
		s = s.trim();
		return s.isEmpty() ? def : s;
	}

	private static int intOf(String s, int def) {
		try {
			if (s == null) {
				return def;
			}
			return Math.max(1, Math.min(65535, Integer.parseInt(s.trim())));
		} catch (NumberFormatException e) {
			return def;
		}
	}
}

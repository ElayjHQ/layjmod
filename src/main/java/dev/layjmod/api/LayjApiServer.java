package dev.layjmod.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import dev.layjmod.LayjMod;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class LayjApiServer {
	private static final int MAX_BODY_BYTES = 8192;
	private static final int COMMAND_TIMEOUT_SEC = 15;
	private static final int STATUS_TIMEOUT_SEC = 10;
	private static final int RATE_LIMIT_PER_MIN = 120;

	private final MinecraftServer server;
	private final LayjConfig config;
	private final long startedAtMs = System.currentTimeMillis();
	private final RateLimiter rateLimiter = new RateLimiter();
	private ExecutorService callbackPool;
	private HttpServer http;

	public LayjApiServer(MinecraftServer server) {
		this.server = server;
		this.config = LayjConfig.load();
	}

	public void start() throws IOException {
		String bind = config.bindAddress;
		if (bind.isEmpty() || bind.equals("*")) {
			bind = "0.0.0.0";
		}
		http = HttpServer.create(new InetSocketAddress(bind, config.port), 8);
		http.createContext("/api", this::handleApi);
		callbackPool = Executors.newFixedThreadPool(4, r -> {
			Thread t = new Thread(r, "layjmod-api");
			t.setDaemon(true);
			return t;
		});
		http.setExecutor(callbackPool);
		http.start();
		LayjMod.LOGGER.info("layjmod: API server listening on http://{}:{} (endpoints: /api/status, /api/command)", bind, config.port);
	}

	public void stop() {
		if (http != null) {
			http.stop(0);
			LayjMod.LOGGER.info("layjmod: API server stopped");
		}
		if (callbackPool != null) {
			callbackPool.shutdown();
		}
	}

	private void handleApi(HttpExchange ex) {
		Response res;
		try {
			String path = ex.getRequestURI().getPath();
			String method = ex.getRequestMethod();
			if ("OPTIONS".equals(method)) {
				res = new Response(204, "");
			} else if (!authorized(ex)) {
				res = new Response(401, errJson("unauthorized: send Authorization: Bearer <token>"));
			} else if (rateLimiter.exceeded(clientIp(ex))) {
				res = new Response(429, errJson("rate limited"));
			} else if ("/api/status".equals(path) && "GET".equals(method)) {
				res = statusLocked();
			} else if ("/api/command".equals(path) && "POST".equals(method)) {
				res = commandLocked(readBody(ex));
			} else {
				res = new Response(404, errJson("not found: " + method + " " + path));
			}
		} catch (Exception e) {
			LayjMod.LOGGER.error("layjmod: API request failed", e);
			res = new Response(500, errJson("internal error: " + e.getClass().getSimpleName()));
		}
		try {
			respond(ex, res);
		} catch (IOException e) {
			LayjMod.LOGGER.warn("layjmod: failed writing response", e);
		}
	}

	/* --- endpoints --- */

	private Response statusLocked() throws InterruptedException, ExecutionException {
		CompletableFuture<String> fut = CompletableFuture.supplyAsync(this::buildStatus, server)
			.orTimeout(STATUS_TIMEOUT_SEC, TimeUnit.SECONDS);
		String json;
		try {
			json = fut.get();
		} catch (ExecutionException | CompletionException e) {
			return new Response(500, errJson("status failed: " + rootMessage(e)));
		}
		return new Response(200, json);
	}

	private String buildStatus() {
		String[] names = server.getPlayerNames();
		Arrays.sort(names);
		long avgNanos = server.getAverageTickTimeNanos();
		double tps = avgNanos > 0 ? Math.min(20.0, 1.0e9 / avgNanos) : 20.0;
		StringBuilder sb = new StringBuilder(256);
		sb.append("{\"ok\":true")
			.append(",\"motd\":").append(jq(server.getMotd()))
			.append(",\"version\":").append(jq(server.getServerVersion()))
			.append(",\"uptimeSec\":").append((System.currentTimeMillis() - startedAtMs) / 1000)
			.append(",\"online\":").append(server.getPlayerCount())
			.append(",\"max\":").append(server.getMaxPlayers())
			.append(",\"tps\":").append(round2(tps))
			.append(",\"players\":").append(jqArray(names))
			.append('}');
		return sb.toString();
	}

	private Response commandLocked(String rawBody) throws InterruptedException, ExecutionException {
		String cmd = extractCommand(rawBody);
		if (cmd == null || cmd.isBlank()) {
			return new Response(400, errJson("empty command"));
		}
		CompletableFuture<String> fut = CompletableFuture.supplyAsync(() -> runCommand(cmd), server)
			.orTimeout(COMMAND_TIMEOUT_SEC, TimeUnit.SECONDS);
		String output;
		try {
			output = fut.get();
		} catch (ExecutionException | CompletionException e) {
			return new Response(500, errJson("command execution failed: " + rootMessage(e)));
		}
		StringBuilder sb = new StringBuilder(64 + output.length() * 2);
		sb.append("{\"ok\":true,\"command\":").append(jq(cmd))
			.append(",\"output\":").append(jq(output)).append('}');
		return new Response(200, sb.toString());
	}

	private String runCommand(String cmd) {
		StringBuilder out = new StringBuilder();
		CommandSource capture = new CommandSource() {
			@Override
			public void sendSystemMessage(Component msg) {
				if (out.length() > 0) {
					out.append('\n');
				}
				out.append(msg.getString());
			}

			@Override
			public boolean acceptsSuccess() {
				return true;
			}

			@Override
			public boolean acceptsFailure() {
				return true;
			}

			@Override
			public boolean shouldInformAdmins() {
				return false;
			}
		};
		CommandSourceStack source = server.createCommandSourceStack().withSource(capture);
		try {
			server.getCommands().performPrefixedCommand(source, "/" + cmd);
		} catch (Exception e) {
			if (out.length() > 0) {
				out.append('\n');
			}
			out.append("error: ").append(rootMessage(e));
		}
		return out.toString();
	}

	/* --- request plumbing --- */

	private boolean authorized(HttpExchange ex) {
		String[] auth = ex.getRequestHeaders().getOrDefault("Authorization", List.of()).toArray(new String[0]);
		if (auth.length == 0) {
			return false;
		}
		String v = auth[0].trim();
		if (v.regionMatches(true, 0, "Bearer ", 0, 7)) {
			return tokenEquals(v.substring(7).trim(), config.token);
		}
		return tokenEquals(v, config.token);
	}

	private static boolean tokenEquals(String given, String real) {
		if (given == null || real == null) {
			return false;
		}
		return MessageDigest.isEqual(
			given.getBytes(StandardCharsets.UTF_8),
			real.getBytes(StandardCharsets.UTF_8));
	}

	private static String clientIp(HttpExchange ex) {
		List<String> remote = ex.getRequestHeaders().getOrDefault("X-Forwarded-For", List.of());
		if (!remote.isEmpty()) {
			return remote.get(0).split(",")[0].trim();
		}
		return String.valueOf(ex.getRemoteAddress().getAddress());
	}

	private String readBody(HttpExchange ex) throws IOException {
		try (InputStream in = ex.getRequestBody()) {
			byte[] buf = in.readNBytes(MAX_BODY_BYTES + 1);
			if (buf.length > MAX_BODY_BYTES) {
				throw new IOException("body too large");
			}
			return new String(buf, StandardCharsets.UTF_8);
		}
	}

	private static String extractCommand(String body) {
		if (body == null) {
			return null;
		}
		int p = 0;
		while (p < body.length() && Character.isWhitespace(body.charAt(p))) {
			p++;
		}
		if (p >= body.length() || body.charAt(p) != '{') {
			// raw body fallback: whole trimmed body is the command
			return body.trim();
		}
		int key = body.indexOf("\"command\"");
		if (key < 0) {
			return body.trim();
		}
		int colon = body.indexOf(':', key + 9);
		if (colon < 0) {
			return body.trim();
		}
		int q = body.indexOf('"', colon + 1);
		if (q < 0) {
			return body.trim();
		}
		StringBuilder sb = new StringBuilder();
		for (int i = q + 1; i < body.length(); i++) {
			char c = body.charAt(i);
			if (c == '\\') {
				i++;
				if (i >= body.length()) {
					break;
				}
				char e = body.charAt(i);
				switch (e) {
					case 'n' -> sb.append('\n');
					case 't' -> sb.append('\t');
					case 'r' -> sb.append('\r');
					default -> sb.append(e);
				}
			} else if (c == '"') {
				break;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private void respond(HttpExchange ex, Response res) throws IOException {
		String origin = ex.getRequestHeaders().getFirst("Origin");
		if (origin != null && !origin.isBlank()) {
			ex.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
			ex.getResponseHeaders().set("Vary", "Origin");
		} else {
			ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		}
		ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
		ex.getResponseHeaders().set("Access-Control-Max-Age", "600");
		ex.getResponseHeaders().set("Cache-Control", "no-store");
		byte[] bytes = res.body.getBytes(StandardCharsets.UTF_8);
		if (bytes.length == 0) {
			ex.sendResponseHeaders(res.status, -1);
			ex.close();
			return;
		}
		ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		ex.sendResponseHeaders(res.status, bytes.length);
		try (OutputStream out = ex.getResponseBody()) {
			out.write(bytes);
		}
	}

	/* --- json helpers --- */

	private static String errJson(String msg) {
		return "{\"ok\":false,\"error\":" + jq(msg) + "}";
	}

	private static String jq(String s) {
		if (s == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder(s.length() + 2);
		sb.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				default -> {
					if (c < 0x20) {
						sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
				}
			}
		}
		sb.append('"');
		return sb.toString();
	}

	private static String jqArray(String[] items) {
		StringBuilder sb = new StringBuilder(32);
		sb.append('[');
		for (int i = 0; i < items.length; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(jq(items[i]));
		}
		sb.append(']');
		return sb.toString();
	}

	private static String round2(double v) {
		return String.valueOf(Math.round(v * 100.0) / 100.0);
	}

	private static String rootMessage(Throwable t) {
		while (t.getCause() != null && t.getCause() != t) {
			t = t.getCause();
		}
		String m = t.getMessage();
		if (m == null || m.isBlank()) {
			m = t.getClass().getSimpleName();
		}
		return m;
	}

	private record Response(int status, String body) {
	}

	private static final class RateLimiter {
		private static final int WINDOW_MS = 60_000;
		private final int limit = RATE_LIMIT_PER_MIN;
		private final java.util.Map<String, List<Long>> hits = new java.util.concurrent.ConcurrentHashMap<>();

		boolean exceeded(String key) {
			if (key == null || key.equals("null")) {
				return false;
			}
			long now = System.currentTimeMillis();
			List<Long> list = hits.compute(key, (k, old) -> old == null ? new java.util.ArrayList<>() : old);
			synchronized (list) {
				list.removeIf(t -> now - t > WINDOW_MS);
				if (list.size() >= limit) {
					return true;
				}
				list.add(now);
				return false;
			}
		}
	}
}

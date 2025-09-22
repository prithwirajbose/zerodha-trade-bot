package com.sribasu.tradebot;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.fusesource.jansi.Ansi;
import org.json.JSONException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.User;

import jakarta.annotation.PostConstruct;

@Service
public class AuthService {
	private ObjectMapper mapper = new ObjectMapper();
	private static final String CONFIG_NODE = "com/sribasu/tradebot";
	private KiteConnect kiteConnect;
	private boolean isSessionActive = false;
	private volatile boolean isSocketRunning = false;
	private volatile String requestToken = null;

	@PostConstruct
	public void init() {
		Preferences prefs = readConfig();
		try {
			if (prefs != null && prefs.keys().length > 0 && isKiteConfigAvailable(prefs)) {
				kiteConnect = new KiteConnect(prefs.get("kiteApiKey", ""));
				kiteConnect.setUserId(prefs.get("kiteUserId", ""));
				kiteConnect.setSessionExpiryHook(new SessionExpiryHook() {
					@Override
					public void sessionExpired() {
						isSessionActive = false;
						System.out.println("Session expired");
					}
				});
			}
			
			if (prefs != null && prefs.keys().length > 0 && prefs.get("kiteAccessToken", null) != null && prefs.get("kitePublicToken", null) != null) {
				kiteConnect.setAccessToken((String) prefs.get("kiteAccessToken", ""));
				kiteConnect.setPublicToken((String) prefs.get("kitePublicToken", ""));
				isSessionActive = true;
			}
		} catch (Throwable ex) {
			// do nothing
		}
	}

	public boolean isKiteConfigAvailable() throws BackingStoreException {
		Preferences prefs = readConfig();
		return prefs != null && prefs.keys().length > 0 && prefs.get("kiteApiKey", null) != null
				&& prefs.get("kiteApiSecret", null) != null && prefs.get("kiteUserId", null) != null;
	}

	public boolean isKiteConfigAvailable(Preferences prefs) throws BackingStoreException {
		return prefs != null && prefs.keys().length > 0 && prefs.get("kiteApiKey", null) != null
				&& prefs.get("kiteApiSecret", null) != null && prefs.get("kiteUserId", null) != null;
	}

	public KiteConnect getKiteConnect() throws KiteException, Exception {
		if (!isSessionActive) {
			refreshSession();
		}
		return kiteConnect;
	}

	public void refreshSession() throws Exception, KiteException {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		java.util.concurrent.Future<String> future = executorService.submit(() -> {
			String url = "";
			try {
				url = kiteConnect.getLoginURL();
				openBrowser(url);
			} catch (MalformedURLException | NullPointerException e) {
				System.out.println("Open this URL in browser to login: " + url);
			}
			startSocketServer();

			return requestToken;
		});
		try {
			Preferences prefs = readConfig();
			String reqToken = future.get();
			User userModel = kiteConnect.generateSession(reqToken, prefs.get("kiteApiSecret", ""));
			kiteConnect.setAccessToken(userModel.accessToken);
			kiteConnect.setPublicToken(userModel.publicToken);
			saveToken(userModel.accessToken, userModel.publicToken);
			isSessionActive = true;
		} finally {
			if (executorService != null) {
				executorService.shutdown();
			}
		}
	}

	private void openBrowser(String url) throws MalformedURLException {
		System.out.println(Ansi.ansi().fgBlue()
				.a("Opening browser to login. "
						+ "If the browser doesn't open automatically visit the following URL to complete the login process: \n")
				.reset().fgBrightBlue().a(url).reset());
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			try {
				Desktop.getDesktop().browse(new URI(url));
			} catch (IOException | URISyntaxException e) {
				System.out.println(Ansi.ansi().fgBrightRed()
						.a("Unable to open browser to authenticate. "
								+ "Open this URL in browser to complete the authentication process: \n")
						.reset().fgBrightBlue().a(url).reset());
			}
		} else {
			try {
				String os = System.getProperty("os.name").toLowerCase();
				if (os.contains("win")) {
					Runtime.getRuntime().exec("cmd /c start " + url);
				} else if (os.contains("mac")) {
					Runtime.getRuntime().exec("open " + url);
				} else if (os.contains("nix") || os.contains("nux")) {
					Runtime.getRuntime().exec("xdg-open " + url); // Common for Linux
				} else {
					throw new IOException("Unsupported OS");
				}
			} catch (IOException e) {
				System.out.println(Ansi.ansi().fgBrightRed()
						.a("Unable to open browser to authenticate. "
								+ "Open this URL in browser to complete the authentication process: \n")
						.reset().fgBrightBlue().a(url).reset());
			}
		}
	}

	public void startSocketServer() {
		if (isSocketRunning) {
			try (java.net.Socket socket = new java.net.Socket("localhost", 8080);
					java.io.OutputStream out = socket.getOutputStream()) {
				out.write("STOP\r\n".getBytes());
				out.flush();
			} catch (IOException e) {
				System.out.println(Ansi.ansi().fgBrightRed()
						.a("Could not stop existing socket server: " + e.getMessage()).reset());
			}
			isSocketRunning = false;
		}
		isSocketRunning = true;
		try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(8080)) {
			while (isSocketRunning) {
				try (java.net.Socket clientSocket = serverSocket.accept();
						java.io.BufferedReader in = new java.io.BufferedReader(
								new java.io.InputStreamReader(clientSocket.getInputStream()));
						java.io.PrintWriter out = new java.io.PrintWriter(clientSocket.getOutputStream(), true)) {

					String line;
					String requestLine = null;
					while ((line = in.readLine()) != null && !line.isEmpty()) {
						if (requestLine == null) {
							requestLine = line;
						}
					}

					if (requestLine != null && requestLine.startsWith("GET")) {
						int idx = requestLine.indexOf(" ");
						int idx2 = requestLine.indexOf(" ", idx + 1);
						String urlPart = requestLine.substring(idx + 1, idx2);
						java.net.URI uri = new java.net.URI(urlPart);
						String query = uri.getQuery();
						if (query != null && query.contains("request_token=")) {
							String[] params = query.split("&");
							for (String param : params) {
								if (param.startsWith("request_token=")) {
									requestToken = param.substring("request_token=".length());
									break;
								}
							}
						} else {
							requestToken = null;
						}
					} else {
						requestToken = null;
					}

					out.println("HTTP/1.1 200 OK");
					out.println("Content-Type: text/html");
					out.println();
					out.println("<html><body>Login received. You may close this window.</body></html>");

					isSocketRunning = false;
				} catch (Exception e) {
					System.out.println(
							Ansi.ansi().fgBrightRed().a("Error handling socket request: " + e.getMessage()).reset());
					requestToken = null;
					isSocketRunning = false;
				}
			}
		} catch (IOException e) {
			System.out.println(Ansi.ansi().fgBrightRed().a("Error starting socket server: " + e.getMessage()).reset());
			requestToken = null;
			isSocketRunning = false;
		}
	}

	private void saveToken(String accessToken, String publicToken) throws IOException, BackingStoreException {
		Preferences prefs = readConfig();
		prefs.put("kiteAccessToken", accessToken);
		prefs.put("kitePublicToken", publicToken);
		prefs.flush();
	}

	private Preferences readConfig() {
		Preferences prefs = Preferences.userRoot().node(CONFIG_NODE);
		return prefs;
	}

	public void saveConfig(String kiteApiKey, String kiteApiSecret, String kiteUserId) throws BackingStoreException {
		Preferences prefs = Preferences.userRoot().node(CONFIG_NODE);
		prefs.put("kiteApiKey", kiteApiKey);
		prefs.put("kiteApiSecret", kiteApiSecret);
		prefs.put("kiteUserId", kiteUserId);
		prefs.flush();
		logout();
		init();
	}

	public void logout() throws BackingStoreException {
		Preferences prefs = Preferences.userRoot().node(CONFIG_NODE);
		prefs.put("kiteAccessToken", "");
		prefs.put("kitePublicToken", "");
		prefs.remove("kiteAccessToken");
		prefs.remove("kitePublicToken");
		prefs.flush();
		isSessionActive = false;
	}
}

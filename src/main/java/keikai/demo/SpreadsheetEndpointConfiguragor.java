package keikai.demo;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.*;
import javax.websocket.server.ServerEndpointConfig.Configurator;

public class SpreadsheetEndpointConfiguragor extends Configurator {

	static public String SESSION = "session"; // the key to store HttpSession
	@Override
	public void modifyHandshake(ServerEndpointConfig config,
			HandshakeRequest request, HandshakeResponse response) {
		config.getUserProperties().put(SESSION, request.getHttpSession());
	}
}

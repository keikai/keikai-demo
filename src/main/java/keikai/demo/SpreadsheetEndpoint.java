package keikai.demo;

import java.io.IOException;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import com.keikai.client.api.Spreadsheet;

@ServerEndpoint(value = "/spreadsheet", configurator = SpreadsheetEndpointConfiguragor.class)
public class SpreadsheetEndpoint {
	private HttpSession httpSession;
	private Spreadsheet spreadsheet;
	
	@OnOpen
	public void onOpen(Session session){
		httpSession = (HttpSession)session.getUserProperties().get(SpreadsheetEndpointConfiguragor.SESSION);
		spreadsheet = (Spreadsheet)httpSession.getAttribute(DemoFilter.SPREADSHEET);
	}

	/**
	 * When a user sends a message to the server, this method will intercept the message
	 * and allow us to react to it. For now the message is read as a String.
	 */
	@OnMessage
	public void onMessage(String message, Session session){
		System.out.println("Message from " + session.getId() + ": " + message);
		try {
			session.getBasicRemote().sendText(message);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * The user closes the connection.
	 * 
	 * Note: you can't send messages to the client from this method
	 */
	@OnClose
	public void onClose(Session session){
		System.out.println("Session " +session.getId()+" has ended");
	}
}

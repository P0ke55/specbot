package p0ke.specbot.spectator;

import java.net.Proxy;
import java.util.Timer;
import java.util.TimerTask;

import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.protocol.MinecraftConstants;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

public class Spectator {

	private String name;
	private String username;
	private String password;
	private String pLeader = "";
	private boolean inUse = false;
	private boolean inParty = false;
	private SpectatorContainer container = null;
	private Client client;
	Timer timer = new Timer();

	public Spectator(String n, String u, String p) {
		name = n;
		username = u;
		password = p;
	}

	public void assignContainer(SpectatorContainer c) {
		container = c;
		inUse = true;
	}
	
	public void isPartied(String n){
		inParty = true;
		pLeader = n;
		container.party(pLeader);
	}
	
	public String getPartyLeader(){
		return pLeader;
	}

	public String getName(){
		return name;
	}
	
	public boolean isInUse(){
		return inUse;
	}
	
	public boolean isInParty(){
		return inParty;
	}
	
	public SpectatorContainer getContainer(){
		return container;
	}
	
	
	public void finish(boolean forced) {
		inUse = false;
		inParty = false;
		pLeader = "";
		container = null;
		if(forced){
			client.getSession().send(new ClientChatPacket("/pchat Spectator session expired! Logging off."));
		}
		client.getSession().send(new ClientChatPacket("/p leave"));
		try {
			timer.schedule(new DisconnectTimer(client), 500);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	public void login() {
		MinecraftProtocol protocol = null;
		try {
			protocol = new MinecraftProtocol(username, password, false);
			System.out.println("Successfully authenticated user.");
		} catch (RequestException e) {
			e.printStackTrace();
			return;
		}

		client = new Client("mc.hypixel.net", 25565, protocol, new TcpSessionFactory(Proxy.NO_PROXY));
		client.getSession().setFlag(MinecraftConstants.AUTH_PROXY_KEY, Proxy.NO_PROXY);
		client.getSession().addListener(new SpectatorChatBot(this));
		client.getSession().connect();
	}
	
	class DisconnectTimer extends TimerTask {
		Client caller;
		
		public DisconnectTimer(Client c){
			caller = c;
		}
		
		
		@Override
		public void run() {
			caller.getSession().disconnect("Finished!");
		}
		
	}

}

package p0ke.specbot.spectator;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerDisplayScoreboardPacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;

import p0ke.specbot.SpecBot;
import p0ke.specbot.util.EmojiMovieCountdown;

public class SpectatorChatBot extends SessionAdapter {

	private Spectator parent;
	
	private boolean is1v1 = false;

	public SpectatorChatBot(Spectator s) {
		parent = s;
	}

	@Override
	public void packetReceived(PacketReceivedEvent event) {
		try {
			if (event.getPacket() instanceof ServerChatPacket) {
				if(parent.isInUse()){
					Message message = ((ServerChatPacket) event.getPacket()).getMessage();
					String content = message.getFullText();
					if (!Pattern.compile(".*\\w:.*").matcher(content).matches()) {
						if (content.contains("has invited you to join") && content.contains("party")
								&& !parent.isInParty()) {
							content = content.replaceAll("-", "").trim();
							if (content.contains("'s")) {
								content = StringUtils.substringBeforeLast(content, "'s");
								if (content.contains("join [")) {
									content = StringUtils.substringAfterLast(content, "] ");
								} else {
									content = StringUtils.substringAfterLast(content, "join ");
								}
							} else {
								if (content.startsWith("[")) {
									content = StringUtils.substringAfter(content, "] ");
								}
								content = StringUtils.substringBefore(content, " has");
							}
	
							if (SpecBot.instance.banList.contains(content.toLowerCase())) {
								return;
							}
	
							if (parent.getContainer().isPartied()
									&& !content.equalsIgnoreCase(parent.getContainer().getPartyLeader())) {
								return;
							}
							parent.isPartied(content);
							parent.sendMessage("/p accept " + content);
							if (parent.getContainer().sentIntro()) {
								parent.sendMessage("/pchat I'm SmashHeroesSpec, an automatic 1v1 spectator bot!");
								parent.sendMessage("/p list");
							}
						}
	
						if (content.contains("joined the party!")) {
							if (content.startsWith("[")) {
								content = StringUtils.substringAfter(content, "] ");
							}
							content = StringUtils.substringBefore(content, " joined");
							if (SpecBot.instance.banList.contains(content.toLowerCase())) {
								if (!parent.getContainer().isFinished()) {
									parent.getContainer().finish(false);
								}
							}
						}
	
						if (content.contains("Party members")) {
							for (String user : SpecBot.instance.banList) {
								if (content.toLowerCase().contains(user)) {
									if (!parent.getContainer().isFinished()) {
										parent.getContainer().finish(false);
									}
									break;
								}
							}
						}
						
						if(content.contains("has joined (2/2)")){
							is1v1 = true;
							parent.sendMessage("/pchat 1v1 mode detected! Engaging training dummy mode.");
						}
						
	
						if (content.contains("                              Smash Heroes")) {
							if(!is1v1){
								parent.sendMessage("/lobby smash");
							}
							is1v1 = false;
							parent.getContainer().registerGame(DateTime.now());
						}
	
						if (content.endsWith("has disbanded the party!")
								|| content.startsWith("You have been kicked from the party")
								|| content.endsWith("the party has been disbanded.")) {
							if (!parent.getContainer().isFinished()) {
								parent.getContainer().finish(false);
							}
						}
						
						if(content.toLowerCase().contains(parent.getName().toLowerCase() + "  to party leader!")){
							parent.sendMessage("/p disband");
							
						}
	
						if (content.contains("Friend request from")) {
							content = StringUtils
									.substringAfterLast(StringUtils.substringBeforeLast(content, "[ACCEPT]").trim(), " ")
									.trim();
							parent.sendMessage("/friend accept " + content);
							parent.sendMessage("/friend add " + content);
						}
	
					} else if (content.startsWith("From")) {
						parent.sendMessage("/r " + EmojiMovieCountdown.getCountdown());
					}
				}
			} else if (event.getPacket() instanceof ServerDisplayScoreboardPacket) {
				String scoreboard = ((ServerDisplayScoreboardPacket)event.getPacket()).getScoreboardName();
				if(!(scoreboard.equalsIgnoreCase("supersmash") || scoreboard.equalsIgnoreCase("health") || scoreboard.equalsIgnoreCase("health_tab") || scoreboard.equalsIgnoreCase(parent.getName()))){
					parent.sendMessage("/lobby smash");
					parent.sendMessage("/pchat The use of SpecBot outside of Smash Heroes is not permitted. Returning to the Smash Heroes lobby...");
					System.out.println("Scoreboard: " + scoreboard);
					is1v1 = false;
				}

			} else if (event.getPacket() instanceof ServerKeepAlivePacket) {
				if (!parent.getMessages().isEmpty()) {
					event.getSession().send(new ClientChatPacket(parent.getMessages().get(0)));
					parent.getMessages().remove(0);
				} else {
					if (parent.isDisconnecting()) {
						parent.disconnect();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void disconnected(DisconnectedEvent event) {
		System.out.println("Disconnected: " + Message.fromString(event.getReason()).getFullText());
		if (!parent.getContainer().isFinished()) {
			parent.getContainer().finish(false);
		}
		parent.setDisconnecting(false);
		if (event.getCause() != null) {
			event.getCause().printStackTrace();
		}
	}

}
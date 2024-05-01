import tage.networking.server.GameConnectionServer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

public class GameAIServerUDP extends GameConnectionServer<UUID> {
  NPCcontroller npcCtrl;
  public GameAIServerUDP(int localPort, NPCcontroller npc){
    super(localPort, ProtocolType.UDP);
    npcCtrl = npc;
  }
  //---additional protocol for NPCs ----
  public void sendCheckForAvatarNear(){
    try{
      String message = new String("isnr");
      message += "," + (npcCtrl.getNPC()).getLocationX();
      message += "," + (npcCtrl.getNPC()).getLocationY();
      message += "," + (npcCtrl.getNPC()).getLocationZ();
      message += "," + (npcCtrl.getCriteria());
      sendPacketToAll();
    }
    catch (IOException e){
      System.out.println("couldn't send msg");
      e.printStackTrace();
    }
  }

  private void sendPacketToAll() {
  }

  public void sendNPCinfo(){

  }
  public void sendNPCstart( UUID clientID){

  }

  @Override
  public void processPacket(Object o, InetAddress senderIP, int port){
    String message = (String) o;
    String[] messageTokens = message.split(",");
    //TODO: might be missing more code above^

    //Case where server recieves request for NPCs
    //Recieved Message Format: (needNPC,id)
    if(messageTokens[0].compareTo("needNPC")==0){
      System.out.println("server got a needNPC message");
      UUID clientID = UUID.fromString(messageTokens[1]);
      sendNPCstart(clientID);
    }

    //Case where server receives notice that an avatar is close to the npc
    //Received Message Format: (isnear, id)
    if(messageTokens[0].compareTo("isnear")==0){
      UUID clientID = UUID.fromString(messageTokens[1]);
      handleNearTiming(clientID);
    }
  }
  public void handleNearTiming(UUID clientID){
    npcCtrl.setNearFlag(true);
  }

  //--------------SENDING NPC MESSAGES ----------------
  //Informs clients of the whereabouts of NPCs.
  public void sendCreateNPCmsg(UUID clientID, String[] position){
    try{
      System.out.println("server telling clients about an NPC");
      String message = new String("createNPC," + clientID.toString());
      message += "," + position[0];
      message += "," + position[1];
      message += "," + position[2];
      forwardPacketToAll(message, clientID);
    }catch (IOException e){
      e.printStackTrace();
    }
  }
}

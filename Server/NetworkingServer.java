import java.io.IOException;

public class NetworkingServer 
{
	private GameServerUDP thisUDPServer;
	private GameServerTCP thisTCPServer;
	//fallowing code14 NPC
	private GameAIServerUDP UDPServer;
	private NPCcontroller npcCtrl;

	public NetworkingServer(int serverPort, String protocol)
	{
		//npcCtrl = new NPCcontroller();
		//start networking server
		try
		{	if(protocol.toUpperCase().compareTo("TCP") == 0)
			{	thisTCPServer = new GameServerTCP(serverPort);
			}
			else
			{	//TODO: choose which udp server?
				thisUDPServer = new GameServerUDP(serverPort);
				//UDPServer = new GameAIServerUDP(serverPort, npcCtrl);
			}
		} 
		catch (IOException e) 
		{	System.out.println("server didn't start");
			e.printStackTrace();
		}
		//npcCtrl.start(UDPServer);
	}

	public static void main(String[] args) 
	{	if(args.length > 1)
		{	NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]), args[1]);
		}
	}

}

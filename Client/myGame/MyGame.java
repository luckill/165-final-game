package myGame;

import net.java.games.input.Component;
import tage.*;
import tage.cameraController.CameraOrbit3D;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;

import java.lang.Math;

import java.awt.event.*;

import java.io.*;
import org.joml.*;
import java.net.InetAddress;

import java.net.UnknownHostException;

import tage.networking.IGameConnection.ProtocolType;

public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	private InputManager inputManager;
	private GhostManager gm;

	private int counter = 0;
	private Vector3f currentPosition;
	private Matrix4f initialTranslation, initialRotation, initialScale;
	private double startTime, prevTime, elapsedTime;

	private GameObject avatar, x, y, z, human, terrain, groundPlane;
	private ObjShape  line1, line2, line3, humanShape, terrainShape, plane;
	private TextureImage humanTexture, grass, terrainHeightMap, groundPlaneTexture;
	private Light light;
	private int fps, desert, volcano;

	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;
	private CameraOrbit3D cameraOrbitController;

	public MyGame(String serverAddress, int serverPort, String protocol)
	{	super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	public static void main(String[] args)
	{	MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes()
	{
		//ghostS = new Sphere();
		line1 = new Line(new Vector3f(0, 0, 0), new Vector3f(100.0f, 0, 0));
		line2 = new Line(new Vector3f(0, 0, 0), new Vector3f(0, 100.0f, 0));
		line3 = new Line(new Vector3f(0, 0, 0), new Vector3f(0, 0, 100.0f));
		System.out.println("loading human now");
		humanShape = new ImportedModel("human.obj");
		grenadeShape = new ImportModel ("grenade.obj");
		terrainShape = new TerrainPlane(1000);
		plane = new Plane();
	}

	@Override
	public void loadSkyBoxes()
	{
		fps = engine.getSceneGraph().loadCubeMap("fps");
		desert = engine.getSceneGraph().loadCubeMap("desert");
		volcano = engine.getSceneGraph().loadCubeMap("volcano");
		engine.getSceneGraph().setActiveSkyBoxTexture(fps);
		engine.getSceneGraph().setSkyBoxEnabled(true);
	}

	@Override
	public void loadTextures()
	{
		//ghostT = new TextureImage("redDolphin.jpg");
		humanTexture = new TextureImage("humanUvUnwrap_colored.png");
		grenadeTexture = new TextureImage("grenade.png");
		grass = new TextureImage("grass.jpg");
		terrainHeightMap = new TextureImage("HeightMap.png");
		groundPlaneTexture = new TextureImage("ground plane.png");
	}

	@Override
	public void buildObjects()
	{
		Matrix4f initialTranslation, initialRotation, initialScale;
		// add X,Y,-Z axes
		x = new GameObject(GameObject.root(), line1);
		y = new GameObject(GameObject.root(), line2);
		z = new GameObject(GameObject.root(), line3);
		(x.getRenderStates()).setColor(new Vector3f(1f,0f,0f));
		(y.getRenderStates()).setColor(new Vector3f(0f,1f,0f));
		(z.getRenderStates()).setColor(new Vector3f(0f,0f,1f));

		avatar = new GameObject(GameObject.root(), humanShape, humanTexture);
		initialTranslation = new Matrix4f().translation(0,0,0);
		initialRotation = new Matrix4f().rotationY((float)java.lang.Math.toRadians(180.0f));
		initialScale = (new Matrix4f()).scaling(0.5f, 0.5f, 0.5f);
		avatar.setLocalTranslation(initialTranslation);
		avatar.setLocalRotation(initialRotation);
		avatar.setLocalScale(initialScale);
		avatar.getRenderStates().setModelOrientationCorrection(new Matrix4f().rotationY((float)Math.toRadians(90.0f)));
		
		//TODO
		grenade = new GameObject (GameObject.root(), grenadeShape, grenadeTexture);
		
		terrain = new GameObject(GameObject.root(), terrainShape, grass);
		terrain.setLocalTranslation(new Matrix4f().translation(0f,-0.01f,0f));
		terrain.setLocalScale(new Matrix4f().scaling(500.0f,100.0f,500.0f));
		terrain.setHeightMap(terrainHeightMap);
		terrain.getRenderStates().setTiling(1);
		terrain.getRenderStates().setTileFactor(10);

		groundPlane = new GameObject(GameObject.root(), plane, groundPlaneTexture);
		groundPlane.getRenderStates().setTiling(1);
		groundPlane.getRenderStates().setTileFactor(12);
		groundPlane.setLocalScale(new Matrix4f().scaling(1000.0f));
	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(.5f, .5f, .5f);
		light = new Light();
		light.setLocation(new Vector3f(0f, 5f, 0f));
		(engine.getSceneGraph()).addLight(light);
	}

	@Override
	public void initializeGame()
	{	prevTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);
		cameraOrbitController = new CameraOrbit3D(engine.getRenderSystem().getViewport("MAIN").getCamera(), avatar, engine);

		// ----------------- INPUTS SECTION -----------------------------
		setupNetworking();
		inputManager = engine.getInputManager();
		inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.A, new InputAction(avatar, InputType.LEFT, protClient), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.D, new InputAction(avatar, InputType.RIGHT, protClient), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.W, new InputAction(avatar,InputType.FORWARD, protClient), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.S, new InputAction(avatar, InputType.BACKWARD, protClient), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
	}

	public GameObject getAvatar() { return avatar; }

	@Override
	public void update()
	{	elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		cameraOrbitController.updateCameraPosition();
		//Vector3f location = avatar.getWorldLocation();
		//float height = terrain.getHeight(location.x(), location.z());
		//avatar.setLocalLocation(new Vector3f(location.x(), height+0.75f, location.y()));
		Vector3f loc = avatar.getWorldLocation();
		float height = terrain.getHeight(loc.x(), loc.z());
		avatar.setLocalLocation(new Vector3f(loc.x(), height + 0.25f, loc.z()));

		// build and set HUD
		int elapsTimeSec = Math.round((float)(System.currentTimeMillis()-startTime)/1000.0f);
		String elapsTimeStr = Integer.toString(elapsTimeSec);
		String counterStr = Integer.toString(counter);
		String dispStr1 = "Time = " + elapsTimeStr;
		String dispStr2 = "camera position = ";
		Vector3f hud1Color = new Vector3f(1,0,0);
		Vector3f hud2Color = new Vector3f(1,1,1);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);
		(engine.getHUDmanager()).setHUD2(dispStr2, hud2Color, 500, 15);

		// update inputs and camera
		inputManager.update((float)elapsedTime);
		processNetworking((float)elapsedTime);
	}

	@Override
	public void keyPressed(KeyEvent e)
	{	switch (e.getKeyCode())
		{
			case KeyEvent.VK_1:
				engine.getSceneGraph().setActiveSkyBoxTexture(fps);
				engine.getSceneGraph().setSkyBoxEnabled(true);
				break;
			case KeyEvent.VK_2:
				engine.getSceneGraph().setActiveSkyBoxTexture(desert);
				engine.getSceneGraph().setSkyBoxEnabled(true);
				break;
			case KeyEvent.VK_3:
				engine.getSceneGraph().setActiveSkyBoxTexture(volcano);
				engine.getSceneGraph().setSkyBoxEnabled(true);
				break;
			case KeyEvent.VK_4:
				engine.getSceneGraph().setSkyBoxEnabled(false);
				break;
		}
		super.keyPressed(e);
	}

	// ---------- NETWORKING SECTION ----------------

	public ObjShape getGhostShape() { return humanShape; }
	public TextureImage getGhostTexture() { return humanTexture; }
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }
	
	private void setupNetworking()
	{	isClientConnected = false;	
		try 
		{	protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} 	catch (UnknownHostException e) 
		{	e.printStackTrace();
		}	catch (IOException e) 
		{	e.printStackTrace();
		}
		if (protClient == null)
		{	System.out.println("missing protocol host");
		}
		else
		{	// Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage();
		}
	}
	
	protected void processNetworking(float elapsTime)
	{	// Process packets received by the client from the server
		if (protClient != null)
			protClient.processPackets();
	}

	public Vector3f getPlayerPosition() { return avatar.getWorldLocation(); }

	public void setIsConnected(boolean value) { this.isClientConnected = value; }
	
	private class SendCloseConnectionPacketAction extends AbstractInputAction
	{	@Override
		public void performAction(float time, net.java.games.input.Event evt) 
		{	if(protClient != null && isClientConnected == true)
			{
				protClient.sendByeMessage();
			}
		}
	}
}
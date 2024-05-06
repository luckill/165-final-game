package myGame;

import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import net.java.games.input.Component;
import tage.*;
import tage.audio.*;
import tage.cameraController.CameraOrbit3D;
import tage.physics.JBullet.JBulletPhysicsEngine;
import tage.physics.JBullet.JBulletPhysicsObject;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;

import java.awt.*;
import java.lang.Math;

import java.awt.event.*;

import java.io.*;
import org.joml.*;
import java.net.InetAddress;

import java.net.UnknownHostException;

import tage.networking.IGameConnection.ProtocolType;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;

public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	private InputManager inputManager;
	private GhostManager gm;
	private int counter = 0;
	private double startTime, prevTime, elapsedTime;
	private GameObject avatar, x, y, z, terrain, groundPlane, grenade, speaker, animatedAvatar, gun;
	private AnimatedShape humanAnimatedShape, gunAnimatedShape;
	private ObjShape  line1, line2, line3, humanShape, ghostShape, terrainShape, plane, dolphinShape, grenadeShape, speakerShape, gunShape;
	private TextureImage humanTexture, grass, terrainHeightMap, groundPlaneTexture, grenadeTexture, ghostTexture;
	private PhysicsEngine physicsEngine;
	private PhysicsObject grenadeCapsule, physicsPlane;
	private boolean running = false;
	private float vals[] = new float[16];
	private Light light;
	private int fps, desert, volcano;
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;
	private CameraOrbit3D cameraOrbitController;
	private IAudioManager audioManager;
	private Sound explosionSound, backgroundMusic, gunShotSound;
	private boolean exploded = false;
	private float[] gravity,up;
	private double[] tempTransform;
	private float mass, radius, height;
	private AudioResource resource1, resource2, resource3;

	private Robot robot;
	private float currentMouseX, currentMouseY, centerMouseX, centerMouseY;
	private float previousMouseX, previousMouseY;
	private boolean isRendering;
	private Camera camera;
	private double time, previousTime;

	//testing
	private boolean stopPrinting = false;
	private boolean readyToExplode = false;
	private long throwTime;

	public MyGame(String serverAddress, int serverPort, String protocol)
	{
		super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	public static void main(String[] args)
	{
		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}


	@Override
	public void loadShapes()
	{
		line1 = new Line(new Vector3f(0, 0, 0), new Vector3f(100.0f, 0, 0));
		line2 = new Line(new Vector3f(0, 0, 0), new Vector3f(0, 100.0f, 0));
		line3 = new Line(new Vector3f(0, 0, 0), new Vector3f(0, 0, 100.0f));
		System.out.println("loading human now");
		humanShape = new ImportedModel("human.obj");
		humanAnimatedShape = new AnimatedShape("humanwave.rkm", "humanwave.rks");
		humanAnimatedShape.loadAnimation("WAVE", "humanwave.rka");
		humanAnimatedShape.loadAnimation("DEAD", "humanDead.rka");

		gunAnimatedShape = new AnimatedShape("gun.rkm", "gun.rks");
		gunAnimatedShape.loadAnimation("SWING", "gun_swing.rka");

		ghostShape = humanShape;
		grenadeShape = new ImportedModel ("grenade.obj");
		terrainShape = new TerrainPlane(1000);
		dolphinShape = new ImportedModel("dolphinHighPoly.obj");
		plane = new Plane();
		speakerShape = new Cube();
		gunShape = new ImportedModel("gun.obj");
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
		humanTexture = new TextureImage("humanUvUnwrap_colored.png");
		grass = new TextureImage("grass.jpg");
		terrainHeightMap = new TextureImage("HeightMap.png");
		groundPlaneTexture = new TextureImage("ground plane.png");
		grenadeTexture = new TextureImage("grenade.png");
		ghostTexture = humanTexture;
	}

	@Override
	public void loadSounds()
	{
		audioManager = engine.getAudioManager();
		resource1 = audioManager.createAudioResource("assets/sounds/explosionSound.wav", AudioResourceType.AUDIO_SAMPLE);
		explosionSound = new Sound(resource1, SoundType.SOUND_EFFECT, 100, false);
		explosionSound.initialize(audioManager);
		explosionSound.setMaxDistance(10.0f);
		explosionSound.setMinDistance(0.5f);
		explosionSound.setRollOff(0.05f);

		resource2 = audioManager.createAudioResource("assets/sounds/backgroundMusic.wav", AudioResourceType.AUDIO_STREAM);
		backgroundMusic = new Sound(resource2, SoundType.SOUND_MUSIC, 100, true);
		backgroundMusic.initialize(audioManager);
		backgroundMusic.setMaxDistance(20.0f);
		backgroundMusic.setMinDistance(0.5f);
		backgroundMusic.setRollOff(0.05f);

		resource3 = audioManager.createAudioResource("assets/sounds/gunShot.wav", AudioResourceType.AUDIO_SAMPLE);
		gunShotSound = new Sound(resource3, SoundType.SOUND_EFFECT, 100, false);
		gunShotSound.initialize(audioManager);
		gunShotSound.setMaxDistance(20.0f);
		gunShotSound.setMinDistance(0.5f);
		gunShotSound.setRollOff(0.05f);
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

		avatar = new GameObject(GameObject.root(), humanAnimatedShape, humanTexture);
		initialTranslation = new Matrix4f().translation(0,0,0);
		initialRotation = new Matrix4f().rotationY((float)java.lang.Math.toRadians(180.0f));
		initialScale = (new Matrix4f()).scaling(0.75f);
		avatar.setLocalTranslation(initialTranslation);
		avatar.setLocalRotation(initialRotation);
		avatar.setLocalScale(initialScale);
		avatar.getRenderStates().setModelOrientationCorrection(new Matrix4f().rotationY((float)Math.toRadians(90.0f)));

		/*terrain = new GameObject(GameObject.root(), terrainShape, grass);
		terrain.setLocalTranslation(new Matrix4f().translation(0f,-0.01f,0f));
		terrain.setLocalScale(new Matrix4f().scaling(500.0f,100.0f,500.0f));
		terrain.setHeightMap(terrainHeightMap);
		terrain.getRenderStates().setTiling(1);
		terrain.getRenderStates().setTileFactor(10);*/

		groundPlane = new GameObject(GameObject.root(), plane, groundPlaneTexture);
		groundPlane.getRenderStates().setTiling(1);
		groundPlane.getRenderStates().setTileFactor(12);
		groundPlane.setLocalScale(new Matrix4f().scaling(5000));

		speaker = new GameObject(GameObject.root(), speakerShape);
		speaker.setParent(avatar);
		speaker.setLocalTranslation(avatar.getWorldTranslation().translate(0,0,-10));
		speaker.propagateRotation(false);

		gun = new GameObject(avatar, gunAnimatedShape);
		gun.setLocalTranslation(new Matrix4f().translate(0,0.75f,2.0f));
		gun.setLocalScale(new Matrix4f().scaling(0.2f));
		//gun.getRenderStates().setModelOrientationCorrection(new Matrix4f().rotationY((float)Math.toRadians(0)));
		gun.applyParentRotationToPosition(true);
	}

	@Override
	public void initializeLights()
	{
		Light.setGlobalAmbient(.5f, .5f, .5f);
		light = new Light();
		light.setLocation(new Vector3f(0f, 5f, 0f));
		(engine.getSceneGraph()).addLight(light);
	}

	@Override
	public void initializeGame()
	{
		//initMouseMode();
		prevTime = System.currentTimeMillis();
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
		inputManager.associateActionWithAllMouse(Component.Identifier.Button.LEFT, new WeaponAction(gunAnimatedShape, gunShotSound), IInputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		//initialize physics system
		gravity = new float[]{0.0f, -5.0f, 0.0f};
		physicsEngine = engine.getSceneGraph().getPhysicsEngine();
		physicsEngine.setGravity(gravity);

		//create physics world
		mass = 1.0f;
		up = new float[]{0, 1, 0};
		radius = 0.25f;
		height = 1.5f;

		Matrix4f translation = new Matrix4f(groundPlane.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		physicsPlane = engine.getSceneGraph().addPhysicsStaticPlane(tempTransform, up,0.0f);
		physicsPlane.setBounciness(1.0f);
		groundPlane.setPhysicsObject(physicsPlane);

		//engine.enableGraphicsWorldRender();
		//engine.enablePhysicsWorldRender();

		backgroundMusic.setLocation(speaker.getWorldLocation());
		setEarParameters();
		backgroundMusic.play();
	}

	public GameObject getAvatar()
	{
		return avatar;
	}

	@Override
	public void update()
	{
		elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		float amt = (float) (elapsedTime * 0.03f);
		cameraOrbitController.updateCameraPosition();
		/*camera = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		Vector3f forwardVector = avatar.getWorldForwardVector();
		Vector3f upVector = avatar.getWorldUpVector();
		Vector3f rightVector = avatar.getWorldRightVector();
		camera.setLocation(avatar.getLocalLocation().add(upVector.mul(2.0f)));*/

		//&& running
		if (grenade == null && running)
		{
			grenade = new GameObject(GameObject.root(), grenadeShape, grenadeTexture);
			grenade.setLocalLocation(avatar.getWorldLocation());
			Matrix4f translation = new Matrix4f(grenade.getLocalTranslation());
			tempTransform = toDoubleArray(translation.get(vals));
			grenadeCapsule = engine.getSceneGraph().addPhysicsCapsuleX(mass, tempTransform, radius, height);
			//float[] linearVelocity = {1.0f, 7.0f, -10.0f};
			grenadeCapsule.setBounciness(0.5f);
			grenadeCapsule.setFriction(50.0f);
			Vector3f location = avatar.getWorldForwardVector();
			System.out.println(location);
			float[] linearVelocity = new float[3];
			linearVelocity[0] = location.x() * 10;
			linearVelocity[1] = 6.0f;
			linearVelocity[2] = location.z() * 10;
			grenadeCapsule.setLinearVelocity(linearVelocity);
			grenade.setPhysicsObject(grenadeCapsule);

			System.out.println("\n\n\nthrowing grenade now");

			throwTime = System.currentTimeMillis();
			//System.out.println("throw time = " + throwTime);
		}

		if (running)
		{
			int elapsedTime = Math.round((float)(System.currentTimeMillis()-throwTime)/1000.0f);
			if(elapsedTime == 5)
			{
				readyToExplode = true;
			}
			//System.out.println(elapsedTime + "second(s)");
			checkForCollision();
			float[] velocity = grenadeCapsule.getLinearVelocity();

			double xzVelocity = Math.sqrt(Math.pow(velocity[0],2) + Math.pow(velocity[2], 2));
			if(!stopPrinting && readyToExplode)
			{
				//System.out.println("x linear velocity is: " +  velocity[0] + "z linear velocity is :" + velocity[1]);
				System.out.println("velocity is: " + xzVelocity);
				System.out.println("grenade ready to explode");
				
			}
			if(xzVelocity < 1.6f && readyToExplode)
			{
				if (!exploded)
				{
					explosionSound.play();
					exploded = true;
					stopPrinting = true;
				}
				engine.getSceneGraph().removeGameObject(grenade);
				engine.getSceneGraph().removePhysicsObject(grenadeCapsule);
				if(!explosionSound.getIsPlaying())
				{
					grenade = null;
					grenadeCapsule = null;
					running = false;
					exploded = false;
					stopPrinting = false;
					readyToExplode = false;
				}
			}
		}

        AxisAngle4f aa = new AxisAngle4f();
        Matrix4f mat = new Matrix4f();
        Matrix4f mat2 = new Matrix4f().identity();
        Matrix4f mat3 = new Matrix4f().identity();
        checkForCollision();
        physicsEngine.update((float) elapsedTime);
        for (GameObject go : engine.getSceneGraph().getGameObjects())
        {
            if (go.getPhysicsObject() != null)
            {
                mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
                mat2.set(3, 0, mat.m30());
                mat2.set(3, 1, mat.m31());
                mat2.set(3, 2, mat.m32());
                go.setLocalTranslation(mat2);
                mat.getRotation(aa);
                mat3.rotation(aa);
                go.setLocalRotation(mat3);
            }
        }

        Vector3f loc = avatar.getWorldLocation();
		//float height = terrain.getHeight(loc.x(), loc.z());
		//avatar.setLocalLocation(new Vector3f(loc.x(), height + 0.25f, loc.z()));

		// build and set HUD
		int elapsTimeSec = Math.round((float)(System.currentTimeMillis()-startTime)/1000.0f);
		String elapsTimeStr = Integer.toString(elapsTimeSec);
		String counterStr = Integer.toString(counter);
		String dispStr1 = "Time = " + elapsTimeStr;
		String dispStr2 = "avatar position = " + avatar.getWorldLocation();
		Vector3f hud1Color = new Vector3f(1,0,0);
		Vector3f hud2Color = new Vector3f(1,1,1);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);
		(engine.getHUDmanager()).setHUD2(dispStr2, hud2Color, 500, 15);

		// update inputs and camera
		inputManager.update((float)elapsedTime);
		processNetworking((float)elapsedTime);

		//update sound
		if (grenade == null)
		{
			explosionSound.setLocation(GameObject.root().getWorldLocation());
		}
		else
		{
			explosionSound.setLocation(grenade.getWorldLocation());
		}
		backgroundMusic.setLocation(speaker.getWorldLocation());
		setEarParameters();

		gunShotSound.setLocation(gun.getWorldLocation());
		humanAnimatedShape.updateAnimation();
		gunAnimatedShape.updateAnimation();
		engine.getRenderSystem().getShape();
	}

	public void setEarParameters()
	{
		Camera camera = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		audioManager.getEar().setLocation(avatar.getWorldLocation());
		audioManager.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
	}

	/*public void mouseMoved(MouseEvent mouseEvent)
	{
		if(isRendering && centerMouseX == mouseEvent.getXOnScreen() && centerMouseY == mouseEvent.getYOnScreen())
		{
			isRendering = false;
		}
		else
		{
			currentMouseX = mouseEvent.getXOnScreen();
			currentMouseY = mouseEvent.getYOnScreen();
			float mouseDeltaX = previousMouseX - currentMouseX;
			float mouseDeltaY = previousMouseY - currentMouseY;
			yaw(mouseDeltaX);
			previousMouseX = currentMouseX;
			previousMouseY = currentMouseY;
     		// tell robot to put the cursor to the center (since user just moved it)
			recenterMouse();
			previousMouseX = centerMouseX; // reset prev to center
			previousMouseY = centerMouseY;
		}
	}
	public void yaw(float mouseDeltaX)
	{
		float tilt;
		Camera camera = engine.getRenderSystem().getViewport("MAIN").getCamera();
		Vector3f rightVector = camera.getU();
		Vector3f upVector  = camera.getV();
		Vector3f fwdVector = camera.getN();

		if (mouseDeltaX < 0.0)
		{
			tilt = -1.0f;
		}
		else if (mouseDeltaX > 0.0)
		{
			tilt = 1.0f;
		}
		else
		{
			tilt = 0.0f;
		}
		rightVector.rotateAxis(0.01f * tilt, upVector.x(), upVector.y(), upVector.z());
		fwdVector.rotateAxis(0.01f * tilt, upVector.x(), upVector.y(), upVector.z());
		avatar.globalYaw(0.01f*tilt);
		camera.setU(rightVector);
		camera.setN(fwdVector);
	}*/

	private void checkForCollision()
	{
		DynamicsWorld dynamicsWorld = ((JBulletPhysicsEngine)physicsEngine).getDynamicsWorld();
		Dispatcher dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();
		for (int i = 0; i < manifoldCount; i++)
		{
			PersistentManifold manifold = dispatcher.getManifoldByIndexInternal(i);
			RigidBody object1 = (RigidBody)manifold.getBody0();
			RigidBody object2 = (RigidBody)manifold.getBody1();
			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);
			for(int j = 0; j < manifold.getNumContacts(); j++)
			{
				ManifoldPoint contactPoint = manifold.getContactPoint(j);
				if(contactPoint.getDistance() < 0.0f)
				{
					//System.out.println("hit between " + obj1 + "and" + obj2);
					break;
				}
			}
		}
	}

	// Method to apply damage to the target
	private void applyDamage(RigidBody targetObject)
	{
		// Apply damage logic here
	}

	private float[] toFloatArray(double[] array)
	{
		if(array == null)
		{
			return null;
		}
		int n = array.length;
		float[] result = new float[n];
		for(int i = 0; i < n; i++)
		{
			result[i] = (float)array[i];
		}
		return result;
	}

	private double[] toDoubleArray(float[] array)
	{
		if(array == null)
		{
			return null;
		}
		int n = array.length;
		double[] result = new double[n];
		for(int i = 0; i < n; i++)
		{
			result[i] = (double)array[i];
		}
		return result;
	}

	/*private void initMouseMode()
	{
		RenderSystem renderSystem = engine.getRenderSystem();
		Viewport viewport = renderSystem.getViewport("MAIN");
		float left = viewport.getActualLeft();
		float bottom = viewport.getActualBottom();
		float width = viewport.getActualWidth();
		float height = viewport.getActualHeight();
		centerMouseX = (int) (left + height/2);
		centerMouseY = (int) (bottom - height/2);
		isRendering = false;
		try
		{
			robot = new Robot();
		}
		catch(AWTException exception)
		{
			throw new RuntimeException("couldn't create robot");
		}
		recenterMouse();
		previousMouseX = centerMouseX;
		previousMouseY = centerMouseY;
	}

	private void recenterMouse()
	{
		RenderSystem renderSystem = engine.getRenderSystem();
		Viewport viewport = renderSystem.getViewport("MAIN");
		float left = viewport.getActualLeft();
		float bottom = viewport.getActualBottom();
		float width = viewport.getActualWidth();
		float height = viewport.getActualHeight();
		centerMouseX = (int) (left + width/2.0f);
		centerMouseY = (int) (bottom - height/2.0f);
		isRendering = true;
		robot.mouseMove((int)centerMouseX, (int)centerMouseY);
	}*/

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
			case KeyEvent.VK_SPACE:
				running = true;
				break;
			case KeyEvent.VK_P:
				if(!backgroundMusic.getIsPlaying())
				{
					backgroundMusic.play();
				}
				else
				{
					backgroundMusic.pause();
				}
				break;
			case KeyEvent.VK_7:
				humanAnimatedShape.playAnimation("WAVE", 0.1f, AnimatedShape.EndType.LOOP, 0);
				System.out.println("animation is playing");
				break;
			case KeyEvent.VK_8:
				humanAnimatedShape.stopAnimation();
				break;
			case KeyEvent.VK_9:
				//humanAnimatedShape.playAnimation("DEAD");
				break;
		}
		super.keyPressed(e);
	}

	// ---------- NETWORKING SECTION ----------------

	public ObjShape getGhostShape() { return ghostShape; }
	public TextureImage getGhostTexture() { return ghostTexture; }
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }
	
	private void setupNetworking()
	{	isClientConnected = false;	
		try 
		{
			protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		if (protClient == null)
		{
			System.out.println("missing protocol host");
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
	{
		@Override
		public void performAction(float time, net.java.games.input.Event evt) 
		{	if(protClient != null && isClientConnected == true)
			{
				protClient.sendByeMessage();
			}
		}
	}
}
package myGame;

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
import java.util.*;
import java.util.Random;

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
    private GameObject avatar, x, y, z, terrain, groundPlane, grenade, speaker, animatedAvatar, gun, target;
    private AnimatedShape humanAnimatedShape, humanGunAnimatedShape, ghostAnimatedShape, ghostGunAnimatedShape;

    private ObjShape line1, line2, line3, ghostGunShape, terrainShape, plane, grenadeShape, speakerShape;

    private TextureImage humanTexture, grass, terrainHeightMap, groundPlaneTexture, grenadeTexture, ghostTexture;
    private PhysicsEngine physicsEngine;
    private PhysicsObject grenadeCapsule, physicsPlane, bullet, sphere, avatarBox, grenadeSphere;
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
    public static boolean shoot = false;
    private float[] gravity, up;
    private double[] tempTransform;
    private float radius, height;
    private float mass = 1.0f;
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
    private boolean grenadeSphereRemoved = false;
    private long throwTime, explodeTime;
    //private Map<Integer, BulletInfo> bullets = new HashMap<>();
    private Map<Integer, PhysicsObject> bullets = new HashMap<>();
    private ArrayList<PhysicsObject> spheres = new ArrayList<>();
    private float[] velocity;
    private double xzVelocity;
    private Timer timer;

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
        humanAnimatedShape = new AnimatedShape("humanwave.rkm", "humanwave.rks");
        humanAnimatedShape.loadAnimation("WAVE", "humanwave.rka");
        humanAnimatedShape.loadAnimation("DEAD", "humanDead.rka");

        humanGunAnimatedShape = new AnimatedShape("gun.rkm", "gun.rks");
        humanGunAnimatedShape.loadAnimation("SWING", "gun_swing.rka");

        ghostAnimatedShape = new AnimatedShape("humanwave.rkm", "humanwave.rks");
        ghostAnimatedShape.loadAnimation("WAVE", "humanwave.rka");
        ghostAnimatedShape.loadAnimation("DEAD", "humanDead.rka");

        ghostGunAnimatedShape = new AnimatedShape("gun.rkm", "gun.rks");
        ghostGunAnimatedShape.loadAnimation("SWING", "gun_swing.rka");

        grenadeShape = new ImportedModel("grenade.obj");
        terrainShape = new TerrainPlane(1000);
        plane = new Plane();
        speakerShape = new Cube();
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
        (x.getRenderStates()).setColor(new Vector3f(1f, 0f, 0f));
        (y.getRenderStates()).setColor(new Vector3f(0f, 1f, 0f));
        (z.getRenderStates()).setColor(new Vector3f(0f, 0f, 1f));

        avatar = new GameObject(GameObject.root(), humanAnimatedShape, humanTexture);
        initialTranslation = new Matrix4f().translation(0, 0, 0);
        initialRotation = new Matrix4f().rotationY((float) java.lang.Math.toRadians(180.0f));
        initialScale = (new Matrix4f()).scaling(0.75f);
        avatar.setLocalTranslation(initialTranslation);
        avatar.setLocalRotation(initialRotation);
        avatar.setLocalScale(initialScale);
        avatar.getRenderStates().setModelOrientationCorrection(new Matrix4f().rotationY((float) Math.toRadians(90.0f)));

        //
        Matrix4f avatarTranslation = avatar.getLocalTranslation().translate(0,0.95f,0);
        tempTransform = toDoubleArray(avatarTranslation.get(vals));
        avatarBox = engine.getSceneGraph().addPhysicsBox(100000.0f, tempTransform, new float[]{1.25f, 2.50f, 0.91f}, "");
        avatarBox.setFriction(0.0f);
        avatarBox.setBounciness(0.0f);
        gravity = new float[]{0.0f, -5.0f, 0.0f};
        //avatarBox.applyForce(gravity[0], gravity[1], gravity[2], 0.625f ,1.125f,0.455f);

		/*terrain = new GameObject(GameObject.root(), terrainShape, grass);
		terrain.setLocalTranslation(new Matrix4f().translation(0f,-0.01f,0f));
		terrain.setLocalScale(new Matrix4f().scaling(500.0f,100.0f,500.0f));
		terrain.setHeightMap(terrainHeightMap);
		terrain.getRenderStates().setTiling(1);
		terrain.getRenderStates().setTileFactor(10);*/
        /*Set<Integer> axisSet = new HashSet<>();
        Random random = new Random();
        while (axisSet.size() < 8)
        {
            axisSet.add(random.nextInt(51) + 20);
        }

        Iterator<Integer> iterator = axisSet.iterator();
        for (int i = 0; i < 4; i++)
        {
            initialTranslation = (new Matrix4f()).translation(iterator.next(), 0, iterator.next());
            tempTransform = toDoubleArray(initialTranslation.get(vals));
            sphere = engine.getSceneGraph().addPhysicsSphere(1.0f, tempTransform, 5.00f);
            spheres.add(sphere);
        }*/

        groundPlane = new GameObject(GameObject.root(), plane, groundPlaneTexture);
        groundPlane.getRenderStates().setTiling(1);
        groundPlane.getRenderStates().setTileFactor(12);
        groundPlane.setLocalScale(new Matrix4f().scaling(5000));

        speaker = new GameObject(GameObject.root(), speakerShape);
        speaker.setParent(avatar);
        speaker.setLocalTranslation(avatar.getWorldTranslation().translate(0, 0, -10));
        speaker.propagateRotation(false);
        //avatar.setPhysicsObject(avatarBox);

        gun = new GameObject(avatar, humanGunAnimatedShape);
        gun.setLocalTranslation(avatar.getWorldTranslation().translate(0, 0.75f, 2.0f));
        gun.setLocalScale(new Matrix4f().scaling(0.2f));
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
        (engine.getRenderSystem()).setWindowDimensions(1900, 1000);
        cameraOrbitController = new CameraOrbit3D(engine.getRenderSystem().getViewport("MAIN").getCamera(), avatar, engine);
        // ----------------- INPUTS SECTION -----------------------------
        setupNetworking();
        inputManager = engine.getInputManager();
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.A, new InputAction(avatar, InputType.LEFT, protClient), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.D, new InputAction(avatar, InputType.RIGHT, protClient), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.W, new InputAction(avatar, InputType.FORWARD, protClient), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.S, new InputAction(avatar, InputType.BACKWARD, protClient), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllMouse(Component.Identifier.Button.LEFT, new WeaponAction(humanGunAnimatedShape, protClient,gunShotSound), IInputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

        //initialize physics system

        physicsEngine = engine.getSceneGraph().getPhysicsEngine();
        physicsEngine.setGravity(gravity);

        //create physics world
        mass = 1.0f;
        up = new float[]{0, 1, 0};
        radius = 0.25f;
        height = 1.5f;

        Matrix4f translation = new Matrix4f(groundPlane.getLocalTranslation());
        tempTransform = toDoubleArray(translation.get(vals));
        physicsPlane = engine.getSceneGraph().addPhysicsStaticPlane(tempTransform, up, 0.0f);
        physicsPlane.setBounciness(1.0f);
        groundPlane.setPhysicsObject(physicsPlane);


        engine.enableGraphicsWorldRender();
        engine.enablePhysicsWorldRender();

        backgroundMusic.setLocation(speaker.getWorldLocation());
        setEarParameters();
        backgroundMusic.play();
        //initMouseMode();
    }

    public GameObject getAvatar()
    {
        return avatar;
    }

    @Override
    public void update()
    {
        System.out.println(avatar.getWorldTranslation());
        elapsedTime = System.currentTimeMillis() - prevTime;
        prevTime = System.currentTimeMillis();
        cameraOrbitController.updateCameraPosition();
        //camera = (engine.getRenderSystem().getViewport("MAIN").getCamera());
        Vector3f upVector = avatar.getWorldUpVector();
        //camera.setLocation(avatar.getLocalLocation().add(upVector.mul(2.0f)));
        Matrix4f avatarTranslation = avatar.getWorldTranslation().translate(0,0.95f,0);
        float x = avatarTranslation.get(3,0);
        float y = avatarTranslation.get(3,1);
        float z = avatarTranslation.get(3,2);
        float[] transform = toFloatArray(avatarBox.getTransform());

        transform[12] = x;
        transform[13] = y;
        transform[14] = z;
        //Matrix4f matrix = new Matrix4f();
        //Matrix4f matrix2 = new Matrix4f();
        //AxisAngle4f axisAngle = new AxisAngle4f();
        //matrix2.set(transform);

        //avatar.getWorldRotation().getRotation(axisAngle);
        //matrix2.rotation(axisAngle);
        //float[] finalTransform = matrix2.get(vals);
        avatarBox.setTransform(toDoubleArray(transform));


        //tempTransform = toDoubleArray(avatarTranslation.get(vals));
        //avatarBox.setTransform(tempTransform);
        inputManager.update((float) elapsedTime);
        if (shoot)
        {
            Matrix4f translation1 = gun.getWorldTranslation().translate(0, 0.80f, -1.0f);
            tempTransform = toDoubleArray(translation1.get(vals));
            bullet = engine.getSceneGraph().addPhysicsCylinderZ(1.0f, tempTransform, 0.05f, 0.05f);
            Vector3f forward = gun.getWorldForwardVector();
            bullet.setLinearVelocity(new float[]{forward.x()*100, 0, forward.z()*100});
            bullets.put(bullet.getUID(), bullet);
            shoot = false;
        }
        Iterator<Map.Entry<Integer, PhysicsObject>> entries = bullets. entrySet().iterator();
        while (entries.hasNext())
        {
            Map.Entry<Integer, PhysicsObject> entry = entries.next();
            if(checkCollision(entry.getValue(), physicsPlane, "bullet", "plane"))
            {
                engine.getSceneGraph().removePhysicsObject(entry.getValue());
                entries.remove();
            }
            for (int i = 0; i < spheres.size(); i++)
            {
                if (checkCollision(entry.getValue(), spheres.get(i), "bullet", ""))
                {
                    engine.getSceneGraph().removePhysicsObject(entry.getValue());
                    //bullet = null;
                    entries.remove();
                }
            }
        }

        if (grenade == null && running)
        {
            grenade = new GameObject(GameObject.root(), grenadeShape, grenadeTexture);
            grenade.setLocalLocation(avatar.getWorldLocation().add(0,0,-2));
            Matrix4f translation = new Matrix4f(grenade.getLocalTranslation());
            tempTransform = toDoubleArray(translation.get(vals));
            grenadeCapsule = engine.getSceneGraph().addPhysicsCapsuleX(1.0f, tempTransform, radius, height);
            grenadeCapsule.setBounciness(0.5f);
            grenadeCapsule.setFriction(50.0f);
            Vector3f location = avatar.getWorldForwardVector();
            float[] linearVelocity = new float[3];
            linearVelocity[0] = location.x() * 10;
            linearVelocity[1] = 6.0f;
            linearVelocity[2] = location.z() * 10;
            grenadeCapsule.setLinearVelocity(linearVelocity);
            grenade.setPhysicsObject(grenadeCapsule);

            throwTime = System.currentTimeMillis();
        }

        if (running)
        {
            int elapsedTime = Math.round((float) (System.currentTimeMillis() - throwTime) / 1000.0f);
            if (elapsedTime == 5)
            {
                readyToExplode = true;
            }
            if(grenadeCapsule != null)
            {
                velocity = grenadeCapsule.getLinearVelocity();
                xzVelocity = Math.sqrt(Math.pow(velocity[0], 2) + Math.pow(velocity[2], 2));
            }

            if ((xzVelocity < 1.6f && readyToExplode))
            {
                if (!exploded)
                {
                    explosionSound.play();
                    exploded = true;
                    engine.getSceneGraph().removePhysicsObject(grenadeCapsule);
                    grenadeCapsule = null;
                }
                if (grenadeSphere == null && exploded)
                {
                    tempTransform = toDoubleArray(grenade.getWorldTranslation().translate(0,4,0).get(vals));
                    grenadeSphere = (engine.getSceneGraph()).addPhysicsSphere(1000.0f, tempTransform, 4.0f);
                    grenadeSphere.setLinearVelocity(new float[]{0, 0, 0});
                    grenadeSphere.setBounciness(0f);
                    grenadeSphere.setFriction(30f);
                    grenade.setPhysicsObject(grenadeSphere);
                    //explodeTime = System.currentTimeMillis();
                }
                /*int time = Math.round((float) (System.currentTimeMillis() - explodeTime) / 1000.0f);
                if (time == 2)
                {
                    engine.getSceneGraph().removePhysicsObject(grenadeSphere);
                    grenadeSphereRemoved = true;
                }*/

                //if(!grenadeSphereRemoved)
                if (grenadeSphere != null)
                {
                    checkCollision(grenadeSphere, avatarBox, "grenade", "avatar");
                }

                if (!explosionSound.getIsPlaying())
                {
                    engine.getSceneGraph().removeGameObject(grenade);
                    if(grenadeSphere != null)
                    {
                        engine.getSceneGraph().removePhysicsObject(grenadeSphere);
                        grenadeSphere = null;
                    }
                    grenade = null;
                    running = false;
                    exploded = false;
                    readyToExplode = false;
                    grenadeSphereRemoved = false;
                }
            }
        }

        AxisAngle4f aa = new AxisAngle4f();
        Matrix4f mat = new Matrix4f();
        Matrix4f mat2 = new Matrix4f().identity();
        Matrix4f mat3 = new Matrix4f().identity();

        physicsEngine.update((float) elapsedTime);
        for (GameObject go : engine.getSceneGraph().getGameObjects())
        {
            if (go.getPhysicsObject() != null)
            {
                double[] physicsObjectTransform = go.getPhysicsObject().getTransform();
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

        //float height = terrain.getHeight(loc.x(), loc.z());
        //avatar.setLocalLocation(new Vector3f(loc.x(), height + 0.25f, loc.z()));

        // build and set HUD
        int elapsTimeSec = Math.round((float) (System.currentTimeMillis() - startTime) / 1000.0f);
        String elapsTimeStr = Integer.toString(elapsTimeSec);
        String counterStr = Integer.toString(counter);
        String dispStr1 = "Time = " + elapsTimeStr;
        String dispStr2 = "avatar position = " + avatar.getWorldLocation();
        Vector3f hud1Color = new Vector3f(1, 0, 0);
        Vector3f hud2Color = new Vector3f(1, 1, 1);
        (engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);
        (engine.getHUDmanager()).setHUD2(dispStr2, hud2Color, 500, 15);

        // update inputs and camera
        processNetworking((float) elapsedTime);

        //update sound
        if (grenade == null)
        {
            explosionSound.setLocation(GameObject.root().getWorldLocation());
        } else
        {
            explosionSound.setLocation(grenade.getWorldLocation());
        }

        backgroundMusic.setLocation(speaker.getWorldLocation());
        setEarParameters();

        gunShotSound.setLocation(gun.getWorldLocation());
        humanAnimatedShape.updateAnimation();
        humanGunAnimatedShape.updateAnimation();

    }

    public void setEarParameters()
    {
        Camera camera = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
        audioManager.getEar().setLocation(avatar.getWorldLocation());
        audioManager.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
    }

    private float[] toFloatArray(double[] array)
    {
        if (array == null)
        {
            return null;
        }
        int n = array.length;
        float[] result = new float[n];
        for (int i = 0; i < n; i++)
        {
            result[i] = (float) array[i];
        }
        return result;
    }

    private double[] toDoubleArray(float[] array)
    {
        if (array == null)
        {
            return null;
        }
        int n = array.length;
        double[] result = new double[n];
        for (int i = 0; i < n; i++)
        {
            result[i] = (double) array[i];
        }
        return result;
    }

    //TODO
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

    public void mouseMoved(MouseEvent mouseEvent)
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
		rightVector.rotateAxis(0.02f * tilt, upVector.x(), upVector.y(), upVector.z());
		fwdVector.rotateAxis(0.02f * tilt, upVector.x(), upVector.y(), upVector.z());
		avatar.globalYaw(0.02f*tilt);
		camera.setU(rightVector);
		camera.setN(fwdVector);
	}*/

    //TODO
    /*private boolean checkForCollision(PhysicsObject bullet, ArrayList<PhysicsObject> spheres)
    {
        DynamicsWorld dynamicsWorld = ((JBulletPhysicsEngine) physicsEngine).getDynamicsWorld();
        Dispatcher dispatcher = dynamicsWorld.getDispatcher();
        int manifoldCount = dispatcher.getNumManifolds();
        for (int i = 0; i < manifoldCount; i++)
        {
            PersistentManifold manifold = dispatcher.getManifoldByIndexInternal(i);
            RigidBody object1 = (RigidBody) manifold.getBody0();
            RigidBody object2 = (RigidBody) manifold.getBody1();
            JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
            JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);

            for (PhysicsObject physicsObject : spheres)
            {
                JBulletPhysicsObject sphereObject = (JBulletPhysicsObject) physicsObject;
                JBulletPhysicsObject bulletObject = (JBulletPhysicsObject) bullet;
                if ((obj1.equals(bulletObject) && obj2.equals(sphereObject)) || (obj1.equals(sphereObject) && obj2.equals(bulletObject)))
                {
                    for (int k = 0; k < manifold.getNumContacts(); k++)
                    {
                        ManifoldPoint contactPoint = manifold.getContactPoint(k);
                        if (contactPoint.getDistance() < 0.0f)
                        {
                            System.out.println("Collision between bullet and avatar detected!");
                            // Add any additional logic here for handling the collision
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }*/

    private boolean checkCollision(PhysicsObject object, PhysicsObject anotherObject, String objectName, String anotherObjectName)
    {
        //System.out.println("In collision detection method");
        DynamicsWorld dynamicsWorld = ((JBulletPhysicsEngine) physicsEngine).getDynamicsWorld();
        Dispatcher dispatcher = dynamicsWorld.getDispatcher();
        int manifoldCount = dispatcher.getNumManifolds();
        for (int i = 0; i < manifoldCount; i++)
        {
            //System.out.println("possible hit detected!!!");
            PersistentManifold manifold = dispatcher.getManifoldByIndexInternal(i);
            RigidBody object1 = (RigidBody) manifold.getBody0();
            RigidBody object2 = (RigidBody) manifold.getBody1();
            JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
            JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);

            JBulletPhysicsObject jbulletObject1 = (JBulletPhysicsObject) object;
            JBulletPhysicsObject jbulletObject2 = (JBulletPhysicsObject) anotherObject;

            if ((obj1.equals(jbulletObject1) && obj2.equals(jbulletObject2)) || (obj1.equals(jbulletObject2) && obj2.equals(jbulletObject1)))
            {
                for (int k = 0; k < manifold.getNumContacts(); k++)
                {
                    ManifoldPoint contactPoint = manifold.getContactPoint(k);
                    if (contactPoint.getDistance() < 0.0f)
                    {
                        if(objectName.equals("grenade") || anotherObjectName.equals("grenade"))
                        {
                            engine.getSceneGraph().removePhysicsObject(grenadeSphere);
                            //grenadeSphere = null;
                            //bullet, grenade, avatar
                            System.out.println("you got hit by grenade");
                            return true;
                        }
                        else if(objectName.equals("bullet") || anotherObjectName.equals("bullet"))
                        {
                            if (objectName.equals("avatar") || anotherObjectName.equals("avatar"))
                            {
                                System.out.println("you got hit by bullet.");
                            }
                            return true;
                        }
                        else
                        {
                            //TODO
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        switch (e.getKeyCode())
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
                if (!backgroundMusic.getIsPlaying())
                {
                    backgroundMusic.play();
                } else
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

    public AnimatedShape getGhostShape()
    {
        return ghostAnimatedShape;
    }

    public AnimatedShape getGhostGunShape()
    {
        return ghostGunAnimatedShape;
    }
    public TextureImage getGhostTexture()
    {
        return ghostTexture;
    }

    public GhostManager getGhostManager()
    {
        return gm;
    }

    public Engine getEngine()
    {
        return engine;
    }

    private void setupNetworking()
    {
        isClientConnected = false;
        try
        {
            protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
        } catch (UnknownHostException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        if (protClient == null)
        {
            System.out.println("missing protocol host");
        } else
        {    // Send the initial join message with a unique identifier for this client
            System.out.println("sending join message to protocol host");
            protClient.sendJoinMessage();
        }
    }

    protected void processNetworking(float elapsTime)
    {    // Process packets received by the client from the server
        if (protClient != null)
            protClient.processPackets();
    }

    public Vector3f getPlayerPosition()
    {
        return avatar.getWorldLocation();
    }

    public void setIsConnected(boolean value)
    {
        this.isClientConnected = value;
    }

    private class SendCloseConnectionPacketAction extends AbstractInputAction
    {
        @Override
        public void performAction(float time, net.java.games.input.Event evt)
        {
            if (protClient != null && isClientConnected == true)
            {
                protClient.sendByeMessage();
            }
        }
    }
}
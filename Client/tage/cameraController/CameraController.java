package tage.cameraController;

import tage.Camera;
import tage.Engine;
import tage.GameObject;

/**
 * abstract class for camera controller
 */
public abstract class CameraController
{
    protected Camera camera;
    protected GameObject avatar;
    protected Engine engine;

    public CameraController(Camera camera, GameObject avatar, Engine engine)
    {
        this.camera = camera;
        this.avatar = avatar;
        this.engine = engine;
    }
    protected abstract void setUpInput();
}

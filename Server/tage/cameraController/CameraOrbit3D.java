package tage.cameraController;

import net.java.games.input.Component;
import net.java.games.input.Event;
import org.joml.Vector3f;
import tage.Camera;
import tage.Engine;
import tage.GameObject;
import tage.input.IInputManager;
import tage.input.InputManager;
import tage.input.action.AbstractInputAction;
/**
 * a 3rd person camera controller that allows you to orbit the camera without alternating avatar's movement
 */
public class CameraOrbit3D extends CameraController
{
    private float cameraAzimuth;
    private float cameraElevation;
    private float cameraRadius;

    public CameraOrbit3D(Camera camera, GameObject avatar, Engine engine)
    {
        super(camera, avatar, engine);
        this.cameraAzimuth = 0.0f;
        this.cameraElevation = 20.0f;
        this.cameraRadius = 5.0f;
        setUpInput();
        updateCameraPosition();
    }

    /**
     * update the position of the camera
     */
    public void updateCameraPosition()
    {
        //double theta = Math.toRadians(cameraAzimuth);
        Vector3f avatarRot = avatar.getWorldForwardVector();
        double avatarAngle = Math.toDegrees((double)avatarRot.angleSigned(new Vector3f(0,0,-1), new Vector3f(0,1,0)));
        float totalAz = cameraAzimuth - (float)avatarAngle;
        double theta = Math.toRadians(totalAz);
        double phi = Math.toRadians(cameraElevation);
        float x = cameraRadius * (float)(Math.cos(phi) * Math.sin(theta));
        float y = cameraRadius * (float)(Math.sin(phi));
        float z = cameraRadius * (float)(Math.cos(phi) * Math.cos(theta));
        camera.setLocation(new Vector3f(x,y,z).add(avatar.getWorldLocation()));
        camera.lookAt(avatar);
    }

    @Override
    protected void setUpInput()
    {
        InputManager inputManager = engine.getInputManager();

        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.U, new CameraOrbitAction(CameraMovementType.ZOOM_IN), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.O, new CameraOrbitAction(CameraMovementType.ZOOM_OUT), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.I, new CameraOrbitAction(CameraMovementType.PITCH_UP), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.K, new CameraOrbitAction(CameraMovementType.PITCH_DOWN), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.J, new CameraOrbitAction(CameraMovementType.ROTATE_LEFT), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.L, new CameraOrbitAction(CameraMovementType.ROTATE_RIGHT), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    }

    private class CameraOrbitAction extends AbstractInputAction
    {
        private CameraMovementType cameraOrbitAction;
        public CameraOrbitAction(CameraMovementType cameraOrbitAction)
        {
            this.cameraOrbitAction = cameraOrbitAction;
        }

        /**
         * perform a series of action related to the orbit camera controller
         * called by the input manager . DO NOT call from the game application
         */
        @Override
        public void performAction(float time, Event evt)
        {
            switch (cameraOrbitAction)
            {
                case PITCH_UP:
                    if(cameraElevation <180)
                    {
                        cameraElevation += 0.5f;
                    }
                    break;
                case PITCH_DOWN:
                    if (cameraElevation > 0)
                    {
                        cameraElevation -= 0.5f;
                    }
                    break;
                case ROTATE_LEFT,ROTATE_RIGHT:
                    float rotateAmount = this.cameraOrbitAction == CameraMovementType.ROTATE_LEFT? -0.5f : 0.5f;
                    cameraAzimuth += rotateAmount;
                    cameraAzimuth = cameraAzimuth % 360;
                    break;
                case ZOOM_IN:
                    if (camera.getLocation().distance(avatar.getWorldLocation()) >= 2)
                    {
                        cameraRadius -= 0.1f;
                    }
                    break;
                case ZOOM_OUT:
                    cameraRadius += 0.1f;
                    break;
            }
            updateCameraPosition();
        }
    }
}

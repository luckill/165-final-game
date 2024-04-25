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
 * an overhead camera controller
 */
public class OverHeadCameraController extends CameraController
{
    private float distance;
    public OverHeadCameraController(Camera camera, GameObject avatar, Engine engine)
    {
        super(camera, avatar, engine);
        this.distance = 10.0f;
        this.camera.setU(new Vector3f(1, 0, 0).negate());
        this.camera.setV(new Vector3f(0, 0, -1));
        this.camera.setN(new Vector3f(0, -1, 0));
        this.camera.setLocation(new Vector3f(0,distance,0));
        setUpInput();
        updateCameraPosition();
    }

    @Override
    protected void setUpInput()
    {
        InputManager inputManager = engine.getInputManager();
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.UP, new OverHeadCameraMovement(CameraMovementType.PAN_FORWARD), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.DOWN, new OverHeadCameraMovement(CameraMovementType.PAN_BACKWARD), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.LEFT, new OverHeadCameraMovement(CameraMovementType.PAN_LEFT), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key.RIGHT, new OverHeadCameraMovement(CameraMovementType.PAN_RIGHT), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key._2, new OverHeadCameraMovement(CameraMovementType.ZOOM_IN), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(Component.Identifier.Key._3, new OverHeadCameraMovement(CameraMovementType.ZOOM_OUT), IInputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    }

    private void updateCameraPosition()
    {
        Vector3f location = camera.getLocation();
        camera.setLocation(new Vector3f(location.x(), distance, location.z()));
    }

    private void updateCameraPanningAction(Vector3f vector)
    {
        camera.setLocation(camera.getLocation().add(vector));
    }


    private class OverHeadCameraMovement extends AbstractInputAction
    {
        private CameraMovementType movementType;

        public OverHeadCameraMovement(CameraMovementType movementType)
        {
            this.movementType = movementType;
        }

        /**
         * perform a series of action related to the orbit camera controller
         * called by the input manager . DO NOT call from the game application
         */
        @Override
        public void performAction(float time, Event evt)
        {
            Vector3f right = camera.getU();
            Vector3f up = camera.getV();
            Vector3f forward = camera.getN();
            switch (movementType)
            {
                case ZOOM_IN:
                    if (camera.getLocation().y >= 3)
                    {
                        distance -= 0.1f;
                        updateCameraPosition();
                    }
                    break;
                case ZOOM_OUT:
                    distance +=0.1f;
                    updateCameraPosition();
                    break;
                case PAN_LEFT:
                    Vector3f leftMovement = new Vector3f(0.2f, 0, 0);
                    updateCameraPanningAction(leftMovement);
                    break;
                case PAN_RIGHT:
                    Vector3f rightMovement = new Vector3f(-0.2f, 0,0);
                    updateCameraPanningAction(rightMovement);
                    break;
                case PAN_FORWARD:
                    Vector3f forwardMovement = new Vector3f(0, 0,-0.2f);
                    updateCameraPanningAction(forwardMovement);
                    break;
                case PAN_BACKWARD:
                    Vector3f backwardMovement = new Vector3f(0, 0,0.2f);
                    updateCameraPanningAction(backwardMovement);
                    break;
            }
        }
    }
}
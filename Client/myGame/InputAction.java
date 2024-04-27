package myGame;
import net.java.games.input.Event;
import tage.GameObject;
import tage.input.action.AbstractInputAction;

public class InputAction extends AbstractInputAction
{
    private GameObject object;
    private ProtocolClient protocolClient;
    private InputType inputType;
    private double currentFrameTime;
    private double lastFrameTime;
    private double elapsedTime;

    public InputAction(GameObject object, InputType inputType, ProtocolClient protocolClient)
    {
        this.protocolClient = protocolClient;
        this.object = object;
        this.inputType = inputType;
        lastFrameTime = System.currentTimeMillis();
        currentFrameTime = System.currentTimeMillis();
        elapsedTime = 0;
    }

    @Override
    public void performAction(float time, Event event)
    {
        lastFrameTime = currentFrameTime;
        currentFrameTime = System.currentTimeMillis();
        elapsedTime += (currentFrameTime - lastFrameTime)/10000000;

        float speed = 0;
        switch (inputType)
        {
            case LEFT:
                object.globalYaw((float) elapsedTime*2);
                protocolClient.sendRotateMessage((float) elapsedTime*2);
                break;
            case RIGHT:
                object.globalYaw((float) -elapsedTime*2);
                protocolClient.sendRotateMessage((float) -elapsedTime*2);
                break;
            case FORWARD:
                speed = (float) elapsedTime * 20;
                object.moveForwardOrBackward(speed);
                protocolClient.sendMoveMessage(object.getWorldLocation());
                break;
            case BACKWARD:
                speed = (float) -elapsedTime * 20;
                object.moveForwardOrBackward(speed);
                protocolClient.sendMoveMessage(object.getWorldLocation());
                break;
            case GLOBAL_YAW_CONTROLLER:
                if(event.getValue()>-0.2 && event.getValue() < 0.2)
                {
                    return;
                }
                else
                {
                    if (event.getValue() < 0)
                    {
                        object.globalYaw((float) elapsedTime*2);
                    }
                    else
                    {
                        object.globalYaw((float) -elapsedTime*2);
                    }
                }
                break;

            case MOVE_FORWARD_OR_BACKWARD_CONTROLLER:
                if (event.getValue()>-0.02 && event.getValue()<0.02)
                {
                    return;
                }
                else
                {
                    if (event.getValue() < 0)
                    {
                        speed = (float) elapsedTime * 20;
                        object.moveForwardOrBackward(speed);
                        protocolClient.sendMoveMessage(object.getWorldLocation());
                    }
                    else
                    {
                        speed = (float) -elapsedTime * 20;
                        object.moveForwardOrBackward(speed);
                        protocolClient.sendMoveMessage(object.getWorldLocation());
                    }
                }
                break;
        }
    }
}
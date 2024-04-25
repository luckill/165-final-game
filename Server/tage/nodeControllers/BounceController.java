package tage.nodeControllers;

import org.joml.Matrix4f;
import tage.Engine;
import tage.GameObject;
import tage.NodeController;

/**
 * A BunceController is a node controller that, when enabled, causes any object
 * it is attached to bounce up and down along its y-axis.
 * @author ZHijun Li
 */
public class BounceController extends NodeController {
    private float scaleRate = .0003f;
    private float cycleTime = 2000.0f;
    private float totalTime = 0.0f;
    private float direction = 1.0f;
    private Matrix4f currentTranslation, newTranslation;
    private Engine engine;

    public BounceController(Engine e, float ctime)
    {
        super();
        cycleTime = ctime;
        engine = e;
    }

    public void apply(GameObject go)
    {
        float elapsedTime = super.getElapsedTime();
        totalTime += elapsedTime / 1000.0f;
        if (totalTime > cycleTime)
        {
            direction = -direction;
            totalTime = 0.0f;
        }
        if (go.getWorldLocation().y() <= 3 && direction == -1.0f)
        {

        }
        else
        {
            go.setLocalTranslation(go.getWorldTranslation().translate(0, direction * 0.05f, 0));
        }
    }

}

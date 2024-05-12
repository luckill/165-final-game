package myGame;

import net.java.games.input.Event;
import tage.Engine;
import tage.Light;
import tage.input.action.AbstractInputAction;

import java.util.Arrays;

public class LightAction extends AbstractInputAction
{
    private Engine engine;
    private LightActionType type;


    public LightAction(Engine engine, LightActionType type)
    {
        this.engine = engine;
        this.type = type;
    }
    @Override
    public void performAction(float time, Event event)
    {
        for (int i = 0; i < engine.getLightManager().getNumLights(); i++)
        {
            Light light = engine.getLightManager().getLight(i);
            if (type == LightActionType.ON)
            {
                light.setAmbient(MyGame.ambient[0], MyGame.ambient[1], MyGame.ambient[2]);
                light.setDiffuse(MyGame.diffuse[0], MyGame.diffuse[1], MyGame.diffuse[2]);
                light.setSpecular(MyGame.specular[0], MyGame.specular[1], MyGame.specular[2]);
            }
            else
            {
                light.setAmbient(0, 0,0);
                light.setDiffuse(0,0,0);
                light.setSpecular(0,0,0);
            }
        }
    }
}

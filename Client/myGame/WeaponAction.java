package myGame;

import net.java.games.input.Event;
import tage.audio.Sound;
import tage.input.action.AbstractInputAction;
import tage.shapes.AnimatedShape;

public class WeaponAction extends AbstractInputAction
{
    private AnimatedShape animatedShape;
    private Sound sound;
    public WeaponAction(AnimatedShape animatedShape, Sound sound)
    {
        this.animatedShape = animatedShape;
        this.sound = sound;
    }
    @Override
    public void performAction(float time, Event evt)
    {
        animatedShape.playAnimation("SWING", 1.0f, AnimatedShape.EndType.STOP, 0);
        sound.play();
        MyGame.shoot = true;
    }
}

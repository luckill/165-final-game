package myGame;

import java.io.IOException;
import java.lang.Math;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;
import tage.physics.PhysicsObject;
import tage.shapes.AnimatedShape;

public class GhostManager
{
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();
	private AnimatedShape gunShape;
	private TextureImage ghosttextureImage;
	private PhysicsObject ghostBox;
	private float[] vals = new float[16];
	private double[] tempTransform;

	public GhostManager(VariableFrameRateGame vfrg)
	{	game = (MyGame)vfrg;
	}
	
	public void createGhostAvatar(UUID id, Vector3f position, String texturePath) throws IOException
	{
		System.out.println("adding ghost with ID --> " + id);
		AnimatedShape s = game.getGhostShape();
		gunShape = game.getGhostGunShape();
		System.out.println("ghost texture path: " + texturePath);
		//TextureImage ghostTexture = new TextureImage(texturePath);
		GhostAvatar newAvatar = new GhostAvatar(id, s, position);
		Matrix4f initialScale = (new Matrix4f()).scaling(0.75f);
		newAvatar.setLocalScale(initialScale);
		newAvatar.getRenderStates().setModelOrientationCorrection(new Matrix4f().rotationY((float) Math.toRadians(90.0f)));
		newAvatar.setTextureImage(MyGame.textureMap.get(texturePath));
		GameObject ghostGun = new GameObject(newAvatar, gunShape);
		ghostGun.setLocalScale(new Matrix4f().scaling((0.2f)));
		ghostGun.setLocalTranslation(newAvatar.getWorldTranslation().translate(0, 0.75f, 2.0f));
		ghostGun.applyParentRotationToPosition(true);



		ghostAvatars.add(newAvatar);
	}
	
	public void removeGhostAvatar(UUID id)
	{
		GhostAvatar ghostAvatar = findAvatar(id);
		if(ghostAvatar != null)
		{
			game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
		}
		else
		{
			System.out.println("tried to remove, but unable to find ghost in list");
		}
	}

	private GhostAvatar findAvatar(UUID id)
	{	GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvatars.iterator();
		while(it.hasNext())
		{	ghostAvatar = it.next();
			if(ghostAvatar.getID().compareTo(id) == 0)
			{
				return ghostAvatar;
			}
		}		
		return null;
	}

	public void updateGhostAvatar(UUID id, Vector3f position) {
		GhostAvatar ghostAvatar = findAvatar(id);



		if (ghostAvatar != null)
		{
			ghostAvatar.setPosition(position);
			if (ghostBox == null)
			{
				if((ghostAvatar.getWorldLocation().x > 4 || ghostAvatar.getWorldLocation().z() > 4))
				{
					Matrix4f ghostTranslation = ghostAvatar.getWorldTranslation().translate(0,0.95f,0);
					tempTransform = game.toDoubleArray(ghostTranslation.get(vals));
					ghostBox = game.getEngine().getSceneGraph().addPhysicsBox(10000.0f, tempTransform, new float[]{1.25f, 2.50f, 0.91f});
					ghostBox.setFriction(0.0f);
					ghostBox.setBounciness(0.0f);
				}
			}
			else
			{
				Vector3f location = ghostAvatar.getWorldLocation();
				float x = location.x();
				float y = location.y();
				float z = location.z();
				float[] transform = game.toFloatArray(ghostBox.getTransform());

				transform[12] = x;
				transform[13] = y;
				transform[14] = z;
				ghostBox.setTransform(game.toDoubleArray(transform));
			}

		}
		else
		{
			System.out.println("tried to update ghost avatar position, but unable to find ghost in list");
		}
	}

	public void rotateGhostAvatar(UUID id, float angle)
	{
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{
			ghostAvatar.globalYaw(angle);
			gunShape.playAnimation("SWING", 1.0f, AnimatedShape.EndType.LOOP, 0);
		}
		else
		{
			System.out.println("tried to update ghost avatar rotation, but unable to find ghost in list");
		}
	}

	//humanAnimatedShape.playAnimation("WAVE", 0.1f, AnimatedShape.EndType.LOOP, 0);
	public void playAnimation(UUID id, String animationShapeType, String animationName, float speed, AnimatedShape.EndType endType, int end)
	{
		GhostAvatar ghostAvatar = findAvatar(id);
		if(ghostAvatar != null)
		{
			System.out.println("ghostAvatar is not null");
			if (animationShapeType.equals("human"))
			{
				AnimatedShape shape = (AnimatedShape) ghostAvatar.getShape();
				shape.playAnimation(animationName, speed, endType, end);
			}
			else
			{
				System.out.println("in the right place");
				//gunShape.playAnimation(animationName, speed, endType, end);
				System.out.println(gunShape);
				System.out.println("Number of animations : " + gunShape.getNumberOfAnimations());
			}
		}
	}
}

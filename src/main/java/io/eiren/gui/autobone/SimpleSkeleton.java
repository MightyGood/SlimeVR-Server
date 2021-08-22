package io.eiren.gui.autobone;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import io.eiren.vr.processor.HumanSkeletonWithLegs;
import io.eiren.vr.processor.HumanSkeletonWithWaist;
import io.eiren.vr.processor.TransformNode;
import io.eiren.yaml.YamlFile;

public class SimpleSkeleton {

	// Waist
	protected final TransformNode hmdNode = new TransformNode("HMD", false);
	protected final TransformNode headNode = new TransformNode("Head", false);
	protected final TransformNode neckNode = new TransformNode("Neck", false);
	protected final TransformNode waistNode = new TransformNode("Waist", false);
	protected final TransformNode chestNode = new TransformNode("Chest", false);

	protected float chestDistance = 0.42f;
	/**
	 * Distance from eyes to waist
	 */
	protected float waistDistance = 0.85f;
	/**
	 * Distance from eyes to the base of the neck
	 */
	protected float neckLength = HumanSkeletonWithWaist.NECK_LENGTH_DEFAULT;
	/**
	 * Distance from eyes to ear
	 */
	protected float headShift = HumanSkeletonWithWaist.HEAD_SHIFT_DEFAULT;

	// Legs
	protected final TransformNode leftHipNode = new TransformNode("Left-Hip", false);
	protected final TransformNode leftKneeNode = new TransformNode("Left-Knee", false);
	protected final TransformNode leftAnkleNode = new TransformNode("Left-Ankle", false);
	protected final TransformNode rightHipNode = new TransformNode("Right-Hip", false);
	protected final TransformNode rightKneeNode = new TransformNode("Right-Knee", false);
	protected final TransformNode rightAnkleNode = new TransformNode("Right-Ankle", false);

	/**
	 * Distance between centers of both hips
	 */
	protected float hipsWidth = HumanSkeletonWithLegs.HIPS_WIDTH_DEFAULT;
	/**
	 * Length from waist to knees
	 */
	protected float kneeHeight = 0.42f;
	/**
	 * Distance from waist to ankle
	 */
	protected float legsLength = 0.84f;

	protected final HashMap<String, TransformNode> nodes = new HashMap<String, TransformNode>();

	public SimpleSkeleton() {
		// Assemble skeleton to waist
		hmdNode.attachChild(headNode);
		headNode.localTransform.setTranslation(0, 0, headShift);

		headNode.attachChild(neckNode);
		neckNode.localTransform.setTranslation(0, -neckLength, 0);

		neckNode.attachChild(chestNode);
		chestNode.localTransform.setTranslation(0, -chestDistance, 0);

		chestNode.attachChild(waistNode);
		waistNode.localTransform.setTranslation(0, -(waistDistance - chestDistance), 0);

		// Assemble skeleton to feet
		waistNode.attachChild(leftHipNode);
		leftHipNode.localTransform.setTranslation(-hipsWidth / 2, 0, 0);

		waistNode.attachChild(rightHipNode);
		rightHipNode.localTransform.setTranslation(hipsWidth / 2, 0, 0);

		leftHipNode.attachChild(leftKneeNode);
		leftKneeNode.localTransform.setTranslation(0, -(legsLength - kneeHeight), 0);

		rightHipNode.attachChild(rightKneeNode);
		rightKneeNode.localTransform.setTranslation(0, -(legsLength - kneeHeight), 0);

		leftKneeNode.attachChild(leftAnkleNode);
		leftAnkleNode.localTransform.setTranslation(0, -kneeHeight, 0);

		rightKneeNode.attachChild(rightAnkleNode);
		rightAnkleNode.localTransform.setTranslation(0, -kneeHeight, 0);

		// Set up a HashMap to get nodes by name easily
		hmdNode.depthFirstTraversal(visitor -> {
			nodes.put(visitor.getName(), visitor);
		});
	}

	public SimpleSkeleton(Iterable<Entry<String, Float>> configs, Iterable<Entry<String, Float>> altConfigs) {
		// Initialize
		this();

		// Set configs
		if (altConfigs != null) {
			// Set alts first, so if there's any overlap it doesn't affect the values
			setSkeletonConfigs(altConfigs);
		}
		setSkeletonConfigs(configs);
	}

	public SimpleSkeleton(Map<String, Float> configs, Map<String, Float> altConfigs) {
		this(configs.entrySet(), altConfigs.entrySet());
	}

	public SimpleSkeleton(Iterable<Entry<String, Float>> configs) {
		this(configs, null);
	}

	public SimpleSkeleton(Map<String, Float> configs) {
		this(configs.entrySet());
	}

	public void setPoseFromSkeleton(HumanSkeletonWithLegs humanSkeleton) {
		TransformNode rootNode = humanSkeleton.getRootNode();

		// Copy headset position
		hmdNode.localTransform.setTranslation(rootNode.localTransform.getTranslation());

		// Copy all rotations
		rootNode.depthFirstTraversal(visitor -> {
			TransformNode targetNode = nodes.get(visitor.getName());

			// Handle unexpected nodes gracefully
			if (targetNode != null) {
				targetNode.localTransform.setRotation(visitor.localTransform.getRotation());
			}
		});
	}

	public void setPoseFromFrame(PoseFrame frame) {
		// Copy headset position
		hmdNode.localTransform.setTranslation(frame.rootPos);

		if (frame.rotations != null) {
			// Copy all rotations
			for (Entry<String, Quaternion> rotation : frame.rotations.entrySet()) {
				TransformNode targetNode = nodes.get(rotation.getKey());

				// Handle unexpected nodes gracefully
				if (targetNode != null) {
					targetNode.localTransform.setRotation(rotation.getValue());
				}
			}
		}

		updatePose();
	}

	public void setSkeletonConfigs(Iterable<Entry<String, Float>> configs) {
		for (Entry<String, Float> config : configs) {
			setSkeletonConfig(config.getKey(), config.getValue());
		}
	}

	public void setSkeletonConfigs(Map<String, Float> configs) {
		setSkeletonConfigs(configs.entrySet());
	}

	public void setSkeletonConfig(String joint, float newLength) {
		setSkeletonConfig(joint, newLength, false);
	}

	public void setSkeletonConfig(String joint, float newLength, boolean updatePose) {
		switch(joint) {
		case "Head":
			headShift = newLength;
			headNode.localTransform.setTranslation(0, 0, headShift);
			if (updatePose) {
				headNode.update();
			}
			break;
		case "Neck":
			neckLength = newLength;
			neckNode.localTransform.setTranslation(0, -neckLength, 0);
			if (updatePose) {
				neckNode.update();
			}
			break;
		case "Waist":
			waistDistance = newLength;
			waistNode.localTransform.setTranslation(0, -(waistDistance - chestDistance), 0);
			if (updatePose) {
				waistNode.update();
			}
			break;
		case "Chest":
			chestDistance = newLength;
			chestNode.localTransform.setTranslation(0, -chestDistance, 0);
			waistNode.localTransform.setTranslation(0, -(waistDistance - chestDistance), 0);
			if (updatePose) {
				chestNode.update();
			}
			break;
		case "Hips width":
			hipsWidth = newLength;
			leftHipNode.localTransform.setTranslation(-hipsWidth / 2, 0, 0);
			rightHipNode.localTransform.setTranslation(hipsWidth / 2, 0, 0);
			if (updatePose) {
				leftHipNode.update();
				rightHipNode.update();
			}
			break;
		case "Knee height":
			kneeHeight = newLength;
			leftAnkleNode.localTransform.setTranslation(0, -kneeHeight, 0);
			rightAnkleNode.localTransform.setTranslation(0, -kneeHeight, 0);
			leftKneeNode.localTransform.setTranslation(0, -(legsLength - kneeHeight), 0);
			rightKneeNode.localTransform.setTranslation(0, -(legsLength - kneeHeight), 0);
			if (updatePose) {
				leftKneeNode.update();
				rightKneeNode.update();
			}
			break;
		case "Legs length":
			legsLength = newLength;
			leftKneeNode.localTransform.setTranslation(0, -(legsLength - kneeHeight), 0);
			rightKneeNode.localTransform.setTranslation(0, -(legsLength - kneeHeight), 0);
			if (updatePose) {
				leftKneeNode.update();
				rightKneeNode.update();
			}
			break;
		}
	}

	public Float getSkeletonConfig(String joint) {
		switch(joint) {
		case "Head":
			return headShift;
		case "Neck":
			return neckLength;
		case "Waist":
			return waistDistance;
		case "Chest":
			return chestDistance;
		case "Hips width":
			return hipsWidth;
		case "Knee height":
			return kneeHeight;
		case "Legs length":
			return legsLength;
		}

		return null;
	}

	public void updatePose() {
		hmdNode.update();
	}

	public Vector3f getNodePosition(String node) {
		TransformNode transformNode = nodes.get(node);
		return transformNode != null ? transformNode.worldTransform.getTranslation() : null;
	}

	public Vector3f getHMDPos() {
		return hmdNode.worldTransform.getTranslation();
	}

	public Vector3f getLeftFootPos() {
		return leftAnkleNode.worldTransform.getTranslation();
	}

	public Vector3f getRightFootPos() {
		return rightAnkleNode.worldTransform.getTranslation();
	}

	public void saveConfigs(YamlFile config) {
		// Save waist configs
		config.setProperty("body.headShift", headShift);
		config.setProperty("body.neckLength", neckLength);
		config.setProperty("body.waistDistance", waistDistance);
		config.setProperty("body.chestDistance", chestDistance);

		// Save leg configs
		config.setProperty("body.hipsWidth", hipsWidth);
		config.setProperty("body.kneeHeight", kneeHeight);
		config.setProperty("body.legsLength", legsLength);
	}
}
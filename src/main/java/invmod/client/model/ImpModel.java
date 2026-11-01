package invmod.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** The imp, rebuilt part for part from the original mod's model. */
public class ImpModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightShin;
    private final ModelPart rightFoot;
    private final ModelPart leftShin;
    private final ModelPart leftFoot;

    public ImpModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.rightShin = root.getChild("right_shin");
        this.rightFoot = root.getChild("right_foot");
        this.leftShin = root.getChild("left_shin");
        this.leftFoot = root.getChild("left_foot");
    }

    private static PartPose pose(float x, float y, float z, float xRot) {
        return PartPose.offsetAndRotation(x, y, z, xRot, 0.0F, 0.0F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(44, 0).addBox(-2.733333F, -3.0F, -2.0F, 5, 3, 4), pose(-0.4F, 9.8F, -3.3F, 0.15807F));
        head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, -4.0F, 1.5F, 1, 1, 1), PartPose.ZERO);
        head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(0, 2).addBox(-1.0F, -4.0F, 1.5F, 1, 1, 1), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(23, 1).addBox(-4.0F, 0.0F, -4.0F, 7, 4, 3),
                pose(0.0F, 9.1F, -0.8666667F, 0.64346F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(26, 9).addBox(-2.0F, -0.7333333F, -1.133333F, 2, 7, 2),
                pose(-4.0F, 10.8F, -2.066667F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(18, 9).addBox(0.0F, -0.8666667F, -1.0F, 2, 7, 2),
                pose(3.0F, 10.8F, -2.1F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 17).addBox(-1.0F, 0.0F, -2.0F, 2, 4, 3),
                pose(-2.0F, 16.9F, -1.0F, -0.15807F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, 0.0F, -2.0F, 2, 4, 3),
                pose(1.0F, 17.0F, -1.0F, -0.15919F));
        root.addOrReplaceChild("right_shin", CubeListBuilder.create().texOffs(10, 17).addBox(-2.0F, 0.6F, -4.4F, 2, 3, 2),
                pose(-1.0F, 16.9F, -1.0F, 0.82623F));
        root.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(18, 18).addBox(-2.0F, 4.2F, -1.0F, 2, 3, 2),
                pose(-1.0F, 16.9F, -1.0F, -0.01403F));
        root.addOrReplaceChild("left_shin", CubeListBuilder.create().texOffs(10, 22).addBox(-1.0F, 0.6F, -4.433333F, 2, 3, 2),
                pose(1.0F, 17.0F, -1.0F, 0.82461F));
        root.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(10, 27).addBox(-1.0F, 4.2F, -1.0F, 2, 3, 2),
                pose(1.0F, 17.0F, -1.0F, -0.01214F));
        root.addOrReplaceChild("body_mid", CubeListBuilder.create().texOffs(1, 1).addBox(0.0F, 0.0F, 0.0F, 7, 5, 3),
                pose(-4.0F, 12.46667F, -2.266667F, -0.15807F));
        root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(44, 7).addBox(0.0F, 0.0F, 0.0F, 3, 2, 2),
                pose(-2.0F, 9.6F, -4.033333F, 0.27662F));
        root.addOrReplaceChild("body_chest", CubeListBuilder.create().texOffs(0, 9).addBox(0.0F, -1.0F, 0.0F, 7, 6, 2),
                pose(-4.0F, 12.36667F, -3.8F, 0.31614F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(18, 23).addBox(0.0F, 0.0F, 0.0F, 1, 8, 1),
                pose(-1.0F, 15.0F, -0.6666667F, 0.47304F));
        root.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(22, 23).addBox(0.0F, 0.0F, 0.0F, 1, 4, 1),
                pose(-1.0F, 22.1F, 2.9F, 1.38309F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float pos = state.walkAnimationPos;
        float speed = state.walkAnimationSpeed;
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.rightArm.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * speed;
        this.leftArm.xRot = Mth.cos(pos * 0.6662F) * speed;
        float right = Mth.cos(pos * 0.6662F) * 1.4F * speed;
        float left = Mth.cos(pos * 0.6662F + Mth.PI) * 1.4F * speed;
        this.rightLeg.xRot = right - 0.158F;
        this.rightShin.xRot = right + 0.82623F;
        this.rightFoot.xRot = right - 0.01403F;
        this.leftLeg.xRot = left - 0.15919F;
        this.leftShin.xRot = left + 0.82461F;
        this.leftFoot.xRot = left - 0.01214F;
        this.rightArm.zRot += Mth.cos(state.ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.leftArm.zRot -= Mth.cos(state.ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.rightArm.xRot += Mth.sin(state.ageInTicks * 0.067F) * 0.05F;
        this.leftArm.xRot -= Mth.sin(state.ageInTicks * 0.067F) * 0.05F;
    }
}

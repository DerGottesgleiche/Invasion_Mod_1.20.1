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

/** The thrower, rebuilt from the original mod's model. The renderer scales it up 2.4x / 2.8x. */
public class ThrowerModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public ThrowerModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(16, 14).addBox(-2.0F, -2.0F, -2.0F, 4, 2, 4),
                PartPose.offset(0.0F, 16.0F, 4.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 1).addBox(-7.0F, 2.0F, -4.0F, 12, 4, 9),
                PartPose.offset(-0.4F, 16.0F, 3.0F));
        root.addOrReplaceChild("body2", CubeListBuilder.create().texOffs(0, 23).addBox(-3.666667F, 0.0F, 0.0F, 12, 2, 7),
                PartPose.offset(-3.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(39, 22).addBox(-3.0F, 0.0F, -1.466667F, 3, 7, 3),
                PartPose.offset(-6.566667F, 16.0F, 5.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).addBox(0.0F, 0.0F, -1.0F, 2, 4, 2),
                PartPose.offset(5.0F, 16.0F, 5.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 14).addBox(-2.0F, 0.0F, -2.0F, 4, 2, 4),
                PartPose.offset(-4.066667F, 22.0F, 4.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 14).addBox(-2.0F, 0.0F, -2.0F, 4, 2, 4),
                PartPose.offset(3.0F, 22.0F, 4.0F));
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
        this.rightLeg.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed;
        this.leftLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.4F * speed;
        this.rightArm.zRot += Mth.cos(state.ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.leftArm.zRot -= Mth.cos(state.ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.rightArm.xRot += Mth.sin(state.ageInTicks * 0.067F) * 0.05F;
        this.leftArm.xRot -= Mth.sin(state.ageInTicks * 0.067F) * 0.05F;
    }
}

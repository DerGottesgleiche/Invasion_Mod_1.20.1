package invmod.block;

import invmod.entity.Invader;
import invmod.registry.ModItems;
import invmod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A trap lying on the ground. The original mod used an entity; this port uses a flat block with the same
 * effects. A sprung trap stays behind empty and can be picked up with a right click.
 */
public class TrapBlock extends Block {
    public enum TrapType implements StringRepresentable {
        EMPTY("empty"), RIFT("rift"), FLAME("flame"), POISON("poison");

        private final String name;

        TrapType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<TrapType> TYPE = EnumProperty.create("type", TrapType.class);
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 1.0, 15.0);

    public TrapBlock(BlockBehaviour.Properties properties) {
        super(properties.noCollision());
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, TrapType.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState, net.minecraft.util.RandomSource random) {
        return direction == Direction.DOWN && !this.canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    public static Item itemFor(TrapType type) {
        return switch (type) {
            case EMPTY -> ModItems.TRAP_EMPTY.get();
            case RIFT -> ModItems.TRAP_RIFT.get();
            case FLAME -> ModItems.TRAP_FLAME.get();
            case POISON -> ModItems.TRAP_POISON.get();
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            ItemStack stack = new ItemStack(itemFor(state.getValue(TYPE)));
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
            level.removeBlock(pos, false);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.5F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        TrapType type = state.getValue(TYPE);
        if (type == TrapType.EMPTY || !(level instanceof ServerLevel serverLevel) || !(entity instanceof LivingEntity living)
                || (entity instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            return;
        }
        switch (type) {
            case RIFT -> {
                living.hurtServer(serverLevel, serverLevel.damageSources().magic(), living instanceof Player ? 12.0F : 38.0F);
                for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(1.9, 1.0, 1.9),
                        e -> e != living)) {
                    other.hurtServer(serverLevel, serverLevel.damageSources().magic(), 8.0F);
                    if (other instanceof Mob mob && other instanceof Invader) {
                        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 6));
                    }
                }
                level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 1.5F,
                        level.getRandom().nextFloat() * 0.25F + 0.55F);
            }
            case FLAME -> {
                level.playSound(null, pos, ModSounds.FIREBALL.get(), SoundSource.BLOCKS, 1.5F,
                        1.15F / (level.getRandom().nextFloat() * 0.3F + 1.0F));
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
                    if (!p.equals(pos) && level.getBlockState(p).isAir() && Blocks.FIRE.defaultBlockState().canSurvive(level, p)) {
                        level.setBlockAndUpdate(p, Blocks.FIRE.defaultBlockState());
                    }
                }
                for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(1.1))) {
                    other.igniteForSeconds(8.0F);
                }
            }
            case POISON -> {
                for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(2.0))) {
                    other.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
                    other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
                }
                level.playSound(null, pos, SoundEvents.SPLASH_POTION_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            default -> {
            }
        }
        level.setBlockAndUpdate(pos, state.setValue(TYPE, TrapType.EMPTY));
    }
}

package com.gregguns.icbm.warhead;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

/**
 * A block lifted by the anti-gravitational warhead (D80, D10): it rises to a target height, hovers, then falls and
 * puts itself back where it lands (or drops as an item when the spot is taken). The block it was is gone from the
 * world while it flies and is conserved: it is placed or dropped, never lost and never duplicated. Only plain blocks
 * are lifted (no tile entities), so no machine or inventory is ever copied. It is saved with its chunk, so a reload
 * keeps it flying, and a hard age cap ends it. The block and its metadata ride in the spawn data for the client
 * (GregGuns claims no data watcher slot).
 * Both sides; the motion is server side.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/blast-types.md (Anti-gravitational)
public final class EntityFlyingBlock extends Entity implements IEntityAdditionalSpawnData {

    /** Rising. */
    static final int RISE = 0;
    /** Hovering. */
    static final int HOVER = 1;
    /** Falling. */
    static final int FALL = 2;
    /** Rise speed, blocks per tick. */
    static final double RISE_SPEED = 0.5;
    /** Gravity once released, blocks per tick squared. */
    static final double GRAVITY = 0.04;
    /** The entity ends and lands wherever it is after this many ticks, whatever phase it is in (3 minutes). */
    public static final int MAX_AGE = 3600;

    private int blockId;
    private int blockMeta;
    private int phase;
    private int age;
    private int hover;
    private double riseTo;

    /** @param world the world */
    public EntityFlyingBlock(World world) {
        super(world);
        setSize(0.98F, 0.98F);
        this.yOffset = 0.0F;
        this.ignoreFrustumCheck = true;
    }

    /**
     * @param world  the world
     * @param x      center X of the cell
     * @param y      bottom Y of the cell
     * @param z      center Z of the cell
     * @param block  the block that was lifted
     * @param meta   its metadata
     * @param riseTo the height to rise to
     * @param hover  ticks to hover once there
     * @param driftX horizontal drift X, blocks per tick
     * @param driftZ horizontal drift Z, blocks per tick
     * @return the entity, not yet spawned
     */
    public static EntityFlyingBlock lift(World world, double x, double y, double z, Block block, int meta,
        double riseTo, int hover, double driftX, double driftZ) {
        EntityFlyingBlock e = new EntityFlyingBlock(world);
        e.setPosition(x, y, z);
        e.setBlock(block, meta);
        e.riseTo = riseTo;
        e.hover = hover;
        e.motionX = driftX;
        e.motionZ = driftZ;
        return e;
    }

    @Override
    protected void entityInit() {}

    private void setBlock(Block block, int meta) {
        this.blockId = Block.getIdFromBlock(block);
        this.blockMeta = meta;
    }

    /** @return the block this carries; stone when the id is unknown */
    public Block block() {
        Block b = Block.getBlockById(blockId);
        return b == null || b == Blocks.air ? Blocks.stone : b;
    }

    /** @return its metadata */
    public int meta() {
        return blockMeta;
    }

    @Override
    public void writeSpawnData(ByteBuf buf) {
        buf.writeInt(blockId);
        buf.writeInt(blockMeta);
    }

    @Override
    public void readSpawnData(ByteBuf buf) {
        blockId = buf.readInt();
        blockMeta = buf.readInt();
    }

    /** @return the phase: 0 rising, 1 hovering, 2 falling (for tests) */
    public int phase() {
        return phase;
    }

    /** @return the ticks this has flown */
    public int age() {
        return age;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) {
            return;
        }
        age++;
        if (age > MAX_AGE) {
            land();
            return;
        }
        switch (phase) {
            case RISE:
                motionY = RISE_SPEED;
                if (posY >= riseTo || isCollidedVertically && age > 2) {
                    phase = HOVER;
                    motionY = 0.0;
                }
                break;
            case HOVER:
                motionX *= 0.9;
                motionZ *= 0.9;
                motionY = Math.sin(age * 0.1) * 0.02;
                if (--hover <= 0) {
                    phase = FALL;
                    motionX = 0.0;
                    motionZ = 0.0;
                }
                break;
            default:
                motionY -= GRAVITY;
                motionY *= 0.98;
                break;
        }
        moveEntity(motionX, motionY, motionZ);
        if (phase == FALL && (onGround || isCollidedVertically && motionY < 0.0)) {
            land();
        }
    }

    /** Puts the block back where it is, or at the first free cell above, or drops it; then ends. */
    private void land() {
        Block block = block();
        int meta = meta();
        int x = MathHelper.floor_double(posX);
        int z = MathHelper.floor_double(posZ);
        int y = MathHelper.floor_double(posY + 0.49);
        boolean placed = false;
        if (worldObj.blockExists(x, y, z)) {
            for (int up = 0; up < 4 && y + up < 255 && !placed; up++) {
                Block there = worldObj.getBlock(x, y + up, z);
                if (there.isAir(worldObj, x, y + up, z) || there.isReplaceable(worldObj, x, y + up, z)
                    || there.getMaterial() == Material.lava
                    || there.getMaterial() == Material.water) {
                    placed = claimAllows(x, y + up, z) && worldObj.setBlock(x, y + up, z, block, meta, 3);
                }
            }
        }
        if (!placed) {
            Item item = Item.getItemFromBlock(block);
            if (item != null) {
                entityDropItem(new ItemStack(item, 1, block.damageDropped(meta)), 0.0F);
            }
        }
        setDead();
    }

    /**
     * Lifting is vetted, so landing must be too (D52): posts a one-cell {@code ExplosionEvent.Detonate} and reports
     * whether the cell survived the listeners. A block that may not land is dropped as an item instead.
     */
    @SuppressWarnings("unchecked")
    private boolean claimAllows(int x, int y, int z) {
        net.minecraft.world.Explosion vanilla = new net.minecraft.world.Explosion(
            worldObj,
            null,
            posX,
            posY,
            posZ,
            3.0F);
        net.minecraft.world.ChunkPosition cell = new net.minecraft.world.ChunkPosition(x, y, z);
        vanilla.affectedBlockPositions.add(cell);
        net.minecraftforge.event.ForgeEventFactory
            .onExplosionDetonate(worldObj, vanilla, new java.util.ArrayList<Entity>(), 0.0);
        return vanilla.affectedBlockPositions.contains(cell);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setString("block", Block.blockRegistry.getNameForObject(block()));
        tag.setInteger("meta", meta());
        tag.setInteger("phase", phase);
        tag.setInteger("age", age);
        tag.setInteger("hover", hover);
        tag.setDouble("riseTo", riseTo);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        Block b = Block.getBlockFromName(tag.getString("block"));
        setBlock(b == null || b == Blocks.air ? Blocks.stone : b, tag.getInteger("meta"));
        phase = Math.max(RISE, Math.min(FALL, tag.getInteger("phase")));
        age = tag.getInteger("age");
        hover = tag.getInteger("hover");
        riseTo = tag.getDouble("riseTo");
    }
}

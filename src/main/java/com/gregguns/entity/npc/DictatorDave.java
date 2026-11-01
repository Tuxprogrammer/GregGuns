package com.gregguns.entity.npc;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.gregguns.entity.npc.loot.NpcProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/**
 * Dictator Dave, the mini-boss: 100 health, melee 7, speed 0.40, follow range 60, 35 base XP. He fires a golden
 * revolver in bursts of six (the gun's own AI stats) and never despawns. Techguns gave him no boss bar, and neither
 * does this class (he is not an {@code IBossDisplayData}). He commands red Military Bases, placed once at his post by
 * the flag (D353), and comes from spawn eggs. At a base he holds his post ({@link PostedNpc}): he chases what attacks
 * him, then walks back and wanders only near it. Both sides.
 */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (section 13), docs/techguns-spec/BUGS.md (NPC-28), D353
public class DictatorDave extends NpcEntity implements PostedNpc {

    /** NBT key of his post, {@code [x, y, z, radius]}. */
    static final String POST_TAG = "GGPost";
    /** Priority of the walk back to the post: just above wandering, below the fight. */
    private static final int RETURN_PRIORITY = 5;

    /** Maximum health. */
    public static final double MAX_HEALTH = 100.0;
    /** Melee damage in half hearts. */
    public static final double ATTACK_DAMAGE = 7.0;
    /** Movement speed attribute. */
    public static final double SPEED = 0.40;
    /** Follow range in blocks. */
    public static final double FOLLOW_RANGE = 60.0;
    /** Base experience. */
    public static final int XP = 35;

    /**
     * @param world the world; the entity registry calls this when it loads him
     */
    public DictatorDave(World world) {
        super(world);
        experienceValue = XP;
        // Does nothing without a post: an egg's Dave roams like any NPC.
        tasks.addTask(RETURN_PRIORITY, new EntityAIMoveTowardsRestriction(this, 1.0));
    }

    @Override
    public void setPost(int x, int y, int z, int radius) {
        setHomeArea(x, y, z, radius);
    }

    @Override
    public int[] post() {
        if (!hasHome()) {
            return null;
        }
        return new int[] { getHomePosition().posX, getHomePosition().posY, getHomePosition().posZ,
            (int) func_110174_bM() };
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        int[] post = post();
        if (post != null) {
            tag.setIntArray(POST_TAG, post);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        int[] post = tag.getIntArray(POST_TAG);
        if (post.length == 4) {
            setPost(post[0], post[1], post[2], post[3]);
        }
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(SPEED);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(FOLLOW_RANGE);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(ATTACK_DAMAGE);
    }

    @Override
    protected NpcProfile npcProfile() {
        return NpcProfiles.get("DictatorDave");
    }

    @Override
    protected boolean isPersistentNpc() {
        return true;
    }
}

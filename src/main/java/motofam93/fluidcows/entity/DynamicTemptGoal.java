package motofam93.fluidcows.entity;

import motofam93.fluidcows.util.BreedingManager;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class DynamicTemptGoal extends Goal {
    private static final TargetingConditions TEMP_TARGETING = TargetingConditions.forNonCombat().range(10.0D).ignoreLineOfSight();
    
    private final FluidCowEntity cow;
    private final double speedModifier;
    @Nullable
    private Player targetPlayer;
    private int calmDown;
    private boolean isRunning;

    public DynamicTemptGoal(FluidCowEntity cow, double speedModifier) {
        this.cow = cow;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.calmDown > 0) {
            --this.calmDown;
            return false;
        }
        
        this.targetPlayer = this.cow.level().getNearestPlayer(TEMP_TARGETING, this.cow);
        if (this.targetPlayer == null) {
            return false;
        }
        
        return this.isTempting(this.targetPlayer);
    }

    private boolean isTempting(Player player) {
        return this.isTemptedBy(player.getMainHandItem()) || this.isTemptedBy(player.getOffhandItem());
    }

    private boolean isTemptedBy(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return BreedingManager.isBreedingItemForParent(this.cow.getFluidRL(), stack);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPlayer == null || !this.targetPlayer.isAlive()) {
            return false;
        }
        
        if (this.cow.distanceToSqr(this.targetPlayer) > 100.0D) {
            return false;
        }
        
        return this.isTempting(this.targetPlayer);
    }

    @Override
    public void start() {
        this.isRunning = true;
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        this.cow.getNavigation().stop();
        this.isRunning = false;
        this.calmDown = reducedTickDelay(40);
    }

    @Override
    public void tick() {
        this.cow.getLookControl().setLookAt(
            this.targetPlayer, 
            (float)(this.cow.getMaxHeadYRot() + 20), 
            (float)this.cow.getMaxHeadXRot()
        );
        
        if (this.cow.distanceToSqr(this.targetPlayer) < 6.25D) {
            this.cow.getNavigation().stop();
        } else {
            this.cow.getNavigation().moveTo(this.targetPlayer, this.speedModifier);
        }
    }
    
    public boolean isRunning() {
        return this.isRunning;
    }
}

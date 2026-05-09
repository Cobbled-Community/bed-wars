package xyz.nucleoid.bedwars.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.bedwars.BedWars;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.stimuli.event.EventResult;

@Mixin(SaplingBlock.class)
public abstract class SaplingBlockMixin {
	@Shadow public abstract void advanceTree(ServerLevel level, BlockPos blockPos, BlockState blockState, RandomSource random);

	/**
	 * Saplings grow faster in bedwars
	 *
	 * @author SuperCoder79
	 */
	@Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
	public void handleRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		var gameSpace = GameSpaceManager.get().byLevel(level);
		if (gameSpace != null && gameSpace.getBehavior().testRule(BedWars.FAST_TREE_GROWTH) == EventResult.ALLOW) {
			if (level.getMaxLocalRawBrightness(pos.above()) >= 9) {
				this.advanceTree(level, pos, state, random);
			}

			ci.cancel();
		}
	}
}

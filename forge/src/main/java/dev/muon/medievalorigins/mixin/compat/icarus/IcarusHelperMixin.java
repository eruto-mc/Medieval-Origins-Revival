package dev.muon.medievalorigins.mixin.compat.icarus;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.cammiescorner.icarus.api.IcarusPlayerValues;
import dev.cammiescorner.icarus.util.IcarusHelper;
import dev.muon.medievalorigins.power.IcarusWingsPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(value = IcarusHelper.class, remap = false)
public class IcarusHelperMixin {
    @ModifyReturnValue(method = "getConfigValues", at = @At("RETURN"))
    private static IcarusPlayerValues modifyConfigValues(IcarusPlayerValues original, LivingEntity entity) {
        if (IcarusWingsPower.hasPower(entity)) {
            return new IcarusPlayerValues() {
                //todo: add these as fields to power json
                @Override
                public float wingsSpeed() {
                    return original.wingsSpeed();
                }

                @Override
                public float maxSlowedMultiplier() {
                    return original.maxSlowedMultiplier();
                }

                @Override
                public boolean armorSlows() {
                    return original.armorSlows();
                }

                @Override
                public boolean canLoopDeLoop() {
                    return original.canLoopDeLoop();
                }

                @Override
                public boolean canSlowFall() {
                    return original.canSlowFall();
                }

                @Override
                public float exhaustionAmount() {
                    // 当部（world-3・2026-09-12）: 上流は /4 していた。
                    // ⚠ 種族の翼を持つ人が唯一 W で推進する側なのに、設定値の 1/4 しか引かれず、
                    //   ⚠⚠ 設定 1.0 のつもりが実際は 0.25/tick（満タンから約 32 秒）だった。
                    // ⚠ 割るのをやめ、icarus.jsonc の exhaustion_amount がそのまま意味を持つようにした。
                    //   → 数字の調整は jar を建て直さずに設定でできる（サーバ側の値だけが使われる）。
                    return original.exhaustionAmount();
                }

                @Override
                public int maxHeightAboveWorld() {
                    return original.maxHeightAboveWorld();
                }

                @Override
                public boolean maxHeightEnabled() {
                    return original.maxHeightEnabled();
                }

                @Override
                public float requiredFoodAmount() {
                    return 0;
                }
            };
        }
        // 当部（world-3・2026-09-12）: 背中の枠に着ける「品の翼」だけ、エリトラと同じ飛び方にする。
        // 種族の翼（上の分岐）は 1 つも触っていないので、ヴァルキリーは据え置きのまま。
        // ⚠ wingsSpeed を 0 にするだけでは足りない——Icarus は W を押しているあいだ
        //    速度に関係なく ApplyBoostPacket を送り、サーバが exhaustionAmount を引く。
        //    進まないのに腹だけ減るので、exhaustionAmount も 0 にする。
        return new IcarusPlayerValues() {
            @Override
            public float wingsSpeed() {
                return 0;
            }

            @Override
            public float maxSlowedMultiplier() {
                return original.maxSlowedMultiplier();
            }

            @Override
            public boolean armorSlows() {
                return original.armorSlows();
            }

            @Override
            public boolean canLoopDeLoop() {
                return original.canLoopDeLoop();
            }

            @Override
            public boolean canSlowFall() {
                return original.canSlowFall();
            }

            @Override
            public float exhaustionAmount() {
                return 0;
            }

            @Override
            public int maxHeightAboveWorld() {
                return original.maxHeightAboveWorld();
            }

            @Override
            public boolean maxHeightEnabled() {
                return original.maxHeightEnabled();
            }

            @Override
            public float requiredFoodAmount() {
                return 0;
            }
        };
    }

    @WrapOperation(method = "hasWings", at = @At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))
    private static boolean hasWingsFromOrigin(Predicate<LivingEntity> instance, Object entity, Operation<Boolean> original) {
        if (IcarusWingsPower.hasPower((LivingEntity) entity)) return true;
        return original.call(instance, entity);
    }

    @WrapOperation(method = "getEquippedWings", at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object getOriginWings(Function<LivingEntity, ItemStack> instance, Object entity, Operation<ItemStack> original) {
        if (IcarusWingsPower.hasPower((LivingEntity) entity)) return null;
        return original.call(instance, entity);
    }
}
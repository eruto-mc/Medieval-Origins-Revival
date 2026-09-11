package dev.muon.medievalorigins.mixin.compat.icarus;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.cammiescorner.icarus.client.IcarusClient;
import dev.cammiescorner.icarus.util.IcarusHelper;
import dev.muon.medievalorigins.enchantment.ModEnchantments;
import dev.muon.medievalorigins.power.IcarusWingsPower;
import dev.muon.medievalorigins.power.PixieWingsPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IcarusClient.class, remap = false)
public class IcarusClientMixin {

    /*
     * 当部（world-3・2026-09-12）: 背中の枠に着ける「品の翼」は、W を押しても前へ進まない
     * ＝エリトラと同じにする。種族の翼（medievalorigins:icarus_wings の power）は素通しなので据え置き。
     *
     * ⚠ IcarusConfig の wings_speed を 0 にしても止まらない。下の modifyArmorModifier が
     *   onPlayerTick の最初の float 変数（＝wings_speed × 向きの倍率）を
     *   「max(1.0, 防具値/20 × maxSlowed)」で丸ごと置き換えるため、wings_speed は捨てられる。
     *   置き換わった値が 1.0 だと計算が「速度 = 視線 × 2.5」に畳まれ、
     *   ⚠⚠ W を押した瞬間に 2.5 ブロック/tick（50 m/s）へ張り付く。
     *
     * ⚠ だから入口で打ち切る。onPlayerTick がするのは推進と ApplyBoostPacket の送信だけなので、
     *   打ち切ると腹の減りも一緒に止まる（描画は getWingsForRendering が別に持っている）。
     */
    @Inject(method = "onPlayerTick(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"), cancellable = true)
    private static void world3$noBoostForItemWings(Player player, CallbackInfo ci) {
        if (!IcarusWingsPower.hasPower(player)) {
            ci.cancel();
        }
    }

    /*
     * TODO: Rewrite to be less invasive
     *  Use ModifyExpressionValue, target getArmorValue
     */
    @ModifyVariable(method = "onPlayerTick(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At(value = "STORE", opcode = Opcodes.FSTORE),
            ordinal = 0)
    private static float modifyArmorModifier(float modifier, Player player) {
        var cfg = IcarusHelper.getConfigValues(player);
        int armorValueSum = 0;
        Iterable<ItemStack> armorItems = player.getArmorSlots();
        for (ItemStack armorItem : armorItems) {
            if (armorItem.isEmpty() || EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.FEATHERWEIGHT.get(), armorItem) > 0) {
                continue;
            }
            if (armorItem.getItem() instanceof ArmorItem armor) {
                armorValueSum += armor.getDefense();
            }
        }
        return cfg.armorSlows() ? Math.max(1.0F, armorValueSum / 20.0F * cfg.maxSlowedMultiplier()) : 1.0F;
    }

    @ModifyReturnValue(method = "getWingsForRendering", at = @At(value = "RETURN"))
    private static ItemStack renderOriginWings(ItemStack original, LivingEntity entity) {
        if (original.isEmpty()) {
            ItemStack wingsType = IcarusWingsPower.getWingsType(entity);
            if (!wingsType.isEmpty()) {
                return wingsType;
            }
        } else if (PixieWingsPower.hasPower(entity)) {
            return new ItemStack(Items.AIR);
        }
        return original;
    }
}

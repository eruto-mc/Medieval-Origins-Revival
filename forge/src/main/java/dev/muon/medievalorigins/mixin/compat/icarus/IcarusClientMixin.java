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
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.CuriosApi;

@Mixin(value = IcarusClient.class, remap = false)
public class IcarusClientMixin {

    /**
     * W を押しているあいだの推進を、当部の計算へ丸ごと置き換える（world-3・2026-09-12）。
     *
     * ⚠ なぜ差し替えるのか（設定では届かない）:
     *   下の modifyArmorModifier が onPlayerTick の最初の float 変数（＝wings_speed × 向きの倍率）を
     *   「max(1.0, 防具値/20 × maxSlowed)」で置き換えるので、⚠⚠ wings_speed は読まれた直後に捨てられる。
     *   置き換わる値は 1.0 を下回らず、Icarus の式が「速度 = 視線 × 2.5 ブロック/tick」に畳まれる。
     *   ⚠⚠ つまり W を押した瞬間に 50 m/s へ張り付き、しかも縦には上限が無い（当部の
     *   elytra_boost_limit は水平しか削らない）ので、真上を向くと 50 m/s で登れてしまっていた。
     *
     * 当部の計算（視線の向きの TARGET へ、毎tick RATE ぶん寄せる）:
     *
     *     速度 ← 速度 + (視線 × TARGET − 速度) × RATE
     *
     *   TARGET = 0.75 ブロック/tick（15 m/s）＝ elytra_boost_limit の水平上限と同じ。
     *   ⚠ どの向きを見ても 15 m/s を超えない（縦も同じ）ので、「上下に振れば速い」は
     *     エリトラと同じく重力と滑空に任せる（当部のエリトラの設計どおり）。
     *   RATE  = IcarusConfig の wings_speed。⚠ **本来の意味（寄る速さ）に読み直して使っている。**
     *     サーバ→クライアントへ同期される値なので、⚠ **jar を建て直さずに設定で調整できる。**
     *
     * ⚠ 種族の翼を持たない人（＝背中に品の翼を着けているだけの人）は推進なし＝エリトラと同じ。
     * ⚠ 腹の減りは ApplyBoostPacket がサーバ側で引く（値は exhaustion_amount。IcarusHelperMixin を参照）。
     */
    @Inject(method = "onPlayerTick(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"), cancellable = true)
    private static void world3$wingBoostBefore(Player player, CallbackInfo ci) {
        world3$beforeBoost = null;
        if (!IcarusWingsPower.hasPower(player)) {
            ci.cancel();  // 品の翼は推進しない（腹も減らない＝包みも送られない）
            return;
        }
        if (player.isFallFlying() && player.zza > 0.0F) {
            if (player.getDeltaMovement().length() >= WORLD3_TARGET_SPEED) {
                // ⚠ すでに目標より速い（急降下の途中など）ときは何もしない。
                //   ⚠⚠ ここで寄せるとブレーキになるうえ、腹だけ減る。包みごと止める。
                ci.cancel();
                return;
            }
            // ⚠ Icarus 本体はそのまま走らせる（腹を引く包みを送るのはあちら。
            //    ⚠⚠ 包みのクラスはこの fork が組むときの Icarus（2.9.0）と実機（2.14.0）で
            //    場所が違うので、こちらから名指しで呼ばない）。速度だけ後から書き換える。
            world3$beforeBoost = player.getDeltaMovement();
        }
    }

    @Inject(method = "onPlayerTick(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("RETURN"))
    private static void world3$wingBoostAfter(Player player, CallbackInfo ci) {
        Vec3 before = world3$beforeBoost;
        world3$beforeBoost = null;
        if (before == null) return;

        float rate = IcarusHelper.getConfigValues(player).wingsSpeed();
        if (rate <= 0.0F) {
            player.setDeltaMovement(before);  // 0 なら推進なしに戻す
            return;
        }
        if (rate > 1.0F) rate = 1.0F;

        Vec3 target = player.getLookAngle().scale(WORLD3_TARGET_SPEED);
        player.setDeltaMovement(before.add(target.subtract(before).scale(rate)));
    }

    /**
     * 当部: W で寄せていく先の速さ（ブロック/tick）。
     *
     * ⚠⚠ **0.75（＝水平上限と同じ）にしたら上昇できなくなった**（2026-09-12・あなたの「上にいかなすぎ」）。
     *   重力は毎tick 約 0.08 を下向きに足すので、真上を向いたときの釣り合いは
     *
     *       v = (0.98 × 寄る速さ × TARGET − 0.08) ÷ (0.02 + 0.98 × 寄る速さ)
     *
     *   TARGET 0.75・寄る速さ 0.05 では **−0.63**（＝登るどころか沈む）。
     * ⚠ 1.2 と寄る速さ 0.2 で **約 +0.72 ブロック/tick（14 m/s）の上昇**になる。
     * ⚠ **水平は elytra_boost_limit が 0.75／1.5 で削る**ので、ここを上げても横には速くならない
     *   （＝「エリトラの滑空としての速さは elytra_nerf に従う」を保ったまま、縦だけ動かせる）。
     */
    private static final double WORLD3_TARGET_SPEED = 1.2D;

    /**
     * 押す前の速度の控え。⚠ クライアント側の自分1人ぶんしか通らない（`IcarusClient` は
     * 自分のプレイヤーにしか呼ばれず、どちらの inject も同じ tick の中で対になる）。
     */
    private static Vec3 world3$beforeBoost = null;

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
        } else if (world3$inHiddenSlot(entity, original)) {
            return ItemStack.EMPTY;
        }
        return original;
    }

    /**
     * 当部（world-3・2026-09-16）: Curios の「表示の切り替え」で消した枠の翼を描かない。
     *
     * ⚠ Icarus 2.14.0 は背中の枠の翼を `findFirstCurio` で取り、枠の表示の設定
     *   （`SlotContext.visible()`）を1度も見ない。⚠ だから切り替えを押しても翼が出たままだった。
     *   上流は Issue #131（未解決）。Elytra Slot と当部の金の腕輪は見ている。
     *
     * ⚠ 見分けは「描こうとしている品が、枠に入っている現物そのものか」（`==`）。
     *   Icarus は枠の現物をそのまま返す（`SlotResult.stack()`）ので当たり、
     *   ⚠ 種族の翼は上の分岐が作った別の品なので当たらない＝ヴァルキリーの翼は巻き込まない。
     *   ⚠ Icarus が将来写しを返すようになったら当たらなくなり、翼が出たままに戻るだけ（落ちない）。
     */
    private static boolean world3$inHiddenSlot(LivingEntity entity, ItemStack wings) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(inventory -> inventory.findCurios(stack -> stack == wings).stream()
                        .anyMatch(result -> !result.slotContext().visible()))
                .orElse(false);
    }
}

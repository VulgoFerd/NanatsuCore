package JinRyuu.JRMCore;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/** Stub de compilacao/teste. NAO e injetado no jogo. */
public class JRMCoreH {
    public static int curTP;
    public static long curTPLong;

    public static NBTTagCompound nbt(Entity entity, String key) {
        if (entity == null) {
            return null;
        }
        // Stub: return a dummy NBTTagCompound instead of accessing pres field
        return new NBTTagCompound();
    }

    public static String numSep(long value) {
        return Long.toString(value);
    }

    /** Converte TP long para short para exibicao (saturado em Short.MAX_VALUE). */
    public static short tpShort(long value) {
        if (value > Short.MAX_VALUE) return Short.MAX_VALUE;
        if (value < Short.MIN_VALUE) return Short.MIN_VALUE;
        return (short) value;
    }

    /** Converte TP long para int para compatibilidade legada (saturado em Integer.MAX_VALUE). */
    public static int clampTP(long value) {
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) value;
    }

    /** Limite maximo de TP (64-bit). Substitui getMaxTP() para gameplay LONG. */
    public static long getMaxTPLong() {
        return Long.MAX_VALUE;
    }

    /** Verifica se o jogador tem TP suficiente para uma habilidade (64-bit). */
    public static boolean canAffordSkill(long currentTP, long skillCost) {
        return currentTP >= 0L && skillCost >= 0L && currentTP >= skillCost;
    }
}

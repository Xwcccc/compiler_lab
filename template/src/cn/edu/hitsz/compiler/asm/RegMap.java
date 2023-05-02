package cn.edu.hitsz.compiler.asm;

import java.util.HashMap;
import java.util.Map;

// 双射
public class RegMap<Reg, String> {
    private final Map<Reg, String> RSmap = new HashMap<>();
    private final Map<String, Reg> SRmap = new HashMap<>();

    public void removeByReg(Reg r) {
        SRmap.remove(RSmap.remove(r));
    }

    public void removeByValue(String value) {
        RSmap.remove(SRmap.remove(value));

    }

    public boolean containsKey(Reg r) {
        return RSmap.containsKey(r);
    }

    public boolean containsValue(String value) {
        return SRmap.containsKey(value);
    }

    public void replace(Reg r, String value) {
        // 对于双射关系, 将会删除交叉项
        removeByReg(r);
        removeByValue(value);
        RSmap.put(r, value);
        SRmap.put(value, r);
    }

    public String getByKey(Reg r) {
        return RSmap.get(r);
    }

    public Reg getByValue(String value) {
        return SRmap.get(value);
    }
}

package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import javax.swing.plaf.ColorUIResource;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static cn.edu.hitsz.compiler.ir.InstructionKind.RET;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    private List<Instruction>  instructionList = new ArrayList<>();
    private List<Instruction> newInstrutionList = new ArrayList<>();
    private Reg ret = new Reg("a0");
    private RegMap<String,Reg> regMap = new RegMap<>();
    private Reg registers[] = new Reg[]{
                    new Reg("t0"),
                    new Reg("t1"),
                    new Reg("t2"),
                    new Reg("t3"),
                    new Reg("t4"),
                    new Reg("t5"),
                    new Reg("t6")  };

    private static class Reg{
        String  id;
        String name;
        boolean free = true;
        boolean live = false;

        public Reg(String id) {
            this.id = id;
        }
    }

    public Reg free_reg(int ins,String text){
        int i = 0;
        while (i < 7 && !registers[i].free){
            i++;
        }
        if (i < 7){
            registers[i].free = false;
            registers[i].name = text;
            regMap.replace(text,registers[i]);
            registers[i].live = findNext(ins,text);
      //     System.out.println("空闲寄存器获取成功  "+registers[i].id+" text:"+text+" 引用记到"+registers[i].live+" ins:"+ins);
            return registers[i];
        }else {
      //      System.out.println("空闲寄存器获取失败，请求获取无引用寄存器");
        }
        return null;
    }

    public Reg unuse_reg(int ins,String text){
        int i = 0,k = 0;
        while (k<7){
            if(findNext(ins,registers[k].name)){
                registers[k].live = true;
            }else {
                registers[k].live = false;
            }
            k++;
        }
        while (i < 7 && registers[i].live){
            i++;
        }
        if (i < 7){
            regMap.replace(text,registers[i]);
            registers[i].name = text;
     //       System.out.println("无引用寄存器获取成功  "+registers[i].id+" text:"+text+" 原来引用记到"+registers[i].live+" ins:"+ins);
            return registers[i];
        }else {
    //        System.out.println("无引用寄存器获取失败");
        }
        return null;
    }

    public boolean findNext(int k,String text){
        int mark = -1;
        int i = k;
        for (;i < instructionList.size(); i++) {
            Instruction cur = instructionList.get(i);
            Iterator<IRValue> l = cur.getOperands().iterator();
            if(cur.getKind()!= RET && cur.getResult().toString().equals(text)){
                mark = i;
            }
            while (l.hasNext()){
                String o = l.next().toString();
                if(o.equals(text)){
                    mark = i;
                }
            }
        }
        return mark>=k;
    }

    public Reg assignReg(int ins,String text){
        Reg ret;
        if((ret=free_reg(ins,text))!=null){
            return ret;
        }else {
            ret = unuse_reg(ins,text);
        }
        return null;
    }

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        //读入前端提供的中间代码并生成所需要的信息 并进行预处理

        InstructionKind kind;
        Instruction current;
        IRValue rs = null;

        for (int i = 0; i < originInstructions.size(); i++) {
            current = originInstructions.get(i);
            kind = current.getKind();

            if(kind.isBinary()){
                if(current.getLHS().isImmediate() && current.getRHS().isImmediate()){
                    int op1 = ((IRImmediate) current.getLHS()).getValue();
                    int op2 = ((IRImmediate) current.getRHS()).getValue();
                    if(kind == InstructionKind.ADD){
                        rs = IRImmediate.of(op1+op2);
                    }else if(kind == InstructionKind.SUB){
                        rs = IRImmediate.of(op1-op2);
                    }else if(kind == InstructionKind.MUL){
                        rs = IRImmediate.of(op1*op2);
                    }

                }else if((current.getLHS().isImmediate() || current.getRHS().isImmediate())){
                    IRImmediate imm = (IRImmediate) (current.getLHS().isImmediate() ? current.getLHS() : current.getRHS());
                    IRVariable variable = (IRVariable)(current.getLHS().isIRVariable() ? current.getLHS() : current.getRHS());

                    if(kind == InstructionKind.ADD){
                        this.instructionList.add(Instruction.createAdd(current.getResult(),variable,imm));
                    }else if(kind == InstructionKind.MUL || (current.getLHS().isImmediate())){
                        IRVariable a = IRVariable.temp();
                        this.instructionList.add(Instruction.createMov(a,current.getLHS()));
                        if(kind == InstructionKind.MUL){
                            this.instructionList.add(Instruction.createMul(current.getResult(),a, current.getRHS()));
                        }else {
                            this.instructionList.add(Instruction.createSub(current.getResult(),a, current.getRHS()));
                        }
                    }else if(kind == InstructionKind.SUB){
                        this.instructionList.add(Instruction.createSub(current.getResult(),variable,imm));
                    }
                }else {
                 this.instructionList.add(current);
                }

            }else if(kind.isReturn()){
                this.instructionList.add(current);
                break;
            }else {
                this.instructionList.add(current);
            }
        }
    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        //  执行寄存器分配与代码生成
        for (int i = 0; i < this.instructionList.size(); i++) {
            Reg result=null,left = null,right=null,argc=null;
            IRValue rImm = null;
            Instruction currentIns = instructionList.get(i);
            if(currentIns.getKind() == RET){
                result = ret;
            }else{
                assignReg(i,currentIns.getResult().toString());
                result = regMap.getByKey(currentIns.getResult().toString());
            }
            if(result == null){
                break;
            }
            if(currentIns.getKind()!= RET && currentIns.getKind()!= InstructionKind.MOV) {
                if (regMap.containsKey(currentIns.getLHS().toString())) {
                    left = regMap.getByKey(currentIns.getLHS().toString());
                } else if (currentIns.getLHS().isIRVariable()) { //在内存中？
                    left = assignReg(i,currentIns.getLHS().toString());
                }
                if (regMap.containsKey(currentIns.getRHS().toString())) {
                    right = regMap.getByKey(currentIns.getRHS().toString());
                } else if (currentIns.getRHS().isIRVariable()) { //在内存中？
                    right = assignReg(i,currentIns.getRHS().toString());
                }
                if(currentIns.getRHS().isImmediate()){
                    rImm = currentIns.getRHS();
                }else {
                    rImm = IRVariable.named(right.id);
                }
            }else if(currentIns.getKind()== InstructionKind.MOV){
                if(regMap.containsKey(currentIns.getFrom().toString())) {
                    argc = regMap.getByKey(currentIns.getFrom().toString());
                }else{
                    argc = assignReg(i,currentIns.getFrom().toString());
                }
                if(currentIns.getFrom().isImmediate()){
                    rImm = currentIns.getFrom();
                }else {
                    rImm = IRVariable.named(argc.id);
                }
            }else if(currentIns.getKind()== RET){
                if(regMap.containsKey(currentIns.getReturnValue().toString())){
                    argc = regMap.getByKey(currentIns.getReturnValue().toString());
                } else if (currentIns.getReturnValue().isIRVariable()) { //在内存中？
                    argc = assignReg(i,currentIns.getReturnValue().toString());
                }
                if(currentIns.getReturnValue().isImmediate()){
                    rImm = currentIns.getReturnValue();
                }else {
                    rImm = IRVariable.named(argc.id);
                }
            }
            switch (currentIns.getKind()){
                case ADD -> {
                    newInstrutionList.add(Instruction.createAssem3(InstructionKind.addi,IRVariable.named(result.id),IRVariable.named(left.id),rImm));
                }
                case SUB -> {
                    newInstrutionList.add(Instruction.createAssem3(InstructionKind.sub,IRVariable.named(result.id),IRVariable.named(left.id),rImm));
                }
                case MUL -> {
                    newInstrutionList.add(Instruction.createAssem3(InstructionKind.mul,IRVariable.named(result.id),IRVariable.named(left.id),rImm));
                }
                case MOV -> {
                    if(instructionList.get(i).getFrom().isImmediate()){
                        newInstrutionList.add(Instruction.createAssem2(InstructionKind.li, IRVariable.named(result.id), rImm));
                    }else {
                        newInstrutionList.add(Instruction.createAssem2(InstructionKind.mv, IRVariable.named(result.id), rImm));
                    }
                }
                case RET -> {
                    newInstrutionList.add(Instruction.createAssem1(InstructionKind.mv,IRVariable.named(result.id),rImm));
                }
                default -> {
                }
            }
        }
    }

    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // 输出汇编代码到文件
        FileUtils.writeLines(path, newInstrutionList.stream().map(Instruction::toString).toList());
    }

}


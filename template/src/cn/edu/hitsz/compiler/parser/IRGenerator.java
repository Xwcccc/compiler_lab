package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Term;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


// TODO: 实验三: 实现 IR 生成

/**
 * 生成四元式 (op,arg1,arg2,result)
 */
public class IRGenerator implements ActionObserver {

    List<Instruction> instructionList = new ArrayList<>();
    private Stack<IRValue> symbol_stack = new Stack<>();
    SymbolTable symbolTable;
    IRVariable none = IRVariable.named("空");

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        if (currentToken.getKind().getCode()==51) {
            //id
            symbol_stack.push(IRVariable.named(currentToken.getText()));
        }else if (currentToken.getKind().getCode()==52){
            //IntConst
            symbol_stack.push(IRImmediate.of(Integer.parseInt(currentToken.getText())));
        } else {
            symbol_stack.push(none);
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        int leng = production.body().size();
        Instruction instruction = null;
        IRVariable result;
        Term k=null;
        String comp=null;
        IRValue op2=null,op1=null;
        Iterator<Term> r = production.body().iterator();
        if(r.hasNext()){
            k = r.next();
        }
        result = IRVariable.temp();
        if(leng >=2 ) {
            comp = r.next().getTermName();
        }
        boolean mark = "+".equals(comp) || "-".equals(comp) || "*".equals(comp) || "=".equals(comp);
        boolean flag = mark || "return".equals(k.toString());

        if(production.index()==10 || production.index()==12 || production.index()==14 || production.index()==15){
        }else if ("(".equals(k.toString())){
            symbol_stack.pop();
            op2 = symbol_stack.pop();
            symbol_stack.pop();
            symbol_stack.push(op2);
        }else if(flag) {
            if (mark) {
                op2 = symbol_stack.pop();
                symbol_stack.pop();
                op1 = symbol_stack.pop();
                if ("+".equals(comp)) {
                    instruction = Instruction.createAdd(result, op1, op2);
                } else if ("-".equals(comp)) {
                    instruction = Instruction.createSub(result, op1, op2);
                } else if ("*".equals(comp)) {
                    instruction = Instruction.createMul(result, op1, op2);
                } else if ("=".equals(comp)) {
                    instruction = Instruction.createMov(IRVariable.named(op1.toString()), op2);
                }
            } else if ("return".equals(k.toString())) {
                op2 = symbol_stack.pop();
                symbol_stack.pop();
                instruction = Instruction.createRet(op2);
            }
            if (instruction != null) {
                instructionList.add(instruction);
                if("return".equals(k.toString())){
                    symbol_stack.push(instruction.getReturnValue());
                }else {
                    symbol_stack.push(instruction.getResult());
                }
            }
        }else {
            for (int i = 0; i < leng; i++) {
                symbol_stack.pop();
            }
           symbol_stack.push(none);
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
       // instructionList.add(Instruction.createRet(IRVariable.named("result")));
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        symbolTable = table;
        symbol_stack.push(none);
    }

    public List<Instruction> getIR() {
        return  instructionList;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }


}

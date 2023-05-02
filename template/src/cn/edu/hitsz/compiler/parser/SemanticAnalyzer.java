package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.lexer.TokenKind;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.symtab.SymbolTableEntry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import static cn.edu.hitsz.compiler.symtab.SourceCodeType.Int;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    private Stack<Type> kind_stack = new Stack<>();
    public SymbolTable symbolTable;

    public SemanticAnalyzer() {
    }


    @Override
    public void whenAccept(Status currentStatus) {
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        //该过程在遇到 reduce production 时要采取的代码动作
        int leng = production.body().size();
        String idName;
        //根据当前产生式的索引，执行相应动作
        int index = production.index();
        switch (index) {
            case 4 -> {     //S -> D id;
                //从属性栈中获取的id对应的Entry，D对应的type
                SymbolTableEntry symbolTalEntry = null;
                SourceCodeType sourCodeType = null;

                symbolTalEntry = kind_stack.pop().symbolTableEntry;
                sourCodeType = kind_stack.pop().sourceCodeType;
                assert symbolTalEntry != null;
                idName = symbolTalEntry.getText();
                symbolTable.get(idName).setType(sourCodeType);
                kind_stack.push(Type.none);
            }
            case 5 -> {     //D -> int;
                kind_stack.pop();
                kind_stack.push(new Type(Int));
            }
            default -> {
                for (int i = 0; i < leng; i++) {
                    kind_stack.pop();
                }
                kind_stack.push(Type.none);
            }
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        //该过程在遇到 shift 时要采取的代码动作
        if ( currentToken.getKind().getCode() == 51) {
            String idName = currentToken.getText();
            if (symbolTable.has(idName)) {
                SymbolTableEntry symbolTableEntry = symbolTable.get(idName);
                kind_stack.push(new Type(symbolTableEntry));
            }else {
                throw new RuntimeException("error");
            }
        }else {
            kind_stack.push(Type.none);
        }
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        //设计你可能需要的符号表存储结构 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        this.symbolTable = table;
        kind_stack.push(Type.none);
    }

    public void dump(String path){
        List<SymbolTableEntry> a = symbolTable.getSymbolTableEntryList();
        SymbolTable newSymboltable = new SymbolTable();
        for (int i = 0; i < a.size(); i++) {
           if( a.get(i).getType()!=null){
               String name = a.get(i).getText();
               SourceCodeType ty = symbolTable.get(name).getType();
               newSymboltable.add(name);
               newSymboltable.get(name).setType(ty);
           }
        }
        newSymboltable.dumpTable(path);
    }

    private static class Type {
        SymbolTableEntry symbolTableEntry;
        SourceCodeType sourceCodeType;
        static Type none = new Type(new SymbolTableEntry("none"));

        public Type(SymbolTableEntry symbolTableEntry) {
            this.symbolTableEntry = symbolTableEntry;
            this.sourceCodeType = symbolTableEntry.getType();
        }
        public Type(SourceCodeType sourceCodeType) {
            this.symbolTableEntry = null;
            this.sourceCodeType = sourceCodeType;
        }

    }
}


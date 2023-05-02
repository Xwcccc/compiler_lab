package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.DoubleToIntFunction;
import java.util.stream.StreamSupport;

import static cn.edu.hitsz.compiler.utils.FilePathConfig.CODING_MAP_PATH;
import static cn.edu.hitsz.compiler.utils.FilePathConfig.OLD_SYMBOL_TABLE;

/**
 * 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @author xwc
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer{
    private final SymbolTable symbolTable;
    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    List<Token> tokenList= new ArrayList<>();
    static Scanner input;
    int markrun=0;

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path)  {
        // 词法分析前的缓冲区 可自由实现各类缓冲区或直接采用完整读入方法
        File file = new File(path);
        try {
            input = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
        public void run() {
            //  自动机实现的词法分析过程
            int line = 1;
            int p =0;
            while (input.hasNextLine()){
                String str=input.nextLine();
                char ch;
                p=0;
                str=str.trim();
                for (;p<str.length();p++){
                    //检查首字母
                    ch=str.charAt(p);
                    if (ch==' ') {
                        continue;
                    } else if (Character.isDigit(ch)){
                        //检查是否IntConst
                        String text_c=null;
                        int mark = p;
                        while (p<str.length() && Character.isDigit(str.charAt(p))){
                            if(p==mark){
                                text_c = String.valueOf(str.charAt(p));
                            }else {
                                text_c = text_c + str.charAt(p);
                            }
                            p++;
                        }
                        p--;
                        tokenList.add(Token.normal("IntConst",text_c));
                    }else if (Character.isLetter(ch)) {
                        //检查是关键字,标识符
                        String text_l = null;
                        int mark = p;
                        while (p<str.length() && Character.isLetter(str.charAt(p))){
                            if(p==mark){
                                text_l = String.valueOf(str.charAt(p));
                            }else {
                                text_l = text_l + str.charAt(p);
                            }
                            p++;
                        }
                        p--;
                        if(text_l.equals("int")){
                            tokenList.add(Token.simple("int"));
                        }else if(text_l .equals("return")){
                            tokenList.add(Token.simple("return"));
                        }else{
                            if(!symbolTable.has(text_l)){
                                symbolTable.add(text_l);
                            }
                            tokenList.add(Token.normal("id",text_l));
                        }
                    }
                    else {//检查是否具体是哪个符号
                        switch (String.valueOf(ch)){
                            case "=" :
                                tokenList.add(Token.simple("="));
                                break;
                            case "," :
                                tokenList.add(Token.simple(","));
                                break;
                            case "+" :
                                tokenList.add(Token.simple("+"));
                                break;
                            case "-" :
                                tokenList.add(Token.simple("-"));
                                break;
                            case ";" :
                                tokenList.add(Token.simple("Semicolon"));
                                break;
                            case "*" :
                                tokenList.add(Token.simple("*"));
                                break;
                            case "/" :
                                tokenList.add(Token.simple("/"));
                                break;
                            case "(" :
                                tokenList.add(Token.simple("("));
                                break;
                            case ")" :
                                tokenList.add(Token.simple(")"));
                                break;
                            default:
                        }
                    }
                }
                line++;
            }
            markrun = 1;
            tokenList.add(Token.eof());
        }
    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        //  从词法分析过程中获取 Token 列表  词法分析过程可以使用 Stream 或 Iterator 实现按需分析亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        while(markrun != 1) {
        }
        return tokenList;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }

    public static void main(String[] args) {
        File file = new File("data/in/input_code.txt");
        try {
            input = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int p =0;
        while (input.hasNextLine()){
            p=0;
            String s = input.nextLine();
            System.out.println(s);
            while (p<s.length()){
                if(String.valueOf(s.charAt(p)).equals(";")){
                    System.out.println((p)+"true");
                }
                p++;
            }
        }
    }
}

package com.nowcoder.community.util;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    // 敏感词替换成
    private static final String REPLACEMENT = "***";

    // 根节点
    private TrieNode rootNode = new TrieNode();

    // 当容器实例化SensitiveFilter这个Bean以后，该方法会被调用（服务启动时）
    @PostConstruct
    public void init() {
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");//类加载器 target/class
                BufferedReader reader = new BufferedReader(new InputStreamReader(is)); // 缓冲流，效率高一点
        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            logger.error("sensitive-words file load failed" + e.getMessage());
        }
    }

    // 将一个敏感词添加到前缀树中
    private void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);//有没有值为c的子节点，如果已经有了就不用重复add了

            if (subNode == null) {
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }

            // 指向子节点，进入下一轮循环
            tempNode = subNode;

            // 最后字符标志，设置结束标识
            if (i == keyword.length() - 1) {
                tempNode.setKeywordEnd(true);
            }
        }
    }

//    // 利用三个指针过滤敏感词。传入待过滤的文本，返回过滤后的文本
//    public String filter(String text) {
//        if (StringUtils.isBlank(text)) {
//            return null;
//        }
//        // 指针1
//        TrieNode tempNode = rootNode;
//        // 指针2
//        int begin = 0;
//        // 指针3
//        int position = 0;
//        // 结果
//        StringBuilder sb = new StringBuilder();
//
//        while (position < text.length()) {
//            char c = text.charAt(position);
//
//            // 跳过符号
//            if (isSymbol(c)) {
//                // 若指针处于根结点
//                if (tempNode == rootNode) {
//                    sb.append(c);
//                    begin++;
//                }
//            }
//            position++;
//            continue;
//        }
//        // 检查下级节点
//        tempNode = tempNode.getSubNode(c);
//        if (tempNode == null) {
//            sb.append(text.charAt(begin));
//            position = begin;
//            tempNode = rootNode;
//        } else if (tempNode.isKeywordEnd()) {
//            sb.append(REPLACEMENT);
//            begin = ++position;
//            tempNode = rootNode;
//        } else {
//            position++;
//        }
//    }
//
//    sb.append(text.subString(begin));
//}


    /**
     * 过滤敏感词
     *
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        // 指针1
        TrieNode tempNode = rootNode;
        // 指针2
        int begin = 0;
        // 指针3
        int position = 0;
        // 结果
        StringBuilder sb = new StringBuilder();

        while (position < text.length()) {
            char c = text.charAt(position);

            // 跳过符号
            if (isSymbol(c)) {
                // 若指针1处于根节点,将此符号计入结果,让指针2向下走一步
                if (tempNode == rootNode) {
                    sb.append(c);
                    begin++;
                }
                // 无论符号在开头或中间,指针3都向下走一步
                position++;
                continue;
            }

            // 检查下级节点
            tempNode = tempNode.getSubNode(c);
            if (tempNode == null) {
                // 以begin开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                // 进入下一个位置
                position = ++begin;
                // 重新指向根节点
                tempNode = rootNode;
            } else if (tempNode.isKeywordEnd()) {
                // 发现敏感词,将begin~position字符串替换掉
                sb.append(REPLACEMENT);
                // 进入下一个位置
                begin = ++position;
                // 重新指向根节点
                tempNode = rootNode;
            } else {
                // 检查下一个字符
                position++;
            }
        }

        // 将最后一批字符计入结果
        sb.append(text.substring(begin));

        return sb.toString();
    }


    // 判断是否为符号
    private boolean isSymbol(Character c){
        // 0x2E80 - 0x9FFF是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c<0x2E80 || c > 0x9FFF);
    }
    // 定义前缀树的节点
    private class TrieNode {

        // 关键词结束的标识
        private boolean isKeywordEnd = false;

        // 子节点
        private Map<Character, TrieNode> subNodes = new HashMap<>();


        public boolean isKeywordEnd(){
            return isKeywordEnd;
        }
        public void setKeywordEnd(boolean keywordEnd){
            isKeywordEnd = keywordEnd;
        }


        // 添加子节点
        public void addSubNode(Character c, TrieNode node){
            subNodes.put(c, node);
        }

        //获取子节点，如果子节点的值是c，那么返回该子节点
        public TrieNode getSubNode(Character c){
            return subNodes.get(c);
        }
    }
}


//          while ((keyword = reader.readLine() != null)) { 报错：程序期望的是 String 类型，但你提供了 boolean 类型。
package net.sjw.blog.utils;

import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.SimTocExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

public class EsSaveArticleUtils {

    public static String markdownToText(String content) {
        //markdown to html
        MutableDataSet options = new MutableDataSet().set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                JekyllTagExtension.create(),
                TocExtension.create(),
                SimTocExtension.create()
        ));
        Parser parser = Parser.builder(options).build();

        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Node parse = parser.parse(content);
        String html = renderer.render(parse);
        //html to text
        return Jsoup.parse(html).text();
    }

    public static void main(String[] args) throws Exception {
        File file = new File("/Users/yun/Desktop/MyJavaNotes/前端/8个ECMAScript语法/testessave.md");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String s = "";
        StringBuilder sb = new StringBuilder();
        while ((s = bufferedReader.readLine()) != null) {
            sb.append(s + "\n");
            //System.out.println(s);
        }
        System.out.println(markdownToText(sb.toString()));
        bufferedReader.close();

    }
}

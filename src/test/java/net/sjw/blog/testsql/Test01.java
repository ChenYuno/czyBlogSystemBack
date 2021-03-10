package net.sjw.blog.testsql;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import net.sjw.blog.entity.Article;
import net.sjw.blog.entity.Setting;
import net.sjw.blog.mapper.ArticleMapper;
import net.sjw.blog.mapper.SettingMapper;


import net.sjw.blog.utils.CookieUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class Test01 {

    @Autowired
    private SettingMapper settingMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Test
    public void test01() {
        System.out.println(settingMapper.selectCount(Wrappers.<Setting>lambdaQuery()
                .eq(Setting::getKey, '1')));
        System.out.println("hello");
    }

    @Test
    public void test02() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        String format = formatter.format(LocalDate.now());
        System.out.println(format);
    }

    @Test
    public void test03() {

//        articleMapper.selectObjs(Wrappers.<Article>lambdaQuery()
//                .select(Article::getLabels)
//                .eq(Article::getId, "1317493271852765186")).stream().forEach(System.out::println);

        String s = "Ass";
        String[] split = s.split("");
        System.out.println(Arrays.toString(split));

    }

    @Test
    public void text04() {
        File file = new File("/Users/yun/Desktop/elasticsearch/funNLP/data/IT词库/it.txt");
        File targetdir = new File("/Users/yun/Desktop/elasticsearch/funNLP/data/IT词库");
        File target = new File("/Users/yun/Desktop/elasticsearch/funNLP/data/IT词库/it.dic");
        FileReader read = null;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        try {
            if (!targetdir.exists()) {
                targetdir.mkdirs();
                if (!target.exists()) {

                    target.createNewFile();
                }
            }
            if (file.exists() && file.isFile()) {
                System.out.println("找到文件了，准备处理");
            }
            read =new FileReader(file);
            bufferedReader =  new BufferedReader(read);
            bufferedWriter = new BufferedWriter(new FileWriter(target));

            String s = null;
            while ((s = bufferedReader.readLine()) != null) {
                int duan = s.indexOf(" ");
                s = s.substring(0, duan)+"\n";

                bufferedWriter.write(s);
            }



        } catch (Exception e) {

            e.printStackTrace();
        }finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }



    @Test
    void test05() {
        System.out.println(CookieUtils.domain);
    }


}

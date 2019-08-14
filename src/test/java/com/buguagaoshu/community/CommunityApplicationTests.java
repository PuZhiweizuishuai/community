package com.buguagaoshu.community;

import com.buguagaoshu.community.util.StringUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CommunityApplicationTests {

    @Test
    public void contextLoads() {
        /*String pwd1 = "123456";
        String pwd2 = "wer123456";

        String pwd3 = StringUtil.BCryptPassword(pwd1);
        String pwd4 = StringUtil.BCryptPassword(pwd2);
        System.out.println("pwd1  " + pwd3 + "   " + pwd3.length());
        System.out.println("pwd2  " + pwd4 + "   " + pwd4.length());
        System.out.println(StringUtil.judgePassword(pwd2, pwd3));*/
        System.out.println(StringUtil.getUUID());
    }

}

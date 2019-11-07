package com.buguagaoshu.community;

import com.buguagaoshu.community.cache.AdvertisementCache;
import com.buguagaoshu.community.mapper.AdvertisementMapper;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.service.LogService;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserService;
import com.buguagaoshu.community.util.FileUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CommunityApplicationTests {
    @Value("${jwt.key}")
    private String key;

    @Value("${File.LOG.PATH}")
    private String LOG_PATH;

    @Autowired
    private QuestionService questionService;

    @Autowired
    QuestionMapper questionMapper;

    @Autowired
    UserService userService;

    @Autowired
    UserMapper userMapper;


    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationMapper notificationMapper;

    @Autowired
    AdvertisementCache advertisementCache;

    @Autowired
    AdvertisementMapper advertisementMapper;

    @Autowired
    LogService logService;

    @Test
    public void contextLoads() {
        System.out.println(logService.loadLogForDown("spring.log").getFilename());

    }

}

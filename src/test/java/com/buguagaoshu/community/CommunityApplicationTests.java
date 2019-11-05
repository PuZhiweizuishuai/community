package com.buguagaoshu.community;

import com.buguagaoshu.community.cache.AdvertisementCache;
import com.buguagaoshu.community.mapper.AdvertisementMapper;
import com.buguagaoshu.community.mapper.NotificationMapper;
import com.buguagaoshu.community.mapper.QuestionMapper;
import com.buguagaoshu.community.mapper.UserMapper;
import com.buguagaoshu.community.model.Advertisement;
import com.buguagaoshu.community.service.NotificationService;
import com.buguagaoshu.community.service.QuestionService;
import com.buguagaoshu.community.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CommunityApplicationTests {
    @Value("${jwt.key}")
    private String key;

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

    @Test
    public void contextLoads() {
        for (int i = 0; i < 27; i++) {
            Advertisement advertisement = new Advertisement();
            advertisement.setTitle("系统默认");
            advertisement.setUrl("/");
            advertisement.setImage("/image/101-desktop-wallpaper.png");
            advertisement.setStatus(0);
            advertisement.setStartTime(0);
            advertisement.setCreateTime(System.currentTimeMillis());
            advertisement.setEndTime(0);
            advertisement.setPosition(" ");
            advertisement.setModifiedUser(0);
            advertisementMapper.insertAdvertisement(advertisement);
        }
    }

}

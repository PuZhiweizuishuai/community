package com.buguagaoshu.community.controller.api;

import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.schedule.HotQuestionTasks;
import com.buguagaoshu.community.schedule.HotTagTasks;
import com.buguagaoshu.community.schedule.HotUserTasks;
import com.buguagaoshu.community.schedule.TagClassTasks;
import com.buguagaoshu.community.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-19 20:49
 */
@RestController
@Slf4j
public class HotCacheApiController {
    private final HotTagTasks hotTagTasks;

    private final HotQuestionTasks hotQuestionTasks;

    private final HotUserTasks hotUserTasks;

    private final TagClassTasks tagClassTasks;

    @Autowired
    public HotCacheApiController(HotTagTasks hotTagTasks, HotQuestionTasks hotQuestionTasks, HotUserTasks hotUserTasks, TagClassTasks tagClassTasks) {
        this.hotTagTasks = hotTagTasks;
        this.hotQuestionTasks = hotQuestionTasks;
        this.hotUserTasks = hotUserTasks;
        this.tagClassTasks = tagClassTasks;
    }

    @GetMapping("/api/admin/updateHotTag")
    public HashMap<String, Object> updateHatTag(HttpServletRequest request) {
        User admin = (User) request.getSession().getAttribute("admin");
        if (admin == null) {
            return StringUtil.dealResultMessage(false, "请先登陆！");
        }
        log.info("管理员 {} 开始手动更新热门话题", admin.getId());
        hotTagTasks.hotTagsCurrentTime();
        log.info("热门话题手动更新完成！");
        return StringUtil.dealResultMessage(true, "更新成功！");
    }

    @GetMapping("/api/admin/updateHotQuestion")
    public HashMap<String, Object> updateHotQuestion(HttpServletRequest request) {
        User admin = (User) request.getSession().getAttribute("admin");
        if (admin == null) {
            return StringUtil.dealResultMessage(false, "请先登陆！");
        }
        log.info("管理员 {} 开始手动更新热门问题", admin.getId());
        hotQuestionTasks.hotQuestionCurrentTime();
        log.info("热门问题手动更新完成！");
        return StringUtil.dealResultMessage(true, "更新成功！");
    }

    @GetMapping("/api/admin/updateHotUser")
    public HashMap<String, Object> updateHotUser(HttpServletRequest request) {
        User admin = (User) request.getSession().getAttribute("admin");
        if (admin == null) {
            return StringUtil.dealResultMessage(false, "请先登陆！");
        }
        log.info("管理员 {} 开始手动更新活跃用户", admin.getId());
        hotUserTasks.hotUserCurrentTime();
        log.info("活跃用户手动更新完成！");
        return StringUtil.dealResultMessage(true, "更新成功！");
    }

    @GetMapping("/api/admin/updateTagClass")
    public Map<String, Object> updateTagClass(HttpServletRequest request) {
        User admin = (User) request.getSession().getAttribute("admin");
        if (admin == null) {
            return StringUtil.dealResultMessage(false, "请先登陆！");
        }
        log.info("管理员 {} 开始手动同步话题页", admin.getId());
        tagClassTasks.TagClassCache();
        log.info("话题同步完成！");
        return StringUtil.dealResultMessage(true, "同步成功！");
    }
}

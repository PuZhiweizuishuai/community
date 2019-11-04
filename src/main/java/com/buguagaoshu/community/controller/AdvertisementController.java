package com.buguagaoshu.community.controller;

import com.buguagaoshu.community.cache.AdvertisementCache;
import com.buguagaoshu.community.dto.CreateAdvertisement;
import com.buguagaoshu.community.dto.PaginationDto;
import com.buguagaoshu.community.model.Advertisement;
import com.buguagaoshu.community.model.User;
import com.buguagaoshu.community.service.AdvertisementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-03 20:36
 */
@Controller
public class AdvertisementController {
    private final AdvertisementService advertisementService;

    private final AdvertisementCache advertisementCache;

    @Autowired
    public AdvertisementController(AdvertisementService advertisementService, AdvertisementCache advertisementCache) {
        this.advertisementService = advertisementService;
        this.advertisementCache = advertisementCache;
    }


    @GetMapping("/adUrl")
    public String redirectAdUrl(@RequestParam(value = "url", defaultValue = "/") String adUrl,
                                @RequestParam(value = "id", defaultValue = "-1") String id) {
        /**
         * TODO 如果不需要记录浏览量，此处可以直接 return url
         * */
        String url = advertisementService.updateAdvertisementViewCount(id);
        if (url.equals(adUrl)) {
            return "redirect:" + url;
        }
        return "redirect:/";
    }

    @GetMapping("/admin/advertisement")
    public String getAdvertisementPage(@RequestParam(value = "page", defaultValue = "1") String page,
                                       @RequestParam(value = "size", defaultValue = "20") String size,
                                       Model model) {
        PaginationDto<Advertisement> paginationDto = advertisementService.getAdvertisementList(page, size);
        model.addAttribute("Advertisements", paginationDto);
        return "admin/advertisement";
    }

    @GetMapping("/admin/advertisement/add")
    public String getAddAdvertisement() {
        return "admin/addAdvertisement";
    }

    @GetMapping("/admin/advertisement/{id}")
    public String alterAdvertisementPage(@PathVariable("id") String id,
                                     Model model) {
        long adId = -1;
        try {
            adId = Long.parseLong(id);
        } catch (Exception e) {
            adId = -1;
        }
        Advertisement advertisement = advertisementService.selectAdvertisementById(adId);
        if (advertisement != null) {
            model.addAttribute("ad", advertisement);
            return "admin/alterAdvertisement";
        }
        return "admin/Advertisement";
    }

    @PostMapping("/admin/advertisement/alter")
    @ResponseBody
    public Map<String, Object> alterAdvertisement(@RequestBody CreateAdvertisement createAdvertisement,
                                                  HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        User user = (User) request.getSession().getAttribute("admin");
        if (user == null) {
            map.put("success", false);
            map.put("msg", "修改失败，请重试！");
            return map;
        }
        int result = advertisementService.alterAdvertisement(createAdvertisement, user);
        if (result == 1) {
            map.put("success", true);
            map.put("msg", "修改成功");
            return map;
        } else {
            map.put("success", false);
            map.put("msg", "修改失败，请重试！");
            map.put("ad", createAdvertisement);
            return map;
        }
    }


    @PostMapping("/admin/advertisement/add")
    @ResponseBody
    public Map<String, Object> addAdvertisement(@RequestBody CreateAdvertisement createAdvertisement,
                                                HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        User user = (User) request.getSession().getAttribute("admin");
        if (user == null) {
            map.put("success", false);
            map.put("msg", "创建失败，请重试！");
            return map;
        }
        int result = advertisementService.insertAdvertisement(createAdvertisement, user);
        if (result == 1) {
            map.put("success", true);
            map.put("msg", "创建成功");
            return map;
        }
        map.put("success", false);
        map.put("msg", "创建失败，请重试！");
        map.put("ad", createAdvertisement);
        return map;
    }

    @PostMapping("/admin/advertisement/setting")
    @ResponseBody
    public Map<String, Object> settingAdvertisement(@RequestBody CreateAdvertisement createAdvertisement,
                                                    HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        User user = (User) request.getSession().getAttribute("admin");
        if (user == null) {
            map.put("success", false);
            map.put("msg", "创建失败，请重试！");
            return map;
        }

        int result = advertisementService.settingAdvertisement(createAdvertisement, user);
        if (result == 1) {
            map.put("success", true);
            map.put("msg", "创建成功");
            return map;
        } else if (result == 2) {
            map.put("success", false);
            map.put("msg", "超过当前分区所能投放的最大数");
            map.put("ad", createAdvertisement);
            return map;
        } else {
            map.put("success", false);
            map.put("msg", "创建失败，请重试！");
            map.put("ad", createAdvertisement);
            return map;
        }
    }

    @PostMapping("/admin/advertisement/close")
    @ResponseBody
    public Map<String, Object> closeAdvertisement(@RequestBody CreateAdvertisement createAdvertisement,
                                                  HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        User user = (User) request.getSession().getAttribute("admin");
        if (user == null) {
            map.put("success", false);
            map.put("msg", "请先登陆");
            return map;
        }
        int result = advertisementService.closeAdvertisement(createAdvertisement, user);
        if (result == 1) {
            map.put("success", true);
            map.put("msg", "结束成功");
            return map;
        } else {
            map.put("success", false);
            map.put("msg", "结束失败，请重试！");
            return map;
        }
    }
}

package com.buguagaoshu.community.service;

import com.buguagaoshu.community.model.User;

import java.util.List;
import java.util.Map;

/**
 * 用户封禁：临时封禁 / 永久封禁 / 解封，以及封禁状态查询。
 */
public interface UserBanService {

    record BanCommand(int type, Integer days, String reason) {
    }

    /** 当前是否处于生效中的封禁（临时封禁到期后自动视为未封禁） */
    boolean isBanned(long userId);

    /** 封禁提示文案，未封禁时返回空字符串 */
    String bannedMessage(long userId);

    /** 封禁详情，供会话接口与后台展示 */
    Map<String, Object> banInfo(long userId);

    /** 封禁用户；type=1 临时封禁（days 天），type=2 永久封禁 */
    void ban(long userId, BanCommand command, User operator);

    /** 解封用户 */
    void unban(long userId, User operator);

    /** 后台用户列表批量装配封禁状态 */
    void fillBanStatus(List<User> users);
}

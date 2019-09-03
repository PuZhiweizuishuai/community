package com.buguagaoshu.community.util;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-09-03 13:55
 */
public class NumberUtils {
    /**
     * 返回分页参数
     * */
    public static long[] getPageAndSize(String page, String size, long allCount) {
        long offset;
        long sizeNumber;
        // 使用 string 防止传入参数不是数字
        try {
            offset = Long.valueOf(page);
            sizeNumber = Long.valueOf(size);
        } catch (Exception e) {
            offset = 1;
            sizeNumber = 10;
        }

        // 控制页面显示问题数量
        if (sizeNumber > 20 || sizeNumber < 5) {
            sizeNumber = 10;
        }


        long totalPage = 1;

        // 计算总页数
        if (allCount % sizeNumber == 0) {
            totalPage = allCount / sizeNumber;
        } else {
            totalPage = (allCount / sizeNumber) + 1;
        }

        // 容错处理，防止用户手动输入过大的数
        if (offset >= totalPage) {
            offset = totalPage;
        }
        // 防止传入参数为负
        if (offset <= 0) {
            offset = 1;
        }


        // 计算分页公式 size * (page - 1)
        long pageParam = sizeNumber * (offset - 1);
        long[] param = new long[4];
        // 页码
        param[0] = pageParam;
        // 每页显示数
        param[1] = sizeNumber;
        // 总页数
        param[2] = totalPage;
        // 当前页
        param[3] = offset;
        return param;
    }
}

package com.buguagaoshu.community;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-11-01 19:43
 */
public class Test {
    public static void main(String[] args) {
        String longString = "非洲在不经意间这样说过，最灵繁的人也看不见自己的背脊。这句话语";
        String shortString = "非洲在不经意间这样说过";
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            System.out.println(longString.length());
        }
        System.out.println(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();

        //System.out.println(longString.substring(0, 25));
    }
}

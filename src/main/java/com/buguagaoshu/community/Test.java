package com.buguagaoshu.community;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-10-19 19:37
 */
public class Test {
    public static void main(String[] args) {
        String emoji = "\uD83D\uDE00 \uD83D\uDE01 \uD83D\uDE02 \uD83D\uDE03 \uD83D\uDE04 \uD83D\uDE05 \uD83D\uDE06 \uD83D\uDE09 \uD83D\uDE0A \uD83D\uDE0B \uD83D\uDE0E \uD83D\uDE0D \uD83D\uDE18 \uD83D\uDE17 \uD83D\uDE19 \uD83D\uDE1A ☺ \uD83D\uDE07 \uD83D\uDE10 \uD83D\uDE11 \uD83D\uDE36 \uD83D\uDE0F \uD83D\uDE23 \uD83D\uDE25 \uD83D\uDE2E \uD83D\uDE2F \uD83D\uDE2A \uD83D\uDE2B \uD83D\uDE34 \uD83D\uDE0C \uD83D\uDE1B \uD83D\uDE1C \uD83D\uDE1D \uD83D\uDE12 \uD83D\uDE13 \uD83D\uDE14 \uD83D\uDE15 \uD83D\uDE32 \uD83D\uDE37 \uD83D\uDE16 \uD83D\uDE1E \uD83D\uDE1F \uD83D\uDE24 \uD83D\uDE22 \uD83D\uDE2D \uD83D\uDE26 \uD83D\uDE27 \uD83D\uDE28 \uD83D\uDE2C \uD83D\uDE30 \uD83D\uDE31 \uD83D\uDE33 \uD83D\uDE35 \uD83D\uDE21 \uD83D\uDE20";
        String[] e = emoji.split(" ");
        for (int i = 0; i < e.length; i++) {
            System.out.println("\"" + i + "\": " + "\"" + e[i] + "\",");
        }
    }
}

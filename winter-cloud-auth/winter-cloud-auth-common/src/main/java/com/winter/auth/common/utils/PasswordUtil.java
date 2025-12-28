//package com.winter.auth.common.utils;
//
//import cn.hutool.crypto.digest.DigestUtil;
//
///**
// * 密码工具类
// */
//public class PasswordUtil {
//
//    /**
//     * 密码加密（使用 MD5）
//     *
//     * @param password 明文密码
//     * @return 加密后的密码
//     */
//    public static String encrypt(String password) {
//        return DigestUtil.md5Hex(password);
//    }
//
//    /**
//     * 验证密码
//     *
//     * @param inputPassword    用户输入的密码
//     * @param encryptedPassword 数据库中加密的密码
//     * @return 是否匹配
//     */
//    public static boolean verify(String inputPassword, String encryptedPassword) {
//        String encrypted = encrypt(inputPassword);
//        return encrypted.equals(encryptedPassword);
//    }
//}

package com.ding.miaosha.util;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {
    public static String md5(String src){
        return DigestUtils.md5Hex(src);
    }

    /**
     * salt.写在客户端，和用户输入的密码做一次拼装，传给客户端
     */
    private static String salt = "1a2b3c4d";
    public static String inputPassFromPass(String inputPass){
        String str = "" + salt.charAt(0) + salt.charAt(2) + inputPass + salt.charAt(5)+salt.charAt(4);
        return md5(str);
    }

    /**
     * 第二次md5,存在数据库里的密码
     *
     * @return
     */

    public static String fromPassToDbPass(String fromPass,String salt){
        String str = "" + salt.charAt(0) + salt.charAt(2) + fromPass + salt.charAt(5)+salt.charAt(4);
        return md5(str);
    }
    public static String inputPassToDbpass(String input,String saltDb){
       String frompass = inputPassFromPass(input);
       String dbPass = fromPassToDbPass(frompass,saltDb);
       return dbPass;
    }

    public static String inputPassToDbPass(String inputPass, String saltDB) {
        String formPass = inputPassFromPass(inputPass);
        String dbPass = fromPassToDbPass(formPass, saltDB);
        return dbPass;
    }

    public static void main(String[] args) {
//        System.out.println(inputPassFromPass("123456"));//12123456c3
//
//        System.out.println(fromPassToDbPass(inputPassFromPass("123456"),"1a2b3c4d"));
        System.out.println(inputPassToDbpass("123456","1a2b3c4d"));
    }
}

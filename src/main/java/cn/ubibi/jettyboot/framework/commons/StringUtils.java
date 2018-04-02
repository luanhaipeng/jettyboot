package cn.ubibi.jettyboot.framework.commons;


import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static String join(Collection collection, String flag) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }

        Object[] objArray = collection.toArray(new Object[collection.size()]);
        return join(objArray, flag);
    }


    public static String join(Object[] o, String flag) {

        if (o == null || o.length == 0) {
            return "";
        }

        StringBuilder str_buff = new StringBuilder();

        for (int i = 0, len = o.length; i < len; i++) {
            str_buff.append(String.valueOf(o[i]));
            if (i < len - 1) str_buff.append(flag);
        }

        return str_buff.toString();
    }

    public static String replaceAll(String str, Map<String, String> map) {
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            str = str.replaceAll(entry.getKey(), entry.getValue());
        }
        return str;
    }


    private static Pattern humpPattern = Pattern.compile("[A-Z]");

    /**
     * 驼峰法转下划线
     *
     * @param str 源字符串
     * @return 转换后的字符串
     */
    public static String camel2Underline(String str) {
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
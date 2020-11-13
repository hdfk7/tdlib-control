package com.fansmore.api.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RandomUtils {

    public synchronized static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static List<String> randomlyGeneratedContacts(String rule, int total) {
        String flag = "#";
        rule = rule.replace("*", flag);
        List<String> data = new ArrayList<>();
        int size = 0;
        int max = 1;
        int count = 0;
        String str = rule;
        while (str.contains(flag)) {
            str = str.substring(str.indexOf(flag) + flag.length());
            max *= 10;
            count++;
        }
        if (max == 1) return data;
        if (total == 0) total = max;
        int init = max > total ? (int) (Math.random() * max) : 0;
        while (init > 0 && init + total > max) {
            init = (int) (Math.random() * max);
        }
        int index = init;
        String org1 = rule.substring(0, rule.indexOf(flag));
        String org2 = rule.substring(rule.lastIndexOf(flag) + 1);
        while (size < total && size < max) {
            StringBuilder tmp = new StringBuilder(String.valueOf(index));
            int i = count - tmp.length();
            for (int j = 0; j < i; j++) {
                tmp.insert(0, "0");
            }
            index++;
            data.add("+" + org1 + tmp.toString() + org2);
            size++;
        }
        return data;
    }
}

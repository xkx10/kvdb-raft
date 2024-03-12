package com.example.kvdbraft;


import lombok.val;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Solution {
    private int getStr(String str){
        int n = str.length();
        int ans = 0;
        for(int i = 0; i<n; i++){
            char ch = str.charAt(i);
            ans = ans*10 + ch - '0';
        }
        return ans;
    }
    public int compareVersion(String version1, String version2) {
        String[] str1 = version1.split("\\.");
        String[] str2 = version2.split("\\.");
        int n = Math.min(str1.length, str2.length);
        for(int i = 0; i < n; i++){
            int sum1 = getStr(str1[i]);
            int sum2 = getStr(str2[i]);
            if(sum1>sum2) return 1;
            else if(sum2>sum1) return -1;
        }
        while(n<str1.length||n<str2.length){
            if(n<str1.length&&getStr(str1[n])>0) return 1;
            if(n<str2.length&&getStr(str2[n])>0) return -1;
        }
        return 0;
    }
    public static void main(String[] args) {
        Solution solution = new Solution();
        System.out.println(solution.compareVersion("1.01","1.001"));
    }
}

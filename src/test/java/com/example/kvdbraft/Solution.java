package com.example.kvdbraft;


import lombok.val;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Solution {
    private int f(List[] list, int index, Set<String> set, int[] vis){
        if(vis[index] == 1) return 0;
        vis[index] = 1;
        int ans = 0;
        for(int i = 0; i<list[index].size(); i++){
            int j = (int)list[index].get(i);
            if(vis[j] == 1) continue;
            String str = index + "-" + j;
            if(set.contains(str)) ans++;
            ans += f(list,j,set,vis);
        }
        return ans;
    }
    private void f1(List[] list, int index, Set<String> set, int[] vis, int[] sumList){
        if(vis[index] == 1) return;
        vis[index] = 1;
        for(int i = 0; i<list[index].size(); i++){
            int j = (int)list[index].get(i);
            if(vis[j] == 1) continue;
            String str = index + "-" + j;
            sumList[j] = sumList[i];
            if(set.contains(str)) sumList[j]--;
            str = j + "-" + index;
            if(set.contains(str)) sumList[j]++;
            f1(list,j,set,vis,sumList);
        }

    }
    private int getN(int[][] edges){
        int n = 0;
        for(int i = 0; i<edges.length;i++){
            n = Math.max(n, Math.max(edges[i][0],edges[i][1]));
        }
        return n+1;
    }
    public int rootCount(int[][] edges, int[][] guesses, int k) {
        int n = getN(edges);
        List[] list = new List[n];
        for(int i = 0; i<n; i++){
            list[i] = new ArrayList<>();
        }
        for(int i = 0; i<edges.length; i++){
            list[edges[i][0]].add(edges[i][1]);
            list[edges[i][1]].add(edges[i][0]);
        }
        Set<String> set = new HashSet<>();
        for(int i = 0; i<guesses.length; i++){
            String str = guesses[i][0] + "-" + guesses[i][1];
            set.add(str);
        }
        int[] sumList = new int[n];
        sumList[0] = f(list,0,set,new int[n]);
        for(int i = 0; i<n; i++){
            sumList[i] = sumList[0];
        }
        f1(list,0,set,new int[n],sumList);
        int ans = 0;
        for(int i = 0; i<n; i++){
            if(sumList[i]>=k) ans++;
        }
        return ans;
    }

    public static void main(String[] args) {
        Solution solution = new Solution();
        System.out.println(solution.rootCount(new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}}, new int[][]{{1,0}, {3, 4}, {2, 1}, {3, 2}}, 2));
    }
}

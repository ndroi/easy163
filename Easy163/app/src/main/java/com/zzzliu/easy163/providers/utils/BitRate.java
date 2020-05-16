package com.zzzliu.easy163.providers.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by andro on 2020/5/10.
 */
public class BitRate
{
    private static Map<Integer, Map<Integer, int[]>> table = new HashMap<>();

    static
    {
        int[] arr_3_3 = {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, -1};
        int[] arr_3_2 = {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, -1};
        int[] arr_3_1 = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, -1};
        int[] arr_2_3 = {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, -1};
        int[] arr_2_2 = {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1};
        Map<Integer, int[]> map_3 = new HashMap<>();
        map_3.put(3, arr_3_3);
        map_3.put(2, arr_3_2);
        map_3.put(1, arr_3_1);
        table.put(3, map_3);
        Map<Integer, int[]> map_2 = new HashMap<>();
        map_2.put(3, arr_2_3);
        map_2.put(2, arr_2_2);
        map_2.put(1, arr_2_2);
        table.put(2, map_2);
        table.put(0, map_2);
    }

    public static int Detect(byte[] bytes)
    {
        int ptr = 0;
        if(bytes[0] == 'f' && bytes[1] == 'L' && bytes[2] == 'a' && bytes[3] == 'C')
        {
            return 999;
        }
        if(bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3')
        {
            ptr = 6;
            int size = 0;
            for(int i = 0; i < 4; i++)
            {
                size += (bytes[ptr + i] & 0x7f) << (7 * (3 - i));
            }
            ptr = 10 + size;
        }
        int version = ((bytes[ptr + 1] & 0xff) >> 3) & 0x3;
        int layer = ((bytes[ptr + 1]  & 0xff) >> 1) & 0x3;
        int bitrate = (bytes[ptr + 2] & 0xff) >> 4;
        int result = table.get(version).get(layer)[bitrate];
        return result;
    }
}

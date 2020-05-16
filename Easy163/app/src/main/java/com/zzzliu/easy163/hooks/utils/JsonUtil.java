package com.zzzliu.easy163.hooks.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * Created by andro on 2020/5/8.
 */
public class JsonUtil
{
    public interface Rule
    {
        void apply(JSONObject object);
    }

    public static void replace(Object object, String key, int value)
    {
        if(object == null)
        {
            return;
        }
        if(object.getClass().equals(JSONObject.class))
        {
            JSONObject jsonObject = (JSONObject) object;
            Object curValue = jsonObject.get(key);
            if(curValue != null && curValue.getClass().equals(Integer.class))
            {
                jsonObject.replace(key, value);
            }
            else
            {
                for(Object subObject : jsonObject.values())
                {
                    replace(subObject, key, value);
                }
            }
        }else if(object.getClass().equals(JSONArray.class))
        {
            JSONArray jsonArray = (JSONArray) object;
            for(Object subObject : jsonArray)
            {
                replace(subObject, key, value);
            }
        }
    }

    public static void replace(Object object, String key, int oldValue, int value)
    {
        if(object == null)
        {
            return;
        }
        if(object.getClass().equals(JSONObject.class))
        {
            JSONObject jsonObject = (JSONObject) object;
            Object curValue = jsonObject.get(key);
            if(curValue != null && curValue.getClass().equals(Integer.class))
            {
                jsonObject.replace(key, oldValue, value);
            }
            else
            {
                for(Object subObject : jsonObject.values())
                {
                    replace(subObject, key, oldValue, value);
                }
            }
        }else if(object.getClass().equals(JSONArray.class))
        {
            JSONArray jsonArray = (JSONArray) object;
            for(Object subObject : jsonArray)
            {
                replace(subObject, key, oldValue, value);
            }
        }
    }

    public static void replace(Object object, Rule rule)
    {
        if(object == null || rule == null)
        {
            return;
        }
        if(object.getClass().equals(JSONObject.class))
        {
            JSONObject jsonObject = (JSONObject) object;
            rule.apply(jsonObject);
            for(Object subObject : jsonObject.values())
            {
                replace(subObject, rule);
            }
        }else if(object.getClass().equals(JSONArray.class))
        {
            JSONArray jsonArray = (JSONArray) object;
            for(Object subObject : jsonArray)
            {
                replace(subObject, rule);
            }
        }
    }
}

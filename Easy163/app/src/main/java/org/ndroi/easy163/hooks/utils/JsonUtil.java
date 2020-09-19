package org.ndroi.easy163.hooks.utils;

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

    public static void traverse(Object object, Rule rule)
    {
        if (object == null || rule == null)
        {
            return;
        }
        if (object.getClass().equals(JSONObject.class))
        {
            JSONObject jsonObject = (JSONObject) object;
            rule.apply(jsonObject);
            for (Object subObject : jsonObject.values())
            {
                traverse(subObject, rule);
            }
        } else if (object.getClass().equals(JSONArray.class))
        {
            JSONArray jsonArray = (JSONArray) object;
            for (Object subObject : jsonArray)
            {
                traverse(subObject, rule);
            }
        }
    }
}

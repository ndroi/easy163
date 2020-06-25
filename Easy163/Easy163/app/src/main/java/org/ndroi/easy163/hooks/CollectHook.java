package org.ndroi.easy163.hooks;

import com.alibaba.fastjson.JSONObject;

import org.ndroi.easy163.proxy.hook.Hook;
import org.ndroi.easy163.proxy.hook.ResponseHookData;
import org.ndroi.easy163.utils.Crypto;

public class CollectHook extends Hook
{
    @Override
    public boolean rule(String method, String uri)
    {
        if(!method.equals("POST") || !uri2Host(uri).endsWith("music.163.com"))
        {
            return false;
        }
        String path = uri2Path(uri);
        return path.endsWith("/playlist/manipulate/tracks");
    }

    @Override
    public void hookResponse(ResponseHookData data) throws Exception
    {
        byte[] bytes = Crypto.aesDecrypt(data.getContent());
        JSONObject jsonObject = JSONObject.parseObject(new String(bytes));
        jsonObject.put("code", 200);
        jsonObject.remove("message");
        if(jsonObject.getString("trackIds") == null)
        {
            jsonObject.put("trackIds", "[999999]");
            jsonObject.put("count", 999);
            jsonObject.put("cloudCount", 0);
        }
        bytes = jsonObject.toString().getBytes();
        bytes = Crypto.aesEncrypt(bytes);
        data.setContent(bytes);
    }
}

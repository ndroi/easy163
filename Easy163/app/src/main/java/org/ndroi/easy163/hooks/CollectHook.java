package org.ndroi.easy163.hooks;

import com.alibaba.fastjson.JSONObject;

import org.ndroi.easy163.utils.Crypto;
import org.ndroi.easy163.vpn.hookhttp.Request;
import org.ndroi.easy163.vpn.hookhttp.Response;

public class CollectHook extends BaseHook
{
    @Override
    public boolean rule(Request request)
    {
        String method = request.getMethod();
        String host = request.getHeaderFields().get("Host");
        if (!method.equals("POST") || !host.endsWith("music.163.com"))
        {
            return false;
        }
        String path = getPath(request);
        return path.endsWith("/playlist/manipulate/tracks");
    }

    @Override
    public void hookResponse(Response response)
    {
        super.hookResponse(response);
        byte[] bytes = Crypto.aesDecrypt(response.getContent());
        JSONObject jsonObject = JSONObject.parseObject(new String(bytes));
        jsonObject.put("code", 200);
        jsonObject.remove("message");
        if (jsonObject.getString("trackIds") == null)
        {
            jsonObject.put("trackIds", "[999999]");
            jsonObject.put("count", 999);
            jsonObject.put("cloudCount", 0);
        }
        bytes = jsonObject.toString().getBytes();
        bytes = Crypto.aesEncrypt(bytes);
        response.setContent(bytes);
    }
}
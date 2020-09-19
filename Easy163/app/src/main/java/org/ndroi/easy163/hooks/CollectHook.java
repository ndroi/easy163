package org.ndroi.easy163.hooks;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.ndroi.easy163.utils.Crypto;
import org.ndroi.easy163.vpn.hookhttp.Request;
import org.ndroi.easy163.vpn.hookhttp.Response;

public class CollectHook extends BaseHook {

  @Override
  public boolean rule(Request request) {
    String method = request.getMethod();
    String host = request.getHeaderFields().get("Host");

    if (!method.equals("POST") || host == null || !host.endsWith("music.163.com")) {
      return false;
    }

    String path = getPath(request);
    return path.endsWith("/playlist/manipulate/tracks");
  }

  public void hookRequest(Request request) {
    super.hookRequest(request);
    Crypto.Request cryptoRequest = Crypto.decryptRequestBody(new String(request.getContent()));
    String trackId = cryptoRequest.json.getString("trackIds");
    trackId = trackId.substring(2, trackId.length() - 2);

    String pid = cryptoRequest.json.getString("pid");
    String op = cryptoRequest.json.getString("op");
    String postData = "trackIds=[" + trackId + "," + trackId + "]&pid=" + pid + "&op=" + op;

    request.setUri("http://music.163.com/api/playlist/manipulate/tracks");
    request.setContent(postData.getBytes());
  }

  @Override
  public void hookResponse(Response response) {
    super.hookResponse(response);
    byte[] bytes = response.getContent();
    JSONObject jsonObject = JSONObject.parseObject(new String(bytes));
    jsonObject.put("code", 200);
    jsonObject.remove("message");

    if (jsonObject.getString("trackIds") == null) {
      jsonObject.put("trackIds", "[999999]");
      jsonObject.put("count", 999);
      jsonObject.put("cloudCount", 0);
    }

    bytes = JSONObject.toJSONString(jsonObject, SerializerFeature.WriteMapNullValue).getBytes();
    bytes = Crypto.aesEncrypt(bytes);
    response.setContent(bytes);
  }
}
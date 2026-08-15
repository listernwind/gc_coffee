package com.gccoffee.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 微信登录工具：未配置 appid/secret 时走本地模拟登录（code 直接作为 openid）
 */
@Slf4j
@Component
public class WxUtil {

    private final String appid;
    private final String secret;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WxUtil(@Value("${app.wx.appid:}") String appid,
                  @Value("${app.wx.secret:}") String secret) {
        this.appid = appid;
        this.secret = secret;
    }

    public boolean mockMode() {
        return appid == null || appid.isBlank();
    }

    /**
     * 通过 wx.login 的 code 换取 openid
     */
    public String resolveOpenid(String code) {
        if (mockMode()) {
            String openid = (code == null || code.isBlank()) ? "mock_" + System.nanoTime() : code;
            log.info("[模拟登录] code={} -> openid={}", code, openid);
            return openid;
        }
        try {
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appid
                    + "&secret=" + secret + "&js_code=" + code + "&grant_type=authorization_code";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpResponse<String> resp = client.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode node = objectMapper.readTree(resp.body());
            String openid = node.path("openid").asText(null);
            if (openid == null) {
                throw new com.gccoffee.common.BizException("微信登录失败: " + node.path("errmsg").asText());
            }
            return openid;
        } catch (Exception e) {
            throw new com.gccoffee.common.BizException("微信登录失败: " + e.getMessage());
        }
    }
}

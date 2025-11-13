package com.cityquest.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 高德地图相关接口
 */
@RestController
@RequestMapping("/admin/map")
public class MapController {

    @Value("${amap.api-key:}")
    private String amapApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 代理请求高德地点搜索接口，解决前端跨域限制
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam("keywords") String keywords,
                                                      @RequestParam(value = "city", required = false, defaultValue = "全国") String city,
                                                      @RequestParam(value = "offset", required = false, defaultValue = "5") int offset,
                                                      @RequestParam(value = "page", required = false, defaultValue = "1") int page) {
        Map<String, Object> response = new HashMap<>();
        if (amapApiKey == null || amapApiKey.isEmpty()) {
            response.put("success", false);
            response.put("message", "未配置高德API Key，无法执行搜索");
            return ResponseEntity.ok(response);
        }
        try {
            String encodedKeywords = UriUtils.encode(keywords, StandardCharsets.UTF_8);
            String encodedCity = UriUtils.encode(city, StandardCharsets.UTF_8);
            URI uri = URI.create(String.format(
                    "https://restapi.amap.com/v3/place/text?key=%s&keywords=%s&city=%s&offset=%d&page=%d&extensions=all",
                    amapApiKey,
                    encodedKeywords,
                    encodedCity,
                    offset,
                    page
            ));

            @SuppressWarnings("unchecked")
            Map<String, Object> amapResult = restTemplate.getForObject(uri, Map.class);
            if (amapResult == null || !"1".equals(String.valueOf(amapResult.get("status")))) {
                response.put("success", false);
                response.put("message", amapResult != null ? String.valueOf(amapResult.get("info")) : "请求高德接口失败");
                return ResponseEntity.ok(response);
            }

            List<Map<String, Object>> pois = new ArrayList<>();
            Object poiList = amapResult.get("pois");
            if (poiList instanceof List<?>) {
                for (Object item : (List<?>) poiList) {
                    if (item instanceof Map) {
                        Map<?, ?> poi = (Map<?, ?>) item;
                        Object locationObj = poi.get("location");
                        String location = locationObj != null ? locationObj.toString() : "";
                        if (location.isEmpty() || !location.contains(",")) {
                            continue;
                        }
                        String[] coords = location.split(",");
                        try {
                            double lng = Double.parseDouble(coords[0]);
                            double lat = Double.parseDouble(coords[1]);
                            Map<String, Object> simplified = new HashMap<>();
                            Object idObj = poi.get("id");
                            Object nameObj = poi.get("name");
                            Object addressObj = poi.get("address");
                            simplified.put("id", idObj != null ? idObj : lng + "-" + lat);
                            String poiName = nameObj != null ? nameObj.toString() : (addressObj != null ? addressObj.toString() : "");
                            String poiAddress = addressObj != null ? addressObj.toString() : (nameObj != null ? nameObj.toString() : "");
                            simplified.put("name", poiName);
                            simplified.put("address", poiAddress);
                            simplified.put("location", Map.of("lng", lng, "lat", lat));
                            pois.add(simplified);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            response.put("success", true);
            response.put("items", pois);
            response.put("raw", amapResult);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "搜索失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}


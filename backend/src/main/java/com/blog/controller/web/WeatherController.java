package com.blog.controller.web;

import com.blog.common.result.Result;
import com.blog.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "天气接口")
@Slf4j
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private static final String DEFAULT_CITY = "北京";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;

    @Autowired
    public WeatherController(RedisService redisService) {
        this(redisService, createRestTemplate(), new ObjectMapper());
    }

    WeatherController(RedisService redisService, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.restTemplate.getMessageConverters().stream()
                .filter(StringHttpMessageConverter.class::isInstance)
                .map(StringHttpMessageConverter.class::cast)
                .findFirst()
                .ifPresent(converter -> converter.setDefaultCharset(StandardCharsets.UTF_8));
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    @Operation(summary = "获取实时天气")
    @GetMapping
    @SuppressWarnings("unchecked")
    public Result<?> getWeather(HttpServletRequest request,
                                @RequestParam(value = "city", required = false) String city,
                                @RequestParam(value = "lat", required = false) Double lat,
                                @RequestParam(value = "lon", required = false) Double lon) {
        try {
            String requestCity = normalizeCityName(city);
            if (requestCity != null) {
                Map<String, Object> cached = getCachedWeather(requestCity, "manual");
                if (cached != null) {
                    log.info("天气来源=manual, city={}, lat={}, lon={}", requestCity, lat, lon);
                    return Result.success(cached);
                }

                Map<String, Object> weather = fetchWeatherForQuery(requestCity, requestCity, "manual");
                log.info("天气来源=manual, city={}, lat={}, lon={}", requestCity, lat, lon);
                return Result.success(weather);
            }

            if (lat != null && lon != null) {
                try {
                    Map gpsData = fetchWeatherDataByCoordinates(lat, lon);
                    String gpsCity = normalizeCityName(extractNearestAreaCity(gpsData));
                    if (gpsCity != null) {
                        Map<String, Object> cached = getCachedWeather(gpsCity, "gps");
                        if (cached != null) {
                            log.info("天气来源=gps, city={}, lat={}, lon={}", gpsCity, lat, lon);
                            return Result.success(cached);
                        }

                        Map<String, Object> weather = buildWeatherResult(gpsCity, gpsData, "gps");
                        redisService.setWithMinutes(RedisService.CACHE_WEATHER + gpsCity, weather, RedisService.EXPIRE_MEDIUM);
                        log.info("天气来源=gps, city={}, lat={}, lon={}", gpsCity, lat, lon);
                        return Result.success(weather);
                    }
                    log.warn("GPS逆地理定位失败，lat={}, lon={}", lat, lon);
                } catch (Exception e) {
                    log.warn("GPS天气查询失败，回退到IP定位: {}", e.getMessage());
                }
            }

            String clientIp = getClientIp(request);
            String ipCity = resolveCityByIp(clientIp);
            if (ipCity != null) {
                Map<String, Object> cached = getCachedWeather(ipCity, "ip");
                if (cached != null) {
                    log.info("天气来源=ip, city={}, ip={}", ipCity, clientIp);
                    return Result.success(cached);
                }

                Map<String, Object> weather = fetchWeatherForQuery(ipCity, ipCity, "ip");
                log.info("天气来源=ip, city={}, ip={}", ipCity, clientIp);
                return Result.success(weather);
            }

            log.warn("天气来源=default, city={}, lat={}, lon={}, ip={}", DEFAULT_CITY, lat, lon, clientIp);
            return getDefaultWeather(DEFAULT_CITY, "default");
        } catch (Exception e) {
            log.error("获取天气信息失败", e);
            return getDefaultWeather(DEFAULT_CITY, "default");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchWeatherDataByCoordinates(Double lat, Double lon) throws Exception {
        String query = lat + "," + lon;
        String response = fetchWeatherResponse(query);
        return objectMapper.readValue(response, Map.class);
    }

    private Map<String, Object> fetchWeatherForQuery(String query, String normalizedCity, String source) throws Exception {
        String response = fetchWeatherResponse(query);
        Map data = objectMapper.readValue(response, Map.class);
        Map<String, Object> result = buildWeatherResult(normalizedCity, data, source);
        redisService.setWithMinutes(RedisService.CACHE_WEATHER + normalizedCity, result, RedisService.EXPIRE_MEDIUM);
        return result;
    }

    private String fetchWeatherResponse(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String weatherUrl = "https://wttr.in/" + encodedQuery + "?format=j1&lang=zh";
        String response;

        try {
            response = restTemplate.getForObject(weatherUrl, String.class);
        } catch (ResourceAccessException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            if (body != null && !body.isEmpty() && body.contains("current_condition")) {
                response = body;
                log.info("wttr.in返回{}但包含有效数据，继续处理", e.getStatusCode());
            } else {
                throw e;
            }
        } catch (HttpServerErrorException e) {
            throw e;
        }

        if (!StringUtils.hasText(response) || !response.trim().startsWith("{")) {
            throw new IllegalStateException("天气API返回非JSON数据");
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildWeatherResult(String cityName, Map data, String source) {
        List<Map> currentCondition = (List<Map>) data.get("current_condition");
        if (currentCondition == null || currentCondition.isEmpty()) {
            throw new IllegalStateException("天气数据中没有current_condition字段");
        }

        Map current = currentCondition.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("city", cityName);
        result.put("temp", current.get("temp_C"));
        result.put("feelsLike", current.get("FeelsLikeC"));
        result.put("humidity", current.get("humidity"));
        result.put("windSpeed", current.get("windspeedKmph"));
        result.put("windDir", current.get("winddir16Point"));
        result.put("weatherCode", current.get("weatherCode"));
        result.put("text", extractWeatherText(current));
        result.put("source", source);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCachedWeather(String cityName, String source) {
        Object cached = redisService.get(RedisService.CACHE_WEATHER + cityName);
        if (!(cached instanceof Map)) {
            return null;
        }

        Map<String, Object> result = new HashMap<>((Map<String, Object>) cached);
        normalizeCachedWeatherText(result);
        result.put("city", cityName);
        result.put("source", source);
        return result;
    }

    private String resolveCityByIp(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return null;
        }

        try {
            String ipUrl = "https://whois.pconline.com.cn/ipJson.jsp?ip=" + clientIp + "&json=true";
            String ipResponse = restTemplate.getForObject(ipUrl, String.class);
            if (!StringUtils.hasText(ipResponse)) {
                return null;
            }

            Map<?, ?> ipData = objectMapper.readValue(ipResponse, Map.class);
            String city = normalizeCityName((String) ipData.get("city"));
            if (city != null) {
                return city;
            }
            return normalizeCityName((String) ipData.get("pro"));
        } catch (Exception e) {
            log.warn("获取IP定位失败，ip={}, error={}", clientIp, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractNearestAreaCity(Map data) {
        List<Map> nearestArea = (List<Map>) data.get("nearest_area");
        if (nearestArea == null || nearestArea.isEmpty()) {
            return null;
        }

        Map area = nearestArea.get(0);
        String localized = extractTextValue(area, "lang_zh");
        if (StringUtils.hasText(localized)) {
            return localized;
        }

        String areaName = extractTextValue(area, "areaName");
        if (StringUtils.hasText(areaName)) {
            return areaName;
        }

        String region = extractTextValue(area, "region");
        if (StringUtils.hasText(region)) {
            return region;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractTextValue(Map area, String key) {
        Object value = area.get(key);
        if (!(value instanceof List)) {
            return null;
        }
        List<Map> values = (List<Map>) value;
        if (values.isEmpty()) {
            return null;
        }
        Object text = values.get(0).get("value");
        return text == null ? null : String.valueOf(text);
    }

    static String resolveRequestCity(String manualCity, String gpsCity, String ipCity, String defaultCity) {
        String normalizedManual = normalizeCityName(manualCity);
        if (normalizedManual != null) {
            return normalizedManual;
        }

        String normalizedGps = normalizeCityName(gpsCity);
        if (normalizedGps != null) {
            return normalizedGps;
        }

        String normalizedIp = normalizeCityName(ipCity);
        if (normalizedIp != null) {
            return normalizedIp;
        }

        return normalizeCityName(defaultCity);
    }

    static String normalizeCityName(String cityName) {
        if (!StringUtils.hasText(cityName)) {
            return null;
        }

        String normalized = cityName.trim().replaceAll("\\s+", "");
        normalized = normalized.replace("特别行政区", "");
        normalized = normalized.replace("自治州", "");
        normalized = normalized.replace("地区", "");
        normalized = normalized.replace("盟", "");
        normalized = normalized.replace("市辖区", "");
        normalized = normalized.replaceAll("(省|市)$", "");

        return normalized.isEmpty() ? null : normalized;
    }
    
    /**
     * 返回默认天气数据（当API不可用时）
     */
    private Result<?> getDefaultWeather(String cityName, String source) {
        Map<String, Object> result = new HashMap<>();
        result.put("city", cityName);
        result.put("temp", "--");
        result.put("feelsLike", "--");
        result.put("humidity", "--");
        result.put("windSpeed", "--");
        result.put("windDir", "--");
        result.put("weatherCode", "113");
        result.put("text", "暂无数据");
        result.put("source", source);
        return Result.success(result);
    }

    @SuppressWarnings("unchecked")
    static String extractWeatherText(Map current) {
        List<Map> weatherDesc = (List<Map>) current.get("lang_zh");
        if (weatherDesc != null && !weatherDesc.isEmpty()) {
            return normalizeWeatherText(String.valueOf(weatherDesc.get(0).get("value")));
        }

        List<Map> weatherDescEn = (List<Map>) current.get("weatherDesc");
        if (weatherDescEn != null && !weatherDescEn.isEmpty()) {
            return normalizeWeatherText(String.valueOf(weatherDescEn.get(0).get("value")));
        }

        return "未知";
    }

    static String normalizeWeatherText(String text) {
        if (text == null) {
            return "未知";
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return "未知";
        }

        if (!looksLikeMojibake(trimmed)) {
            return trimmed;
        }

        String repaired = new String(trimmed.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
        return containsHanCharacters(repaired) ? repaired : trimmed;
    }

    private static void normalizeCachedWeatherText(Map<?, ?> cached) {
        Object text = cached.get("text");
        if (text instanceof String) {
            ((Map<Object, Object>) cached).put("text", normalizeWeatherText((String) text));
        }
    }

    private static boolean looksLikeMojibake(String text) {
        if (containsHanCharacters(text)) {
            return false;
        }

        return text.indexOf('Ã') >= 0
                || text.indexOf('Â') >= 0
                || text.indexOf('å') >= 0
                || text.indexOf('ä') >= 0
                || text.indexOf('é') >= 0
                || text.indexOf('ï') >= 0
                || text.indexOf('è') >= 0
                || text.indexOf('æ') >= 0;
    }

    private static boolean containsHanCharacters(String text) {
        return text.codePoints()
                .mapToObj(Character.UnicodeScript::of)
                .anyMatch(Character.UnicodeScript.HAN::equals);
    }
    
    /**
     * 获取用户真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多个IP，取第一个
                int index = ip.indexOf(',');
                if (index > 0) {
                    ip = ip.substring(0, index).trim();
                }
            }
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

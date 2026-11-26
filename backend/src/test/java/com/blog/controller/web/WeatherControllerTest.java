package com.blog.controller.web;

import com.blog.service.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class WeatherControllerTest {

    @Test
    void shouldConfigureRestTemplateStringResponsesAsUtf8() {
        WeatherController controller = new WeatherController(mock(RedisService.class));

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(controller, "restTemplate");
        StringHttpMessageConverter converter = restTemplate.getMessageConverters().stream()
                .filter(StringHttpMessageConverter.class::isInstance)
                .map(StringHttpMessageConverter.class::cast)
                .findFirst()
                .orElseThrow();

        assertEquals(StandardCharsets.UTF_8, converter.getDefaultCharset());
    }

    @Test
    void shouldRepairMojibakeChineseWeatherDescriptions() {
        String repaired = WeatherController.normalizeWeatherText("å°é¨");

        assertEquals("小雨", repaired);
    }

    @Test
    void shouldPreferChineseDescriptionWhenPresent() {
        Map<String, Object> current = Map.of(
                "lang_zh", List.of(Map.of("value", "å¤äº")),
                "weatherDesc", List.of(Map.of("value", "Cloudy"))
        );

        assertEquals("多云", WeatherController.extractWeatherText(current));
    }

    @Test
    void shouldFallBackToEnglishDescriptionWhenChineseMissing() {
        Map<String, Object> current = Map.of(
                "weatherDesc", List.of(Map.of("value", "Light rain"))
        );

        assertEquals("Light rain", WeatherController.extractWeatherText(current));
    }

    @Test
    void shouldReturnUnknownWhenNoDescriptionExists() {
        assertEquals("未知", WeatherController.extractWeatherText(Map.of()));
    }

    @Test
    void shouldKeepNormalChineseTextUntouched() {
        String text = "晴转多云";

        assertEquals(text, WeatherController.normalizeWeatherText(text));
    }

    @Test
    void shouldKeepEnglishTextUntouched() {
        String text = "Partly cloudy";

        assertEquals(text, WeatherController.normalizeWeatherText(text));
    }

    @Test
    void repairedTextShouldContainChineseCharacters() {
        String repaired = WeatherController.normalizeWeatherText("é·éµé¨");

        assertTrue(repaired.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN));
    }

    @Test
    void shouldNormalizeCityNamesForMunicipalityAndPrefectureSuffixes() {
        assertEquals("北京", WeatherController.normalizeCityName("北京市"));
        assertEquals("郑州", WeatherController.normalizeCityName("郑州市"));
        assertEquals("香港", WeatherController.normalizeCityName("香港特别行政区"));
    }

    @Test
    void shouldResolveRequestCityWithPriorityManualThenGpsThenIpThenDefault() {
        assertEquals("郑州", WeatherController.resolveRequestCity("郑州市", "北京", "上海", "北京"));
        assertEquals("上海", WeatherController.resolveRequestCity("", "上海市", "北京", "北京"));
        assertEquals("北京", WeatherController.resolveRequestCity(null, null, "北京市", "北京"));
        assertEquals("北京", WeatherController.resolveRequestCity(null, null, null, "北京"));
    }

    @Test
    void shouldReturnNullForBlankCityAfterNormalization() {
        assertNull(WeatherController.normalizeCityName("   "));
        assertNull(WeatherController.normalizeCityName(null));
    }
}

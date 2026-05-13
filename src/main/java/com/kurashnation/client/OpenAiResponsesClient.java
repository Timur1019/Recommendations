package com.kurashnation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kurashnation.config.AiIntegrationProperties;
import com.kurashnation.dto.response.HandbookWeekPlanLlm;
import com.kurashnation.dto.response.LlmWeekFocus;
import com.kurashnation.util.LogUtil;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calls OpenAI <a href="https://platform.openai.com/docs/api-reference/responses">Responses API</a>
 * ({@code POST /v1/responses}) — same family of endpoints as {@code curl .../v1/responses}.
 */
@Component
public class OpenAiResponsesClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(3);

    private static final List<String> WEEK_DAY_KEYS = List.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    );

    private static final List<String> HANDBOOK_DAY_KEYS_LOWER = List.of(
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
    );

    private final AiIntegrationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(45))
            .build();

    public OpenAiResponsesClient(AiIntegrationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * @param instructions optional system-style instructions (Responses API field {@code instructions})
     * @param input        user payload (string), maps to {@code input}
     */
    public Optional<String> createResponse(String instructions, String input) {
        if (!properties.isConfigured()) {
            LogUtil.info("OpenAI: пропуск вызова — ключ API не задан (app.ai.api-key)");
            return Optional.empty();
        }
        if (input == null || input.isBlank()) {
            LogUtil.warn("OpenAI: пропуск вызова — пустой input");
            return Optional.empty();
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", properties.model());
            if (instructions != null && !instructions.isBlank()) {
                body.put("instructions", instructions);
            }
            body.put("input", input);

            String json = objectMapper.writeValueAsString(body);
            String base = properties.baseUrl().trim();
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            URI uri = URI.create(base + "/responses");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = response.statusCode();
            if (code < 200 || code >= 300) {
                logUnsuccessfulHttp(code, response.body());
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (hasNonEmptyError(root)) {
                logTopLevelError(root);
                return Optional.empty();
            }
            String text = extractAssistantText(root);
            if (text.isBlank()) {
                LogUtil.warn(
                        "OpenAI Responses: не удалось извлечь текст (см. структуру ответа). raw=%s",
                        truncate(response.body(), 2800)
                );
                return Optional.empty();
            }
            String trimmed = text.trim();
            LogUtil.info(
                    "OpenAI Responses: ok http=%s model=%s textChars=%d",
                    code,
                    properties.model(),
                    trimmed.length()
            );
            return Optional.of(trimmed);
        } catch (Exception e) {
            LogUtil.warn("OpenAI Responses call failed: %s", e.toString());
            return Optional.empty();
        }
    }

    /**
     * Запрос к модели с последующим разбором JSON {@code {summary, byDay}} для сетки «фокус по дням».
     */
    public Optional<LlmWeekFocus> requestWeekFocus(String instructions, String userPayload) {
        return createResponse(instructions, userPayload).flatMap(this::parseWeekFocusJson);
    }

    /**
     * JSON {@code {summary, weekPlan:{monday..sunday}}} для плана из справочника.
     */
    public Optional<HandbookWeekPlanLlm> requestHandbookWeekPlan(String instructions, String userPayload) {
        return createResponse(instructions, userPayload).flatMap(this::parseHandbookWeekPlanJson);
    }

    private Optional<HandbookWeekPlanLlm> parseHandbookWeekPlanJson(String raw) {
        try {
            String s = stripMarkdownFence(raw);
            JsonNode root = objectMapper.readTree(s);
            String summary = root.path("summary").asText("").trim();
            JsonNode week = root.path("weekPlan");
            Map<String, String> plan = new LinkedHashMap<>();
            if (week != null && week.isObject()) {
                for (String day : HANDBOOK_DAY_KEYS_LOWER) {
                    String v = firstNonBlank(
                            week.path(day).asText(""),
                            week.path(day.toUpperCase()).asText("")
                    );
                    plan.put(day, v.isEmpty() ? "—" : v);
                }
            } else {
                for (String day : HANDBOOK_DAY_KEYS_LOWER) {
                    plan.put(day, "—");
                }
            }
            LogUtil.info("OpenAI: разобран JSON справочника, summaryChars=%d", summary.length());
            return Optional.of(new HandbookWeekPlanLlm(summary, plan));
        } catch (Exception e) {
            LogUtil.warn("OpenAI: не удалось разобрать JSON справочника: %s фрагмент=%s", e.toString(), truncate(raw, 1200));
            return Optional.empty();
        }
    }

    private static String firstNonBlank(String a, String b) {
        String x = a == null ? "" : a.trim();
        if (!x.isEmpty()) {
            return x;
        }
        return b == null ? "" : b.trim();
    }

    private Optional<LlmWeekFocus> parseWeekFocusJson(String raw) {
        try {
            String s = stripMarkdownFence(raw);
            JsonNode root = objectMapper.readTree(s);
            String summary = root.path("summary").asText("").trim();
            JsonNode byDay = root.path("byDay");
            Map<String, String> map = new LinkedHashMap<>();
            if (byDay != null && byDay.isObject()) {
                for (String day : WEEK_DAY_KEYS) {
                    String v = byDay.path(day).asText("").trim();
                    map.put(day, v.isEmpty() ? "—" : v);
                }
            } else {
                for (String day : WEEK_DAY_KEYS) {
                    map.put(day, "—");
                }
            }
            LogUtil.info("OpenAI: разобран JSON плана, summaryChars=%d", summary.length());
            return Optional.of(new LlmWeekFocus(summary, map));
        } catch (Exception e) {
            LogUtil.warn("OpenAI: не удалось разобрать JSON недели: %s фрагмент=%s", e.toString(), truncate(raw, 1200));
            return Optional.empty();
        }
    }

    private static String stripMarkdownFence(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) {
                s = s.substring(nl + 1);
            }
            int end = s.lastIndexOf("```");
            if (end >= 0) {
                s = s.substring(0, end);
            }
        }
        return s.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /**
     * В успешном JSON иногда есть ключ {@code "error": null} — {@link JsonNode#has(String)} всё равно true,
     * поэтому проверяем только «настоящую» ошибку.
     */
    private static boolean hasNonEmptyError(JsonNode root) {
        if (root == null || !root.has("error")) {
            return false;
        }
        JsonNode e = root.get("error");
        if (e == null || e.isNull() || e.isMissingNode()) {
            return false;
        }
        if (e.isObject()) {
            return e.size() > 0;
        }
        return e.isTextual() && !e.asText("").isBlank();
    }

    private void logTopLevelError(JsonNode root) {
        JsonNode e = root.get("error");
        if (e != null && e.isObject()) {
            LogUtil.warn(
                    "OpenAI Responses error code=%s type=%s msg=%s",
                    e.path("code").asText("—"),
                    e.path("type").asText("—"),
                    truncate(e.path("message").asText(""), 800)
            );
        } else {
            LogUtil.warn("OpenAI Responses error: %s", String.valueOf(e));
        }
    }

    /**
     * Разбор тела ошибки OpenAI: отдельно 429 / квота, без дублирования целого JSON в лог.
     */
    private void logUnsuccessfulHttp(int code, String body) {
        if (body == null || body.isBlank()) {
            LogUtil.warn("OpenAI Responses HTTP %s (пустое тело ответа)", code);
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode err = root.get("error");
            if (err != null && err.isObject()) {
                String errCode = err.path("code").asText("");
                String errType = err.path("type").asText("");
                String msg = err.path("message").asText("");
                if (code == 429) {
                    if ("insufficient_quota".equals(errCode) || "insufficient_quota".equals(errType)) {
                        LogUtil.warn(
                                "OpenAI: квота или баланс API исчерпаны (HTTP 429, insufficient_quota). "
                                        + "Пополните счёт / проверьте план: https://platform.openai.com/account/billing"
                        );
                        return;
                    }
                    LogUtil.warn(
                            "OpenAI: лимит запросов (HTTP 429). code=%s type=%s — %s",
                            errCode.isBlank() ? "—" : errCode,
                            errType.isBlank() ? "—" : errType,
                            truncate(msg, 400)
                    );
                    return;
                }
                LogUtil.warn(
                        "OpenAI Responses HTTP %s errorCode=%s type=%s msg=%s",
                        code,
                        errCode.isBlank() ? "—" : errCode,
                        errType.isBlank() ? "—" : errType,
                        truncate(msg, 600)
                );
                return;
            }
        } catch (Exception ignored) {
            // ниже — сырой фрагмент
        }
        LogUtil.warn("OpenAI Responses HTTP %s body=%s", code, truncate(body, 2000));
    }

    /**
     * Collects assistant-visible text from a Responses payload. OpenAI may nest {@code output_text}
     * at different levels; we scan the whole tree and also handle top-level {@code output_text}.
     */
    static String extractAssistantText(JsonNode root) {
        if (root == null) {
            return "";
        }
        if (root.hasNonNull("output_text")) {
            JsonNode ot = root.get("output_text");
            if (ot.isTextual()) {
                return ot.asText("").trim();
            }
        }
        StringBuilder sb = new StringBuilder();
        JsonNode output = root.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                collectOutputText(item, sb);
            }
        } else if (output != null && output.isObject()) {
            output.fields().forEachRemaining(entry -> collectOutputText(entry.getValue(), sb));
        }
        String fromOutput = sb.toString().trim();
        if (!fromOutput.isBlank()) {
            return fromOutput;
        }
        StringBuilder deep = new StringBuilder();
        collectOutputTextDeep(root, deep);
        return deep.toString().trim();
    }

    private static void collectOutputText(JsonNode node, StringBuilder sb) {
        if (node == null || node.isNull()) {
            return;
        }
        String type = node.path("type").asText("");
        if ("output_text".equals(type) || "text".equals(type)) {
            append(sb, node.path("text").asText(""));
            return;
        }
        if ("message".equals(type) || node.has("content")) {
            JsonNode content = node.get("content");
            if (content != null && content.isArray()) {
                for (JsonNode c : content) {
                    collectOutputText(c, sb);
                }
            } else if (content != null && content.isObject()) {
                collectOutputText(content, sb);
            }
        }
    }

    /** Fallback: any {@code {"type":"output_text","text":"..."}} anywhere in the JSON. */
    private static void collectOutputTextDeep(JsonNode node, StringBuilder sb) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            String t = node.path("type").asText("");
            if (("output_text".equals(t) || "text".equals(t)) && node.has("text")) {
                append(sb, node.get("text").asText(""));
            }
            node.fields().forEachRemaining(e -> collectOutputTextDeep(e.getValue(), sb));
        } else if (node.isArray()) {
            for (JsonNode n : node) {
                collectOutputTextDeep(n, sb);
            }
        }
    }

    private static void append(StringBuilder sb, String chunk) {
        if (chunk == null || chunk.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(chunk.trim());
    }
}

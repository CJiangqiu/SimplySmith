package net.simplysmith.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
极简 TOML 读取，只负责读；写出由 SimplySmithConfig 自己拼，方便控制注释排版

支持的语法：[section] 段落头、key = value、# 整行注释与行尾注释
值类型：布尔、整数、小数、双引号字符串
不支持数组、内联表、多行字符串——配置项全是标量用不上
自己写是为了不给 Fabric 端引 TOML 库（Forge 自带的那套 Fabric 没有）

结果按「段落.键」扁平存放，段落外的键直接用键名
*/
public final class Toml {

    private final Map<String, String> values = new LinkedHashMap<>();

    private Toml() {
    }

    // 文件不存在返回空结果而非抛异常，首次启动没配置文件是正常情况
    public static Toml parse(Path file) throws IOException {
        Toml toml = new Toml();
        if (!Files.exists(file)) {
            return toml;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String section = "";
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = stripInlineComment(line.substring(eq + 1).trim());
            if (key.isEmpty()) {
                continue;
            }
            toml.values.put(section.isEmpty() ? key : section + "." + key, value);
        }
        return toml;
    }

    // 只在引号外的 # 处截断，避免把字符串里的井号当注释
    private static String stripInlineComment(String value) {
        boolean inQuotes = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == '#' && !inQuotes) {
                return value.substring(0, i).trim();
            }
        }
        return value;
    }

    // 以下 getter 在缺失或格式非法时一律回落默认值，不抛异常
    public double getDouble(String key, double fallback) {
        String raw = values.get(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public int getInt(String key, int fallback) {
        String raw = values.get(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean getBoolean(String key, boolean fallback) {
        String raw = values.get(key);
        if (raw == null) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return false;
        }
        return fallback;
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }
}

package com.tianji.agent.application;

import com.tianji.agent.api.AgentException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TranscriptDocumentCodec {
    private static final Pattern STORED_LINE = Pattern.compile("^\\[\\[(\\d+):(-?\\d*)]]\\s*(.*)$");
    private static final Pattern TIME_RANGE = Pattern.compile(
            "(?m)^(?:\\d+\\s*\\R)?\\s*(\\d{1,2}):(\\d{2}):(\\d{2})[,.](\\d{3})\\s*-->\\s*" +
                    "(\\d{1,2}):(\\d{2}):(\\d{2})[,.](\\d{3})(?:[^\\r\\n]*)\\R");

    public record Segment(Integer startMoment, Integer endMoment, String text) { }
    public record TimelineChunk(Integer startMoment, Integer endMoment, String content) { }

    public String encode(List<Segment> segments) {
        validate(segments);
        StringBuilder value = new StringBuilder();
        for (Segment segment : segments) {
            value.append("[[").append(segment.startMoment()).append(':')
                    .append(segment.endMoment() == null ? "" : segment.endMoment()).append("]] ")
                    .append(clean(segment.text())).append('\n');
        }
        return value.toString().strip();
    }

    public List<Segment> parseSubtitle(String filename, String content) {
        String extension = extension(filename);
        if (!extension.equals("srt") && !extension.equals("vtt") && !extension.equals("txt")) {
            throw AgentException.badRequest("字幕文件只支持 SRT、VTT 或 TXT");
        }
        String normalized = content == null ? "" : content.replace("\uFEFF", "").replace("\r\n", "\n");
        if (extension.equals("txt")) {
            if (normalized.isBlank()) throw AgentException.badRequest("字幕内容不能为空");
            return List.of(new Segment(0, null, clean(normalized)));
        }
        Matcher matcher = TIME_RANGE.matcher(normalized);
        List<Segment> result = new ArrayList<>();
        while (matcher.find()) {
            int textStart = matcher.end();
            int nextBlank = normalized.indexOf("\n\n", textStart);
            String text = normalized.substring(textStart, nextBlank < 0 ? normalized.length() : nextBlank)
                    .replaceAll("(?m)^<[^>]+>\\s*$", " ").replace('\n', ' ').strip();
            if (!text.isBlank()) result.add(new Segment(seconds(matcher, 1), seconds(matcher, 5), clean(text)));
        }
        if (result.isEmpty()) throw AgentException.badRequest("未能从字幕文件解析出时间轴");
        validate(result);
        return result;
    }

    public List<TimelineChunk> chunks(String stored, int targetCharacters) {
        List<Segment> segments = decode(stored);
        List<TimelineChunk> result = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        Integer start = null;
        Integer end = null;
        for (Segment segment : segments) {
            if (!text.isEmpty() && text.length() + segment.text().length() > targetCharacters) {
                result.add(new TimelineChunk(start, end, text.toString().strip()));
                text.setLength(0);
                start = null;
            }
            if (start == null) start = segment.startMoment();
            end = segment.endMoment();
            if (!text.isEmpty()) text.append('\n');
            text.append(segment.text());
        }
        if (!text.isEmpty()) result.add(new TimelineChunk(start, end, text.toString().strip()));
        return result;
    }

    private List<Segment> decode(String stored) {
        List<Segment> result = new ArrayList<>();
        for (String line : stored.split("\\R")) {
            Matcher matcher = STORED_LINE.matcher(line);
            if (!matcher.matches()) continue;
            String end = matcher.group(2);
            result.add(new Segment(Integer.parseInt(matcher.group(1)), end.isBlank() ? null : Integer.valueOf(end),
                    matcher.group(3)));
        }
        validate(result);
        return result;
    }

    private void validate(List<Segment> segments) {
        if (segments == null || segments.isEmpty()) throw AgentException.badRequest("转写时间轴不能为空");
        if (segments.size() > 10_000) throw AgentException.badRequest("单个转写最多包含 10000 个片段");
        int previous = -1;
        for (Segment segment : segments) {
            if (segment == null || segment.startMoment() == null || segment.startMoment() < 0
                    || segment.text() == null || segment.text().isBlank()) {
                throw AgentException.badRequest("转写片段的开始时间和文本不能为空");
            }
            if (segment.startMoment() < previous || (segment.endMoment() != null && segment.endMoment() < segment.startMoment())) {
                throw AgentException.badRequest("转写时间轴顺序不正确");
            }
            previous = segment.startMoment();
        }
    }

    private int seconds(Matcher matcher, int offset) {
        int hours = Integer.parseInt(matcher.group(offset));
        int minutes = Integer.parseInt(matcher.group(offset + 1));
        int seconds = Integer.parseInt(matcher.group(offset + 2));
        int millis = Integer.parseInt(matcher.group(offset + 3));
        return hours * 3600 + minutes * 60 + seconds + (millis >= 500 ? 1 : 0);
    }

    private String clean(String value) {
        return value.replace("\u0000", "").replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("<[^>]+>", " ").replaceAll("[ \\t]+", " ").strip();
    }

    private String extension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }
}

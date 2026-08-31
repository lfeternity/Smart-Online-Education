package com.tianji.agent.application;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CitationValidator {
    private static final Pattern REFERENCE = Pattern.compile("\\[(\\d{1,3})]");

    public record Result(String content, List<KnowledgeService.SearchHit> citations, boolean changed) { }

    public Result validate(String content, List<KnowledgeService.SearchHit> retrieved) {
        String original = content == null ? "" : content;
        List<KnowledgeService.SearchHit> unique = retrieved == null ? List.of() : retrieved.stream().distinct().toList();
        Matcher matcher = REFERENCE.matcher(original);
        StringBuffer corrected = new StringBuffer();
        Set<Integer> referenced = new LinkedHashSet<>();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            if (index >= 1 && index <= unique.size()) {
                referenced.add(index);
                matcher.appendReplacement(corrected, Matcher.quoteReplacement(matcher.group()));
            } else {
                matcher.appendReplacement(corrected, "");
            }
        }
        matcher.appendTail(corrected);
        if (referenced.isEmpty() && !unique.isEmpty()) {
            corrected.append("\n\n依据：[1] ").append(unique.get(0).title());
            referenced.add(1);
        }
        List<KnowledgeService.SearchHit> citations = new ArrayList<>();
        for (Integer index : referenced) citations.add(unique.get(index - 1));
        String value = corrected.toString();
        return new Result(value, List.copyOf(citations), !value.equals(original));
    }
}

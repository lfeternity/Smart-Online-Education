package com.tianji.agent.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CitationValidatorTest {
    private final CitationValidator validator = new CitationValidator();

    @Test
    void removesUnknownReferencesAndKeepsOnlyReferencedHits() {
        var result = validator.validate("answer [2] invalid [9]", List.of(hit("a"), hit("b")));
        assertEquals("answer [2] invalid ", result.content());
        assertEquals(List.of("b"), result.citations().stream().map(KnowledgeService.SearchHit::chunkId).toList());
        assertTrue(result.changed());
    }

    @Test
    void addsEvidenceWhenRetrievalExistsButModelForgotCitation() {
        var result = validator.validate("answer", List.of(hit("a")));
        assertTrue(result.content().contains("依据：[1] title-a"));
        assertEquals("a", result.citations().get(0).chunkId());
    }

    private KnowledgeService.SearchHit hit(String id) {
        return new KnowledgeService.SearchHit(id, "DOCUMENT", id, 1L, 2L, 3L,
                null, null, "title-" + id, "content", 0.8);
    }
}

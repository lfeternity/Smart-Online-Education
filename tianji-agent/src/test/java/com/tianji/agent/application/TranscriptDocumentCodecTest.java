package com.tianji.agent.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TranscriptDocumentCodecTest {
    private final TranscriptDocumentCodec codec = new TranscriptDocumentCodec();

    @Test
    void parsesSrtAndPreservesTimelineInChunks() {
        String srt = "1\n00:00:01,000 --> 00:00:03,500\n第一段\n\n"
                + "2\n00:00:04,000 --> 00:00:08,000\n第二段\n";
        var segments = codec.parseSubtitle("lesson.srt", srt);
        var chunks = codec.chunks(codec.encode(segments), 100);

        assertEquals(2, segments.size());
        assertEquals(1, chunks.size());
        assertEquals(1, chunks.get(0).startMoment());
        assertEquals(8, chunks.get(0).endMoment());
    }

    @Test
    void rejectsOutOfOrderSegments() {
        assertThrows(RuntimeException.class, () -> codec.encode(List.of(
                new TranscriptDocumentCodec.Segment(10, 12, "later"),
                new TranscriptDocumentCodec.Segment(2, 3, "earlier"))));
    }
}

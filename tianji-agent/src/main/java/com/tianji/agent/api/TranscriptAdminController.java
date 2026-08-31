package com.tianji.agent.api;

import com.tianji.agent.application.TranscriptService;
import com.tianji.agent.application.TranscriptService.TranscriptInput;
import com.tianji.agent.application.TranscriptService.TranscriptMetadata;
import com.tianji.agent.config.AgentProperties;
import com.tianji.agent.domain.KnowledgeDocumentEntity;
import com.tianji.agent.security.AgentAuthorization;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/admin/knowledge/transcripts")
public class TranscriptAdminController {
    private final TranscriptService transcripts;
    private final AgentAuthorization authorization;
    private final AgentProperties properties;

    public TranscriptAdminController(TranscriptService transcripts, AgentAuthorization authorization,
                                     AgentProperties properties) {
        this.transcripts = transcripts;
        this.authorization = authorization;
        this.properties = properties;
    }

    @PostMapping
    public Mono<ApiResponse<KnowledgeDocumentEntity>> create(@RequestHeader("user-info") Long userId,
                                                              @RequestHeader(value = "role-info", required = false) Long roleId,
                                                              @RequestBody TranscriptInput input) {
        requireUploader(userId, roleId);
        return blocking(() -> ApiResponse.ok(transcripts.create(input)));
    }

    @PostMapping(value = "/subtitles:upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<KnowledgeDocumentEntity>> upload(@RequestHeader("user-info") Long userId,
                                                              @RequestHeader(value = "role-info", required = false) Long roleId,
                                                              @RequestPart("file") FilePart file,
                                                              @RequestParam Long courseId,
                                                              @RequestParam(required = false) Long chapterId,
                                                              @RequestParam(required = false) Long sectionId,
                                                              @RequestParam String sourceId,
                                                              @RequestParam String title,
                                                              @RequestParam(defaultValue = "ENROLLED") String visibility) {
        requireUploader(userId, roleId);
        TranscriptMetadata metadata = new TranscriptMetadata(courseId, chapterId, sectionId, sourceId, title, visibility);
        return bytes(file).publishOn(Schedulers.boundedElastic())
                .map(value -> ApiResponse.ok(transcripts.upload(metadata, file.filename(), value)));
    }

    @PostMapping(value = "/audio:transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<KnowledgeDocumentEntity>> transcribe(@RequestHeader("user-info") Long userId,
                                                                  @RequestHeader(value = "role-info", required = false) Long roleId,
                                                                  @RequestPart("file") FilePart file,
                                                                  @RequestParam Long courseId,
                                                                  @RequestParam(required = false) Long chapterId,
                                                                  @RequestParam(required = false) Long sectionId,
                                                                  @RequestParam String sourceId,
                                                                  @RequestParam String title,
                                                                  @RequestParam(defaultValue = "ENROLLED") String visibility) {
        requireUploader(userId, roleId);
        TranscriptMetadata metadata = new TranscriptMetadata(courseId, chapterId, sectionId, sourceId, title, visibility);
        return bytes(file).publishOn(Schedulers.boundedElastic())
                .map(value -> ApiResponse.ok(transcripts.transcribe(metadata, file.filename(), value)));
    }

    private Mono<byte[]> bytes(FilePart file) {
        return DataBufferUtils.join(file.content(), properties.getAsr().getMaxFileBytes()).map(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            return bytes;
        }).onErrorMap(IllegalStateException.class, error -> AgentException.badRequest("上传文件超过大小限制"));
    }

    private void requireAdmin(Long userId, Long roleId) {
        authorization.requireAdmin(userId, roleId, "需要知识管理员权限");
    }

    private void requireUploader(Long userId, Long roleId) {
        authorization.requireKnowledgeUploader(userId, roleId);
    }

    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> action) {
        return Mono.fromCallable(action).subscribeOn(Schedulers.boundedElastic());
    }
}

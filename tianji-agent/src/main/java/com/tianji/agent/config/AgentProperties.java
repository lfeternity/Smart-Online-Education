package com.tianji.agent.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

@Data
@Validated
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    @Valid
    private Ai ai = new Ai();
    @Valid
    private Qdrant qdrant = new Qdrant();
    @Valid
    private Clients clients = new Clients();
    @Valid
    private Memory memory = new Memory();
    @Valid
    private Security security = new Security();
    @Valid
    private Limits limits = new Limits();
    @Valid
    private Knowledge knowledge = new Knowledge();
    @Valid
    private Rerank rerank = new Rerank();
    @Valid
    private Asr asr = new Asr();
    @Valid
    private Discovery discovery = new Discovery();

    @Data
    public static class Ai {
        private boolean enabled;
        private String apiKey = "";
        /** OpenAI-compatible protocol: CHAT_COMPLETIONS or RESPONSES. */
        private Protocol protocol = Protocol.CHAT_COMPLETIONS;
        @NotBlank
        private String baseUrl = "https://api.openai.com/v1";
        private String embeddingBaseUrl = "";
        private String embeddingApiKey = "";
        private EmbeddingProvider embeddingProvider = EmbeddingProvider.OPENAI;
        @NotBlank
        private String chatModel = "gpt-4o-mini";
        @NotBlank
        private String embeddingModel = "text-embedding-3-small";
        private Double temperature = 0.2;
        private Duration timeout = Duration.ofSeconds(30);
        @Min(1)
        @Max(8)
        private int maxToolCalls = 4;
        @Min(1)
        @Max(50)
        private int retrievalTopK = 8;
        @Min(1)
        @Max(100)
        private int retrievalCandidateK = 30;
        private double retrievalMinScore = 0.55;
        private long inputPriceMicrosPer1k = 0L;
        private long outputPriceMicrosPer1k = 0L;
        private String priceVersion = "unpriced";
        @Valid
        private Fallback fallback = new Fallback();
        @Valid
        private Circuit circuit = new Circuit();

        @Data
        public static class Fallback {
            private boolean enabled;
            private String apiKey = "";
            private Protocol protocol = Protocol.CHAT_COMPLETIONS;
            private String baseUrl = "";
            private String chatModel = "";
            private Double temperature = 0.2;
        }

        @Data
        public static class Circuit {
            @Min(1) private int failureThreshold = 3;
            private Duration openDuration = Duration.ofSeconds(30);
        }

        public enum Protocol { CHAT_COMPLETIONS, RESPONSES }
        public enum EmbeddingProvider { OPENAI, LOCAL_HASH }
    }

    @Data
    public static class Qdrant {
        private boolean enabled;
        @NotBlank
        private String baseUrl = "http://localhost:6333";
        @NotBlank
        private String collection = "course_knowledge_v1";
        @Min(1)
        private int dimension = 1536;
        private String apiKey = "";
    }

    @Data
    public static class Clients {
        private String courseBaseUrl = "http://localhost:8086";
        private String learningBaseUrl = "http://localhost:8090";
        private String examBaseUrl = "http://localhost:8089";
        private String searchBaseUrl = "http://localhost:8083";
        private Duration timeout = Duration.ofSeconds(3);
    }

    @Data
    public static class Memory {
        private boolean redisEnabled;
        @Min(4)
        @Max(40)
        private int maxMessages = 12;
        private Duration ttl = Duration.ofHours(24);
    }

    @Data
    public static class Security {
        private String userHeader = "user-info";
        private String roleHeader = "role-info";
        private Set<Long> adminUserIds = new HashSet<>();
        private Set<Long> adminRoleIds = new HashSet<>();
        private Set<Long> teacherRoleIds = new HashSet<>();
        private boolean allowAnonymousHealth = true;
        private boolean jwtEnabled = false;
        private String jwkSetUri = "";
        private String issuerUri = "";
        private String requiredAudience = "";
    }

    @Data
    public static class Limits {
        private boolean enabled = true;
        private boolean redisEnabled = false;
        @Min(1) private int requestsPerMinute = 20;
        @Min(1) private int concurrentStreams = 2;
        @Min(1) private int messagesPerDay = 200;
        @Min(1) private long tokensPerDay = 200_000;
        @Min(1) private long globalTokensPerDay = 20_000_000;
        @Min(0) private long costMicrosPerDay = 0;
        @Min(0) private long globalCostMicrosPerDay = 0;
    }

    @Data
    public static class Knowledge {
        private boolean mqEnabled;
        private long ingestionUserId = 1L;
        @Min(0) private int maxRetries = 3;
        private Duration retryPollDelay = Duration.ofSeconds(5);
        private Duration retryBaseDelay = Duration.ofSeconds(10);
    }

    @Data
    public static class Rerank {
        private boolean enabled;
        private String baseUrl = "";
        private String endpoint = "/rerank";
        private String apiKey = "";
        private String model = "rerank-multilingual-v3.0";
        private Duration timeout = Duration.ofSeconds(5);
    }

    @Data
    public static class Asr {
        private boolean enabled;
        private String baseUrl = "";
        private String endpoint = "/audio/transcriptions";
        private String apiKey = "";
        private String model = "whisper-1";
        private Duration timeout = Duration.ofMinutes(2);
        @Min(1) private int maxFileBytes = 50 * 1024 * 1024;
    }

    @Data
    public static class Discovery {
        private boolean enabled;
        @NotBlank private String serverAddr = "localhost:8848";
        @NotBlank private String serviceName = "agent-service";
        @NotBlank private String group = "DEFAULT_GROUP";
        private String namespace = "";
        private String cluster = "DEFAULT";
        private String username = "";
        private String password = "";
        private String ip = "";
        @Min(0) private int port = 0;
        private boolean failFast;
        private Map<String, String> metadata = new HashMap<>();
    }
}

package io.hephaistos.flagforge.common.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ApiKeyHasherTest {

    @Test
    void hashReturns64CharacterHexString() {
        String apiKey = "a1b2c3d4e5f6789012345678901234567890123456789012345678901234";

        String hash = ApiKeyHasher.hash(apiKey);

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]+");
    }

    @Test
    void hashIsDeterministic() {
        String apiKey = "testApiKey123456";

        String hash1 = ApiKeyHasher.hash(apiKey);
        String hash2 = ApiKeyHasher.hash(apiKey);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void hashProducesDifferentOutputsForDifferentInputs() {
        String apiKey1 = "apiKey1";
        String apiKey2 = "apiKey2";

        String hash1 = ApiKeyHasher.hash(apiKey1);
        String hash2 = ApiKeyHasher.hash(apiKey2);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void hashProducesCorrectSha256() {
        // Known SHA-256 hash for "test"
        // echo -n "test" | sha256sum = 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08
        String input = "test";
        String expectedHash = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

        String actualHash = ApiKeyHasher.hash(input);

        assertThat(actualHash).isEqualTo(expectedHash);
    }
}

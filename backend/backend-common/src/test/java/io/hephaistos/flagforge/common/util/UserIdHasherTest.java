package io.hephaistos.flagforge.common.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdHasherTest {

    @Test
    void sameInputProducesSameUuid() {
        String userId = "user@example.com";

        UUID result1 = UserIdHasher.toUuid(userId);
        UUID result2 = UserIdHasher.toUuid(userId);

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void differentInputsProduceDifferentUuids() {
        UUID result1 = UserIdHasher.toUuid("user1@example.com");
        UUID result2 = UserIdHasher.toUuid("user2@example.com");

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void emptyStringProducesValidUuid() {
        UUID result = UserIdHasher.toUuid("");

        assertThat(result).isNotNull();
    }

    @Test
    void numericIdProducesValidUuid() {
        UUID result = UserIdHasher.toUuid("12345");

        assertThat(result).isNotNull();
    }

    @Test
    void uuidStringProducesValidUuid() {
        String uuidString = "550e8400-e29b-41d4-a716-446655440000";
        UUID result = UserIdHasher.toUuid(uuidString);

        assertThat(result).isNotNull();
        // The result should be different from the input UUID
        assertThat(result).isNotEqualTo(UUID.fromString(uuidString));
    }

    @Test
    void specialCharactersHandledCorrectly() {
        UUID result = UserIdHasher.toUuid("user+test@example.com");

        assertThat(result).isNotNull();
    }

    @Test
    void unicodeCharactersHandledCorrectly() {
        UUID result = UserIdHasher.toUuid("用户@example.com");

        assertThat(result).isNotNull();
    }

    @Test
    void longStringProducesValidUuid() {
        String longString = "a".repeat(1000);
        UUID result = UserIdHasher.toUuid(longString);

        assertThat(result).isNotNull();
    }

    @Test
    void resultIsUuidVersion3() {
        UUID result = UserIdHasher.toUuid("test");

        // UUID v3 has version 3 (bits 12-15 of time_hi_and_version)
        assertThat(result.version()).isEqualTo(3);
    }
}
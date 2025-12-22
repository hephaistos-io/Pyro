package io.hephaistos.flagforge.common.enums;

/**
 * Represents the type of an API key. - READ: Key can only read data (feature flags, configurations)
 * - WRITE: Key can read and write data
 */
public enum KeyType {
    READ,
    WRITE
}

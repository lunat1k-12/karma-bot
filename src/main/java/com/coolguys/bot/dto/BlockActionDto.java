package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Arrays;

@Data
@Builder
public class BlockActionDto {
    private Long id;
    private ChatAccount acc;
    private LocalDateTime expires;
    private BlockType type;

    public enum BlockType {
        DOCTOR_DISEASE("doctor_disease"), SICK("sick");

        private final String value;
        BlockType(String value) {
            this.value = value;
        }

        public static BlockType getByValue(String value) {
            return Arrays.stream(values())
                    .filter(type -> type.value.equals(value))
                    .findFirst()
                    .orElseThrow();
        }

        public String getValue() {
            return value;
        }
    }
}

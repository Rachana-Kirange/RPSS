package com.eventra.eventra.config;

import com.eventra.eventra.enums.RoleEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts String to RoleEnum for Spring form binding
 */
@Component
public class StringToRoleEnumConverter implements Converter<String, RoleEnum> {

    @Override
    public RoleEnum convert(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        try {
            return RoleEnum.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + source);
        }
    }
}

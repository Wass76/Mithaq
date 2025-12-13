package com.Shakwa.utils.converter;

import com.Shakwa.complaint.Enum.Governorate;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter to handle String to Governorate enum conversion.
 * Supports both enum name format (ريف_دمشق) and display format (ريف دمشق).
 * This allows the API to accept governorate names with spaces (as returned by getLabel())
 * and convert them back to the enum value.
 */
@Component
public class StringToGovernorateConverter implements Converter<String, Governorate> {

    @Override
    public Governorate convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = source.trim();
        
        // First, try to match the enum name directly (e.g., "ريف_دمشق")
        try {
            return Governorate.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            // If that fails, try to match by replacing spaces with underscores
            // This handles cases like "ريف دمشق" -> "ريف_دمشق"
            String normalized = trimmed.replace(" ", "_");
            try {
                return Governorate.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                // If still not found, try to match by the display label (getLabel() method)
                // This handles reverse lookup: "ريف دمشق" -> ريف_دمشق
                for (Governorate governorate : Governorate.values()) {
                    if (governorate.getLabel().equals(trimmed)) {
                        return governorate;
                    }
                }
                // If nothing matches, throw a more descriptive exception
                throw new IllegalArgumentException(
                    String.format("Invalid governorate: '%s'. Valid values are: %s", 
                        source, 
                        String.join(", ", 
                            java.util.Arrays.stream(Governorate.values())
                                .map(Governorate::getLabel)
                                .toArray(String[]::new)
                        )
                    )
                );
            }
        }
    }
}

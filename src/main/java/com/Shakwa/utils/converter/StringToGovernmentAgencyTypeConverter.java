package com.Shakwa.utils.converter;

import com.Shakwa.user.Enum.GovernmentAgencyType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter to handle String to GovernmentAgencyType enum conversion.
 * Supports both enum name format (وزارة_الطاقة) and display label format (وزارة الطاقة).
 * This allows the API to accept agency names with spaces (as returned by getLabel())
 * and convert them back to the enum value.
 */
@Component
public class StringToGovernmentAgencyTypeConverter implements Converter<String, GovernmentAgencyType> {

    @Override
    public GovernmentAgencyType convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = source.trim();
        
        // First, try to match the enum name directly (e.g., "وزارة_الطاقة")
        try {
            return GovernmentAgencyType.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            // If that fails, try to match by replacing spaces with underscores
            // This handles cases like "وزارة الطاقة" -> "وزارة_الطاقة"
            String normalized = trimmed.replace(" ", "_");
            try {
                return GovernmentAgencyType.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                // If still not found, try to match by the display label (getLabel() method)
                // This handles reverse lookup: "وزارة الطاقة" -> وزارة_الطاقة
                for (GovernmentAgencyType agency : GovernmentAgencyType.values()) {
                    if (agency.getLabel().equals(trimmed)) {
                        return agency;
                    }
                }
                // If nothing matches, throw a more descriptive exception
                throw new IllegalArgumentException(
                    String.format("Invalid government agency: '%s'. Valid values are: %s", 
                        source, 
                        String.join(", ", 
                            java.util.Arrays.stream(GovernmentAgencyType.values())
                                .map(GovernmentAgencyType::getLabel)
                                .toArray(String[]::new)
                        )
                    )
                );
            }
        }
    }
}

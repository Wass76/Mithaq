package com.Shakwa.utils.converter;

import com.Shakwa.complaint.Enum.ComplaintType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converter to handle String to ComplaintType enum conversion.
 * Supports both enum name format (تعقيد_في_الإجراءات) and display format (تعقيد في الإجراءات).
 * This allows the API to accept complaint types with spaces (as returned by getLabel())
 * and convert them back to the enum value.
 */
@Component
public class StringToComplaintTypeConverter implements Converter<String, ComplaintType> {

    @Override
    public ComplaintType convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = source.trim();
        
        // First, try to match the enum name directly (e.g., "تعقيد_في_الإجراءات")
        try {
            return ComplaintType.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            // If that fails, try to match by replacing spaces with underscores
            // This handles cases like "تعقيد في الإجراءات" -> "تعقيد_في_الإجراءات"
            String normalized = trimmed.replace(" ", "_");
            try {
                return ComplaintType.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                // If still not found, try to match by the display label (getLabel() method)
                // This handles reverse lookup: "تعقيد في الإجراءات" -> تعقيد_في_الإجراءات
                for (ComplaintType type : ComplaintType.values()) {
                    if (type.getLabel().equals(trimmed)) {
                        return type;
                    }
                }
                // If nothing matches, throw a more descriptive exception
                throw new IllegalArgumentException(
                    String.format("Invalid complaint type: '%s'. Valid values are: %s", 
                        source, 
                        String.join(", ", 
                            java.util.Arrays.stream(ComplaintType.values())
                                .map(ComplaintType::getLabel)
                                .toArray(String[]::new)
                        )
                    )
                );
            }
        }
    }
}

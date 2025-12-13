package com.Shakwa.utils.config;

import com.Shakwa.utils.converter.StringToComplaintStatusConverter;
import com.Shakwa.utils.converter.StringToComplaintTypeConverter;
import com.Shakwa.utils.converter.StringToGovernorateConverter;
import com.Shakwa.utils.converter.StringToGovernmentAgencyTypeConverter;
import com.Shakwa.utils.resolver.MultipartJsonArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Unified WebMvc configuration for the application.
 * 
 * This configuration class consolidates:
 * - All custom enum converters (for enums that use underscore/space pattern)
 * - Custom argument resolvers (for multipart JSON parsing)
 * 
 * Having a single WebMvcConfigurer implementation prevents ordering issues
 * and ensures consistent configuration across all modules.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final MultipartJsonArgumentResolver multipartJsonArgumentResolver;
    private final StringToComplaintStatusConverter stringToComplaintStatusConverter;
    private final StringToComplaintTypeConverter stringToComplaintTypeConverter;
    private final StringToGovernorateConverter stringToGovernorateConverter;
    private final StringToGovernmentAgencyTypeConverter stringToGovernmentAgencyTypeConverter;

    public WebMvcConfig(
            MultipartJsonArgumentResolver multipartJsonArgumentResolver,
            StringToComplaintStatusConverter stringToComplaintStatusConverter,
            StringToComplaintTypeConverter stringToComplaintTypeConverter,
            StringToGovernorateConverter stringToGovernorateConverter,
            StringToGovernmentAgencyTypeConverter stringToGovernmentAgencyTypeConverter) {
        this.multipartJsonArgumentResolver = multipartJsonArgumentResolver;
        this.stringToComplaintStatusConverter = stringToComplaintStatusConverter;
        this.stringToComplaintTypeConverter = stringToComplaintTypeConverter;
        this.stringToGovernorateConverter = stringToGovernorateConverter;
        this.stringToGovernmentAgencyTypeConverter = stringToGovernmentAgencyTypeConverter;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // Remove our resolver if it's already in the list (to avoid duplicates)
        resolvers.removeIf(resolver -> resolver instanceof MultipartJsonArgumentResolver);
        // Add our custom resolver at the beginning to ensure it runs before Spring's default RequestPartArgumentResolver
        // This MUST be first so it intercepts before Spring tries to deserialize with HttpMessageConverters
        resolvers.add(0, multipartJsonArgumentResolver);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // Register complaint-related converters
        
        // ComplaintStatus: "IN PROGRESS" -> ComplaintStatus.IN_PROGRESS
        registry.addConverter(stringToComplaintStatusConverter);
        
        // ComplaintType: "تعقيد في الإجراءات" -> ComplaintType.تعقيد_في_الإجراءات
        registry.addConverter(stringToComplaintTypeConverter);
        
        // Governorate: "ريف دمشق" -> Governorate.ريف_دمشق
        registry.addConverter(stringToGovernorateConverter);
        
        // GovernmentAgencyType: "وزارة الطاقة" -> GovernmentAgencyType.وزارة_الطاقة
        registry.addConverter(stringToGovernmentAgencyTypeConverter);
    }
}

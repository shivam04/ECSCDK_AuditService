package com.shvmsnha.auditservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.jakarta.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.strategy.sampling.CentralizedSamplingStrategy;

import jakarta.servlet.Filter;

import java.io.FileNotFoundException;
import java.net.URL;

@Configuration
public class XRayConfig {
    private static final Logger LOG = LoggerFactory.getLogger(XRayConfig.class);

    public XRayConfig() {
        try {
            URL ruleFile = ResourceUtils.getURL("classpath:xray/xray-sampling-rules.json");
            AWSXRayRecorder awsXRayRecorder = AWSXRayRecorderBuilder.standard()
                .withDefaultPlugins()
                .withSamplingStrategy(new CentralizedSamplingStrategy(ruleFile))
                .build();
            AWSXRay.setGlobalRecorder(awsXRayRecorder);
        } catch(FileNotFoundException e) {
            LOG.error("XRay config file not found");
        }
    }

    @Bean
    public Filter TracingFilter() {
        return new AWSXRayServletFilter("productsservice");
    }
}

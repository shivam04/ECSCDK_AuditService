package com.shvmsnha.auditservice.products.repository;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.shvmsnha.auditservice.events.dto.ProductEventDto;
import com.shvmsnha.auditservice.events.dto.ProductEventType;
import com.shvmsnha.auditservice.events.dto.ProductFailureEventDto;
import com.shvmsnha.auditservice.products.models.ProductFailureEvent;
import com.shvmsnha.auditservice.products.models.ProductFailureInfoEvent;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class ProductFailureEventsRepository {
    private static final Logger LOG = LogManager.getLogger(ProductFailureEventsRepository.class);
    private final DynamoDbAsyncTable<ProductFailureEvent> productFailureEventsTable;
    private final DynamoDbEnhancedAsyncClient dbEnhancedAsyncClient;

    @Autowired
    public ProductFailureEventsRepository(@Value("${aws.events.ddb}") String eventsDdbName, 
                            DynamoDbEnhancedAsyncClient dbEnhancedAsyncClient) {
        this.dbEnhancedAsyncClient = dbEnhancedAsyncClient;
        this.productFailureEventsTable = this.dbEnhancedAsyncClient.table(eventsDdbName, TableSchema.fromBean(ProductFailureEvent.class));
    }

    public CompletableFuture<Void> create(ProductFailureEventDto productFailureEventDto, 
                                          ProductEventType productEventType, 
                                          String messageId, String requestId, String traceId) {
        long timestamp = Instant.now().toEpochMilli();
        long ttl = Instant.now().plusSeconds(300).getEpochSecond();
        ProductFailureEvent productFailureEvent = new ProductFailureEvent();
        productFailureEvent.setPk("#product_".concat(productEventType.name())); // #product_PRODUCT_FAILED
        productFailureEvent.setSk(String.valueOf(timestamp));
        productFailureEvent.setCreatedAt(timestamp);
        productFailureEvent.setTtl(ttl);
        productFailureEvent.setEmail(productFailureEventDto.email());

        ProductFailureInfoEvent productFailureInfoEvent = new ProductFailureInfoEvent();
        productFailureInfoEvent.setId(productFailureEventDto.id());
        productFailureInfoEvent.setError(productFailureEventDto.error());
        productFailureInfoEvent.setStatus(productFailureEventDto.status());
        productFailureInfoEvent.setMessageId(messageId);
        productFailureInfoEvent.setRequestId(requestId);
        productFailureInfoEvent.setTraceId(traceId);

        productFailureEvent.setInfo(productFailureInfoEvent);
        return productFailureEventsTable.putItem(productFailureEvent);
    }
}

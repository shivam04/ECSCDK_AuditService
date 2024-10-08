package com.shvmsnha.auditservice.products.repository;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import com.shvmsnha.auditservice.events.dto.ProductEventDto;
import com.shvmsnha.auditservice.events.dto.ProductEventType;
import com.shvmsnha.auditservice.products.models.ProductEvent;
import com.shvmsnha.auditservice.products.models.ProductInfoEvent;

import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
@XRayEnabled
public class ProductEventsRepository {

    private static final Logger LOG = LogManager.getLogger(ProductEventsRepository.class);
    private final DynamoDbAsyncTable<ProductEvent> productEventsTable;
    private final DynamoDbEnhancedAsyncClient dbEnhancedAsyncClient;

    @Autowired
    public ProductEventsRepository(@Value("${aws.events.ddb}") String eventsDdbName, 
                            DynamoDbEnhancedAsyncClient dbEnhancedAsyncClient) {
        this.dbEnhancedAsyncClient = dbEnhancedAsyncClient;
        this.productEventsTable = this.dbEnhancedAsyncClient.table(eventsDdbName, TableSchema.fromBean(ProductEvent.class));
    }

    public CompletableFuture<Void> create(ProductEventDto productEventDto, 
                                          ProductEventType productEventType, 
                                          String messageId, String requestId, String traceId) {
        long timestamp = Instant.now().toEpochMilli();
        long ttl = Instant.now().plusSeconds(300).getEpochSecond();
        ProductEvent productEvent = new ProductEvent();
        productEvent.setPk("#product_".concat(productEventType.name())); // #product_PRODUCT_CREATED
        productEvent.setSk(String.valueOf(timestamp));
        productEvent.setCreatedAt(timestamp);
        productEvent.setTtl(ttl);
        productEvent.setEmail(productEventDto.email());

        ProductInfoEvent productInfoEvent = new ProductInfoEvent();
        productInfoEvent.setId(productEventDto.id());
        productInfoEvent.setCode(productEventDto.code());
        productInfoEvent.setPrice(productEventDto.price());
        productInfoEvent.setMessageId(messageId);
        productInfoEvent.setRequestId(requestId);
        productInfoEvent.setTraceId(traceId);

        productEvent.setInfo(productInfoEvent);
        return productEventsTable.putItem(productEvent);
    }

    private Map<String, AttributeValue> buildExclusiveStartKey(String pk, String exclusiveStartTimeStamp) {
        return (exclusiveStartTimeStamp != null) ? 
            Map.of(
                "pk", AttributeValue.builder().s(pk).build(),
                "sk", AttributeValue.builder().s(exclusiveStartTimeStamp).build()
            ) : null;
    }

    public SdkPublisher<Page<ProductEvent>> findByType(String productEventType, String exclusiveStartTimeStamp, int limit) {
        String pk = "#product_".concat(productEventType);
        return productEventsTable.query(QueryEnhancedRequest.builder()
            .queryConditional(QueryConditional.keyEqualTo(Key.builder()
                        .partitionValue(pk)
                        .build()
                    )
                )
            .exclusiveStartKey(buildExclusiveStartKey(pk ,exclusiveStartTimeStamp))
            .limit(limit)
            .build()
        ).limit(1);
    }


    public SdkPublisher<Page<ProductEvent>> findByTypeAndRange(String productEventType, 
            String exclusiveStartTimeStamp, 
            String from, 
            String to, 
            int limit) {
        String pk = "#product_".concat(productEventType);
        return productEventsTable.query(QueryEnhancedRequest.builder()
            .queryConditional(QueryConditional.sortBetween(
                Key.builder().partitionValue(pk).sortValue(from).build(), 
                Key.builder().partitionValue(pk).sortValue(to).build()))
            .exclusiveStartKey(buildExclusiveStartKey(pk ,exclusiveStartTimeStamp))
            .limit(limit)
            .build()
        ).limit(1);
    }

}

package com.shvmsnha.auditservice.products.services;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.TraceID;
import com.amazonaws.xray.AWSXRay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shvmsnha.auditservice.events.dto.ProductEventDto;
import com.shvmsnha.auditservice.events.dto.ProductEventType;
import com.shvmsnha.auditservice.events.dto.SnsMessageDto;
import com.shvmsnha.auditservice.products.repository.ProductEventsRepository;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;

@Service
public class ProductEventsConsumer {

    private static final Logger LOG = LogManager.getLogger(ProductEventsConsumer.class);
    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsAsyncClient;
    private final String productEventsQueueUrl;
    private final ReceiveMessageRequest receiveMessageRequest;
    private final ProductEventsRepository productEventsRepository;

    @Autowired
    public ProductEventsConsumer(ObjectMapper objectMapper, SqsAsyncClient sqsAsyncClient, 
                                 @Value("${aws.sqs.queue.product.events.url}") String productEventsQueueUrl, 
                                 ProductEventsRepository productEventsRepository) { 
        this.objectMapper = objectMapper;
        this.sqsAsyncClient = sqsAsyncClient;
        this.productEventsQueueUrl = productEventsQueueUrl;
        this.receiveMessageRequest = ReceiveMessageRequest.builder()
            .maxNumberOfMessages(5)
            .queueUrl(this.productEventsQueueUrl)
            .build();
        this.productEventsRepository = productEventsRepository;
    }

    @Scheduled(fixedDelay = 1000)
    public void receiveProductEventMessages() {
        List<Message> messages;
        while (!(messages = sqsAsyncClient.receiveMessage(receiveMessageRequest).join().messages()).isEmpty()) {
            LOG.info("Reading [{}] messages", messages.size());
            messages.parallelStream().forEach(message -> {
                SnsMessageDto snsMessageDto;
                try {
                    snsMessageDto = objectMapper.readValue(message.body(), SnsMessageDto.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                String messageId = snsMessageDto.messageId();
                String requestId = snsMessageDto.messageAttributes().requestId().value();
                String traceId = snsMessageDto.messageAttributes().traceId().value();
                Segment segment = AWSXRay.beginSegment("product-events-consumer");
                segment.setOrigin("AWS::ECS::Container");
                segment.setStartTime(Instant.now().getEpochSecond());
                segment.setTraceId(TraceID.fromString(traceId));
                segment.run(() -> {
                    try {
                        ThreadContext.put("messageId", messageId);
                        ThreadContext.put("requestId", requestId);
                        ThreadContext.put("traceId", traceId);
                        CompletableFuture<Void> productEventFuture; 
                        ProductEventType eventType = ProductEventType.valueOf(snsMessageDto.messageAttributes().eventType().value());
                        switch (eventType) {
                            case PRODUCT_CREATED, PRODUCT_UPDATED, PRODUCT_DELETED -> {
                                ProductEventDto productEventDto = 
                                    objectMapper.readValue(snsMessageDto.message(), ProductEventDto.class);
                                    productEventFuture = productEventsRepository.create(productEventDto, eventType, messageId, requestId, traceId);
                                LOG.info("Product event: [{}] - Id: [{}]", eventType, productEventDto.id());
                            }
                            default -> {
                                LOG.info("Invaliid product event: [{}]", eventType);
                                throw new Exception("Invalid product event");
                            }
                        }
                        CompletableFuture<DeleteMessageResponse> deleteMessageFuture = sqsAsyncClient.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(productEventsQueueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build());

                        CompletableFuture.allOf(productEventFuture, deleteMessageFuture).join();
                        LOG.info("Messsage deleted....");
                    } catch(Exception ex) {
                        LOG.error("Failed to parse product event message");
                        throw new RuntimeException(ex);
                    } finally {
                        ThreadContext.clearAll();
                        segment.setEndTime(Instant.now().getEpochSecond());
                        segment.end();
                        segment.close();
                    }
                }, AWSXRay.getGlobalRecorder());
            });
        }
        AWSXRay.endSegment();
    }

}

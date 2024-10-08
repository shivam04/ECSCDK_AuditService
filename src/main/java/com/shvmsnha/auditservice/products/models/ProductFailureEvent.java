package com.shvmsnha.auditservice.products.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class ProductFailureEvent {
    private String pk; // #product_PRODUCT_FAILED
    private String sk; // timestamp
    private Long createdAt;
    private Long ttl;
    private String email;
    private ProductFailureInfoEvent info;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    /**
     * @return Long return the createdAt
     */
    public Long getCreatedAt() {
        return createdAt;
    }

    /**
     * @param createdAt the createdAt to set
     */
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * @return Long return the ttl
     */
    public Long getTtl() {
        return ttl;
    }

    /**
     * @param ttl the ttl to set
     */
    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    /**
     * @return String return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return ProductFailureInfoEvent return the info
     */
    public ProductFailureInfoEvent getInfo() {
        return info;
    }

    /**
     * @param info the info to set
     */
    public void setInfo(ProductFailureInfoEvent info) {
        this.info = info;
    }

}

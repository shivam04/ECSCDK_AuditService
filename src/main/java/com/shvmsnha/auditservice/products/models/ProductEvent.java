package com.shvmsnha.auditservice.products.models;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class ProductEvent {
    private String pk; // #product_PRODUCT_CREATED
    private String sk; // timestamp
    private Long createdAt;
    private Long ttl;
    private String email;
    private ProductInfoEvent info;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getTtl() {
        return ttl;
    }

    public String getEmail() {
        return email;
    }

    public ProductInfoEvent getInfo() {
        return info;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setInfo(ProductInfoEvent info) {
        this.info = info;
    }

}

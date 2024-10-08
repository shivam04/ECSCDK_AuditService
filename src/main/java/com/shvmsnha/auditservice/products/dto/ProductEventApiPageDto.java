package com.shvmsnha.auditservice.products.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ProductEventApiPageDto(
    List<ProductEventApiDto> items,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String lastEvaluatedTimeStamp,
    int count
) {

}

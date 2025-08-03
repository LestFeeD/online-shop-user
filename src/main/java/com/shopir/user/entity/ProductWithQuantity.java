package com.shopir.user.entity;

import com.shopir.user.dto.ProductKafkaDto;

public record ProductWithQuantity(ProductKafkaDto product, int quantity) {}


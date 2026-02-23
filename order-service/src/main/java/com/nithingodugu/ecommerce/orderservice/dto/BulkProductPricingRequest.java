package com.nithingodugu.ecommerce.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class BulkProductPricingRequest {
    public List<Long> ids;
}

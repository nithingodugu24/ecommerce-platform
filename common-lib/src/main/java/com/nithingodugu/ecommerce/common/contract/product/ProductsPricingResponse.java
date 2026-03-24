package com.nithingodugu.ecommerce.common.contract.product;

import java.math.BigDecimal;
import java.util.List;


public record ProductsPricingResponse(
        ProductsPricingStatus status,
        String message,
        List<ProductPriceDetail> items,
        BigDecimal totalAmount
) {
}

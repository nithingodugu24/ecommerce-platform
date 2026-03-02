package com.nithingodugu.ecommerce.common.contract.product;

import java.math.BigDecimal;

public record ProductPriceDetail(
        String productId,
        String name,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}

package com.nithingodugu.ecommerce.common.contract.product;

import java.util.List;

public record ProductsPricingRequest(
        List<ProductPricingItem> items
){ }

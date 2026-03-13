package com.nithingodugu.ecommerce.common.event;

import java.util.List;

public record OrderConfirmedEvent(
        String orderId
) {
}

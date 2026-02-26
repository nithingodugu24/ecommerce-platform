package com.nithingodugu.ecommerce.common.event;

import lombok.Data;

@Data
public class InventoryFailedEvent {

    private Long orderId;

}

package com.nithingodugu.ecommerce.common.event;

import lombok.Data;

@Data
public class InventoryFailedEvent {

    private String orderId;

}

package com.nithingodugu.ecommerce.inventoryservice.controller.admin;

import com.nithingodugu.ecommerce.inventoryservice.dto.InventoryResponseDto;
import com.nithingodugu.ecommerce.inventoryservice.dto.InventoryUpdateRequestDto;
import com.nithingodugu.ecommerce.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/inventory")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public InventoryResponseDto getInventory(@PathVariable String productId){
        return inventoryService.getInventory(productId);
    }

    @PostMapping("/{productId}")
    public InventoryResponseDto updateInventory(
            @PathVariable String productId,
            @RequestBody InventoryUpdateRequestDto request
    ){
        return inventoryService.updateInventory(productId, request);
    }
}

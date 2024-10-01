package com.sellersphere.orderservice.invalidation;

import java.time.ZonedDateTime;

public interface OrderInvalidationService {
    void schedule(String userId, String orderId, ZonedDateTime dateTime);
}

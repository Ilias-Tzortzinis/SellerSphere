package com.sellersphere.paymentservice;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public final class PaymentsRestController {

    @PutMapping("/{orderId}")
    public void createPayment(@PathVariable String orderId){

    }

}

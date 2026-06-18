package com.example.cs.controller;

import com.example.cs.dto.GatewayCompareResponse;
import com.example.cs.dto.GatewayDemoRequest;
import com.example.cs.dto.GatewayDemoResponse;
import com.example.cs.dto.GatewayOption;
import com.example.cs.service.GatewayCatalogService;
import com.example.cs.service.GatewayDemoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo-only API that helps visualize whether the active gateway passes or blocks a request.
 */
@RestController
@RequestMapping("/demo")
public class GatewayDemoController {

    private final GatewayDemoService gatewayDemoService;
    private final GatewayCatalogService gatewayCatalogService;

    public GatewayDemoController(GatewayDemoService gatewayDemoService, GatewayCatalogService gatewayCatalogService) {
        this.gatewayDemoService = gatewayDemoService;
        this.gatewayCatalogService = gatewayCatalogService;
    }

    @GetMapping("/gateways")
    public java.util.List<GatewayOption> gateways() {
        return gatewayCatalogService.options();
    }

    @PostMapping("/gateway-flow")
    public GatewayDemoResponse run(@Valid @RequestBody GatewayDemoRequest request) {
        return gatewayDemoService.run(request);
    }

    @PostMapping("/gateway-compare")
    public GatewayCompareResponse compare(@Valid @RequestBody GatewayDemoRequest request) {
        return gatewayDemoService.compare(request);
    }
}

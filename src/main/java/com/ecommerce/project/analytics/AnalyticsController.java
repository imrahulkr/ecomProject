package com.ecommerce.project.analytics;

import com.ecommerce.project.analytics.dto.AnalyticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/admin/app/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(){
        AnalyticsResponse response = analyticsService.getAnalyticsData();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}

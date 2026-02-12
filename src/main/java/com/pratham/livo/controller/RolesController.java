package com.pratham.livo.controller;

import com.pratham.livo.dto.roles.ProcessRequestDto;
import com.pratham.livo.dto.roles.RoleRequestDto;
import com.pratham.livo.service.RolesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Slf4j
public class RolesController {

    private final RolesService rolesService;

    @PostMapping("/request/{role}")
    public ResponseEntity<Void> requestForRoleAddition(@PathVariable String role){
        log.info("Attempting to request for role addition");
        rolesService.requestForRoleAddition(role);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('LIVO_INTERNAL')")
    @GetMapping("/requests")
    public ResponseEntity<PagedModel<RoleRequestDto>> viewRoleAdditionPendingRequests(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ){
        log.info("Attempting to get all pending requests for role addition");
        return ResponseEntity.ok(rolesService.viewRoleAdditionPendingRequests(page,Math.min(size,100)));
    }

    @PreAuthorize("hasRole('LIVO_INTERNAL')")
    @PutMapping("request/{requestId}/process")
    public ResponseEntity<Void> processRoleAdditionPendingRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ProcessRequestDto processRequestDto
    ){
        log.info("Attempting to process request for role addition with id: {}",requestId);
        rolesService.processRoleAdditionPendingRequest(requestId,processRequestDto);
        return ResponseEntity.noContent().build();
    }
}
package com.pratham.livo.service;

import com.pratham.livo.dto.roles.ProcessRequestDto;
import com.pratham.livo.dto.roles.RoleRequestDto;
import org.springframework.data.web.PagedModel;

public interface RolesService {
    void requestForRoleAddition(String role);
    PagedModel<RoleRequestDto> viewRoleAdditionPendingRequests(Integer page, Integer size);
    void processRoleAdditionPendingRequest(Long requestId, ProcessRequestDto processRequestDto);
}

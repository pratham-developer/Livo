package com.pratham.livo.dto.roles;

import com.pratham.livo.enums.Role;
import com.pratham.livo.enums.RoleRequestStatus;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleRequestDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Role requestedRole;
    private RoleRequestStatus status;
}

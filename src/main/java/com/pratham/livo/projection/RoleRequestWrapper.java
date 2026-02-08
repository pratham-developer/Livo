package com.pratham.livo.projection;

import com.pratham.livo.enums.Role;
import com.pratham.livo.enums.RoleRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleRequestWrapper {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Role requestedRole;
    private RoleRequestStatus status;
}

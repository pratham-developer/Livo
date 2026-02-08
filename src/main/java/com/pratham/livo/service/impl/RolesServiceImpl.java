package com.pratham.livo.service.impl;

import com.pratham.livo.dto.auth.AuthenticatedUser;
import com.pratham.livo.dto.roles.ProcessRequestDto;
import com.pratham.livo.dto.roles.RoleRequestDto;
import com.pratham.livo.entity.RoleRequest;
import com.pratham.livo.entity.User;
import com.pratham.livo.enums.Role;
import com.pratham.livo.enums.RoleRequestStatus;
import com.pratham.livo.exception.BadRequestException;
import com.pratham.livo.exception.ResourceNotFoundException;
import com.pratham.livo.exception.SessionNotFoundException;
import com.pratham.livo.projection.RoleRequestWrapper;
import com.pratham.livo.repository.RoleRequestRepository;
import com.pratham.livo.repository.UserRepository;
import com.pratham.livo.security.SecurityHelper;
import com.pratham.livo.service.RolesService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolesServiceImpl implements RolesService {
    private final SecurityHelper securityHelper;
    private final RoleRequestRepository roleRequestRepository;
    private final EntityManager entityManager;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void requestForRoleAddition(String role) {
        log.info("Requesting for role addition");
        Role requestedRole = Role.from(role);

        //prevent requesting for internal role
        if(requestedRole.equals(Role.LIVO_INTERNAL)){
            throw new BadRequestException("Cannot request for this role");
        }

        //get the authenticated user
        AuthenticatedUser currentUser = currentUser();

        //check if the user already has this role
        if(currentUser.getRoles().contains(requestedRole)){
            throw new BadRequestException("user already has the requested role");
        }

        //check if there already exists a request for this user with this role
        RoleRequest existingRequest = roleRequestRepository.findByUserIdAndRole(currentUser.getId(),requestedRole)
                .orElse(null);
        //if exists
        if(existingRequest!=null){
            //if pending then throw exception
            if(existingRequest.getStatus().equals(RoleRequestStatus.PENDING)){
                throw new BadRequestException("request is already pending");
            }
            //if approved then throw exception and perform refresh
            else if(existingRequest.getStatus().equals(RoleRequestStatus.APPROVED)){
                throw new AccessDeniedException("request is already approved");
            }
            //if rejected, then relaunch request
            existingRequest.setStatus(RoleRequestStatus.PENDING);
            roleRequestRepository.save(existingRequest);
        }else{
            //build new request
            User dbUser = entityManager.getReference(User.class,currentUser.getId());
            RoleRequest newRequest = RoleRequest.builder()
                    .role(requestedRole).user(dbUser)
                    .status(RoleRequestStatus.PENDING)
                    .build();
            roleRequestRepository.save(newRequest);
        }
        log.info("Request successfully created for role addition");
    }

    @Override
    @Transactional(readOnly = true)
    public PagedModel<RoleRequestDto> viewRoleAdditionPendingRequests(Integer page, Integer size) {
        log.info("Retrieving all pending requests for role addition");
        Pageable pageable = PageRequest.of(page,size, Sort.by("user.name").ascending());
        Page<RoleRequestWrapper> requestPage = roleRequestRepository.findRequestsWithStatus(RoleRequestStatus.PENDING,pageable);
        Page<RoleRequestDto> requestDtoPage = requestPage.map(roleRequest -> modelMapper.map(roleRequest,RoleRequestDto.class));
        log.info("Successfully retrieved all pending requests for role addition");
        return new PagedModel<>(requestDtoPage);
    }

    @Override
    @Transactional
    public void processRoleAdditionPendingRequest(Long requestId, ProcessRequestDto processRequestDto) {
        //ensure approve boolean is present
        Boolean approve = processRequestDto.getApprove();
        if(approve==null){
            throw new BadRequestException("body cannot be null");
        }
        //fetch request by id
        RoleRequest roleRequest = roleRequestRepository.findById(requestId).orElseThrow(
                                ()->new ResourceNotFoundException("request does not exist")
                );

        //ensure request is pending
        if(roleRequest.getStatus()!=RoleRequestStatus.PENDING){
            throw new BadRequestException("request has been already processed");
        }

        //if approve = false then just mark status as rejected
        if(approve.equals(Boolean.FALSE)){
            roleRequest.setStatus(RoleRequestStatus.REJECTED);
        }
        else{
            //get the user
            User user = roleRequest.getUser();
            //add role if not present
            if(!user.getRoles().contains(roleRequest.getRole())){
                user.getRoles().add(roleRequest.getRole());
                userRepository.save(user);
            }
            //update request status
            roleRequest.setStatus(RoleRequestStatus.APPROVED);
        }
        roleRequestRepository.save(roleRequest);
        log.info("Processing request for role addition with id: {}",requestId);
        log.info("Successfully processed request for role addition with id: {}",requestId);
    }

    private AuthenticatedUser currentUser() {
        return securityHelper.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new SessionNotFoundException("Cannot identify the authenticated user"));
    }
}

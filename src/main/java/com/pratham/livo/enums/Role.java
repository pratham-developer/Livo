package com.pratham.livo.enums;

import com.pratham.livo.exception.BadRequestException;

public enum Role {
    GUEST,
    HOTEL_MANAGER,
    LIVO_INTERNAL;

    public static Role from(String value){
        try{
            return Role.valueOf(value.toUpperCase());
        }catch (IllegalArgumentException | NullPointerException e){
            throw new BadRequestException("invalid role");
        }
    }
}

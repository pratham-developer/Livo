package com.pratham.livo.exception;

public class InventoryBusyException extends RuntimeException{
    public InventoryBusyException(String message){
        super(message);
    }
}

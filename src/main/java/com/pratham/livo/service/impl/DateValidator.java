package com.pratham.livo.service.impl;

import com.pratham.livo.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class DateValidator{

    public long countDays(LocalDate startDate, LocalDate endDate){
        //date validation
        if (startDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Booking start date cannot be in the past");
        }

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > 30) {
            throw new BadRequestException("Cannot book for more than 30 days");
        }
        return days;
    }
}

package com.pratham.livo.projection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewWrapper {
    private Integer rating;
    private String text;
    private String reviewerName;
}

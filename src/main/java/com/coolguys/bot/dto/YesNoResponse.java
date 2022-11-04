package com.coolguys.bot.dto;

import lombok.Data;

@Data
public class YesNoResponse {

    private String answer;
    private Boolean forced;
    private String image;
}

package com.example.demo;

import lombok.Data;

@Data
public class SMSMessage {
    private String to;
    private String from;
    private String text;
}

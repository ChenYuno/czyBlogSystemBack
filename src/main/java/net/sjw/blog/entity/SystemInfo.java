package net.sjw.blog.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class SystemInfo {
    private Double cpu;
    private Double memory;
    private Map<String,Object> disks = new HashMap<>();
}

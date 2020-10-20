package com.xiawenxi.XWXSpring.demo.controller;

import com.xiawenxi.XWXSpring.demo.service.INameService;
import com.xiawenxi.XWXSpring.framework.annotation.XWXAutowired;
import com.xiawenxi.XWXSpring.framework.annotation.XWXController;
import com.xiawenxi.XWXSpring.framework.annotation.XWXRequestMapping;

@XWXController
public class NameController {
    @XWXAutowired
    private INameService nameService;

    @XWXRequestMapping("/getByCode")
    public String getByCode(String code) {
        return "My name is " + code;
    }

}

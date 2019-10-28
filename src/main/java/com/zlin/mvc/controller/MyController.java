package com.zlin.mvc.controller;

import com.zlin.mvc.annotation.Controller;
import com.zlin.mvc.annotation.RequestMapping;
import com.zlin.mvc.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/zlin")
public class MyController {

    @RequestMapping("/test")
    @ResponseBody
    public String test(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String name = request.getParameter("name");
        return "name:" + name;
    }
}

package com.zlin.mvc.controller;

import com.zlin.mvc.annotation.Controller;
import com.zlin.mvc.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/zlin")
public class MyController {

    @RequestMapping("/test")
    public void test(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String name = request.getParameter("name");
        response.getWriter().write("name:" + name);
    }
}

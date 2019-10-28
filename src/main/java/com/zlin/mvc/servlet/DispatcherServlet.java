package com.zlin.mvc.servlet;

import com.zlin.mvc.annotation.Controller;
import com.zlin.mvc.annotation.RequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

/**
 * 定义自己的前端控制器
 */
public class DispatcherServlet extends HttpServlet {
    private static final long seriaVersionUID = 1L;

    // 读取配置
    private Properties properties = new Properties();

    // 类的全路径名集合
    private List<String> classNames = new ArrayList<>();

    // ioc
    private Map<String, Object> ioc = new HashMap<>();

    // handlerMapping, url和对应具体方法的映射关系
    private Map<String, Method> handlerMapping = new HashMap<>();

    // controllerMap, url和对应controller的映射关系
    private Map<String, Object> controllerMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        // 2.初始化所有关联的类,扫描配置的包下面所有的类
        doScanner(properties.getProperty("scanPackage"));

        // 3.拿到扫描到的类,通过反射,实例化,并且放到IOC容器中(k-v beanName-bean) beanName默认首字母小写
        doInstance();

        // 4.初始化handlerMapping(url和method的映射, 将请求的url能对应上被执行的method)
        initHandlerMapping();

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // 处理请求
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("Server Exception!");
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        if(handlerMapping.isEmpty()){
            return;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();

        // 拼接url 并把多个/替换成一个
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 NOT FOUND!");
            return;
        }

        Method method = this.handlerMapping.get(url);

        // 获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        // 获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();

        // 保存参数值
        Object[] paramValues = new Object[parameterTypes.length];

        // 方法的参数列表
        for (int i = 0; i < parameterTypes.length; i++) {
            // 根据参数名称, 做某些处理
            String requestParam = parameterTypes[i].getSimpleName();
            if (requestParam.equals("HttpServletRequest")) {
                // 参数类型已经明确 这里强转类型
                paramValues[i] = req;
                continue;
            }
            if (requestParam.equals("HttpServletResponse")) {
                paramValues[i] = resp;
                continue;
            }
            if (requestParam.equals("String")) {
                for (Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",", "");
                    paramValues[i] = value;
                }
            }
        }
        // 利用反射机制来调用
        try {
            method.invoke(this.controllerMap.get(url), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doLoadConfig(String location){
        if (location.startsWith("classpath:")) {
            location = location.replace("classpath:", "");
        } else if (location.contains("/")) {
            int lastSplitIndex = location.lastIndexOf('/');
            location = location.substring(lastSplitIndex+1, location.length());
        }

        // 把web.xml中的contextConfigLocation对应的value值的文件加载到流里面
        InputStream resourcesAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            // 用Properties文件加载文件里的内容
            properties.load(resourcesAsStream);
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            // 关闭流
            if (resourcesAsStream != null) {
                try {
                    resourcesAsStream.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String packageName){
        // 把所有的.替换成/   com.zlin.mvc => com/zlin/mvc
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                // 递归扫描包
                doScanner(packageName + "." + file.getName());
            } else {
                String className = packageName + "." +file.getName().replace(".class", "");
                classNames.add(className);
                System.out.println("Spring容器扫描到的类有:" + packageName + "." +file.getName());
            }
        }
    }

    private void doInstance(){
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);

                // 通过反射来实例化, 只有加@Controller需要实例化
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Controller controller = clazz.getAnnotation(Controller.class);
                    String key = controller.value();
                    if (!"".equals(key) && key != null) {
                        ioc.put(key, clazz.newInstance());
                    } else {
                        // 只拿字节码上含有Controller.class 对象的信息
                        ioc.put(toLowerFirstWord(clazz.getSimpleName()), clazz.newInstance());
                    }
                } else {
                    continue;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
    * 建立映射关系
    */
    private void initHandlerMapping(){
        if (ioc.isEmpty()) {
            return;
        }
        try {
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                Class<? extends Object> clazz = entry.getValue().getClass();
                if (!clazz.isAnnotationPresent(Controller.class)) {
                    continue;
                }

                // 拼接url, Controller头的url + 方法上面的url
                String baseUrl = "";
                if (clazz.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                    baseUrl = requestMapping.value();
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (!method.isAnnotationPresent(RequestMapping.class)) {
                        continue;
                    }
                    RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                    String methodUrl = requestMapping.value();

                    String url = (baseUrl + "/" + methodUrl).replaceAll("/+", "/");
                    // 这里存放实例和method
                    handlerMapping.put(url, method);

                    Object tmpValue = null;
                    String ctlName = toLowerFirstWord(clazz.getSimpleName());
                    if (ioc.containsKey(ctlName)) {
                        tmpValue = ioc.get(ctlName);
                    } else {
                        tmpValue = clazz.newInstance();
                    }
                    controllerMap.put(url, tmpValue);
                    // controllerMap.put(url, clazz.newInstance()); 不这样,因为这样可能会重复创建Controller,ioc容器可能已经包含了Controller实例

                    System.out.println(url + "," +method);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toLowerFirstWord(String string){
        if (string == null || string.length() == 0) {
            return string;
        } else {
            String upperFirstWord = string.substring(0, 1);
            String lowerFirstWord = upperFirstWord.toLowerCase();
            string = string.replaceFirst(upperFirstWord, lowerFirstWord);
            return string;
        }
    }
}

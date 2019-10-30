# MySpringMvc
 手写自己的spring mvc框架


SpringMVC的底层加载流程
加载：

通过web.xml加载DispatcherServlet
加载Spring的xml配置文件（init-param来指定哪个xml文件作为SpringMVC的参数启动）
配置Servlet的URL地址，拦截哪些地址（URL-Pattern）
初始化：

SpringMVC的核心流程：初始化9大组件（init）
扫描指定包下的类，扫描指定注解的类（scanpackage=com.xxx.xxx）
实例化，把扫描到的类实例化后放在上下文Context中（IOC的功能）
mapping操作（HandlerMapping），处理请求的url地址和对应类的映射关系
运行：

DispatcherServlet前端控制器拦截请求，调用doDispatche，从HandlerMapping（map）中获取请求url的method
得到method和处理类的全路径后，通过反射来调用执行（invoke）
response.print()/response.getWriter() 响应输出，可以设置Http头信息，将响应结果输出为什么类型（JSON/XML/HTML/PDF）
设计自己的SpringMVC框架
读取配置：通过web.xml加载自己写的MyDispatcherServlet和读取配置文件。
初始化，这里不需要把9大组件全部实现，只要实现HandlerMapping就行。
（1）加载配置文件
（2）扫描配置的包下的类
（3）通过反射机制实例化包下的类，并放到ioc容器中（beanName-bean）beanName默认首字母小写；
（4）实例化HandlerMapping
运行
（1）获取请求传入的参数并处理参数
（2）通过初始化的HandlerMapping中拿出url对应的方法名，反射调用

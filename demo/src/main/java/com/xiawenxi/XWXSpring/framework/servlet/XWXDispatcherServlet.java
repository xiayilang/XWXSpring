package com.xiawenxi.XWXSpring.framework.servlet;

import com.xiawenxi.XWXSpring.framework.annotation.XWXController;
import com.xiawenxi.XWXSpring.framework.annotation.XWXService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;


public class XWXDispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();
    private List<String> classNames = new ArrayList();
    private Map<String, Object> ioc = new HashMap();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("doPost(HttpServletRequest req, HttpServletResponse resp)");
        super.doPost(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        // 加载配置文件
        doLoadConfig(config.getInitParameter("applicationContext"));

        // 扫描文件
        doScanner(contextConfig.getProperty("scanPackage"));

        // 实例化对象
        doInstance();

        // 自动装配
        doAutowire();

        // web映射关系
        doHandlerMapping();

        super.init(config);
    }

//    public static void main(String[] args) {
//        System.out.println((char)65);
//    }


    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = (scanPackage + "." + file.getName().replace(".class", ""));
                classNames.add(className);
            }
        }

    }

    private void doHandlerMapping() {
    }

    /**
     * 实例化实例
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isInterface()) {
                    continue;
                }
                if (clazz.isAnnotationPresent(XWXController.class)) {
                    ioc.put(toLowerFirstCase(clazz.getSimpleName()), clazz.newInstance());
                } else if (clazz.isAnnotationPresent(XWXService.class)) {
                    String beanName = clazz.getAnnotation(XWXService.class).value();
                    if (beanName == null || "".equals(beanName)) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    for (Class<?> interfaceClass : clazz.getInterfaces()) {
                        String instanceName = toLowerFirstCase(interfaceClass.getSimpleName());
                        if (!ioc.containsKey(instanceName)) {
                            ioc.put(instanceName, instance);
                        }
                    }
                    System.out.println(clazz.getName());
                } else {
                    continue;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("aa", "bbb");
        map.put("aa", "ccc");
        System.out.println(map.get("aa"));
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 注入
     */
    private void doAutowire() {
        if (ioc.isEmpty()) {
            return;
        }


    }

    /**
     * 加载配置资源
     *
     * @param applicationContext
     */
    private void doLoadConfig(String applicationContext) {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(applicationContext)) {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

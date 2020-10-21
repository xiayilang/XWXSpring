package com.xiawenxi.XWXSpring.framework.servlet;

import com.xiawenxi.XWXSpring.framework.annotation.XWXAutowired;
import com.xiawenxi.XWXSpring.framework.annotation.XWXController;
import com.xiawenxi.XWXSpring.framework.annotation.XWXRequestMapping;
import com.xiawenxi.XWXSpring.framework.annotation.XWXService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;


public class XWXDispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();
    private List<String> classNames = new ArrayList();
    private Map<String, Object> ioc = new HashMap();
    private Map<String, Method> handlerMapping = new HashMap<>();
    private transient List<Class<?>> classList = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("doPost(HttpServletRequest req, HttpServletResponse resp)");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Content-Type", "application/json; charset=UTF-8");
//        System.out.println(req.getContextPath());
        System.out.println(req.getRequestURI());
        Map<String, String[]> parametrMap =  req.getParameterMap();
        for (Map.Entry<String,String[]> entry:parametrMap.entrySet()){
            System.out.println(entry.getKey() + " --> " + Arrays.asList(entry.getValue()));
        }
//        req.getReader().
        resp.getWriter().println("夏文熙");
//        super.doPost(req, resp);
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
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> team : ioc.entrySet()) {
            Class<?> calzz = team.getValue().getClass();
            if (!calzz.isAnnotationPresent(XWXController.class)) {
                continue;
            }
            String basePath = "";
            if (calzz.isAnnotationPresent(XWXRequestMapping.class)) {
                XWXRequestMapping mapping = calzz.getAnnotation(XWXRequestMapping.class);
                basePath += mapping.value().trim();
            }
            Method[] methods = calzz.getDeclaredMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(XWXRequestMapping.class)) {
                    continue;
                }
                XWXRequestMapping mapping = method.getAnnotation(XWXRequestMapping.class);
                String path = basePath + mapping.value();
                path = path.replaceAll("/+", "/").replaceAll("\\s+", "");
                handlerMapping.put(path, method);
            }
        }

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
//        map.put("aa", "bbb");
//        map.put("aa", "ccc");
        System.out.println(map.getClass());
        System.out.println(map.getClass());
        System.out.println(map.getClass());
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
        for (Map.Entry<String, Object> temp : ioc.entrySet()) {
            Class<?> clazz = temp.getValue().getClass();
            if (classList.contains(clazz)) {
                continue;
            }

            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(XWXAutowired.class)) {
                    continue;
                }
                XWXAutowired autowired = field.getAnnotation(XWXAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getName();
                }
                field.setAccessible(true);
                Object bean = ioc.get(beanName);
                try {
                    if (bean != null) {
                        field.set(temp.getValue(), bean);
                        field.setAccessible(false);
                        continue;
                    }
                    bean = ioc.get(toLowerFirstCase(field.getType().getSimpleName()));
                    field.set(temp.getValue(), bean);
                    field.setAccessible(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        classList.clear();

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

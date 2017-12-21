package com.alibaba.dubbo.container.javaconfig;

import com.alibaba.dubbo.container.javaconfig.demo.Course;
import com.alibaba.dubbo.container.javaconfig.demo.resource.AppContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by Lenovo on 2017/12/21.
 */
public class Test {

    static AnnotationConfigApplicationContext context;//该方法是静态的,一旦启动后就可以用该方法了

    public static final String DEFAULT_SPRING_JAVACONFIG = "com.alibaba.dubbo.container.javaconfig.demo.resource";

    public void test1(){
        String configPath = DEFAULT_SPRING_JAVACONFIG;
        //context = new AnnotationConfigApplicationContext(AppContext.class);
        context = new AnnotationConfigApplicationContext(configPath);//可以存储包名,也可以是具体的class
        Course course = ((Course) context.getBean("course"));
        course.getModule().getAssignment().test();
        context.start();
    }
    public static void main(String[] args) throws InterruptedException {
        Test test = new Test();
        test.test1();

        Thread.sleep(5000);

        new Test2().test1();

        boolean running = true;
        synchronized (Test.class) {
            while (running) {
                try {
                    Test.class.wait();
                } catch (Throwable e) {
                }
            }
        }
    }

}

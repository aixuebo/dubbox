package com.alibaba.dubbo.container.log4j;

import com.alibaba.dubbo.common.logger.*;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import org.apache.log4j.*;
import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.Properties;

public class Test {

    com.alibaba.dubbo.common.logger.Logger log = LoggerFactory.getLogger(Test.class);

    public static final String LOG4J_FILE = "d://log/ccc";

    public static final String LOG4J_LEVEL = "info";

    public static final String LOG4J_SUBDIRECTORY = "test";

    public static final String DEFAULT_LOG4J_LEVEL = "ERROR";

    public void start() {
        String file = LOG4J_FILE;
        if (file != null && file.length() > 0) {
            String level = LOG4J_LEVEL;
            if (level == null || level.length() == 0) {
                level = DEFAULT_LOG4J_LEVEL;
            }
            Properties properties = new Properties();
            properties.setProperty("log4j.rootLogger", level + ",application");//log4j.rootLogger=info,application
            properties.setProperty("log4j.appender.application", "org.apache.log4j.DailyRollingFileAppender");
            properties.setProperty("log4j.appender.application.File", file);
            properties.setProperty("log4j.appender.application.Append", "true");
            properties.setProperty("log4j.appender.application.DatePattern", "'.'yyyy-MM-dd");
            properties.setProperty("log4j.appender.application.layout", "org.apache.log4j.PatternLayout");
            properties.setProperty("log4j.appender.application.layout.ConversionPattern", "%d [%t] %-5p %C{6} (%F:%L) - %m%n");
            PropertyConfigurator.configure(properties);
        }
        String subdirectory = LOG4J_SUBDIRECTORY;
        if (subdirectory != null && subdirectory.length() > 0) {
            Enumeration<Logger> ls = LogManager.getCurrentLoggers();
            while (ls.hasMoreElements()) {
                org.apache.log4j.Logger l = ls.nextElement();
                if (l != null) {
                    Enumeration<Appender> as = l.getAllAppenders();
                    while (as.hasMoreElements()) {
                        Appender a = as.nextElement();
                        if (a instanceof FileAppender) {
                            FileAppender fa = (FileAppender)a;
                            String f = fa.getFile();
                            if (f != null && f.length() > 0) {
                                int i = f.replace('\\', '/').lastIndexOf('/');
                                String path;
                                if (i == -1) {
                                    path = subdirectory;
                                } else {
                                    path = f.substring(0, i);
                                    if (! path.endsWith(subdirectory)) {
                                        path = path + "/" + subdirectory;
                                    }
                                    f = f.substring(i + 1);
                                }
                                fa.setFile(path + "/" + f);
                                fa.activateOptions();
                            }
                        }
                    }
                }
            }
        }
    }

    public void test1(){
        log.info("aaa");
        log.debug("bbb");
        log.error("ccc");
    }

    public static void main(String[] args) {
        String logger = System.setProperty("dubbo.application.logger", "log4j");
        Test test = new Test();
        test.start();
        test.test1();
    }
}

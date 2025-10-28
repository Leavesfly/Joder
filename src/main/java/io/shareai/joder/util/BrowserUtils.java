package io.shareai.joder.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 浏览器工具类
 * 对应 Kode 的 browser.ts
 */
public class BrowserUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(BrowserUtils.class);
    
    /**
     * 在默认浏览器中打开 URL
     * 
     * @param url 要打开的 URL
     * @return 是否成功打开
     */
    public static boolean openBrowser(String url) {
        if (url == null || url.trim().isEmpty()) {
            logger.warn("URL 为空，无法打开浏览器");
            return false;
        }
        
        String os = System.getProperty("os.name").toLowerCase();
        String command;
        
        if (os.contains("win")) {
            command = "cmd /c start " + url;
        } else if (os.contains("mac")) {
            command = "open " + url;
        } else {
            // Linux 或其他 Unix 系统
            command = "xdg-open " + url;
        }
        
        try {
            Runtime.getRuntime().exec(command);
            logger.info("已在浏览器中打开: {}", url);
            return true;
        } catch (IOException e) {
            logger.error("打开浏览器失败: {}", url, e);
            return false;
        }
    }
    
    /**
     * 使用 Desktop API 打开 URL（更可靠的方法）
     * 
     * @param url 要打开的 URL
     * @return 是否成功打开
     */
    public static boolean openBrowserWithDesktop(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(url));
                    logger.info("已在浏览器中打开: {}", url);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("使用 Desktop API 打开浏览器失败", e);
        }
        
        // 回退到命令行方式
        return openBrowser(url);
    }
}

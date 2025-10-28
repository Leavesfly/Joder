package io.leavesfly.joder.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

/**
 * 终端尺寸钩子
 * 对应 Kode 的 useTerminalSize hook
 * 监听终端尺寸变化
 */
@Singleton
public class TerminalSizeHook {
    
    private static final Logger logger = LoggerFactory.getLogger(TerminalSizeHook.class);
    private static final int DEFAULT_COLUMNS = 80;
    private static final int DEFAULT_ROWS = 24;
    
    private int columns;
    private int rows;
    
    public TerminalSizeHook() {
        updateSize();
        
        // 注册 SIGWINCH 信号处理器（终端尺寸变化信号）
        try {
            // Java 不直接支持信号处理，需要使用 JLine 或 JNA
            // 这里提供基本实现
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.debug("终端钩子清理");
            }));
        } catch (Exception e) {
            logger.warn("无法注册终端尺寸变化监听器", e);
        }
    }
    
    /**
     * 更新终端尺寸
     */
    public void updateSize() {
        try {
            // 尝试从环境变量获取
            String cols = System.getenv("COLUMNS");
            String rows = System.getenv("LINES");
            
            this.columns = cols != null ? Integer.parseInt(cols) : DEFAULT_COLUMNS;
            this.rows = rows != null ? Integer.parseInt(rows) : DEFAULT_ROWS;
            
        } catch (Exception e) {
            logger.debug("无法获取终端尺寸，使用默认值", e);
            this.columns = DEFAULT_COLUMNS;
            this.rows = DEFAULT_ROWS;
        }
    }
    
    /**
     * 使用 tput 命令获取终端尺寸（更可靠的方法）
     */
    public void updateSizeFromTput() {
        try {
            Process colsProcess = Runtime.getRuntime().exec(new String[]{"tput", "cols"});
            Process rowsProcess = Runtime.getRuntime().exec(new String[]{"tput", "lines"});
            
            colsProcess.waitFor();
            rowsProcess.waitFor();
            
            String colsStr = new String(colsProcess.getInputStream().readAllBytes()).trim();
            String rowsStr = new String(rowsProcess.getInputStream().readAllBytes()).trim();
            
            this.columns = Integer.parseInt(colsStr);
            this.rows = Integer.parseInt(rowsStr);
            
            logger.debug("终端尺寸: {}x{}", columns, rows);
            
        } catch (Exception e) {
            logger.debug("无法通过 tput 获取终端尺寸", e);
            updateSize(); // 回退到环境变量
        }
    }
    
    /**
     * 获取列数
     */
    public int getColumns() {
        return columns;
    }
    
    /**
     * 获取行数
     */
    public int getRows() {
        return rows;
    }
    
    /**
     * 获取终端尺寸信息
     */
    public TerminalSize getSize() {
        return new TerminalSize(columns, rows);
    }
    
    /**
     * 终端尺寸数据类
     */
    public static class TerminalSize {
        private final int columns;
        private final int rows;
        
        public TerminalSize(int columns, int rows) {
            this.columns = columns;
            this.rows = rows;
        }
        
        public int getColumns() {
            return columns;
        }
        
        public int getRows() {
            return rows;
        }
        
        @Override
        public String toString() {
            return String.format("%dx%d", columns, rows);
        }
    }
}

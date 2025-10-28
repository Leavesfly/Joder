package io.shareai.joder.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

/**
 * 清理工具类
 * 对应 Kode 的 cleanup.ts
 */
public class CleanupUtils {

    private static final Logger logger = LoggerFactory.getLogger(CleanupUtils.class);
    private static final long THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000;
    private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss-SSS'Z'");

    /**
     * 清理结果
     */
    public static class CleanupResult {
        private int messages = 0;
        private int errors = 0;

        public int getMessages() { return messages; }
        public int getErrors() { return errors; }
        
        public void incrementMessages() { messages++; }
        public void incrementErrors() { errors++; }

        @Override
        public String toString() {
            return String.format("CleanupResult{messages=%d, errors=%d}", messages, errors);
        }
    }

    /**
     * 将文件名转换为日期
     */
    public static Date convertFileNameToDate(String filename) throws ParseException {
        // 文件名格式：2024-01-01T12-00-00-000Z.log
        String isoStr = filename.split("\\.")[0];
        return ISO_FORMAT.parse(isoStr);
    }

    /**
     * 清理旧的消息文件
     */
    public static CleanupResult cleanupOldMessageFiles(String messagesPath, String errorsPath) {
        CleanupResult result = new CleanupResult();
        Date thirtyDaysAgo = new Date(System.currentTimeMillis() - THIRTY_DAYS_MS);

        // 清理消息目录
        if (messagesPath != null) {
            cleanupDirectory(Paths.get(messagesPath), thirtyDaysAgo, result, true);
        }

        // 清理错误目录
        if (errorsPath != null) {
            cleanupDirectory(Paths.get(errorsPath), thirtyDaysAgo, result, false);
        }

        return result;
    }

    /**
     * 清理指定目录
     */
    private static void cleanupDirectory(Path directory, Date cutoffDate, 
                                        CleanupResult result, boolean isMessageDir) {
        try {
            if (!Files.exists(directory)) {
                return;
            }

            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        String filename = file.getFileName().toString();
                        Date fileDate = convertFileNameToDate(filename);

                        if (fileDate.before(cutoffDate)) {
                            Files.delete(file);
                            if (isMessageDir) {
                                result.incrementMessages();
                            } else {
                                result.incrementErrors();
                            }
                            logger.debug("已删除旧文件: {}", file);
                        }
                    } catch (ParseException e) {
                        logger.warn("无法解析文件名日期: {}", file.getFileName());
                    } catch (IOException e) {
                        logger.error("删除文件失败: {}", file, e);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    logger.warn("访问文件失败: {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            logger.error("清理目录失败: {}", directory, e);
        }
    }

    /**
     * 在后台清理旧文件
     */
    public static void cleanupOldMessageFilesInBackground(String messagesPath, String errorsPath) {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "cleanup-thread");
            thread.setDaemon(true);  // 设置为守护线程
            return thread;
        });

        executor.submit(() -> {
            try {
                CleanupResult result = cleanupOldMessageFiles(messagesPath, errorsPath);
                logger.info("后台清理完成: {}", result);
            } catch (Exception e) {
                logger.error("后台清理失败", e);
            }
        });

        executor.shutdown();
    }

    /**
     * 清理指定天数之前的文件
     */
    public static int cleanupFilesByAge(Path directory, int daysOld) throws IOException {
        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);
        int deletedCount = 0;

        if (!Files.exists(directory)) {
            return 0;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    long fileTime = Files.getLastModifiedTime(file).toMillis();
                    if (fileTime < cutoffTime) {
                        Files.delete(file);
                        deletedCount++;
                    }
                }
            }
        }

        return deletedCount;
    }

    /**
     * 清理空目录
     */
    public static int cleanupEmptyDirectories(Path rootDirectory) throws IOException {
        int deletedCount = 0;

        if (!Files.exists(rootDirectory)) {
            return 0;
        }

        Files.walkFileTree(rootDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!dir.equals(rootDirectory)) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                        if (!stream.iterator().hasNext()) {
                            Files.delete(dir);
                            logger.debug("已删除空目录: {}", dir);
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return deletedCount;
    }

    /**
     * 清理指定大小以上的文件
     */
    public static int cleanupLargeFiles(Path directory, long maxSizeBytes) throws IOException {
        int deletedCount = 0;

        if (!Files.exists(directory)) {
            return 0;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    long fileSize = Files.size(file);
                    if (fileSize > maxSizeBytes) {
                        Files.delete(file);
                        deletedCount++;
                        logger.info("已删除大文件: {} ({}MB)", file, fileSize / (1024 * 1024));
                    }
                }
            }
        }

        return deletedCount;
    }

    /**
     * 获取目录大小
     */
    public static long getDirectorySize(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return 0;
        }

        final long[] size = {0};
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                size[0] += attrs.size();
                return FileVisitResult.CONTINUE;
            }
        });

        return size[0];
    }
}

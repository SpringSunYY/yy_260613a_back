package com.lz.framework.excel.core.util;

import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.metadata.data.ImageData;
import com.lz.framework.common.biz.infra.file.FileCommonApi;
import com.lz.framework.common.enums.FileConstants;
import com.lz.framework.common.util.spring.SpringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 文件处理工具类
 *
 * <p>提供统一的图片文件读取和转换功能，支持：
 * <ul>
 *     <li>云存储文件（通过 FileCommonApi）</li>
 *     <li>本地文件</li>
 * </ul>
 *
 * @author 荔枝源码
 */
@Slf4j
public class FileProcessUtils {

    /**
     * 当前单元格所有文件最大大小（MB）
     */
    public static final int MAX_FILE_SIZE = 10;

    /**
     * 当前单元格所有文件最大字节数
     */
    public static final long MAX_FILE_BYTES = MAX_FILE_SIZE * 1024 * 1024L;


    /**
     * 获取 FileCommonApi（延迟加载，避免循环依赖）。
     */
    private static FileCommonApi getFileCommonApi() {
        return SpringUtils.getBean(FileCommonApi.class);
    }

    /**
     * 创建 ImageData。
     *
     * @param bytes 图片字节数据
     * @return ImageData
     */
    public static ImageData createImageData(byte[] bytes) {
        ImageData imageData = new ImageData();
        imageData.setImage(bytes);
        imageData.setImageType(detectImageType(bytes));
        return imageData;
    }

    /**
     * 检测图片类型。
     *
     * @param imageData 图片字节数据
     * @return 图片类型
     */
    public static ImageData.ImageType detectImageType(byte[] imageData) {
        if (imageData == null || imageData.length < 4) {
            return ImageData.ImageType.PICTURE_TYPE_JPEG;
        }
        int b0 = imageData[0] & 0xFF;
        int b1 = imageData[1] & 0xFF;
        // PNG: 89 50 4E 47
        if (b0 == 0x89 && b1 == 0x50) {
            return ImageData.ImageType.PICTURE_TYPE_PNG;
        }
        // JPEG: FF D8
        if (b0 == 0xFF && b1 == 0xD8) {
            return ImageData.ImageType.PICTURE_TYPE_JPEG;
        }
        // BMP: 42 4D
        if (b0 == 0x42 && b1 == 0x4D) {
            return ImageData.ImageType.PICTURE_TYPE_DIB;
        }
        // 默认 JPEG
        return ImageData.ImageType.PICTURE_TYPE_JPEG;
    }

    /**
     * 流式处理 String 路径：复用 {@link FileCommonApi#getFileContents(String)} 一次性拿多张图，
     * 然后逐张回调，避免一次性在 Handler 内持有所有图的 byte[]。
     * <p><b>v4 优化</b>：
     * <ul>
     *   <li>getFileContents 多张拿（保留业务要求）</li>
     *   <li>Handler 回调里 addPicture → POI 复制 → 立即把 List 中对应位置置 null</li>
     *   <li>byte[] 生命周期缩短：到回调返回就被释放</li>
     * </ul>
     *
     * <p><b>回调契约</b>：
     * <ul>
     *   <li>回调接收 (index, byte[]) → index 从 0 开始；byte[] 是图片字节数据</li>
     *   <li>回调返回 true → 继续下一张；返回 false → 停止（异常中断）</li>
     * </ul>
     *
     * @param path     原始路径（可能含 {@link FileConstants#FILE_PATH_SEPARATOR} 分隔的多张图）
     * @param key      单元格标识（用于日志）
     * @param consumer 每张图的回调：(index, bytes) -> continue?
     * @return 处理结果：overLimit=true 表示总和超限；false 表示正常消费
     */
    public static StringPathResult processStringPathStream(String path, String key,
                                                            java.util.function.BiFunction<Integer, byte[], Boolean> consumer) {
        if (path == null || path.isEmpty()) {
            return StringPathResult.ofEmpty();
        }
        try {
            FileCommonApi fileCommonApi = getFileCommonApi();
            // 按业务要求一次性拿多张
            List<byte[]> contents = fileCommonApi.getFileContents(path);
            if (contents == null || contents.isEmpty()) {
                return StringPathResult.ofEmpty();
            }

            // 先算总大小（避免回调里多次累加；仍用 List<byte[]> 一次性遍历完即可）
            long totalSize = contents.stream()
                    .filter(Objects::nonNull)
                    .mapToLong(content -> content.length)
                    .sum();
            if (totalSize > MAX_FILE_BYTES) {
                log.warn("[FileProcessUtils] 单元格 [{}] 文件总大小 {} MB 超过限制 {} MB，不写入图片",
                        key, formatMB(totalSize), MAX_FILE_SIZE);
                // 超限：拼接完整路径作为 fallback
                List<String> filePaths = fileCommonApi.getFilePaths(path);
                String fallbackPath = (filePaths != null && !filePaths.isEmpty())
                        ? String.join(FileConstants.EXCEL_LINE_SEPARATOR, filePaths)
                        : path;
                return StringPathResult.ofOverLimit(fallbackPath);
            }

            // 流式消费：逐张回调，每张回调返回后立即把 List 中对应位置置 null
            int totalRead = 0;
            for (int i = 0; i < contents.size(); i++) {
                byte[] content = contents.get(i);
                // null 或空 → 跳过（continue 保持索引递增给回调）
                if (content == null || content.length == 0) {
                    continue;
                }
                // 回调：Handler 里 addPicture → POI 复制 → 回调返回后 byte[] 局部变量出作用域
                Boolean continueNext = consumer.apply(i, content);
                if (continueNext == null || !continueNext) {
                    // 回调要求停止
                    break;
                }
                totalRead++;
                // 立即清引用：让 List<byte[]> 不再持有此 byte[]，byte[] 立即可 GC
                contents.set(i, null);
            }
            if (totalRead == 0) {
                return StringPathResult.ofEmpty();
            }
            return new StringPathResult(false, null, null);
        } catch (Exception e) {
            log.error("[FileProcessUtils] 流式读取文件失败 [{}]: {}", path, e.getMessage(), e);
            return StringPathResult.ofEmpty();
        }
    }

    /**
     * 格式化文件大小。
     *
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    public static String formatMB(long bytes) {
        return String.format("%.2f", bytes / 1024.0 / 1024.0);
    }

    // ====================================================================================
    // Convert 阶段：byte[] / File / InputStream 处理方法
    // ====================================================================================

    /**
     * 读取本地文件为 byte[]。
     */
    public static byte[] readFileBytes(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        try {
            return FileUtil.readBytes(file);
        } catch (Exception e) {
            log.error("[FileProcessUtils] 读取文件失败 [{}]: {}", file.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    /**
     * 读取 InputStream 为 byte[]。
     */
    public static byte[] readInputStream(InputStream is) {
        if (is == null) {
            return null;
        }
        try {
            return is.readAllBytes();
        } catch (Exception e) {
            log.error("[FileProcessUtils] 读取 InputStream 失败: {}", e.getMessage());
            return null;
        } finally {
            try {
                is.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 从 byte[] 添加到 ImageData 列表。
     *
     * @return true 添加成功，false 失败
     */
    public static boolean addImageData(byte[] bytes, List<ImageData> imageDataList) {
        if (bytes == null || bytes.length == 0 || imageDataList == null) {
            return false;
        }
        imageDataList.add(createImageData(bytes));
        return true;
    }

    /**
     * 从 File 添加到 ImageData 列表。
     *
     * @return true 添加成功，false 失败
     */
    public static boolean addImageData(File file, List<ImageData> imageDataList) {
        if (file == null || !file.exists() || !file.isFile() || imageDataList == null) {
            return false;
        }
        byte[] bytes = readFileBytes(file);
        return addImageData(bytes, imageDataList);
    }

    /**
     * 从 InputStream 添加到 ImageData 列表。
     *
     * @return true 添加成功，false 失败
     */
    public static boolean addImageData(InputStream is, List<ImageData> imageDataList) {
        if (is == null || imageDataList == null) {
            return false;
        }
        byte[] bytes = readInputStream(is);
        return addImageData(bytes, imageDataList);
    }

    /**
     * 处理 String 路径，返回结果封装。
     */
    public static class StringPathResult {
        /**
         * 是否总大小超限
         */
        public final boolean overLimit;
        /**
         * 图片数据列表（未超限时填充）
         */
        public final List<ImageData> imageDataList;
        /**
         * 拼接后的路径字符串（超限时填充）
         */
        public final String fallbackPath;

        private StringPathResult(boolean overLimit, List<ImageData> imageDataList, String fallbackPath) {
            this.overLimit = overLimit;
            this.imageDataList = imageDataList;
            this.fallbackPath = fallbackPath;
        }

        public static StringPathResult ofImages(List<ImageData> list) {
            return new StringPathResult(false, list, null);
        }

        public static StringPathResult ofOverLimit(String fallbackPath) {
            return new StringPathResult(true, null, fallbackPath);
        }

        public static StringPathResult ofEmpty() {
            return new StringPathResult(false, new ArrayList<>(), null);
        }
    }

}

package com.lz.framework.excel.core.convert;

import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.converters.WriteConverterContext;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.data.ImageData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.lz.framework.common.biz.infra.file.FileCommonApi;
import com.lz.framework.common.util.spring.SpringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Excel 图片转换器
 *
 * <p>
 * 支持将文件图片写入 Excel 单元格对应行列中。
 *
 * <p>
 * 支持多种数据类型：
 * <ul>
 *     <li>byte[]</li>
 *     <li>List&lt;byte[]&gt;</li>
 *     <li>String 文件路径</li>
 *     <li>List&lt;String&gt; 文件路径列表</li>
 *     <li>File</li>
 *     <li>InputStream</li>
 * </ul>
 *
 * <p>
 * 防OOM策略：
 * <ul>
 *     <li>按照「当前单元格所有文件的总大小」进行限制</li>
 *     <li>总大小超过 {@link #MAX_FILE_SIZE} MB 时，不写入任何图片</li>
 *     <li>超过限制后，Excel 单元格直接写入原始文件路径</li>
 * </ul>
 *
 * @author 荔枝源码
 */
@Slf4j
public class ImagesConvert implements Converter<Object> {
    /**
     * 当前单元格所有文件最大大小（MB）
     * <p> 例如设置为 1：
     * <ul>
     *     <li>图片1：500KB</li>
     *     <li>图片2：400KB</li>
     *     <li>总计：900KB → 正常写入图片</li>
     *     <li>如果总计超过 1MB → 整个单元格不写图片，直接写路径</li>
     * </ul>
     */
    private static final int MAX_FILE_SIZE = 1;
    /**
     * 当前单元格所有文件最大字节数
     */
    private static final long MAX_FILE_BYTES = MAX_FILE_SIZE * 1024 * 1024L;
    /**
     * FileCommonApi 实例
     * 延迟加载，避免循环依赖。
     */
    private FileCommonApi fileCommonApi;

    @Override
    public Class<?> supportJavaTypeKey() {
        // 返回 null： 让 EasyExcel 通过 @ExcelProperty 显式指定本 Converter。
        return null;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        //图片最终通过 ImageData 写入， 这里使用 STRING 作为 Converter 的 Excel 类型。
        return CellDataTypeEnum.STRING;
    }

    /**
     * 将文件数据转换为 Excel 单元格数据。
     * <p>
     * 注意：
     * 最终 Excel 单元格写入什么内容，取决于这里返回的 WriteCellData。
     * <p> 不使用 context.setValue() 修改最终输出，
     * 超过大小限制时直接返回 STRING 类型的 WriteCellData。
     *
     * @param context EasyExcel 转换上下文 * @return Excel 单元格数据
     */
    @Override
    public WriteCellData<?> convertToExcelData(WriteConverterContext<Object> context) throws Exception {
        Object value = context.getValue();
        if (value == null) {
            return new WriteCellData<>(CellDataTypeEnum.STRING, "");
        }
        //当前单元格最终需要回填的路径。
        //注意：这是局部变量，不使用成员变量， 避免 Converter 被 EasyExcel 复用时出现数据串行问题。
        String fallbackPath = getOriginalPath(value);
        //当前单元格所有图片。
        List<ImageData> imageDataList = new ArrayList<>();
        try { // 处理 byte[]
            if (value instanceof byte[] bytes) {
                processBytes(bytes, imageDataList); // 处理 List<byte[]> / List<String> / List<File>
            } else if (value instanceof List<?> list) {
                // processList 返回真正应该回填的路径。
                // 如果没有超限，则返回原始路径。
                // 如果超限，则返回 getFilePaths()  获取到的完整路径。
                fallbackPath = processList(list, imageDataList, fallbackPath); //String
            } else if (value instanceof String str) {
                fallbackPath = processStringPath(str, imageDataList); //File
            } else if (value instanceof File file) {
                fallbackPath = file.getAbsolutePath();
                processFile(file, imageDataList); // 处理 File
            } else if (value instanceof InputStream is) {
                processInputStream(is, imageDataList);
            } else {
                log.warn("[ImagesConvert] 不支持的文件类型: {}，将输出原始值", value.getClass().getName());
                return new WriteCellData<>(CellDataTypeEnum.STRING, String.valueOf(value));
            }
        } catch (TotalSizeExceededException e) {
            //理论上 String / List 的超限会在对应方法内部处理，这里作为统一兜底
            log.warn("[ImagesConvert] 当前单元格文件总大小超过 {} MB，不写入图片", MAX_FILE_SIZE);
            return new WriteCellData<>(CellDataTypeEnum.STRING, fallbackPath);
        } catch (Exception e) {
            log.error("[ImagesConvert] 转换文件数据失败: {}", e.getMessage(), e);
        }
        //没有图片数据： 可能原因：
        // 1. 文件为空
        // 2. 文件不存在
        // 3. 文件读取失败
        // 4. 文件总大小超过限制 * * 此时直接写路径。
        if (imageDataList.isEmpty()) {
            return new WriteCellData<>(CellDataTypeEnum.STRING, fallbackPath);
        }
        //有图片：  创建 Excel 单元格图片数据。
        WriteCellData<List<ImageData>> cellData = new WriteCellData<>();
        cellData.setType(CellDataTypeEnum.EMPTY);
        cellData.setImageDataList(imageDataList);
        return cellData;
    }

    /**
     * 处理 List。
     * <p>  List 中可以混合：
     * <ul>
     *     <li>byte[]</li>
     *     <li>String</li>
     *     <li>File</li>
     *     <li>InputStream</li>
     * </ul> * * @param list 文件列表 * @param imageDataList 图片数据列表 * @param fallbackPath 默认回填路径 * @return 最终应该回填到 Excel 的路径
     */
    private String processList(List<?> list, List<ImageData> imageDataList, String fallbackPath) {
        if (list == null || list.isEmpty()) {
            return fallbackPath;
        }
        //当前 List 所有文件的总大小。
        long totalSize = 0; // 第一阶段：先处理 List 中的数据，并统计总大小。
        for (Object item : list) {
            if (item == null) {
                continue;
            }
            if (item instanceof byte[] bytes) {
                if (bytes.length == 0) {
                    continue;
                }
                totalSize += bytes.length;
                //已经超过限制： 直接返回路径，不再写图片。
                if (totalSize > MAX_FILE_BYTES) {
                    log.warn("[ImagesConvert] List 文件总大小 {} MB 超过限制 {} MB，不写入图片", formatMB(totalSize), MAX_FILE_SIZE);
                    return buildListFallbackPath(list, fallbackPath);
                }
                ImageData imageData = createImageData(bytes);
                imageDataList.add(imageData);
            } else if (item instanceof String path) {
                // String 路径交给 FileCommonApi 处理。
                String result = processStringPath(path, imageDataList);
                if (!result.equals(path)) {
                    return result;
                }
            } else if (item instanceof File file) {
                if (!file.exists() || !file.isFile()) {
                    continue;
                }
                long fileSize = file.length();
                totalSize += fileSize;
                if (totalSize > MAX_FILE_BYTES) {
                    log.warn("[ImagesConvert] List 文件总大小 {} MB 超过限制 {} MB，不写入图片", formatMB(totalSize), MAX_FILE_SIZE);
                    return buildListFallbackPath(list, fallbackPath);
                }
                try {
                    byte[] bytes = FileUtil.readBytes(file);
                    ImageData imageData = createImageData(bytes);
                    imageDataList.add(imageData);
                } catch (Exception e) {
                    log.error("[ImagesConvert] 读取文件失败 [{}]: {}", file.getAbsolutePath(), e.getMessage());
                }
            } else if (item instanceof InputStream is) {
                try {
                    byte[] bytes = is.readAllBytes();
                    totalSize += bytes.length;
                    if (totalSize > MAX_FILE_BYTES) {
                        log.warn("[ImagesConvert] List 文件总大小 {} MB 超过限制 {} MB，不写入图片", formatMB(totalSize), MAX_FILE_SIZE);
                        return buildListFallbackPath(list, fallbackPath);
                    }
                    ImageData imageData = createImageData(bytes);
                    imageDataList.add(imageData);
                } catch (Exception e) {
                    log.error("[ImagesConvert] 读取 InputStream 失败: {}", e.getMessage());
                } finally {
                    try {
                        is.close();
                    } catch (Exception ignored) {
                    }
                }
            } else {
                log.warn("[ImagesConvert] List 中遇到不支持的类型: {}", item.getClass().getName());
            }
        }
        return fallbackPath;
    }

    /**
     * 处理 String 文件路径。
     * <p> 一个 path 可能通过 FileCommonApi 对应多个文件。
     * <p> 如果所有文件总大小超过限制：
     * 直接返回完整文件路径。
     *
     * @param path          原始路径
     * @param imageDataList 图片数据列表
     * @return 最终 Excel 应该显示的路径
     */
    private String processStringPath(String path, List<ImageData> imageDataList) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        try {
            //获取文件内容。
            List<byte[]> contents = getFileCommonApi().getFileContents(path);
            if (contents != null && !contents.isEmpty()) {
                //计算所有文件总大小。
                long totalSize = contents.stream().filter(Objects::nonNull).mapToLong(content -> content.length).sum(); /* * 总大小超过限制。 */
                if (totalSize > MAX_FILE_BYTES) {
                    log.warn("[ImagesConvert] 文件总大小 {} MB 超过限制 {} MB，不写入图片", formatMB(totalSize), MAX_FILE_SIZE); /* * 获取完整文件路径。 */
                    List<String> filePaths = getFileCommonApi().getFilePaths(path);
                    if (filePaths != null && !filePaths.isEmpty()) {
                        // 拼接多个路径。
                        String fallbackPath = String.join("\n", filePaths);
                        log.info("[ImagesConvert] 文件超过限制，Excel 回填路径: {}", fallbackPath);
                        // 不再 context.setValue() * * 直接返回最终需要写入 Excel 的字符串。
                        return fallbackPath;
                    }
                    //获取完整路径失败， * 则使用原始 path。
                    return path;
                }
                //没有超过限制： * 正常写入图片。
                for (byte[] content : contents) {
                    if (content == null || content.length == 0) {
                        continue;
                    }
                    processBytes(content, imageDataList);
                }
                return path;
            }
            //FileCommonApi 没有读取到文件： * 尝试作为本地文件处理。
            File file = FileUtil.file(path);
            if (file.exists() && file.isFile()) {
                processFile(file, imageDataList);
                return path;
            }
            log.warn("[ImagesConvert] 无法读取文件路径: {}", path);
            return path;
        } catch (Exception e) {
            log.error("[ImagesConvert] 读取文件失败 [{}]: {}", path, e.getMessage(), e);
            return path;
        }
    }

    /**
     * 处理 byte[]。
     */
    private void processBytes(byte[] bytes, List<ImageData> imageDataList) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        ImageData imageData = createImageData(bytes);
        imageDataList.add(imageData);
    }

    /**
     * 创建 ImageData。
     */
    private ImageData createImageData(byte[] bytes) {
        ImageData imageData = new ImageData();
        imageData.setImage(bytes);
        // 设置图片类型，便于 POI 正确写入 Excel。
        imageData.setImageType(detectImageType(bytes));
        return imageData;
    }

    /**
     * 处理 File。
     */
    private void processFile(File file, List<ImageData> imageDataList) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        try {
            byte[] bytes = FileUtil.readBytes(file);
            processBytes(bytes, imageDataList);
        } catch (Exception e) {
            log.error("[ImagesConvert] 读取文件失败 [{}]: {}", file.getAbsolutePath(), e.getMessage(), e);
        }
    }

    /**
     * 处理 InputStream。
     */
    private void processInputStream(InputStream is, List<ImageData> imageDataList) {
        if (is == null) {
            return;
        }
        try {
            byte[] bytes = is.readAllBytes();
            processBytes(bytes, imageDataList);
        } catch (Exception e) {
            log.error("[ImagesConvert] 读取 InputStream 失败: {}", e.getMessage(), e);
        } finally {
            try {
                is.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 获取 List 超限时的回填路径。
     * <p> 对于 List<String>：  直接使用 String.join("||", ...)
     * <p>  对于 List<File>： 使用绝对路径。
     */
    private String buildListFallbackPath(List<?> list, String fallbackPath) {
        if (list == null || list.isEmpty()) {
            return fallbackPath;
        }
        List<String> paths = new ArrayList<>();
        for (Object item : list) {
            if (item == null) {
                continue;
            }
            if (item instanceof String path) { //如果 String 本身可能是多个路径， 这里直接保留。
                paths.add(path);
            } else if (item instanceof File file) {
                paths.add(file.getAbsolutePath());
            }
        }
        if (!paths.isEmpty()) {
            return String.join("||", paths);
        }
        return fallbackPath;
    }

    /**
     * 获取原始路径。 * * <p> * 当无法写入图片时作为 Excel 文本内容。
     */
    private String getOriginalPath(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String str) {
            return str;
        }
        if (value instanceof File file) {
            return file.getAbsolutePath();
        }
        if (value instanceof List<?> list) {
            return buildListFallbackPath(list, "");
        }
        return String.valueOf(value);
    }

    /**
     * 检测图片类型。
     */
    private ImageData.ImageType detectImageType(byte[] imageData) {
        if (imageData == null || imageData.length < 4) {
            return ImageData.ImageType.PICTURE_TYPE_JPEG;
        }
        int b0 = imageData[0] & 0xFF;
        int b1 = imageData[1] & 0xFF; //PNG  89 50 4E 47
        if (b0 == 0x89 && b1 == 0x50) {
            return ImageData.ImageType.PICTURE_TYPE_PNG;
        } //JPEG  FF D8
        if (b0 == 0xFF && b1 == 0xD8) {
            return ImageData.ImageType.PICTURE_TYPE_JPEG;
        } //BMP  42 4D
        if (b0 == 0x42 && b1 == 0x4D) {
            return ImageData.ImageType.PICTURE_TYPE_DIB;
        } //默认 JPEG。
        return ImageData.ImageType.PICTURE_TYPE_JPEG;
    }

    /**
     * 格式化文件大小。
     */
    private String formatMB(long bytes) {
        return String.format("%.2f", bytes / 1024.0 / 1024.0);
    }

    /**
     * 获取 FileCommonApi。 * * <p> * 延迟加载，避免循环依赖。
     */
    private FileCommonApi getFileCommonApi() {
        if (fileCommonApi == null) {
            fileCommonApi = SpringUtils.getBean(FileCommonApi.class);
        }
        return fileCommonApi;
    }

    /**
     * 文件总大小超过限制异常。
     */
    private static class TotalSizeExceededException extends RuntimeException {
    }
}

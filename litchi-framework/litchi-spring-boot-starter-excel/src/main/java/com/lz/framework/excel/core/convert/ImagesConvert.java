package com.lz.framework.excel.core.convert;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.converters.WriteConverterContext;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.data.ImageData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.lz.framework.common.enums.FileConstants;
import com.lz.framework.excel.core.util.FileProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
 *     <li>byte[]、File、InputStream 直接处理（已在内存或可流式读取）</li>
 *     <li>String / List&lt;String&gt; 路径交给 Handler 流式处理，避免一次性加载所有文件</li>
 * </ul>
 *
 * @author 荔枝源码
 */
@Slf4j
public class ImagesConvert implements Converter<Object> {

    @Override
    public Class<?> supportJavaTypeKey() {
        // 返回 null：让 EasyExcel 通过 @ExcelProperty 显式指定本 Converter。
        return null;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        // 图片最终通过 ImageData 写入，这里使用 STRING 作为 Converter 的 Excel 类型。
        return CellDataTypeEnum.STRING;
    }

    /**
     * 将文件数据转换为 Excel 单元格数据。
     *
     * @param context EasyExcel 转换上下文
     * @return Excel 单元格数据
     */
    @Override
    public WriteCellData<?> convertToExcelData(WriteConverterContext<Object> context) throws Exception {
        Object value = context.getValue();
        if (value == null) {
            return new WriteCellData<>(CellDataTypeEnum.STRING, "");
        }

        try {
            if (value instanceof byte[] bytes) {
                // 单个 byte[]：Convert 直接处理为图片
                return processBytes(bytes);
            } else if (value instanceof List<?> list) {
                // List 类型：根据 List 内容类型分支处理
                return processList(list);
            } else if (value instanceof String str) {
                // 单个 String 路径：Convert 不处理，原样返回，由 Handler 调用 FileProcessUtils 处理
                return new WriteCellData<>(CellDataTypeEnum.STRING, str);
            } else if (value instanceof File file) {
                // 单个 File：Convert 直接处理为图片
                return processFile(file);
            } else if (value instanceof InputStream is) {
                // 单个 InputStream：Convert 直接处理为图片
                return processInputStream(is);
            } else {
                log.warn("[ImagesConvert] 不支持的文件类型: {}，将输出原始值", value.getClass().getName());
                return new WriteCellData<>(CellDataTypeEnum.STRING, String.valueOf(value));
            }
        } catch (Exception e) {
            log.error("[ImagesConvert] 转换文件数据失败: {}", e.getMessage(), e);
            // 失败：返回空字符串，Handler 看到空就不处理
            return new WriteCellData<>(CellDataTypeEnum.STRING, "");
        }
    }

    /**
     * 处理 byte[] 类型。
     *
     * @param bytes 图片字节数据
     * @return Excel 单元格数据（图片或空字符串）
     */
    private WriteCellData<?> processBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new WriteCellData<>(CellDataTypeEnum.STRING, "");
        }
        List<ImageData> imageDataList = new ArrayList<>();
        FileProcessUtils.addImageData(bytes, imageDataList);
        return toWriteCellData(imageDataList);
    }

    /**
     * 处理 File 类型。
     *
     * @param file 本地文件
     * @return Excel 单元格数据（图片或空字符串）
     */
    private WriteCellData<?> processFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return new WriteCellData<>(CellDataTypeEnum.STRING, "");
        }
        List<ImageData> imageDataList = new ArrayList<>();
        FileProcessUtils.addImageData(file, imageDataList);
        return toWriteCellData(imageDataList);
    }

    /**
     * 处理 InputStream 类型。
     *
     * @param is 输入流
     * @return Excel 单元格数据（图片或空字符串）
     */
    private WriteCellData<?> processInputStream(InputStream is) {
        if (is == null) {
            return new WriteCellData<>(CellDataTypeEnum.STRING, "");
        }
        List<ImageData> imageDataList = new ArrayList<>();
        FileProcessUtils.addImageData(is, imageDataList);
        return toWriteCellData(imageDataList);
    }

    /**
     * 处理 List。
     * <p>List 中可能包含：
     * <ul>
     *     <li>byte[] / File / InputStream ─ Convert 直接处理</li>
     *     <li>String 路径 ─ 合并为 FILE_PATH_SEPARATOR（||）分隔的字符串，交给 Handler 处理</li>
     * </ul>
     *
     * @param list 文件列表
     * @return Excel 单元格数据
     */
    private WriteCellData<?> processList(List<?> list) {
        if (list == null || list.isEmpty()) {
            return new WriteCellData<>(CellDataTypeEnum.STRING, "");
        }

        // 收集可由 Convert 直接处理的图片数据
        List<ImageData> imageDataList = new ArrayList<>();
        // 收集 String 路径（合并后交给 Handler 处理）
        List<String> stringPaths = new ArrayList<>();

        for (Object item : list) {
            if (item == null) {
                continue;
            }
            if (item instanceof byte[] bytes) {
                FileProcessUtils.addImageData(bytes, imageDataList);
            } else if (item instanceof File file) {
                FileProcessUtils.addImageData(file, imageDataList);
            } else if (item instanceof InputStream is) {
                FileProcessUtils.addImageData(is, imageDataList);
            } else if (item instanceof String path) {
                stringPaths.add(path);
            } else {
                log.warn("[ImagesConvert] List 中遇到不支持的类型: {}", item.getClass().getName());
            }
        }

        // 有 Convert 直接处理的图片数据：优先返回（如果还有 String 路径也会一并带过去给 Handler）
        if (!imageDataList.isEmpty()) {
            WriteCellData<List<ImageData>> cellData = new WriteCellData<>();
            cellData.setType(CellDataTypeEnum.EMPTY);
            cellData.setImageDataList(imageDataList);
            return cellData;
        }

        // 纯 String 路径列表：用 FILE_PATH_SEPARATOR（||）合并，返回给 Handler 处理
        if (!stringPaths.isEmpty()) {
            String merged = String.join(FileConstants.FILE_PATH_SEPARATOR, stringPaths);
            return new WriteCellData<>(CellDataTypeEnum.STRING, merged);
        }

        // 空列表或全是无效数据：返回空字符串，Handler 跳过
        return new WriteCellData<>(CellDataTypeEnum.STRING, "");
    }

    /**
     * 将 imageDataList 转换为 WriteCellData。
     *
     * @param imageDataList 图片数据列表
     * @return 有图片数据时返回 EMPTY+ImageData 单元格；否则返回空字符串单元格
     */
    private WriteCellData<?> toWriteCellData(List<ImageData> imageDataList) {
        if (imageDataList == null || imageDataList.isEmpty()) {
            return new WriteCellData<>(CellDataTypeEnum.STRING, "");
        }
        WriteCellData<List<ImageData>> cellData = new WriteCellData<>();
        cellData.setType(CellDataTypeEnum.EMPTY);
        cellData.setImageDataList(imageDataList);
        return cellData;
    }
}

package com.lz.framework.excel.core.handler;

import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.data.ImageData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.WorkbookWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.alibaba.excel.write.handler.context.WorkbookWriteHandlerContext;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.Units;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * Excel 文件（图片）单元格写入处理器。
 *
 * <h3>功能</h3>
 * 让导出 Excel 中的图片按原始宽高比例横向并排显示，并自动调整行高，使图片
 * 在单元格内上下居中、水平方向按配置的边距排列。
 * <h3>注意：请不要把边距和图片间隔太多，否则多图的时候可能会收到影响</h3>
 * <h3>布局公式（每个 cell 内）</h3>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │              列宽 = @ColumnWidth（按 Excel 精确公式换算像素）    │
 * │  [水平 margin][图1][gap][图2]...[图n][水平 margin]             │
 * └─────────────────────────────────────────────────────────────┘
 *
 *  内容宽度   = 列宽 - 2 × 水平 margin = 列宽 - 左 margin - 右 margin
 *  水平 margin = IMAGE_HORIZONTAL_MARGIN_CHARS 字符（左右共用，居中场景左右留白相等）
 *  垂直 margin = IMAGE_VERTICAL_MARGIN_CHARS 字符（上下共用，居中场景上下留白相等）
 *  图间距     = IMAGE_GAP_CHARS 字符
 *  图渲染宽   = min(原图宽, 单图目标宽)     ← 不放大原图
 *  图渲染高   = 图渲染宽 × (原图高/原图宽)   ← 保持原图比例
 *  高度上限   = @ColumnWidth × MAX_IMAGE_RENDER_HEIGHT_MULTIPLE
 *              = 20 × 3 = 60 字符 ≈ 420 px（@ColumnWidth=20 的例子）
 *  超出上限   → 等比缩小渲染宽和渲染高
 *  行高       = 图渲染高 + 2 × 垂直 margin
 *  水平居中   = 水平 margin + (内容宽度 - 图组宽) / 2
 *  垂直居中   = (行高 - 图渲染高) / 2
 * </pre>
 *
 * <h3>常量说明</h3>
 * <ul>
 *   <li>{@code IMAGE_HORIZONTAL_MARGIN_CHARS}：图距离 cell 左右边界的留白（字符），左右共用。</li>
 *   <li>{@code IMAGE_VERTICAL_MARGIN_CHARS}：图距离 cell 上下边界的留白（字符），上下共用。</li>
 *   <li>{@code IMAGE_GAP_CHARS}：同一单元格内多张图之间的水平间距（字符）。</li>
 *   <li>{@code MAX_IMAGE_RENDER_HEIGHT_MULTIPLE}：图高度上限 = @ColumnWidth × 倍数。</li>
 * </ul>
 *
 * @author 荔枝源码
 */
@Slf4j
public class ImagesSheetWriteHandler implements CellWriteHandler, WorkbookWriteHandler {

    // ====================================================================================
    // 常量（可调）
    // ====================================================================================
    /**
     * 当用户没有设置 {@code @ColumnWidth} 时使用的基准列宽（字符数）。
     */
    private static final int FALLBACK_BASE_COLUMN_WIDTH_CHARS = 20;

    /**
     * 图距离 cell 左右边界的水平留白（单位：字符）。
     * <p>例如 1 字符 ≈ 7 px。{@code float} 支持小数（如 0.5 字符）。
     * <p>水平居中场景：左右留白相等。
     */
    private static final float IMAGE_HORIZONTAL_MARGIN_CHARS = 1f;

    /**
     * 图距离 cell 上下边界的垂直留白（单位：字符）。
     * <p>例如 1 字符 ≈ 7 px。{@code float} 支持小数。
     * <p>垂直居中场景：上下留白相等。
     */
    private static final float IMAGE_VERTICAL_MARGIN_CHARS = 1f;

    /**
     * 同一单元格内多张图之间的水平间距（单位：字符）。
     * 例如 1 字符 ≈ 7 px。
     */
    private static final float IMAGE_GAP_CHARS = 1f;

    /**
     * 单张图渲染高度上限——倍数，相对于该列的 @ColumnWidth。
     * <p>例如 @ColumnWidth(20) × 3 = 60 字符 ≈ 420 px。
     * <p>作用：防止细长图（如条形码）把行高撑到几百 px。
     */
    private static final double MAX_IMAGE_RENDER_HEIGHT_MULTIPLE = 3.0;

    /**
     * 默认行高（point）。
     * <p>Excel 默认行高 12.75 pt；这里用 13.5 pt 是给兜底场景一个安全余量（行内有图片时会被覆盖）。
     */
    private static final float DEFAULT_ROW_HEIGHT_POINTS = 13.5f;

    /**
     * 读取图片尺寸失败时的兜底尺寸（像素）。
     */
    private static final int DEFAULT_IMAGE_WIDTH_PIXELS = 200;
    private static final int DEFAULT_IMAGE_HEIGHT_PIXELS = 200;

    // ====================================================================================
    // 跨阶段缓存（一个 Handler 实例可能被多次写，用 sheetIndex 隔离）
    // ====================================================================================

    /**
     * sheetIdx → cellKey(row_col) → 图列表
     */
    private final Map<Integer, Map<String, List<ImageData>>> perSheetImageDataMap = new HashMap<>();

    /**
     * sheetIdx → cellKey → 每张图的原始 [w, h]
     */
    private final Map<Integer, Map<String, List<int[]>>> perSheetCellImageSizesListMap = new HashMap<>();

    /**
     * sheetIdx → colIdx → 该列基础列宽（POI 单位 = 字符数 × 256）
     */
    private final Map<Integer, Map<Integer, Integer>> perSheetOriginalColumnWidthMap = new HashMap<>();

    /**
     * 已处理过的 sheet 索引（幂等保护）
     */
    private final Set<Integer> processedSheetIndexes = new HashSet<>();

    // ====================================================================================
    // 第一步：afterCellDataConverted —— 收集图片数据（不画图）
    // ====================================================================================
    @Override
    public void afterCellDataConverted(CellWriteHandlerContext context) {
        if (context == null || Boolean.TRUE.equals(context.getHead())) return;
        WriteCellData<?> cellData = context.getFirstCellData();
        if (cellData == null) return;
        List<? extends ImageData> imageDataList = cellData.getImageDataList();
        if (imageDataList == null || imageDataList.isEmpty()) return;

        int rowIndex = context.getRowIndex();
        int columnIndex = context.getColumnIndex();
        int sheetIndex = currentSheetIndex(context.getWriteSheetHolder());

        try {
            // 取或创建该 sheet 的缓存
            Map<String, List<ImageData>> imageDataMap =
                    perSheetImageDataMap.computeIfAbsent(sheetIndex, k -> new HashMap<>());
            Map<String, List<int[]>> cellImageSizesListMap =
                    perSheetCellImageSizesListMap.computeIfAbsent(sheetIndex, k -> new HashMap<>());
            Map<Integer, Integer> originalColumnWidthPerColumnMap =
                    perSheetOriginalColumnWidthMap.computeIfAbsent(sheetIndex, k -> new HashMap<>());

            String key = rowIndex + "_" + columnIndex;

            // 拷贝 ImageData（保留 byte[] 不变）
            List<ImageData> dataCopy = new ArrayList<>(imageDataList);
            imageDataMap.put(key, dataCopy);

            // 读取每张图的原始尺寸（用于后续按比例缩放）
            List<int[]> sizes = new ArrayList<>(dataCopy.size());
            for (ImageData img : dataCopy) {
                sizes.add(readImageSizeFast(img));
            }
            cellImageSizesListMap.put(key, sizes);

            //  缓存该列基础列宽：从 VO 字段的 @ColumnWidth 注解读取
            // 不能用 sheet.getColumnWidth()，因为 LongestMatchColumnWidthStyleStrategy
            // 已经按图片宽度把列宽撑大了（60 字符），不是用户期望的 @ColumnWidth(20)。
            // 这里直接读 VO 字段的 @ColumnWidth 注解，确保拿到用户原始设置。
            if (!originalColumnWidthPerColumnMap.containsKey(columnIndex)) {
                int origWidth = readColumnWidth(context, columnIndex);
                if (origWidth <= 0) {
                    origWidth = FALLBACK_BASE_COLUMN_WIDTH_CHARS * 256;
                }
                originalColumnWidthPerColumnMap.put(columnIndex, origWidth);
            }

            //  告诉 EasyExcel 不要画图（避免和我们的绘制重复/冲突）
            cellData.setType(CellDataTypeEnum.EMPTY);
            cellData.setImageDataList(new ArrayList<>());
        } catch (Exception e) {
            log.error("[FileCellWriteHandler] 处理图片数据失败: {}", e.getMessage(), e);
        }
    }

    // ====================================================================================
    // 第二步：afterWorkbookDispose —— 一次性处理：列宽 + 行高 + 画图
    // ====================================================================================

    @Override
    public void afterWorkbookDispose(WorkbookWriteHandlerContext context) {
        if (context == null) return;
        WriteWorkbookHolder workbookHolder = context.getWriteWorkbookHolder();
        if (workbookHolder == null) return;
        Workbook workbook = workbookHolder.getWorkbook();
        if (workbook == null) return;

        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            if (sheet == null) continue;
            // 幂等：同一个 sheet 不重复处理
            if (!processedSheetIndexes.add(sheetIndex)) continue;
            processSheet(workbook, sheet, sheetIndex);
        }
    }

    /**
     * 处理单个 sheet：列宽 + 行高 + 画图。
     *
     * <p>顺序很关键：<b>先还原列宽，再算行高、画图</b>。
     * <ul>
     *   <li>行高按整行所有图片 cell 中渲染高最大者计算，每行只 setHeight 一次</li>
     * </ul>
     */
    private void processSheet(Workbook workbook, Sheet sheet, int sheetIndex) {
        Map<Integer, Integer> originalColumnWidthPerColumnMap = perSheetOriginalColumnWidthMap.get(sheetIndex);
        Map<String, List<ImageData>> imageDataMap = perSheetImageDataMap.get(sheetIndex);
        Map<String, List<int[]>> cellImageSizesListMap = perSheetCellImageSizesListMap.get(sheetIndex);

        if (imageDataMap == null || imageDataMap.isEmpty()) return;

        // ================================================================
        // 步骤 1：把图列列宽还原为 @ColumnWidth 原始值
        // ================================================================
        // LongestMatchColumnWidthStyleStrategy 会按 cell 内容（图片 base64 很长）把列宽撑大，
        // 这里用 @ColumnWidth 原始值覆盖回去，保证后续按用户设置的列宽布局。
        for (Map.Entry<Integer, Integer> cwEntry : originalColumnWidthPerColumnMap.entrySet()) {
            int columnIndex = cwEntry.getKey();
            int origWidthUnits = cwEntry.getValue();
            if (origWidthUnits > 0) {
                sheet.setColumnWidth(columnIndex, origWidthUnits);
            }
        }

        // ================================================================
        // 步骤 2：算每张图的「渲染宽/高」（阶段 A）
        // ================================================================
        // key = "row_col"，value = 该 cell 内每张图的渲染宽（像素）
        Map<String, float[]> cellRenderWidthsPxMap = new HashMap<>();
        // key = "row_col"，value = 该 cell 内最高的图渲染高（像素）
        Map<String, Float> cellMaxRenderHPxMap = new HashMap<>();
        // rowIdx → 该行所有 cell 中最高的图渲染高（字符）
        Map<Integer, Float> rowMaxRenderHCharsMap = new HashMap<>();

        // 把字符常量转像素（1 字符 ≈ 7 px）
        float horizontalMarginPx = IMAGE_HORIZONTAL_MARGIN_CHARS * Units.DEFAULT_CHARACTER_WIDTH;
        float verticalMarginPx = IMAGE_VERTICAL_MARGIN_CHARS * Units.DEFAULT_CHARACTER_WIDTH;
        float gapPx = IMAGE_GAP_CHARS * Units.DEFAULT_CHARACTER_WIDTH;

        for (Map.Entry<String, List<ImageData>> e : imageDataMap.entrySet()) {
            String key = e.getKey();
            int sepIdx = key.indexOf('_');
            int rowIndex = Integer.parseInt(key.substring(0, sepIdx));
            int columnIndex = Integer.parseInt(key.substring(sepIdx + 1));

            // 取该列的 @ColumnWidth（字符数）
            Integer base2 = originalColumnWidthPerColumnMap.get(columnIndex);
            int baseWidthUnits2 = (base2 != null) ? base2 : FALLBACK_BASE_COLUMN_WIDTH_CHARS * 256;
            int baseColWidthChars = baseWidthUnits2 / 256;
            float baseColWidthPx = baseWidthUnits2 / 256f * Units.DEFAULT_CHARACTER_WIDTH;

            //  高度上限 = @ColumnWidth 字符数 × 倍数 × 7 px
            // 例如 @ColumnWidth(20) × 3 × 7 = 420 px
            float maxRenderHPx = (float) (baseColWidthChars * MAX_IMAGE_RENDER_HEIGHT_MULTIPLE * Units.DEFAULT_CHARACTER_WIDTH);

            List<ImageData> list = e.getValue();
            List<int[]> sizes = (cellImageSizesListMap != null) ? cellImageSizesListMap.get(key) : null;
            int n = list.size();

            //  单图目标宽 = (内容宽度 - 图间距×(n-1)) / n
            //   内容宽度 = 列宽 - 左留白 - 右留白
            //             = 列宽 - 2 × 水平 margin（水平 margin 是左右各一个，共减 2 个）
            float baseContentAreaPx = baseColWidthPx - 2f * horizontalMarginPx;
            float singleSlotTargetPx = (baseContentAreaPx - (n - 1) * gapPx) / Math.max(n, 1);
            if (singleSlotTargetPx < 1f) singleSlotTargetPx = 1f;

            float[] renderWidthsPx = new float[n];
            float cellMaxRenderHPx = 0f;

            for (int i = 0; i < n; i++) {
                // 取原图尺寸；拿不到就用默认 200×200
                int origW = DEFAULT_IMAGE_WIDTH_PIXELS;
                int origH = DEFAULT_IMAGE_HEIGHT_PIXELS;
                if (sizes != null && i < sizes.size()) {
                    origW = sizes.get(i)[0];
                    origH = sizes.get(i)[1];
                }

                //  渲染宽 = min(原图宽, 单图目标宽) ← 不放大
                float renderW = Math.clamp((float) origW, 1f, singleSlotTargetPx);
                //  原图宽高比（保持比例）
                float ratio = (origW > 0) ? ((float) origH / origW) : 1f;
                //  渲染高 = 渲染宽 × 比例
                float renderH = renderW * ratio;

                //  高度上限保护：超出上限就等比缩
                // 例如：原图 100×500，renderH=500，但上限 420
                // scale = 420/500 = 0.84，renderH=420，renderW = 100×0.84 = 84
                if (renderH > maxRenderHPx) {
                    float scale = maxRenderHPx / renderH;
                    renderH = maxRenderHPx;
                    renderW = renderW * scale;
                }

                renderWidthsPx[i] = renderW;
                if (renderH > cellMaxRenderHPx) {
                    cellMaxRenderHPx = renderH;
                }
            }

            cellRenderWidthsPxMap.put(key, renderWidthsPx);
            cellMaxRenderHPxMap.put(key, cellMaxRenderHPx);

            // 行级聚合：该行所有 cell 中取最大的图渲染高
            float cellMaxHChars = cellMaxRenderHPx / Units.DEFAULT_CHARACTER_WIDTH;
            Float rowPrev = rowMaxRenderHCharsMap.get(rowIndex);
            if (rowPrev == null || cellMaxHChars > rowPrev) {
                rowMaxRenderHCharsMap.put(rowIndex, cellMaxHChars);
            }
        }

        // ================================================================
        // 步骤 3：写行高 = 该行最大图渲染高 + 上下边距
        // ================================================================
        Map<Integer, Integer> rowHeightTwipsMap = new HashMap<>();
        for (Map.Entry<Integer, Float> re : rowMaxRenderHCharsMap.entrySet()) {
            int rowIndex = re.getKey();
            float rowMaxHChars = re.getValue();
            float rowMaxHPx = rowMaxHChars * Units.DEFAULT_CHARACTER_WIDTH;
            // 行高 = 图渲染高 + 上留白 + 下留白 = 图渲染高 + 2 × 垂直 margin（上+下 共用）
            // twips = 1/20 point，POI 行高用 twips；1 px = 15 twips（96 DPI）
            int newRowHeightTwips = (int) Math.round(
                    (rowMaxHPx + 2f * verticalMarginPx) * 15f);
            if (newRowHeightTwips < 15) newRowHeightTwips = 15;
            Row row2 = sheet.getRow(rowIndex);
            if (row2 != null) {
                row2.setHeight((short) newRowHeightTwips);
                rowHeightTwipsMap.put(rowIndex, newRowHeightTwips);
            }
        }

        // ================================================================
        // 步骤 4：逐个 cell 画图
        // ================================================================
        for (Map.Entry<String, List<ImageData>> e : imageDataMap.entrySet()) {
            String key = e.getKey();
            int sepIdx = key.indexOf('_');
            int rowIndex = Integer.parseInt(key.substring(0, sepIdx));
            int columnIndex = Integer.parseInt(key.substring(sepIdx + 1));

            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            Cell cell = row.getCell(columnIndex);
            if (cell == null) continue;

            List<ImageData> list = e.getValue();
            List<int[]> sizes = (cellImageSizesListMap != null) ? cellImageSizesListMap.get(key) : null;
            if (list == null || list.isEmpty()) continue;

            // 取该列 @ColumnWidth 原始字符数（用于高度上限计算）
            Integer origBase = originalColumnWidthPerColumnMap.get(columnIndex);
            int originalColWidthChars = (origBase != null) ? origBase / 256 : FALLBACK_BASE_COLUMN_WIDTH_CHARS;

            // 取该行真实行高 twips
            int rowHeightTwips = rowHeightTwipsMap.getOrDefault(rowIndex, 15);

            drawImagesInCell(workbook, sheet, cell, list, sizes,
                    columnIndex, rowHeightTwips, originalColWidthChars);
        }
    }

    /**
     * 在指定单元格内绘制多张图片（横向并排）。
     *
     * <h3>绘制算法（基于 POI 官方 getPreferredSize 算法）</h3>
     *
     * <h4>OOXML 关键事实</h4>
     * <ul>
     *   <li>{@code colOff/rowOff} 用 EMU（English Metric Unit），1 inch = 914400 EMU = 96 px → <b>1 px = 9525 EMU</b></li>
     *   <li>colOff 是相对 col 的<b>物理左上角</b>（不是相对 cell 内容区），没有 cell padding</li>
     *   <li>对 TwoCellAnchor，Excel 在打开文件时会按 (to.colOff - from.colOff + Σ中间列宽) 计算图片实际宽度</li>
     * </ul>
     *
     * <h4>POI 标准反算算法（来自 SXSSFPicture.getPreferredSize）</h4>
     * <pre>
     *   // 水平反算（从 col1 开始，accumW 是累计像素）
     *   float w = 0;
     *   int col2 = col1 - 1;
     *   while (w <= scaledWidth) {
     *       w += getColumnWidthInPixels(++col2);
     *   }
     *   // 跳出时 w > scaledWidth
     *   double cw = getColumnWidthInPixels(col2);   //  当前 col2 的列宽
     *   double deltaW = w - scaledWidth;            // 超出 scaledWidth 的部分
     *   dx2 = EMU * (cw - deltaW);                  //  在 col2 内从右往左的距离
     * </pre>
     *
     * <h4>支持 dx1 &gt; 0 的扩展（参考 XSSFPicture.getPreferredSize）</h4>
     * <pre>
     *   // 如果 dx1 > 0，先算 from 所在 col 还剩多少宽度（要 /9525 转像素）
     *   float dx1Px = anchor.getDx1() / EMU_PER_PIXEL;
     *   if (dx1Px > 0) {
     *       w += getColumnWidthInPixels(col2) - dx1Px;   // 当前 col 剩余
     *       col2++;
     *   }
     *   // 然后正常累加
     * </pre>
     */
    private void drawImagesInCell(Workbook workbook, Sheet sheet, Cell cell,
                                  List<ImageData> imageDataList, List<int[]> sizes,
                                  int columnIndex, int rowHeightTwips,
                                  int originalColWidthChars) {
        // ---- 1. 防御 ----
        if (imageDataList == null || imageDataList.isEmpty()) return;
        int n = imageDataList.size();

        // ---- 2. 列宽（物理像素）----
        //  重要：不要用 sheet.getColumnWidthInPixels()！
        //   POI 公式 = chars × 7（假设 Calibri 11pt），但 Excel/WPS 用精确公式
        //   truncate(((256×W + truncate(128/maxDigitWidth))/256) × maxDigitWidth)
        //   字体不一致时会算偏 → 图渲染与代码预期不一致 → 居中失效
        //
        // 改用 originalColWidthChars（从 @ColumnWidth 注解取）+ Excel 精确公式：
        float colWidthPx = excelCharsToPx(originalColWidthChars);
        if (colWidthPx <= 1f) colWidthPx = FALLBACK_BASE_COLUMN_WIDTH_CHARS * Units.DEFAULT_CHARACTER_WIDTH;

        // ---- 3. 行高（物理像素）----
        // twips = points × 20 → 1 pt = 1/72 inch → 1 px = 1/96 inch
        // → 1 twip = 1/20 pt = 1/1440 inch → px = twips × 96 / 1440 = twips / 15
        float rowHeightPx = (rowHeightTwips > 0)
                ? (float) (rowHeightTwips / 15.0)
                : (float) (DEFAULT_ROW_HEIGHT_POINTS * 96.0 / 72.0);

        // ---- 4. 边距 / 间距 ----
        float horizontalMarginPx = IMAGE_HORIZONTAL_MARGIN_CHARS * Units.DEFAULT_CHARACTER_WIDTH;
        float verticalMarginPx = IMAGE_VERTICAL_MARGIN_CHARS * Units.DEFAULT_CHARACTER_WIDTH;
        float gapPx = IMAGE_GAP_CHARS * Units.DEFAULT_CHARACTER_WIDTH;

        // 高度上限（防细长图撑高行）
        float maxRenderHPx = (float) (originalColWidthChars * MAX_IMAGE_RENDER_HEIGHT_MULTIPLE * Units.DEFAULT_CHARACTER_WIDTH);

        // ---- 5. 计算每张图的「渲染宽/高」（保比例）----
        //  关键：OOXML colOff 是相对 col 物理左边界。
        //   图片位置不受 cell padding 影响（图片用 OOXML anchor 直接定位），
        //   但我们希望图相对"视觉中心"居中，所以可用宽度直接从 colWidth - 2*margin 算。
        //   cell padding（左 5px、右 5px）让 cell 内容区 < col 物理宽度，但图片可以"溢出"内容区。
        float usableWPx = colWidthPx - 2f * horizontalMarginPx;
        float usableHPx = rowHeightPx - 2f * verticalMarginPx;
        float singleSlotTargetPx = (usableWPx - (n - 1) * gapPx) / Math.max(n, 1);
        if (singleSlotTargetPx < 1f) singleSlotTargetPx = 1f;

        float[] renderWidthsPx = new float[n];
        float[] renderHeightsPx = new float[n];
        for (int i = 0; i < n; i++) {
            int origW = DEFAULT_IMAGE_WIDTH_PIXELS;
            int origH = DEFAULT_IMAGE_HEIGHT_PIXELS;
            if (sizes != null && i < sizes.size()) {
                origW = sizes.get(i)[0];
                origH = sizes.get(i)[1];
            }
            float renderW = Math.clamp((float) origW, 1f, singleSlotTargetPx);
            float ratio = (origW > 0) ? ((float) origH / origW) : 1f;
            float renderH = renderW * ratio;
            if (renderH > maxRenderHPx) {
                float scale = maxRenderHPx / renderH;
                renderH = maxRenderHPx;
                renderW = renderW * scale;
            }
            renderWidthsPx[i] = renderW;
            renderHeightsPx[i] = renderH;
        }

        // ---- 6. 图组总宽 + 居中 ----
        float groupW = 0f;
        for (int i = 0; i < n; i++) {
            groupW += renderWidthsPx[i];
            if (i < n - 1) groupW += gapPx;
        }
        if (groupW > usableWPx + 0.01f) {
            float scale = usableWPx / groupW;
            for (int i = 0; i < n; i++) {
                renderWidthsPx[i] *= scale;
                renderHeightsPx[i] *= scale;
            }
            groupW = usableWPx;
        }
        float centeringPx = Math.max(0f, (usableWPx - groupW) / 2f);

        // ---- 7. 计算每张图的 dx1/dy1（在 col/row 物理坐标系里）----
        //  重要事实（OOXML spec）：
        //   - dx1/dy1（EMU）是相对 col/row 的物理左上角（不含 cell padding）
        //   - colOff/rowOff 直接对应"距离 col 物理左边多少 EMU"
        //   - 文字 cell 有 left padding(5px) / top padding(4px)，但图片不受 padding 影响
        //   - 图片和文字是两套定位机制
        //
        //   所以想让图相对 cell 物理边界 "left margin" 距离，只需 dx1 = horizontalMarginPx。
        //   想居中：dx1 = horizontalMarginPx + centeringPx。
        float dx1Px = horizontalMarginPx + centeringPx;
        // dy1: 视觉垂直居中（同样不考虑 row padding）
        float groupMaxH = 0f;
        for (float h : renderHeightsPx) if (h > groupMaxH) groupMaxH = h;
        float dy1Px = verticalMarginPx + Math.max(0f, (usableHPx - groupMaxH) / 2f);
        // ---- 8. 创建 Drawing ----
        Drawing<?> drawing = sheet.getDrawingPatriarch();
        if (drawing == null) drawing = sheet.createDrawingPatriarch();
        CreationHelper helper = workbook.getCreationHelper();
        double emuPerPixel = Units.EMU_PER_PIXEL;

        float accXPx = dx1Px;

        for (int i = 0; i < n; i++) {
            float renderW = renderWidthsPx[i];
            float renderH = renderHeightsPx[i];

            ImageData imageData = imageDataList.get(i);
            if (imageData == null || imageData.getImage() == null || imageData.getImage().length == 0) {
                accXPx += renderW + gapPx;
                continue;
            }

            try {
                int pictureType = toPoiPictureType(imageData.getImageType());
                int pictureIdx = workbook.addPicture(imageData.getImage(), pictureType);

                // =============================================================
                //  方案：MOVE_AND_RESIZE（POI 默认 twoCellAnchor editAs="twoCell"）
                //   Excel 按 from+dx1 到 to+dx2 渲染图（位置和大小）
                // =============================================================
                ClientAnchor anchor = helper.createClientAnchor();
                int fromRow = cell.getRowIndex();

                //  第 i 张图从左往右累加：from 起点 = accXPx
                float fromDx1Px = accXPx;
                int fromDx1Emu = (int) Math.round(fromDx1Px * emuPerPixel);
                int fromDy1Emu = (int) Math.round(dy1Px * emuPerPixel);

                float toDy2Px = dy1Px + renderH;

                anchor.setCol1(columnIndex);
                anchor.setRow1(fromRow);
                anchor.setDx1(fromDx1Emu);
                anchor.setDy1(fromDy1Emu);
                anchor.setRow2(fromRow);     // 行不跨
                if (i == n - 1) {
                    //  最后一张：跨到下一列，dx2 = -margin，右边距由 Excel 精确算（摆脱列宽像素估算误差）
                    anchor.setCol2(columnIndex + 1);
                    anchor.setDx2(-(int) Math.round(horizontalMarginPx * emuPerPixel));
                } else {
                    //  中间图：同列，宽度 = renderW（按 (列宽-左右margin-(n-1)*gap)/n 算出）
                    anchor.setCol2(columnIndex);
                    anchor.setDx2((int) Math.round((fromDx1Px + renderW) * emuPerPixel));
                }
                anchor.setDy2((int) Math.round(toDy2Px * emuPerPixel));
                anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);

                // 创建图片
                drawing.createPicture(anchor, pictureIdx);

                accXPx += renderW + gapPx;
            } catch (Exception ex) {
                log.error("[FileCellWriteHandler] 绘制第 {} 张图片失败: {}", i, ex.getMessage());
                accXPx += renderW + gapPx;
            }
        }

        // ---- 9. 单元格样式 ----
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cell.setCellStyle(cellStyle);
    }

    // ====================================================================================
    // 工具方法
    // ====================================================================================
    private int currentSheetIndex(WriteSheetHolder holder) {
        if (holder == null || holder.getSheetNo() == null) return 0;
        return holder.getSheetNo();
    }

    /**
     * 从 EasyExcel 的 {@code Head} 对象读取该列的 {@code @ColumnWidth} 注解值。
     * <p>为什么不用 {@code sheet.getColumnWidth()}：因为 EasyExcel 的
     * {@code LongestMatchColumnWidthStyleStrategy} 会按 cell 内容自适应列宽，
     * 已经把列宽撑大了，不是用户原始设置的值。
     * <p>直接从 EasyExcel 内部维护的 {@link com.alibaba.excel.metadata.Head#getColumnWidthProperty()}
     * 拿（无需反射）。
     *
     * @return POI 列宽单位（字符数 × 256），如果读不到返回 -1
     */
    private int readColumnWidth(CellWriteHandlerContext context, int columnIndex) {
        try {
            WriteSheetHolder sheetHolder = context.getWriteSheetHolder();
            if (sheetHolder == null) return -1;

            com.alibaba.excel.metadata.property.ExcelHeadProperty headProperty =
                    sheetHolder.getExcelWriteHeadProperty();
            if (headProperty == null) return -1;

            java.util.Map<Integer, com.alibaba.excel.metadata.Head> headMap = headProperty.getHeadMap();
            if (headMap == null) return -1;

            com.alibaba.excel.metadata.Head head = headMap.get(columnIndex);
            if (head == null) return -1;

            // 直接读 EasyExcel Head 里的 columnWidthProperty
            com.alibaba.excel.metadata.property.ColumnWidthProperty widthProperty = head.getColumnWidthProperty();
            if (widthProperty != null && widthProperty.getWidth() != null && widthProperty.getWidth() > 0) {
                int width = widthProperty.getWidth();
                return width * 256;  // POI 列宽单位 = 字符数 × 256
            }
        } catch (Exception ex) {
            log.warn("[FileCellWriteHandler] 读取 @ColumnWidth 失败: {}", ex.getMessage(), ex);
        }
        return -1;
    }

    /**
     * 把 EasyExcel 的 ImageData.ImageType 转换为 POI 的图片类型常量
     */
    private int toPoiPictureType(ImageData.ImageType imageType) {
        if (imageType == null) return Workbook.PICTURE_TYPE_PNG;
        switch (imageType) {
            case PICTURE_TYPE_PNG:
                return Workbook.PICTURE_TYPE_PNG;
            case PICTURE_TYPE_JPEG:
                return Workbook.PICTURE_TYPE_JPEG;
            case PICTURE_TYPE_DIB:
                return Workbook.PICTURE_TYPE_DIB;
            default:
                return Workbook.PICTURE_TYPE_PNG;
        }
    }

    /**
     * 读取图片真实宽高（像素）。
     * 优先用 ImageReader 仅解析头（不加载全图），失败兜底默认 200×200。
     */
    private int[] readImageSizeFast(ImageData imageData) {
        if (imageData == null || imageData.getImage() == null || imageData.getImage().length == 0) {
            return new int[]{DEFAULT_IMAGE_WIDTH_PIXELS, DEFAULT_IMAGE_HEIGHT_PIXELS};
        }
        try (ImageInputStream iis = ImageIO.createImageInputStream(
                new ByteArrayInputStream(imageData.getImage()))) {
            if (iis != null) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(iis);
                        int w = reader.getWidth(0);
                        int h = reader.getHeight(0);
                        if (w > 0 && h > 0) return new int[]{w, h};
                    } finally {
                        reader.dispose();
                    }
                }
            }
        } catch (Exception ignored) {
            // fallback：用 BufferedImage 再次尝试
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData.getImage())) {
                BufferedImage bi = ImageIO.read(bis);
                if (bi != null) return new int[]{bi.getWidth(), bi.getHeight()};
            } catch (Exception ignored2) {
            }
        }
        return new int[]{DEFAULT_IMAGE_WIDTH_PIXELS, DEFAULT_IMAGE_HEIGHT_PIXELS};
    }

    // ====================================================================================
    // 列宽工具
    // ====================================================================================

    /**
     * Excel 列宽精确换算：{@code W} 字符数 → 像素。
     * <p>公式：{@code px = truncate(((256*W + truncate(128/maxDigitWidth)) / 256) * maxDigitWidth)}
     * <p>默认字体（Calibri 11pt）下 {@code maxDigitWidth = 7}，比 POI 的简化 {@code W*7} 更精确。
     */
    private static float excelCharsToPx(float chars) {
        final float maxDigitWidth = Units.DEFAULT_CHARACTER_WIDTH;  // 默认字体下为 7
        float num = (256f * chars + (float) Math.floor(128.0 / maxDigitWidth)) / 256f;
        return (float) Math.floor(num * maxDigitWidth);
    }
}

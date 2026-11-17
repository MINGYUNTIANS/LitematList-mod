package com.litematlist.preview;

/**
 * 内置全屏预览用到的固定参数（取自上游 SchematicPreview 的配置默认值）。
 * 这些参数原本可在模组配置界面调整，搬入后固定为默认值。
 */
public final class PreviewOptions {

    /** 是否渲染方块实体 */
    public static final boolean RENDER_TILE = true;
    /** 3D 预览 FOV */
    public static final double PREVIEW_FOV = 50.0;
    /** 3D 预览 Y 旋转 */
    public static final double PREVIEW_ROTATION_Y = -45.0;
    /** 3D 预览 X 旋转 */
    public static final double PREVIEW_ROTATION_X = 30.0;
    /** 列表内小预览的最大体积（内置全屏预览走 nonStatic 分支，此值不可达，保留原值备用） */
    public static final int PREVIEW_MAX_VOLUME = 125000;

    private PreviewOptions() {
    }
}
//#if MC == 11701
//$$
//#endif
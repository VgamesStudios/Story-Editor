package com.story_editor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 负责故事数据的加载、保存、文件列表管理等非 UI 逻辑。
 */
public class StoryEditorDataManager {
    private JsonObject currentStoryData;
    private File currentLoadedFile;

    // 元数据字段（直接从 JSON 读取）
    private String storyTitle = "";
    private String storyVersion = "";
    private String storyDate = "";
    private String storyStartNode = "";

    // 文件列表缓存
    private final List<StoryEditorListEntry> fileList = new ArrayList<>();

    // 单例（或由 UI 持有，这里使用实例方式）
    public StoryEditorDataManager() {
        // 可初始化
    }

    // ==================== 文件浏览 ====================

    /**
     * 刷新文件列表，扫描 storyeditor 目录下的所有 .json 文件。
     */
    public void refreshFileList() {
        fileList.clear();
        File dir = getStoryDir();
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                fileList.add(new StoryEditorListEntry(f.getName(), f.getName(), "点击加载"));
            }
        }
    }

    public List<StoryEditorListEntry> getFileList() {
        return fileList;
    }

    // ==================== 加载 / 保存 ====================

    /**
     * 加载指定 JSON 文件。
     * @return true 表示加载成功，false 表示失败。
     */
    public boolean loadStoryData(File file) {
        if (!file.exists()) {
            sendFeedback("文件不存在: " + file.getName());
            return false;
        }
        try (FileReader reader = new FileReader(file)) {
            currentStoryData = JsonParser.parseReader(reader).getAsJsonObject();
            storyTitle = getSafeString(currentStoryData, "title");
            storyVersion = getSafeString(currentStoryData, "version");
            storyDate = getSafeString(currentStoryData, "date");
            storyStartNode = getSafeString(currentStoryData, "startNode");
            currentLoadedFile = file;
            sendFeedback("已加载: " + file.getName());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            sendFeedback("加载失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 保存当前数据到当前加载的文件。
     * @return true 表示保存成功。
     */
    public boolean saveCurrentFile() {
        if (currentLoadedFile == null) {
            sendFeedback("请先加载一个文件");
            return false;
        }
        if (currentStoryData == null) {
            sendFeedback("没有数据可保存");
            return false;
        }
        // 注意：UI 会先更新元数据到 currentStoryData，然后再调用此方法
        try (FileWriter writer = new FileWriter(currentLoadedFile)) {
            writer.write(new Gson().toJson(currentStoryData));
            sendFeedback("已保存: " + currentLoadedFile.getName());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            sendFeedback("保存失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 更新 JSON 中的元数据（由 UI 调用，传入最新的标题/版本/开始节点）。
     */
    public void updateMetadata(String title, String version, String startNode) {
        if (currentStoryData != null) {
            currentStoryData.addProperty("title", title);
            currentStoryData.addProperty("version", version);
            currentStoryData.addProperty("startNode", startNode);
            currentStoryData.addProperty("date", LocalDate.now().toString());
            // 同步更新本地字段
            storyTitle = title;
            storyVersion = version;
            storyStartNode = startNode;
            storyDate = LocalDate.now().toString();
        }
    }

    // ==================== 获取数据 ====================

    public JsonObject getCurrentStoryData() {
        return currentStoryData;
    }

    public File getCurrentLoadedFile() {
        return currentLoadedFile;
    }

    public String getStoryTitle() {
        return storyTitle;
    }

    public String getStoryVersion() {
        return storyVersion;
    }

    public String getStoryDate() {
        return storyDate;
    }

    public String getStoryStartNode() {
        return storyStartNode;
    }

    public boolean hasData() {
        return currentStoryData != null;
    }

    // ==================== 工具方法 ====================

    private String getSafeString(JsonObject obj, String key) {
        return obj.has(key) ? obj.get(key).getAsString() : "";
    }

    private File getStoryDir() {
        File gameDir = Minecraft.getInstance().gameDirectory;
        File storyDir = new File(gameDir, "storyeditor");
        if (!storyDir.exists()) storyDir.mkdirs();
        return storyDir;
    }

    private void sendFeedback(String msg) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("[故事编辑器] " + msg), false
            );
        }
    }

    /**
     * 释放资源（如果需要）。
     */
    public void close() {
        // 可清理
    }

    // ==================== 节点列表相关（可进一步拆分，但暂时保留） ====================

    /**
     * 获取所有节点的列表（用于“节点”子标签显示）。
     */
    public List<StoryEditorListEntry> getNodeList() {
        List<StoryEditorListEntry> entries = new ArrayList<>();
        if (currentStoryData == null) return entries;
        JsonObject nodes = currentStoryData.getAsJsonObject("node");
        if (nodes == null) return entries;

        for (String key : nodes.keySet()) {
            JsonObject node = nodes.getAsJsonObject(key);
            String narrator = node.has("Narrator") ? node.get("Narrator").getAsString() : "未知";
            String text = node.has("text") ? node.get("text").getAsString() : "";
            int newlineIdx = text.indexOf('\n');
            String previewText = (newlineIdx != -1) ? text.substring(0, newlineIdx) : text;
            previewText = truncateByWidth(previewText, 25);
            entries.add(new StoryEditorListEntry(key, narrator + "：" + previewText, "节点ID: " + key));
        }
        return entries;
    }

    private String truncateByWidth(String text, int maxWidth) {
        if (text == null || text.isEmpty()) return text;
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isWide = Character.isIdeographic(c) || (c >= 0xFF00 && c <= 0xFFEF);
            int charWidth = isWide ? 2 : 1;
            if (width + charWidth > maxWidth) {
                return text.substring(0, i) + "...";
            }
            width += charWidth;
        }
        return text;
    }
}
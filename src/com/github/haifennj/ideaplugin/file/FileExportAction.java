package com.github.haifennj.ideaplugin.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jetbrains.annotations.NotNull;

import com.github.haifennj.ideaplugin.helper.NotificationUtil;
import com.github.haifennj.ideaplugin.helper.PluginConst;
import com.github.haifennj.ideaplugin.helper.PluginUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;

public class FileExportAction extends AnAction {

	private String label = "";
	private String exportId = "";

	public FileExportAction() {
	}

	public FileExportAction(String label, String exportId) {
		super(label);
		this.label = label;
		this.exportId = exportId;
	}

	// 文件路径映射表，根据 actionId 区分不同文件组
	public static final List<Map<String, Object>> FILE_PATHS_LIST = FileExportPathList.FILE_PATHS_LIST;

	@Override
	public void actionPerformed(AnActionEvent event) {
		Module releaseModule = PluginUtil.getReleaseModule(event.getProject(), true);
		if (releaseModule == null) {
			return;
		}
		String moduleName = releaseModule.getName();
		if (moduleName.equals("aws.release")) {
			moduleName = "release";
		}

		List<String> filePaths = findFilePathsById(findExportIdByActionId(event.getActionManager().getId(this)));
		if (filePaths.isEmpty()) {
			return;
		}

		SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyyMMdd_HH");
		String time = datetimeFormat.format(System.currentTimeMillis());
		Presentation p = event.getPresentation();
		String name = p.getText().replace("一键导出", "");

		String userHome = System.getProperty("user.home");
		String fileSeparator = System.getProperty("file.separator");
		VirtualFile releaseModuleFile = PluginUtil.findReleaseModuleFile(event.getProject());
		if (releaseModuleFile == null) {
			return;
		}
		String baseSourceDir = releaseModuleFile.getPath() + fileSeparator;
		String defaultOutput = userHome + fileSeparator + "Desktop" + fileSeparator + event.getProject().getName() + fileSeparator + event.getProject().getName()+"@"+time+name;
		String defaultOutputZip = defaultOutput + ".zip";

		// 检查并删除已存在的临时目录和ZIP文件
		File tempOutputDir = new File(defaultOutput);
		File zipOutputFile = new File(defaultOutputZip);

		try {
			// 删除已存在的临时目录
			if (tempOutputDir.exists()) {
				deleteDirectory(tempOutputDir);
			}

			// 删除已存在的ZIP文件
			if (zipOutputFile.exists()) {
				Files.delete(zipOutputFile.toPath());
			}

			// 创建新的临时输出目录
			tempOutputDir.mkdirs();

		} catch (IOException e) {
			NotificationUtil.error("清理旧文件失败: " + e.getMessage());
			return;
		}

		for (String path : filePaths) {
			try {
				exportFiles(event.getProject(), baseSourceDir, defaultOutput + fileSeparator + moduleName + fileSeparator,  path);
			} catch (Exception e) {
				// 不处理异常
			}
		}

		// 将临时目录压缩为ZIP文件
		try {
			createZipFile(tempOutputDir, new File(defaultOutputZip));
			// 压缩成功后删除临时目录
			deleteDirectory(tempOutputDir);
			NotificationUtil.info("导出成功 🚀\n文件位置: " + defaultOutputZip);
		} catch (IOException e) {
			NotificationUtil.error("压缩文件失败: " + e.getMessage());
		}
	}

	@Override
	public void update(@NotNull AnActionEvent e) {
		Presentation p = e.getPresentation();
		if (p.getText().equals(PluginConst.SEPARATOR)) {
			p.setEnabled(false);
			return;
		}
		String staticActionId = e.getActionManager().getId(this);
		if (staticActionId == null) {
			return;
		}
		String index = staticActionId.replace(PluginConst.MY_PLUGIN_ACTION_PREFIX, ""); // 提取 1,2,...
		String name = findNamesByActionId(index);
		if (!name.isEmpty()) {
			p.setText(name);
			if (name.equals(PluginConst.SEPARATOR)) {
				p.setEnabled(false);
			} else {
				p.setEnabledAndVisible(true);
			}
		} else {
			p.setEnabledAndVisible(false);
		}
	}

	private String findNamesByActionId(String actionId) {
		for (Map<String, Object> map : FILE_PATHS_LIST) {
			if (actionId.equals(map.get("actionId"))) {
				if (map.containsKey("level")) {
					int level = Integer.parseInt(map.get("level").toString());
					if (level > 1) {
						return "";
					}
				}
				if (map.containsKey("separator")) {
					return PluginConst.SEPARATOR;
				}
				if (map.containsKey("name")) {
					int ver = Integer.parseInt(map.get("ver").toString());
					return map.get("name") + (ver == 7 ? " (AWS" + map.get("ver") + ")" : "");
				}
			}
		}
		return "";
	}

	private String findExportIdByActionId(String staticActionId) {
		if (staticActionId == null) {
			return exportId;
		}
		String index = staticActionId.replace(PluginConst.MY_PLUGIN_ACTION_PREFIX, ""); // 提取 1,2,...
		for (Map<String, Object> map : FILE_PATHS_LIST) {
			if (index.equals(map.get("actionId"))) {
				return (String) map.get("id");
			}
		}
		return "";
	}

	private List<String> findFilePathsById(String id) {
		for (Map<String, Object> map : FILE_PATHS_LIST) {
			if (id.equals(map.get("id"))) {
				return (List<String>) map.get("paths");
			}
		}
		return new ArrayList<>();
	}

	public void exportFiles(Project project, String baseSourceDir, String targetDir, String relativeFilePath) throws IOException {
		File sourcePath = new File(baseSourceDir, relativeFilePath);
		File targetPath = new File(targetDir, relativeFilePath);
		if (!sourcePath.exists()) {
			return;
		}
		FileUtil.copyFileOrDir(sourcePath, targetPath);
	}

	/**
	 * 创建ZIP压缩文件
	 * @param sourceDir 要压缩的源目录
	 * @param zipFile 目标ZIP文件
	 * @throws IOException 压缩过程中发生IO异常
	 */
	private void createZipFile(File sourceDir, File zipFile) throws IOException {
		// 确保ZIP文件的父目录存在
		if (zipFile.getParentFile() != null && !zipFile.getParentFile().exists()) {
			zipFile.getParentFile().mkdirs();
		}

		try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
			Path sourcePath = sourceDir.toPath();

			Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
					// 排除mac系统的.DS_Store文件
					if (isDsStoreFile(file)) {
						return FileVisitResult.CONTINUE;
					}

					// 计算相对路径
					Path targetFile = sourcePath.relativize(file);

					// 创建ZIP条目
					ZipEntry zipEntry = new ZipEntry(targetFile.toString().replace(File.separator, "/"));
					zos.putNextEntry(zipEntry);

					// 写入文件内容
					Files.copy(file, zos);
					zos.closeEntry();

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
					// 排除包含.DS_Store的目录，但保留目录结构本身
					if (!dir.equals(sourcePath)) {
						// 为目录创建ZIP条目（确保以/结尾）
						Path targetDir = sourcePath.relativize(dir);
						ZipEntry zipEntry = new ZipEntry(targetDir.toString().replace(File.separator, "/") + "/");
						zos.putNextEntry(zipEntry);
						zos.closeEntry();
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					// 忽略访问文件失败的情况（比如.DS_Store文件权限问题）
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	/**
	 * 判断是否为mac系统的.DS_Store文件
	 * @param file 文件路径
	 * @return 如果是.DS_Store文件返回true，否则返回false
	 */
	private boolean isDsStoreFile(Path file) {
		String fileName = file.getFileName().toString();
		return ".DS_Store".equals(fileName) ||
				fileName.endsWith("/.DS_Store") ||
				fileName.contains("/.DS_Store/");
	}

	/**
	 * 递归删除目录
	 * @param directory 要删除的目录
	 */
	private void deleteDirectory(File directory) {
		if (directory.exists()) {
			File[] files = directory.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isDirectory()) {
						deleteDirectory(file);
					} else {
						file.delete();
					}
				}
			}
			directory.delete();
		}
	}

}

package com.github.haifennj.ideaplugin.helper;

import java.io.File;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Created by Haiifenng on 2017.01.16.
 */
public class PluginUtil {

	public static Module getReleaseModule(Project project, boolean isMsg) {
		// 1. 查找可能的模块名称
		Module releaseModule = findReleaseModule(project);
		Module[] allModules = ModuleManager.getInstance(project).getModules();
		for (Module module : allModules) {
			String moduleName = module.getName().toLowerCase();

			// 多种匹配条件
			if (moduleName.equals("release") ||
					moduleName.contains("release") ||
					moduleName.equals("aws.release")) {
				releaseModule = module;
				break;
			}
		}

		// 2. 检查模块是否存在
		if (releaseModule == null) {
			if (isMsg) {
				NotificationUtil.showErrorNotification(project, "当前Project中没有命名为[release]的Module");
			}
			System.err.println("当前Project中没有命名为[release]的Module");
			return null;
		}
		if (!"aws.release".equals(releaseModule.getName()) && releaseModule.getModuleFile() == null) {
			if (isMsg) {
				NotificationUtil.showErrorNotification(project, "当前Project中的[release]的不是一个有效的AWS资源");
			}
			System.err.println("当前Project中的[release]的不是一个有效的AWS资源");
			return null;
		}
		VirtualFile file = findReleaseModuleFile(project);
		//校验是不是一个有效的release
		if (file != null) {
			return releaseModule;
		} else {
			if (isMsg) {
				NotificationUtil.showErrorNotification(project, "当前Project中的[release]的不是一个有效的AWS资源");
			}
			System.err.println("当前Project中的[release]的不是一个有效的AWS资源");
			return null;
		}
	}

	public static Module findReleaseModule(Project project) {
		Module releaseModule = null;
		Module[] allModules = ModuleManager.getInstance(project).getModules();
		for (Module module : allModules) {
			String moduleName = module.getName().toLowerCase();

			// 多种匹配条件
			if (moduleName.equals("release") ||
					moduleName.contains("release") ||
					moduleName.equals("aws.release")) {
				releaseModule = module;
				break;
			}
		}
		return releaseModule;
	}

	public static VirtualFile findReleaseModuleFile(Project project) {
		Module releaseModule = findReleaseModule(project);
		if (releaseModule == null) {
			return null;
		}
		ModuleRootManager rootManager = ModuleRootManager.getInstance(releaseModule);
		VirtualFile[] contentRoots = rootManager.getContentRoots();
		for (VirtualFile virtualFile : contentRoots) {
			if (virtualFile.isDirectory()) {
				boolean releaseDir = isReleaseDir(virtualFile);
				if (releaseDir) {
					return virtualFile;
				}
			}
		}
		return null;
	}



	public static boolean isReleaseDir(VirtualFile file) {
		//校验是不是一个有效的release
		String releasePath = file.getPath();
		File file_release7_1 = new File(releasePath + "/bin/conf/application-dev.yml");
		File file_release7_2 = new File(releasePath + "/bin/conf/application.yml");

		File file_release6_1 = new File(releasePath + "/bin/conf/server.xml");
		File file_release6_2 = new File(releasePath + "/bin/lib/aws-license.jar");

		File file_release5_1 = new File(releasePath + "/bin/system.xml");
		File file_release5_2 = new File(releasePath + "/bin/lib/aws.platform.jar");

		if (file_release7_1.exists() && file_release7_2.exists()) {//AWS7版本
			return true;
		} else if (file_release6_1.exists() && file_release6_2.exists()) {//AWS6版本
			return true;
		} else if (file_release5_1.exists() && file_release5_2.exists()) {//AWS5版本
			return true;
		} else {
			return false;
		}
	}

	public static boolean checkManifestXml(VirtualFile file) {
		if (file == null) {
			return false;
		}
		File manifestFile = null;
		if (file.isDirectory()) {
			manifestFile = new File(file.getPath() + "/manifest.xml");
		} else {
			manifestFile = new File(file.getParent().getPath() + "/manifest.xml");
		}
		return manifestFile.exists();
	}

}

/**
 * Copyright 2012 NetDigital Sweden AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.nginious.http.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.LogMF;
import org.apache.log4j.Logger;

import com.nginious.http.HttpException;
import com.nginious.http.HttpRequest;
import com.nginious.http.HttpResponse;
import com.nginious.http.HttpStatus;
import com.nginious.http.common.PathParameters;
import com.nginious.http.server.HttpServerConfiguration;
import com.nginious.http.stats.HttpRequestStatistics;
import com.nginious.http.stats.WebSocketSessionStatistics;

public class ApplicationManagerImpl implements ApplicationManager {
	
	private static Logger logger = Logger.getLogger(ApplicationManagerImpl.class);
	
	private static final String DEFAULT_BACKUP_DIR_NAME = "backup";
	
	private static final String DEPLOY_APP_NAME = "admin";
	
	private static final String ROOT_APP = "root";

	private static Object deployLock = new Object();
	
	private String applicationsDirName;
	
	private String rootApplicationFileName;
	
	private String password;
	
	private String tmpDirName;
	
	private String backupDirName;
	
	private ConcurrentHashMap<String, ApplicationImpl> applications;
	
	private ApplicationClassLoader classLoader;
	
	private ControllerServiceFactory controllerFactory;
	
	private HttpService applicationService;
	
	private HttpService applicationsService;
	
	private HttpService httpStatsService;
	
	private HttpService wsStatsService;
	
	private HttpRequestStatistics httpStatistics;
	
	private WebSocketSessionStatistics wsStatistics;
	
	public ApplicationManagerImpl(HttpServerConfiguration configuration) {
		super();
		this.applicationsDirName = configuration.getWebappsDir();
		this.rootApplicationFileName = configuration.getWebappDirOrFile();
		
		if(this.rootApplicationFileName != null) {
			this.applicationsDirName = null;
		}
		
		this.password = configuration.getAdminPwd();
		this.tmpDirName = System.getProperty("java.io.tmpdir");
		
		if(this.applicationsDirName != null) {
			this.backupDirName = createBackupDirName(this.applicationsDirName);
			File backupDir = new File(this.backupDirName);
			
			if(!backupDir.exists()) {
				if(!backupDir.mkdir()) {
					LogMF.warn(logger, "Can't create backup dir {0}", this.backupDirName);
					this.backupDirName = null;
				}
			}
		} else {
			this.applicationsDirName = this.tmpDirName;
		}
		
		this.applications = new ConcurrentHashMap<String, ApplicationImpl>();

		this.classLoader = new ApplicationClassLoader(Thread.currentThread().getContextClassLoader());
		this.controllerFactory = new ControllerServiceFactory(this.classLoader);		
	}
	
	public void setHttpRequestStatistics(HttpRequestStatistics httpStatistics) {
		this.httpStatistics = httpStatistics;
	}
	
	public void setWebSocketSessionStatistics(WebSocketSessionStatistics wsStatistics) {
		this.wsStatistics = wsStatistics;
	}
	
	public void start() {
		logger.info("Start");
		
		this.applicationService = createApplicationService(this.password);
		this.applicationsService = createApplicationsService(this.password);
		this.httpStatsService = createHttpStatsService(this.password);
		this.wsStatsService = createWebSocketSessionStatsService(this.password);
		
		if(this.rootApplicationFileName != null) {
			try {
				publish("root", new File(this.rootApplicationFileName));
			} catch(ApplicationException e) {
				logger.warn("Unable to publish root web application", e);
			}
		}
		
		if(this.applicationsDirName == null || applicationsDirName.equals(this.tmpDirName)) {
			return;
		}
		
		File applicationsDir = new File(this.applicationsDirName);
		
		synchronized(deployLock) {
			if(!applicationsDir.exists()) {
				return;
			}
			
			if(!applicationsDir.isDirectory()) {
				return;
			}
			
			File[] appFiles = applicationsDir.listFiles();
			
			for(File appFile : appFiles) {
				if(appFile.isDirectory()) {
					try {
						publish(appFile.getName(), appFile);
					} catch(ApplicationException e) {
						Object[] params = { appFile.getName() };
						LogMF.warn(logger, e, "Unable to publish {0}", params);
					} catch(Throwable t) {
						Object[] params = { appFile.getName() };
						LogMF.warn(logger, t, "Unable to publish {0}", params);
					}
				} else if(appFile.isFile() && appFile.getName().endsWith(".war")) {
					String appName = appFile.getName();
					appName = appName.substring(0, appName.length() - 4);
					
					try {
						publish(appName, appFile);
					} catch(ApplicationException e) {
						Object[] params = { appFile.getName() };
						LogMF.warn(logger, e, "Unable to publish {0}", params);
					} catch(Throwable t) {
						Object[] params = { appFile.getName() };
						LogMF.warn(logger, t, "Unable to publish {0}", params);
					}
				}
			}
		}
	}
	
	public void stop() {
		logger.info("Stop");
		Set<String> names = applications.keySet();
		
		synchronized(deployLock) {
			for(String name : names) {
				ApplicationImpl application = applications.get(name);
				application.unpublish();
			}
		}
		
		applications.clear();
	}
	
	public HttpServiceResult execute(HttpRequest request, HttpResponse response) throws HttpException, IOException {
		PathParameters params = new PathParameters(request);
		String possibleAppName = params.get(0);
		String localPath = request.getPath();
		localPath = localPath.substring(possibleAppName.length() + 1);
		
		if(possibleAppName.equals(DEPLOY_APP_NAME)) {
			return executeAdmin(localPath, request, response);
		} else {
			return execute(localPath, request, response);
		}
	}
	
	public HttpServiceResult execute(String localPath, HttpRequest request, HttpResponse response) throws HttpException, IOException {
		PathParameters params = new PathParameters(request);
		String possibleAppName = params.get(0);
		HttpServiceResult result = HttpServiceResult.DONE;
		
		ApplicationImpl application = applications.get(possibleAppName);
		
		if(application == null) {
			localPath = request.getPath();
			application = applications.get(ROOT_APP);
		}
				
		if(application != null) {
			result = application.execute(localPath, request, response);
			
			if(result == HttpServiceResult.CONTINUE) {
				result = HttpServiceResult.DONE;
			}
		} else if(possibleAppName.equals("favicon.ico")) {
			// Server empty favicon if none exists
			ApplicationImpl.sendEmptyFavicon(response);
		} else {
			throw new HttpException(HttpStatus.NOT_FOUND, localPath);
		}
		
		return result;
	}
	
	private HttpServiceResult executeAdmin(String localPath, HttpRequest request, HttpResponse response) throws HttpException, IOException {
		PathParameters params = new PathParameters(request);
		String serviceName = params.get(1);
		
		if(serviceName != null) {
			if(serviceName.equals("application")) {
				applicationService.invoke(request, response);
				return HttpServiceResult.DONE;
			} else if(serviceName.equals("applications")) {
				applicationsService.invoke(request, response);
				return HttpServiceResult.DONE;
			} else if(serviceName.equals("httpstats")) {
				httpStatsService.invoke(request, response);
				return HttpServiceResult.DONE;
			} else if(serviceName.equals("wsstats")) {
				wsStatsService.invoke(request, response);
				return HttpServiceResult.DONE;
			}
		}
		
		throw new HttpException(HttpStatus.NOT_FOUND, request.getPath());
	}
	
	public Application getApplication(String name) {
		return applications.get(name);
	}
	
	public Application createApplication(String name) throws ApplicationException {
		if(applications.containsKey(name)) {
			throw new ApplicationException("Duplicate application name '" + name + "'");
		}
		
		ApplicationImpl application = new ApplicationImpl(name);
		application.setMemory(true);
		return application;
	}
	
	public Application createApplication(String name, File baseDir) throws ApplicationException {
		ApplicationClassLoader classLoader = new ApplicationClassLoader(Thread.currentThread().getContextClassLoader(), baseDir);
		ApplicationImpl application = new ApplicationImpl(name);
		application.setBaseDir(baseDir);
		application.setClassLoader(classLoader);
		return application;
	}

	public List<Application> getApplications() {
		ArrayList<Application> outApplications = new ArrayList<Application>(applications.size());
		outApplications.addAll(applications.values());
		return outApplications;
	}
	
	public Application publish(Application application) throws ApplicationException {
		LogMF.info(logger, "Publish name={0}", application.getName());
		if(applications.containsKey(application.getName())) {
			throw new ApplicationException("Application with name '" + application.getName() + "' already exists");
		}
		
		ApplicationImpl applicationImpl = (ApplicationImpl)application;
		applicationImpl.publish();
		applications.put(applicationImpl.getName(), applicationImpl);
		return applicationImpl;
	}
	
	public Application publish(String name, File warFileOrAppDir) throws ApplicationException {
		LogMF.info(logger, "Publish name={0}, file={1}", name, warFileOrAppDir.getAbsolutePath());
		
		if(this.applicationsDirName == null) {
			throw new ApplicationException("Unable to publish application '" + name + "', no application directory configured");
		}
		
		ApplicationImpl prevApplication = applications.get(name);
		
		if(prevApplication != null) {
			return upgrade(name, warFileOrAppDir);
		} else {
			return create(name, warFileOrAppDir);
		}
	}
	
	public Application rollback(String name) throws ApplicationException {
		LogMF.info(logger, "Rollback name={0}", name);
		
		synchronized(deployLock) {
			ApplicationImpl prevApplication = applications.get(name);
			
			if(prevApplication == null) {
				throw new ApplicationException("Application " + name + " not found");
			}
			
			if(!prevApplication.isWar()) {
				throw new ApplicationException("Can't rollback unpacked application '" + name + "'");
			}
			
			if(this.backupDirName == null) {
				throw new ApplicationException("Can't rollback '" + name + "' no backup directory exists");
			}
			
			File backupFile = new File(this.backupDirName, name + ".war.1.bak");
			
			if(!backupFile.exists()) {
				throw new ApplicationException("Can't rollback '" + name + "' , no backup copy exists");
			}
			
			File appFile = new File(this.applicationsDirName, name + ".war");
			
			ApplicationConfigurator configurator = new ApplicationConfigurator(backupFile);
			ApplicationImpl newApplication = configurator.configure();
			newApplication.setName(name);
			applications.put(name, newApplication);
			
			prevApplication.unpublish();
			
			if(appFile.exists() && !appFile.delete()) {
				throw new ApplicationException("Unable to remove old application war archive");
			}
			
			if(!backupFile.renameTo(appFile)) {
				throw new ApplicationException("Unable to move application war arhive to application directory");
			}
			
			moveDownBackupNumbers(name);
			return newApplication;
		}
	}

	public void unpublish(Application application) throws ApplicationException {
		unpublish(application.getName());		
	}

	public void unpublish(String name) throws ApplicationException {
		LogMF.info(logger, "Unpublish name={0}", name);
		
		synchronized(deployLock) {
			ApplicationImpl application = applications.remove(name);
			
			if(application == null) {
				throw new ApplicationException("Application '" + name + "' not found");
			}
			
			if(application.isWar()) {
				application.unpublish();
			}
		}		
	}
		
	public void delete(Application application) throws ApplicationException {
		delete(application.getName());
	}
	
	public void delete(String name) throws ApplicationException {
		LogMF.info(logger, "Unpublish name={0}", name);
		
		synchronized(deployLock) {
			ApplicationImpl application = applications.get(name);
			
			if(application == null) {
				throw new ApplicationException("Application '" + name + "' not found");
			}

			unpublish(name);
			
			File appFile = new File(this.applicationsDirName, name + ".war");
			appFile.delete();
			
			int idx = 1;
			File bakFile = new File(this.backupDirName, name + ".war." + idx + ".bak");
			
			while(bakFile.exists()) {
				bakFile.delete();
				idx++;
				bakFile = new File(this.backupDirName, name + ".war." + idx + ".bak");
			}
			
		}
	}
	
	HttpRequestStatistics getHttpRequestStatistics() {
		return this.httpStatistics;
	}
	
	WebSocketSessionStatistics getWebSocketSessionStatistics() {
		return this.wsStatistics;
	}
	
	ApplicationImpl getApplicationImpl(String name) {
		return applications.get(name);
	}
	
	String getApplicationDir() {
		return this.applicationsDirName;
	}
	
	String getBackupDir() {
		return this.backupDirName;
	}
	
	ApplicationInfo createApplicationInfo(ApplicationImpl application) {
		if(application.isMemory()) {
			ApplicationInfo info = new ApplicationInfo();
			info.setName(application.getName());
			info.addVersion(0, application.getPublishTime().getTime());
			return info;
			
		}
		
		String applicationDirName = getApplicationDir();
		
		if(application.isDirectory()) {
			ApplicationInfo info = new ApplicationInfo();
			File appDir = new File(applicationDirName, application.getName());
			info.setName(application.getName());
			info.addVersion(0, appDir.lastModified());;
			return info;
		}
		
		ApplicationInfo info = new ApplicationInfo();
		File warFile = new File(applicationDirName, application.getName() + ".war");
		info.setName(application.getName());
		info.addVersion(0, warFile.lastModified());
		
		if(this.backupDirName != null) {
			int idx = 1;
			String backupDirName = getBackupDir();
			
			warFile = new File(backupDirName, application.getName() + ".war." + idx + ".bak");
			
			while(warFile.exists()) {
				info.addVersion(idx, warFile.lastModified());
				idx++;
				warFile = new File(backupDirName, application.getName() + ".war." + idx + ".bak");
			}
		}
		
		return info;
	}
	
	private ApplicationImpl create(String name, File warFileOrAppDir) throws ApplicationException {
		LogMF.info(logger, "Create name={0},  file={1}", name, warFileOrAppDir.getAbsolutePath());
		
		synchronized(deployLock) {
			File destFile = new File(this.applicationsDirName, name + ".war");
			
			if(warFileOrAppDir.isFile() && !destFile.equals(warFileOrAppDir)) {
				moveToAppsDir(name, warFileOrAppDir);
				warFileOrAppDir = destFile;
			}
			
			ApplicationConfigurator configurator = new ApplicationConfigurator(name, warFileOrAppDir);
			ApplicationImpl application = configurator.configure();
			application.publish();
			applications.put(application.getName(), application);
			return application;
			
		}
	}
	
	private ApplicationImpl upgrade(String name, File warFile) throws ApplicationException {
		LogMF.info(logger, "Upgrade name={0}, warFile={1}", name, warFile.getAbsolutePath());
		
		synchronized(deployLock) {
			ApplicationConfigurator configurator = new ApplicationConfigurator(name, warFile);
			ApplicationImpl application = configurator.configure();
			ApplicationImpl prevApplication = applications.put(name, application);
			
			if(prevApplication != null && this.backupDirName != null) {
				moveUpBackupNumbers(name);
				moveToBackupDir(name);
			}
			
			moveToAppsDir(name, warFile);
			
			if(prevApplication != null && this.backupDirName != null) {
				prevApplication.unpublish();
			}
			
			return application;
		}
	}
	
	private void moveDownBackupNumbers(String appName) {
		int idxFrom = 2;
		int idxTo = 1;
		File fromFile = new File(this.backupDirName, appName + ".war." + idxFrom + ".bak");
		
		while(fromFile.exists()) {
			File toFile = new File(this.backupDirName, appName + ".war." + idxTo + ".bak");
			fromFile.renameTo(toFile);
			idxFrom++;
			idxTo++;
			fromFile = new File(this.backupDirName, appName + ".war." + idxFrom + ".bak");
		}
	}
	
	private void moveUpBackupNumbers(String appName) {
		int idxLast = 1;
		File fromFile = new File(this.backupDirName, appName + ".war." + idxLast + ".bak");
		
		while(fromFile.exists()) {
			idxLast++;
			fromFile = new File(this.backupDirName, appName + ".war." + idxLast + ".bak");
		}
		
		idxLast--;
		
		if(idxLast == 9) {
			idxLast--;
		}
		
		for(int i = idxLast; i > 0; i--) {
			int idxTo = i + 1;
			fromFile = new File(this.backupDirName, appName + ".war." + i + ".bak");
			File toFile = new File(this.backupDirName, appName + ".war." + idxTo + ".bak");
			fromFile.renameTo(toFile);
		}
	}
	
	private void moveToBackupDir(String appName) throws ApplicationException {
		File appFile = new File(this.applicationsDirName, appName + ".war");
		File backupFile = new File(this.backupDirName, appName + ".war.1.bak");
		
		if(!appFile.renameTo(backupFile)) {
			throw new ApplicationException("Unable to move war archive " + appFile.getAbsolutePath() + 
					" for app " + appName + " to backup dir " + backupFile.getAbsolutePath());
		}
	}
	
	private void moveToAppsDir(String appName, File warFile) throws ApplicationException {
		File destFile = new File(this.applicationsDirName, appName + ".war");
		
		FileInputStream in = null;
		FileOutputStream out = null;
		
		try {
			in = new FileInputStream(warFile);
			out = new FileOutputStream(destFile);
			byte[] buff = new byte[1024];
			int len = 0;
			
			while((len = in.read(buff)) > 0) {
				out.write(buff, 0, len);
			}
			
			warFile.delete();
		} catch(IOException e) {
			throw new ApplicationException("Unable to copy war archive " + warFile.getAbsolutePath() + " for " + appName, e);
		} finally {
			if(in != null) {
				try { in.close(); } catch(IOException e) {}
			}
			
			if(out != null) {
				try { out.close(); } catch(IOException e) {}
			}
		}
	}
	
	private String createBackupDirName(String appsDirName) {
		int startIndex = appsDirName.endsWith(File.separator) ? appsDirName.length() - 2 : appsDirName.length() - 1; 
		int idx = appsDirName.lastIndexOf(File.separator, startIndex);
		
		if(idx == -1 && !appsDirName.endsWith(File.separator)) {
			return DEFAULT_BACKUP_DIR_NAME;
		}
		
		String backupDirName = appsDirName.substring(0, idx + 1) + DEFAULT_BACKUP_DIR_NAME;
		return backupDirName;
	}	

	private HttpService createApplicationService(String password) {
		try {
			ControllerChain chain = new ControllerChain();
			ApplicationAuthenticationFilter authService = new ApplicationAuthenticationFilter(password);
			chain.addServiceLast(authService);
			ApplicationController service = new ApplicationController(this);
			HttpService invokerService = controllerFactory.createControllerService(service);
			chain.addServiceLast(invokerService);
			return chain;
		} catch(ControllerServiceFactoryException e) {
			throw new RuntimeException("Unable to create application controller service", e);
		}
	}
	
	private HttpService createApplicationsService(String password) {
		try {
			ControllerChain chain = new ControllerChain();
			ApplicationAuthenticationFilter authService = new ApplicationAuthenticationFilter(password);
			chain.addServiceLast(authService);
			ApplicationsController service = new ApplicationsController(this);
			HttpService invokerService = controllerFactory.createControllerService(service);
			chain.addServiceLast(invokerService);
			return chain;
		} catch(ControllerServiceFactoryException e) {
			throw new RuntimeException("Unable to create applications controller service", e);
		}
	}
	
	private HttpService createHttpStatsService(String password) {
		try {
			ControllerChain chain = new ControllerChain();
			ApplicationAuthenticationFilter authService = new ApplicationAuthenticationFilter(password);
			chain.addServiceLast(authService);
			HttpRequestStatisticsService service = new HttpRequestStatisticsService(this);
			HttpService invokerService = controllerFactory.createControllerService(service);
			chain.addServiceLast(invokerService);
			return chain;
		} catch(ControllerServiceFactoryException e) {
			throw new RuntimeException("Unable to create HTTP requests statistics controller service", e);
		}
	}
	
	private HttpService createWebSocketSessionStatsService(String password) {
		try {
			ControllerChain chain = new ControllerChain();
			ApplicationAuthenticationFilter authService = new ApplicationAuthenticationFilter(password);
			chain.addServiceLast(authService);
			WebSocketSessionStatisticsService service = new WebSocketSessionStatisticsService(this);
			HttpService invokerService = controllerFactory.createControllerService(service);
			chain.addServiceLast(invokerService);
			return chain;
		} catch(ControllerServiceFactoryException e) {
			throw new RuntimeException("Unable to creat web socket session statistics controller service", e);
		}
	}
}

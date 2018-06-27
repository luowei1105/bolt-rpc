package com.fmgame.bolt.extension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fmgame.bolt.common.Constants;

/**
 * 扩展点获取
 * <ul>
 * <li>自动注入关联扩展点。</li>
 * <li>自动Wrap上扩展点的Wrap类。</li>
 * </ul>
 * 
 * @author luowei
 * @date 2017年11月2日 上午11:29:30
 */
public class ExtensionLoader<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);
    
    /** 扩展类所在路径前缀 */
    private static final String PREFIX = "META-INF/services/";
    /** 扩展点加载器 */
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
    
    /** 指定扩展类 */
    private final Class<T> type;
    /** 是否初始化 */
    private volatile boolean init = false;
    /** <key,扩展类实现> */
    private final Map<String, T> singletonInstances = new ConcurrentHashMap<>();
    /** <key,扩展类class> */
    private final Map<String, Class<T>> cachedClasses = new ConcurrentHashMap<>();

    private ExtensionLoader(Class<T> type) {
        this.type = type;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
        }
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type(" + type +
                    ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
        }

        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }
    
    /**
     * 返回已经加载的扩展点的名字。
     * <p/>
     * 一般应该调用{@link #getSupportedExtensions()}方法获得扩展，这个方法会返回所有的扩展点。
     *
     * @see #getSupportedExtensions()
     */
    public Set<String> getLoadedExtensions() {
        return Collections.unmodifiableSet(new TreeSet<String>(cachedClasses.keySet()));
    }
    
    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }
    
    /**
     * 有些地方需要spi的所有激活的instances，所以需要能返回一个列表的方法
     * 
     * @param key
     * @param group
     * @return
     */
    public List<T> getActivateExtension(String key, String group) {
    	String[] values = Constants.COMMA_SPLIT_PATTERN.split(key);
    	Map<String, Class<T>> extensionClasses = getExtensionClasses();
        List<T> exts = new ArrayList<T>(extensionClasses.size());
        
        List<String> names = values == null ? new ArrayList<String>(0) : Arrays.asList(values);
        // 多个实现，按优先级排序返回
        for (Map.Entry<String, Class<T>> entry : extensionClasses.entrySet()) {
        	Activate activate = entry.getValue().getAnnotation(Activate.class);
        	if (activate != null && activate.key() != null) {
                if (!isMatchGroup(group, activate.group())) 
                	continue;
                
                for (String k : activate.key()) {
                    if (names.contains(k)) {
                        exts.add(getExtension(entry.getKey()));
                        break;
                    }
                }
            }
        }
        exts.sort(Comparator.comparing(o -> o.getClass().getAnnotation(Activate.class).order()));
        return exts;
    }
    
    private boolean isMatchGroup(String group, String[] groups) {
        if (group == null || group.length() == 0) {
            return true;
        }
        if (groups != null && groups.length > 0) {
            for (String g : groups) {
                if (group.equals(g)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 返回指定名字的扩展。如果指定名字的扩展不存在，则抛异常 {@link IllegalStateException}.
     *
     * @param name
     * @return
     */
    public T getExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        
        try {
            SPI spi = type.getAnnotation(SPI.class);
            Class<T> clz = getExtensionClass(name);
            if (clz == null) {
            	return null;
            }
            
            if (spi.singleton() == true) {
                return getSingletonInstance(name, clz);
            } else {
                return clz.newInstance();
            }
        } catch (Exception e) {
        	throw new IllegalArgumentException("Error when getExtension " + name, e);
        }
    }
    
    private T getSingletonInstance(String name, Class<T> clz) throws InstantiationException, IllegalAccessException {
        T obj = singletonInstances.get(name);
        if (obj == null) {
            synchronized (singletonInstances) {
                obj = singletonInstances.get(name);
                if (obj == null) {
                    obj = clz.newInstance();
                    singletonInstances.put(name, obj);
                }
            }

        }
        return obj;
    }
    
	private Class<T> getExtensionClass(String name) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        if (name == null)
            throw new IllegalArgumentException("Extension name == null");
        Class<T> clazz = getExtensionClasses().get(name);
        if (clazz == null)
            throw new IllegalStateException("No such extension \"" + name + "\" for " + type.getName() + "!");
        return clazz;
	}
	
	private Map<String, Class<T>> getExtensionClasses() {
        if (!init) {
            loadExtensionClasses();
            init = true;
        }
    	return cachedClasses;
    }
    
    private synchronized void loadExtensionClasses() {
    	String fullName = PREFIX + type.getName();
    	
    	try {
            // 定义一个枚举的集合 并进行循环来处理这个目录下的things
            Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader().getResources(fullName);
            while (dirs.hasMoreElements()){
                // 获取下一个元素
                URL url = dirs.nextElement();
                parseUrl(type, url, cachedClasses);
            }
    	} catch (Exception e) {
    		throw new IllegalArgumentException(e);
		}
    }
    
    @SuppressWarnings("unchecked")
	private void parseUrl(Class<T> type, URL url, Map<String, Class<T>> cachedClasses) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream(), Constants.DEFAULT_CHARACTER));
            String name = null;
            String line = null;
            while ((line = reader.readLine()) != null) {
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                	continue;
                }
                int i = line.indexOf('=');
                if (i > 0) {
                    name = line.substring(0, i).trim();
                    line = line.substring(i + 1).trim();
                }
                Class<?> clazz = Class.forName(line, true, Thread.currentThread().getContextClassLoader());
            	if (!type.isAssignableFrom(clazz)) {
            		 throw new IllegalStateException("Error when load extension class(interface: " +
                             type + ", class line: " + clazz.getName() + "), class "
                             + clazz.getName() + "is not subtype of interface.");
            	}

				int modifier = clazz.getModifiers();
				if (Modifier.isInterface(modifier) || Modifier.isAbstract(clazz.getModifiers())) {
		     		 throw new IllegalStateException("Error when load extension class(interface: " +
                             type + ", class line: " + clazz.getName() + "), class "
                             + clazz.getName() + "is abstract or interface.");
				}
                
                cachedClasses.put(name, (Class<T>) clazz);
            }
        } catch (Exception e) {
        	logger.error("Error reading spi configuration file.class = " + type, e);
        } finally {
            try {
            	 reader.close();
            } catch (IOException e) {
            	logger.error("Error closing spi configuration file.class = " + type, e);
            }
        }
    }
    
}

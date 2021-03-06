package org.infinispan.protostream;

import static java.util.Collections.unmodifiableMap;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregator for source protofiles.
 *
 * @author gustavonalle
 * @author anistor@redhat.com
 * @since 2.0
 */
public final class FileDescriptorSource {

   private static final String ENCODING = "UTF-8";
   private static final int BUFFER_SIZE = 1024;

   private final Map<String, char[]> descriptors = new ConcurrentHashMap<>();

   private ProgressCallback progressCallback;

   public interface ProgressCallback {

      void handleError(String fileName, DescriptorParserException exception);

      void handleSuccess(String fileName);
   }

   public FileDescriptorSource withProgressCallback(ProgressCallback progressCallback) throws IOException {
      this.progressCallback = progressCallback;
      return this;
   }

   public FileDescriptorSource addProtoFiles(String... classpathResources) throws IOException {
      return addProtoFiles(null, classpathResources);
   }

   public FileDescriptorSource addProtoFiles(ClassLoader userClassLoader, String... classpathResources) throws IOException {
      for (String classpathResource : classpathResources) {
         if (classpathResource == null) {
            throw new IllegalArgumentException("classpathResource cannot be null");
         }
         // enforce absolute resource path
         String absPath = classpathResource.startsWith("/") ? classpathResource : "/" + classpathResource;
         InputStream resourceAsStream = getResourceAsStream(userClassLoader, absPath);
         if (resourceAsStream == null) {
            throw new IOException("Resource not found in class path : " + classpathResource);
         }
         // discard the leading slash
         String path = classpathResource.startsWith("/") ? classpathResource.substring(1) : classpathResource;
         addProtoFile(path, resourceAsStream);
      }
      return this;
   }

   public FileDescriptorSource addProtoFile(String name, String contents) {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null");
      }
      if (contents == null) {
         throw new IllegalArgumentException("contents cannot be null");
      }
      // discard the leading slash
      String path = name.startsWith("/") ? name.substring(1) : name;
      descriptors.put(path, contents.toCharArray());
      return this;
   }

   public FileDescriptorSource addProtoFile(String name, InputStream contents) throws IOException {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null");
      }
      if (contents == null) {
         throw new IllegalArgumentException("contents cannot be null");
      }
      // discard the leading slash
      String path = name.startsWith("/") ? name.substring(1) : name;
      descriptors.put(path, toCharArray(contents));
      return this;
   }

   public FileDescriptorSource addProtoFile(String name, Reader contents) throws IOException {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null");
      }
      if (contents == null) {
         throw new IllegalArgumentException("contents cannot be null");
      }
      // discard the leading slash
      String path = name.startsWith("/") ? name.substring(1) : name;
      descriptors.put(path, toCharArray(contents));
      return this;
   }

   public FileDescriptorSource addProtoFile(String name, File protofile) throws IOException {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null");
      }
      if (protofile == null) {
         throw new IllegalArgumentException("protofile cannot be null");
      }
      // discard the leading slash
      String path = name.startsWith("/") ? name.substring(1) : name;
      descriptors.put(path, toCharArray(protofile));
      return this;
   }

   public static FileDescriptorSource fromResources(ClassLoader userClassLoader, String... classPathResources) throws IOException {
      return new FileDescriptorSource().addProtoFiles(userClassLoader, classPathResources);
   }

   public static FileDescriptorSource fromResources(String... classPathResources) throws IOException {
      return new FileDescriptorSource().addProtoFiles(classPathResources);
   }

   public static FileDescriptorSource fromString(String name, String protoSource) {
      return new FileDescriptorSource().addProtoFile(name, protoSource);
   }

   public Map<String, char[]> getFileDescriptors() {
      return unmodifiableMap(descriptors);
   }

   public ProgressCallback getProgressCallback() {
      return progressCallback;
   }

   private char[] toCharArray(File file) throws IOException {
      FileInputStream is = new FileInputStream(file);
      try {
         return toCharArray(is);
      } finally {
         is.close();
      }
   }

   private char[] toCharArray(InputStream is) throws IOException {
      Reader reader = new InputStreamReader(is, ENCODING);
      try {
         return toCharArray(reader);
      } finally {
         reader.close();
      }
   }

   private char[] toCharArray(Reader reader) throws IOException {
      try {
         CharArrayWriter writer = new CharArrayWriter();
         char[] buffer = new char[BUFFER_SIZE];
         int count;
         while ((count = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, count);
         }
         return writer.toCharArray();
      } finally {
         reader.close();
      }
   }

   private static InputStream getResourceAsStream(ClassLoader userClassLoader, String resourcePath) {
      if (resourcePath.startsWith("/")) {
         resourcePath = resourcePath.substring(1);
      }
      ClassLoader[] classLoaders = {userClassLoader,
                                    FileDescriptorSource.class.getClassLoader(),
                                    ClassLoader.getSystemClassLoader(),
                                    Thread.currentThread().getContextClassLoader()};
      InputStream is = null;
      for (ClassLoader cl : classLoaders) {
         if (cl != null) {
            is = cl.getResourceAsStream(resourcePath);
            if (is != null) {
               break;
            }
         }
      }
      return is;
   }
}

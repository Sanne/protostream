package org.infinispan.protostream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.ByteArrayOutputStreamEx;
import org.infinispan.protostream.impl.RawProtoStreamReaderImpl;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.infinispan.protostream.impl.SerializationContextImpl;

/**
 * @author anistor@redhat.com
 * @since 1.0
 */
public final class ProtobufUtil {

   private static final String WRAPPING_DEFINITIONS_RES = "/org/infinispan/protostream/message-wrapping.proto";

   public static SerializationContext newSerializationContext(Configuration configuration) {
      SerializationContextImpl serializationContext = new SerializationContextImpl(configuration);

      try {
         serializationContext.registerProtoFiles(FileDescriptorSource.fromResources(WRAPPING_DEFINITIONS_RES));
      } catch (IOException e) {
         throw new RuntimeException("Failed to initialize serialization context", e);
      } catch (DescriptorParserException e) {
         throw new RuntimeException("Failed to initialize serialization context", e);
      }

      serializationContext.registerMarshaller(new WrappedMessage.Marshaller());

      return serializationContext;
   }

   private static <A> void writeTo(SerializationContext ctx, RawProtoStreamWriter out, A t) throws IOException {
      if (t == null) {
         throw new IllegalArgumentException("Object to marshall cannot be null");
      }
      BaseMarshallerDelegate marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(t.getClass());
      marshallerDelegate.marshall(null, t, null, out);
      out.flush();
   }

   public static void writeTo(SerializationContext ctx, OutputStream out, Object t) throws IOException {
      writeTo(ctx, RawProtoStreamWriterImpl.newInstance(out), t);
   }

   public static byte[] toByteArray(SerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      writeTo(ctx, baos, t);
      return baos.toByteArray();
   }

   public static ByteBuffer toByteBuffer(SerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx();
      writeTo(ctx, baos, t);
      return baos.getByteBuffer();
   }

   private static <A> A readFrom(SerializationContext ctx, RawProtoStreamReader in, Class<A> clazz) throws IOException {
      BaseMarshallerDelegate<A> marshallerDelegate = ((SerializationContextImpl) ctx).getMarshallerDelegate(clazz);
      return marshallerDelegate.unmarshall(null, null, in);
   }

   public static <A> A readFrom(SerializationContext ctx, InputStream in, Class<A> clazz) throws IOException {
      return readFrom(ctx, RawProtoStreamReaderImpl.newInstance(in), clazz);
   }

   public static <A> A fromByteArray(SerializationContext ctx, byte[] bytes, Class<A> clazz) throws IOException {
      return readFrom(ctx, RawProtoStreamReaderImpl.newInstance(bytes), clazz);
   }

   //todo [anistor] what happens with remaining trailing bytes? signal error?
   public static <A> A fromByteArray(SerializationContext ctx, byte[] bytes, int offset, int length, Class<A> clazz) throws IOException {
      return readFrom(ctx, RawProtoStreamReaderImpl.newInstance(bytes, offset, length), clazz);
   }

   public static <A> A fromByteBuffer(SerializationContext ctx, ByteBuffer byteBuffer, Class<A> clazz) throws IOException {
      return readFrom(ctx, RawProtoStreamReaderImpl.newInstance(byteBuffer), clazz);
   }

   /**
    * Parses a top-level message that was wrapped according to the org.infinispan.protostream.WrappedMessage proto
    * definition.
    *
    * @param ctx
    * @param bytes
    * @return
    * @throws IOException
    */
   public static Object fromWrappedByteArray(SerializationContext ctx, byte[] bytes) throws IOException {
      return fromWrappedByteArray(ctx, bytes, 0, bytes.length);
   }

   public static Object fromWrappedByteArray(SerializationContext ctx, byte[] bytes, int offset, int length) throws IOException {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes, offset, length);
      return WrappedMessage.readMessage(ctx, RawProtoStreamReaderImpl.newInstance(bais));
   }

   public static Object fromWrappedByteBuffer(SerializationContext ctx, ByteBuffer byteBuffer) throws IOException {
      return WrappedMessage.readMessage(ctx, RawProtoStreamReaderImpl.newInstance(byteBuffer));
   }

   public static byte[] toWrappedByteArray(SerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      WrappedMessage.writeMessage(ctx, RawProtoStreamWriterImpl.newInstance(baos), t);
      return baos.toByteArray();
   }

   public static ByteBuffer toWrappedByteBuffer(SerializationContext ctx, Object t) throws IOException {
      ByteArrayOutputStreamEx baos = new ByteArrayOutputStreamEx();
      WrappedMessage.writeMessage(ctx, RawProtoStreamWriterImpl.newInstance(baos), t);
      return baos.getByteBuffer();
   }
}

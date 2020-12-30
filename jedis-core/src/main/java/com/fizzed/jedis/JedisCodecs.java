package com.fizzed.jedis;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class JedisCodecs {

    static public final Function<String,byte[]> STRING_ENCODE
        = (String value) -> value.getBytes(StandardCharsets.UTF_8);

    static public final Function<byte[],String> STRING_DECODE
        = (byte[] bytes) -> new String(bytes, StandardCharsets.UTF_8);

//    static public final Function<Object,byte[]> OJBECT_ENCODE = (Object value) -> {
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//            ObjectOutput out = new ObjectOutputStream(baos);
//            out.writeObject(value);
//            out.flush();
//            return baos.toByteArray();
//        } catch (IOException e) {
//            throw new UncheckedIOException(e);
//        }
//    };
//
//    static public final Function<byte[],Object> OJBECT_DECODE = (byte[] bytes) -> {
//        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
//            ObjectInput in = new ObjectInputStream(bais);
//            return in.readObject();
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new UncheckedIOException(e);
//        }
//    };
    
}

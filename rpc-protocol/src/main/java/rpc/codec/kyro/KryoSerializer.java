package rpc.codec.kyro;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import rpc.codec.Serializer;
import rpc.exception.SerializeException;
import rpc.protocol.RpcRequest;
import rpc.protocol.RpcResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @author yhw
 */
@Slf4j
public class KryoSerializer implements Serializer {

    /**
     * 因为Kyro不是线程安全的，所以我们需要ThreadLocal来存储Kyro 对象
     */
    private  final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();

        //要用Kryo进行序列化的类要先注册到Kryo，可以提供性能和减小序列化结果体积
        kryo.register(RpcResponse.class);
        kryo.register(RpcRequest.class);
        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             Output output = new Output(byteArrayOutputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            // Object->byte:将对象序列化为byte数组
            kryo.writeObject(output, obj);
            kryoThreadLocal.remove();
            return output.toBytes();
        } catch (Exception e) {
            throw new SerializeException("Serialization failed");
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            // byte->Object:从byte数组中反序列化出对对象
            Object o = kryo.readObject(input, clazz);
            kryoThreadLocal.remove();
            return clazz.cast(o);
        } catch (Exception e) {
            throw new SerializeException("Deserialization failed");
        }
    }

    public static void main(String[] args) {
        KryoSerializer kryoSerializer = new KryoSerializer();
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestId("!1111");
        byte[] serialize = kryoSerializer.serialize(rpcRequest);
        System.out.println(serialize);
        System.out.println(kryoSerializer.deserialize(serialize,RpcRequest.class));
    }
}

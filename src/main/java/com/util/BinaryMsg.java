package com.util;

/**
 * @author sunrui
 * @date 2018-07-17
 * @descprition
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 二进制消息
 *
 * 支持消息头先解析
 *
 * @author zhenkun.wei
 *
 */
public class BinaryMsg {

    private static Logger logger = LoggerFactory.getLogger(BinaryMsg.class);
    /**
     * 写入的消息体
     */
    private ByteBuf buffer = Unpooled.buffer();
    /**
     * 消息头
     */
    private byte[] head = new byte[0];
    /**
     * 缓存发送的buffer
     */
    private byte[] body;

    private short msgCode;

    public BinaryMsg() {
    }

    public BinaryMsg(short msgCode) {
        this.msgCode = msgCode;
    }

    /**
     * 执行编码
     *
     */
    public byte[] encode() {
        if (body == null) {
            ByteBuf data = Unpooled.buffer(2 + 4 + head.length + buffer.readableBytes());
            data.writeInt(head.length);
            data.writeBytes(head);
            data.writeShort(msgCode);
            data.writeBytes(buffer);
            byte[] body = new byte[data.readableBytes()];
            data.readBytes(body);
            data.release();
            data = null;
            buffer.release();
            buffer = null;
        }
        return this.body;
    }

    /**
     * 执行解码
     */
    public void decode(byte[] body) {
        buffer = Unpooled.copiedBuffer(body);
        int length = buffer.readInt();
        head = new byte[length];
        buffer.readBytes(head);
        msgCode = buffer.readShort();
    }

    /**
     * 以UTF8编码写入字符串
     *
     * @param str
     */
    public void writeString(String str) {
        if (str == null) {
            str = "";
        }
        try {
            byte[] content = str.getBytes("UTF-8");
            buffer.writeInt(content.length);
            buffer.writeBytes(content);
        } catch (Exception e) {
            logger.error("writeString exception:", e);
        }
    }

    /**
     * 写入整型
     *
     * @param value
     */
    public void writeInt(int value) {
        buffer.writeInt(value);
    }

    /**
     * 写入短整型
     *
     * @param value
     */
    public void writeShort(int value) {
        buffer.writeShort((short) value);
    }

    /**
     * 写入字节
     *
     * @param value
     */
    public void writeByte(byte value) {
        buffer.writeByte(value);
    }

    public void writeLong(long value) {
        buffer.writeLong(value);
    }

    public void writeFloat(float value) {
        buffer.writeFloat(value);
    }

    public void writeDouble(double value) {
        buffer.writeDouble(value);
    }

    public void writeObject(Object value) {
        String lastValue = "";
        if (value instanceof String) {
            lastValue = (String) value;
        } else {
            lastValue = JSON.toJSONString(value);
        }
        writeString(lastValue);
    }

    public void writeBytes(byte[] bytes) {
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
    }

    public String readString() {
        try {
            int length = buffer.readInt();
            byte[] b = new byte[length];
            buffer.readBytes(b);
            return new String(b,"UTF-8");
        } catch (Exception e) {
            logger.error("readString error", e);
            return null;
        }
    }

    public int readInt() {
        return this.buffer.readInt();
    }

    public short readShort() {
        return this.buffer.readShort();
    }

    public byte readByte() {
        return buffer.readByte();
    }

    public long readLong() {
        return buffer.readLong();
    }

    public float readFloat() {
        return buffer.readFloat();
    }

    public double readDouble() {
        return buffer.readDouble();
    }

    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> clazz) {
        try {
            String value = readString();
            if (value != null) {
                if (String.class.equals(clazz)) {
                    return (T) value;
                } else {
                    return JSON.parseObject(value, clazz);
                }
            }
        } catch (Exception e) {
            logger.error("readObject error", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T readObject(TypeReference<T> type) {
        try {
            String value = readString();
            if (value != null) {
                if (String.class.equals(type.getType())) {
                    return (T) value;
                } else {
                    return JSON.parseObject(value, type);
                }
            }
        } catch (Exception e) {
            logger.error("readObject error", e);
        }
        return null;
    }

    public byte[] readByteArray() {
        int length = buffer.readInt();
        byte[] res = new byte[length];
        buffer.readBytes(res);
        return res;
    }

    public short getMsgCode() {
        return msgCode;
    }

    public byte[] readHead() {
        return head;
    }

    public void writeHead(byte[] head) {
        this.head = head;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BinaryMsg [msgCode=").append(msgCode).append("]");
        return builder.toString();
    }

}

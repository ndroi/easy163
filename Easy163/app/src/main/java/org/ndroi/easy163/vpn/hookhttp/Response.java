package org.ndroi.easy163.vpn.hookhttp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class Response
{
    private static class Chunks
    {
        private static class Chunk
        {
            private byte[] data;

            public Chunk(int size)
            {
                data = new byte[size];
            }

            public void setData(byte[] bytes)
            {
                setData(bytes, 0, bytes.length);
            }

            public void setData(byte[] bytes, int offset, int length)
            {
                System.arraycopy(bytes, offset, data, 0, length);
            }

            public byte[] getData()
            {
                return data;
            }

            public int getSize()
            {
                return data.length;
            }
        }

        private enum Status
        {
            RECV_LENGTH,
            RECV_CONTENT,
            RECV_OVER,
        }

        private ByteArrayOutputStream remainingStream = new ByteArrayOutputStream();
        private List<Chunk> chunks = new ArrayList<>();
        private Status status = Status.RECV_LENGTH;

        private Chunk getCurrent()
        {
            if (chunks.isEmpty())
            {
                return null;
            }
            return chunks.get(chunks.size() - 1);
        }

        private int checkCRLF(byte[] bytes)
        {
            for (int i = 0; i < bytes.length - 3; i++)
            {
                if (bytes[i] == 13 && bytes[i + 1] == 10)
                {
                    return i;
                }
            }
            return -1;
        }

        private void addNew(int length)
        {
            chunks.add(new Chunk(length));
        }

        public void putBytes(byte[] bytes)
        {
            remainingStream.write(bytes, 0, bytes.length);
            while (true)
            {
                if (status == Status.RECV_LENGTH)
                {
                    bytes = remainingStream.toByteArray();
                    int crlf = checkCRLF(bytes);
                    if (crlf != -1)
                    {
                        int length = Integer.parseInt(new String(bytes, 0, crlf), 16);
                        addNew(length);
                        remainingStream.reset();
                        int offset = crlf + 2;
                        remainingStream.write(bytes, offset, bytes.length - offset);
                        status = Status.RECV_CONTENT;
                    } else
                    {
                        break;
                    }
                }
                if (status == Status.RECV_CONTENT)
                {
                    Chunk curChunk = getCurrent();
                    int chunkSize = curChunk.getSize();
                    if (remainingStream.size() >= chunkSize + 2)
                    {
                        bytes = remainingStream.toByteArray();
                        curChunk.setData(bytes, 0, chunkSize);
                        remainingStream.reset();
                        int offset = chunkSize + 2;
                        remainingStream.write(bytes, offset, bytes.length - offset);
                        if (chunkSize == 0)
                        {
                            status = Status.RECV_OVER;
                            break;
                        } else
                        {
                            status = Status.RECV_LENGTH;
                        }
                    } else
                    {
                        break;
                    }
                }
            }
        }

        public boolean finished()
        {
            return status == Status.RECV_OVER;
        }

        public byte[] dump()
        {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (Chunk chunk : chunks)
            {
                byteArrayOutputStream.write(chunk.getData(), 0, chunk.getSize());
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private Map<String, String> headerFields = new LinkedHashMap<>();
    private int headerLen = 0;
    private String version;
    private String code;
    private String desc;
    private byte[] content = null;
    private Chunks chunks = null;

    private boolean headerReceived()
    {
        return headerLen != 0;
    }

    public boolean finished()
    {
        if (headerLen == 0)
        {
            return false;
        }
        if (chunks == null)
        {
            int contentLen = (content == null ? 0 : content.length);
            return byteArrayOutputStream.size() >= contentLen;
        }
        return chunks.finished();
    }

    public void putBytes(byte[] bytes)
    {
        putBytes(bytes, 0, bytes.length);
    }

    public Map<String, String> getHeaderFields()
    {
        return headerFields;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getDesc()
    {
        return desc;
    }

    public void setDesc(String desc)
    {
        this.desc = desc;
    }

    public byte[] getContent()
    {
        return content;
    }

    public void setContent(byte[] content)
    {
        this.content = content;
        headerFields.put("Content-Length", "" + content.length);
    }

    public void putBytes(byte[] bytes, int offset, int length)
    {
        if (finished()) return;
        byteArrayOutputStream.write(bytes, offset, length);
        if (!headerReceived())
        {
            tryDecode();
            if (headerReceived())
            {
                bytes = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.reset();
                byteArrayOutputStream.write(bytes, headerLen, bytes.length - headerLen);
                bytes = byteArrayOutputStream.toByteArray();
            }
        }
        if (chunks != null && !chunks.finished())
        {
            chunks.putBytes(bytes);
        }
        if (finished())
        {
            if (content != null)
            {
                content = byteArrayOutputStream.toByteArray();
            }
            if (chunks != null)
            {
                content = chunks.dump();
                headerFields.remove("Transfer-Encoding");
                if (headerFields.containsKey("Content-Encoding"))
                {
                    content = unzip(content);
                    headerFields.remove("Content-Encoding");
                }
                headerFields.put("Content-Length", "" + content.length);
            }
        }
    }

    private byte[] unzip(byte[] bytes)
    {
        byte[] result = null;
        try
        {
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (true)
            {
                int len = gzipInputStream.read(buffer);
                if (len == -1) break;
                byteArrayOutputStream.write(buffer, 0, len);
            }
            gzipInputStream.close();
            byteArrayOutputStream.close();
            result = byteArrayOutputStream.toByteArray();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return result;
    }


    private void tryDecode()
    {
        int crlf = checkCRLF();
        if (crlf != -1)
        {
            headerLen = crlf + 4;
            decode();
            if (headerFields.containsKey("Content-Length"))
            {
                int contentLen = Integer.parseInt(headerFields.get("Content-Length"));
                content = new byte[contentLen];
            } else if (headerFields.containsKey("Transfer-Encoding"))
            {
                chunks = new Chunks();
            }
        }
    }

    private int checkCRLF()
    {
        byte[] bytes = byteArrayOutputStream.toByteArray();
        for (int i = 0; i < bytes.length - 3; i++)
        {
            if (bytes[i] == 13 && bytes[i + 1] == 10 &&
                    bytes[i + 2] == 13 && bytes[i + 3] == 10)
            {
                return i;
            }
        }
        return -1;
    }

    private void decode()
    {
        byte[] bytes = byteArrayOutputStream.toByteArray();
        String headerStr = new String(bytes, 0, headerLen - 4);
        String[] lines = headerStr.split("\r\n");
        String requestLine = lines[0];
        int _p = requestLine.indexOf(' ');
        version = requestLine.substring(0, _p);
        requestLine = requestLine.substring(_p + 1);
        _p = requestLine.indexOf(' ');
        code = requestLine.substring(0, _p);
        desc = requestLine.substring(_p + 1);
        for (int i = 1; i < lines.length; i++)
        {
            String line = lines[i];
            _p = line.indexOf(':');
            String key = line.substring(0, _p).trim();
            String value = line.substring(_p + 1).trim();
            headerFields.put(key, value);
        }
    }

    private void encode()
    {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(version + " " + code + " " + desc + "\r\n");
        for (String key : headerFields.keySet())
        {
            String value = headerFields.get(key);
            stringBuffer.append(key + ": " + value + "\r\n");
        }
        stringBuffer.append("\r\n");
        try
        {
            byteArrayOutputStream.reset();
            byteArrayOutputStream.write(stringBuffer.toString().getBytes());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public byte[] dump()
    {
        encode();
        if (content != null)
        {
            try
            {
                byteArrayOutputStream.write(content);
                byteArrayOutputStream.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return byteArrayOutputStream.toByteArray();
    }
}

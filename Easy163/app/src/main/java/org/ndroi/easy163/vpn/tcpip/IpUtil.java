package org.ndroi.easy163.vpn.tcpip;

import org.ndroi.easy163.vpn.util.ByteBufferPool;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class IpUtil
{
    public static Packet buildUdpPacket(InetSocketAddress source, InetSocketAddress dest, int ipId)
    {
        Packet packet = new Packet();
        packet.isTCP = false;
        packet.isUDP = true;
        Packet.IP4Header ip4Header = new Packet.IP4Header();
        ip4Header.version = 4;
        ip4Header.IHL = 5;
        ip4Header.destinationAddress = dest.getAddress();
        ip4Header.headerChecksum = 0;
        ip4Header.headerLength = 20;

        //int ipId=0;
        int ipFlag = 0x40;
        int ipOff = 0;

        ip4Header.identificationAndFlagsAndFragmentOffset = ipId << 16 | ipFlag << 8 | ipOff;

        ip4Header.optionsAndPadding = 0;
        ip4Header.protocol = Packet.IP4Header.TransportProtocol.UDP;
        ip4Header.protocolNum = 17;
        ip4Header.sourceAddress = source.getAddress();
        ip4Header.totalLength = 60;
        ip4Header.typeOfService = 0;
        ip4Header.TTL = 64;

        Packet.UDPHeader udpHeader = new Packet.UDPHeader();
        udpHeader.sourcePort = source.getPort();
        udpHeader.destinationPort = dest.getPort();
        udpHeader.length = 0;

        ByteBuffer byteBuffer = ByteBufferPool.acquire();
        byteBuffer.flip();

        packet.ip4Header = ip4Header;
        packet.udpHeader = udpHeader;
        packet.backingBuffer = byteBuffer;
        return packet;
    }

    public static Packet buildTcpPacket(InetSocketAddress source, InetSocketAddress dest, byte flag, long ack, long seq, int ipId)
    {
        Packet packet = new Packet();
        packet.isTCP = true;
        packet.isUDP = false;
        Packet.IP4Header ip4Header = new Packet.IP4Header();
        ip4Header.version = 4;
        ip4Header.IHL = 5;
        ip4Header.destinationAddress = dest.getAddress();
        ip4Header.headerChecksum = 0;
        ip4Header.headerLength = 20;

        //int ipId=0;
        int ipFlag = 0x40;
        int ipOff = 0;

        ip4Header.identificationAndFlagsAndFragmentOffset = ipId << 16 | ipFlag << 8 | ipOff;

        ip4Header.optionsAndPadding = 0;
        ip4Header.protocol = Packet.IP4Header.TransportProtocol.TCP;
        ip4Header.protocolNum = 6;
        ip4Header.sourceAddress = source.getAddress();
        ip4Header.totalLength = 60;
        ip4Header.typeOfService = 0;
        ip4Header.TTL = 64;

        Packet.TCPHeader tcpHeader = new Packet.TCPHeader();
        tcpHeader.acknowledgementNumber = ack;
        tcpHeader.checksum = 0;
        tcpHeader.dataOffsetAndReserved = -96;
        tcpHeader.destinationPort = dest.getPort();
        tcpHeader.flags = flag;
        tcpHeader.headerLength = 40;
        tcpHeader.optionsAndPadding = null;
        tcpHeader.sequenceNumber = seq;
        tcpHeader.sourcePort = source.getPort();
        tcpHeader.urgentPointer = 0;
        tcpHeader.window = 65535;

        ByteBuffer byteBuffer = ByteBufferPool.acquire();
        byteBuffer.flip();

        packet.ip4Header = ip4Header;
        packet.tcpHeader = tcpHeader;
        packet.backingBuffer = byteBuffer;
        return packet;
    }
}

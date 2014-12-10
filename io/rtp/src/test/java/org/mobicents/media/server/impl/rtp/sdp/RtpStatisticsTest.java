package org.mobicents.media.server.impl.rtp.sdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.net.ntp.TimeStamp;
import org.junit.Test;
import org.mobicents.media.server.impl.rtcp.RtcpBye;
import org.mobicents.media.server.impl.rtcp.RtcpPacket;
import org.mobicents.media.server.impl.rtcp.RtcpPacketFactory;
import org.mobicents.media.server.impl.rtcp.RtcpPacketType;
import org.mobicents.media.server.impl.rtcp.RtcpSdes;
import org.mobicents.media.server.impl.rtcp.RtcpSdesChunk;
import org.mobicents.media.server.impl.rtcp.RtcpSdesItem;
import org.mobicents.media.server.impl.rtcp.RtcpSenderReport;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.impl.rtp.WallTestClock;
import org.mobicents.media.server.impl.rtp.statistics.RtpMember;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class RtpStatisticsTest {
	
	private final String CNAME = "127.0.0.1";
	private final long SSRC = 123456789L;
	
	private final WallTestClock wallClock;
	private final RtpClock rtpClock;
	
	public RtpStatisticsTest() {
		wallClock = new WallTestClock();
		rtpClock = new RtpClock(wallClock);
		rtpClock.setClockRate(8000);
	}

	@Test
	public void testInitialization() {
		// given
		RtpStatistics stats = new RtpStatistics(rtpClock, SSRC, CNAME);

		// then
		assertTrue(stats.getSsrc() > 0);
		
		assertEquals(0, stats.getRtpPacketsReceived());
		assertEquals(0, stats.getRtpOctetsReceived());
		assertEquals(0, stats.getRtpPacketsSent());
		assertEquals(0, stats.getRtpOctetsSent());

		assertEquals(0, stats.getSenders());
		assertFalse(stats.isSender(stats.getSsrc()));
		assertEquals(1, stats.getPmembers());
		assertEquals(1, stats.getMembers());
		assertEquals(1, stats.getMembers());
		assertNotNull(stats.getMember(stats.getSsrc()));
		assertTrue(stats.getRtcpAvgSize() > 0);
	}

	@Test
	public void testOnRtpSent() {
		// given
		RtpStatistics stats = new RtpStatistics(rtpClock, SSRC, CNAME);
		RtpPacket p1 = new RtpPacket(172, false);
		RtpPacket p2 = new RtpPacket(172, false);
		p1.wrap(false, 8, 1, 160 * 1, stats.getSsrc(), new byte[160], 0, 160);
		p2.wrap(false, 8, 1, 160 * 2, stats.getSsrc(), new byte[160], 0, 160);
		
		// when
		stats.onRtpSent(p1);
		wallClock.tick(20000000L);
		stats.onRtpSent(p2);
		
		// then
		assertEquals(2, stats.getRtpPacketsSent());
		assertEquals(p1.getLength() + p2.getLength(), stats.getRtpOctetsSent());
		assertEquals(wallClock.getCurrentTime(), stats.getRtpSentOn());
		assertTrue(stats.hasSent());
		assertTrue(stats.isSender(stats.getSsrc()));
		assertEquals(1, stats.getSenders());
	}

	@Test
	public void testOnRtpReceive() {
		// given
		RtpStatistics stats = new RtpStatistics(rtpClock, SSRC, CNAME);
		RtpPacket p1 = new RtpPacket(172, false);
		RtpPacket p2 = new RtpPacket(172, false);
		p1.wrap(false, 8, 1, 160 * 1, 123, new byte[160], 0, 160);
		p2.wrap(false, 8, 1, 160 * 2, 456, new byte[160], 0, 160);
		
		// when
		stats.onRtpReceive(p1);
		wallClock.tick(20000000L);
		stats.onRtpReceive(p2);
		
		// then
		assertEquals(2, stats.getRtpPacketsReceived());
		assertEquals(p1.getLength() + p2.getLength(), stats.getRtpOctetsReceived());
		assertEquals(wallClock.getTime(), stats.getRtpReceivedOn());
		assertFalse(stats.hasSent());
		assertEquals(2, stats.getSenders());
		assertTrue(stats.isSender(p1.getSyncSource()));
		assertTrue(stats.isMember(p1.getSyncSource()));
		assertNotNull(stats.getMember(p1.getSyncSource()));
		assertTrue(stats.isSender(p2.getSyncSource()));
		assertTrue(stats.isMember(p2.getSyncSource()));
		assertNotNull(stats.getMember(p2.getSyncSource()));
		assertEquals(3, stats.getMembers());
		assertEquals(1, stats.getPmembers());
	}
	
	@Test
	public void testOnRtcpSent() {
		// given
		RtpStatistics stats = new RtpStatistics(rtpClock, SSRC, CNAME);
		RtcpPacket p1 = RtcpPacketFactory.buildReport(stats);
		RtcpPacket p2 = RtcpPacketFactory.buildReport(stats);
		
		double initialAvgSize = stats.getRtcpAvgSize();
		
		// when
		// Need to encode the packets first (as required per sending over datagram channel)
		// so that the RtcpPacket.size variable is initialized!
		p1.encode(new byte[RtpPacket.RTP_PACKET_MAX_SIZE], 0);
		p2.encode(new byte[RtpPacket.RTP_PACKET_MAX_SIZE], 0);
		
		stats.onRtcpSent(p1);
		wallClock.tick(20000000L);
		stats.onRtcpSent(p2);
		
		// then
		double avgSize = calculateAvgSize(initialAvgSize, p1.getSize(), p2.getSize());
		assertEquals(avgSize, stats.getRtcpAvgSize(), 0);
	}
	
	@Test
	public void testOnRtcpReceiveReport() {
		// given
		RtpStatistics stats = new RtpStatistics(rtpClock, SSRC, CNAME);
		double initialAvgSize = stats.getRtcpAvgSize();
		stats.setRtcpPacketType(RtcpPacketType.RTCP_REPORT);
		
		RtpPacket rtp1 = new RtpPacket(172, false);
		RtpPacket rtp2 = new RtpPacket(172, false);
		rtp1.wrap(false, 8, 1, 160 * 1, 123, new byte[160], 0, 160);
		rtp2.wrap(false, 8, 1, 160 * 2, 123, new byte[160], 0, 160);
		
		TimeStamp ntp = new TimeStamp(System.currentTimeMillis());
		RtcpSenderReport sr = new RtcpSenderReport(false, 123, ntp.getSeconds(), ntp.getFraction(), 160 * 1, 5, 5 * 160);
		RtcpSdes sdes = new RtcpSdes(false);
		RtcpSdesChunk sdesChunk = new RtcpSdesChunk(123);
		sdesChunk.addRtcpSdesItem(new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, CNAME));
		sdes.addRtcpSdesChunk(sdesChunk);
		RtcpPacket rtcp1 = new RtcpPacket(sr, sdes);
		// Need to encode the packets first (as required per sending over the network)
		// so that the RtcpPacket.size variable is initialized!
		rtcp1.encode(new byte[RtpPacket.RTP_PACKET_MAX_SIZE], 0);
		
		// when
		stats.onRtpReceive(rtp1);
		wallClock.tick(20000000L);
		stats.onRtpReceive(rtp2);
		wallClock.tick(20000000L);
		stats.onRtcpReceive(rtcp1);
		
		// then
		RtpMember member = stats.getMember(123);
		assertTrue(stats.isMember(sr.getSsrc()));
		assertNotNull(member);
		assertEquals(rtp2.getSeqNumber(), member.getExtHighSequence());
		assertEquals(0, member.getReceivedSinceSR());
		
		double avgSize = calculateAvgSize(initialAvgSize, rtcp1.getSize());
		assertEquals(avgSize, stats.getRtcpAvgSize(), 0);
	}
	
	@Test
	public void testOnRtcpReceiveReportWithByeScheduled() {
		// given
		RtpStatistics stats = new RtpStatistics(rtpClock, SSRC, CNAME);
		double initialAvgSize = stats.getRtcpAvgSize();
		stats.setRtcpPacketType(RtcpPacketType.RTCP_BYE);
		
		RtpPacket rtp1 = new RtpPacket(172, false);
		RtpPacket rtp2 = new RtpPacket(172, false);
		rtp1.wrap(false, 8, 1, 160 * 1, 123, new byte[160], 0, 160);
		rtp2.wrap(false, 8, 1, 160 * 2, 123, new byte[160], 0, 160);
		
		TimeStamp ntp = new TimeStamp(System.currentTimeMillis());
		RtcpSenderReport sr = new RtcpSenderReport(false, 123, ntp.getSeconds(), ntp.getFraction(), 160 * 1, 5, 5 * 160);
		RtcpSdes sdes = new RtcpSdes(false);
		RtcpSdesChunk sdesChunk = new RtcpSdesChunk(123);
		sdesChunk.addRtcpSdesItem(new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, CNAME));
		sdes.addRtcpSdesChunk(sdesChunk);
		RtcpPacket rtcp1 = new RtcpPacket(sr, sdes);
		// Need to encode the packets first (as required per sending over the network)
		// so that the RtcpPacket.size variable is initialized!
		rtcp1.encode(new byte[RtpPacket.RTP_PACKET_MAX_SIZE], 0);
		
		// when
		stats.onRtpReceive(rtp1);
		wallClock.tick(20000000L);
		stats.onRtpReceive(rtp2);
		wallClock.tick(20000000L);
		stats.onRtcpReceive(rtcp1);
		
		// then
		RtpMember member = stats.getMember(123);
		assertFalse(stats.isMember(sr.getSsrc()));
		assertNull(member);
		
		double avgSize = calculateAvgSize(initialAvgSize, rtcp1.getSize());
		assertEquals(avgSize, stats.getRtcpAvgSize(), 0);
	}
	
	private double calculateAvgSize(double initialSize, int ...packetSizes) {
		double avgSize = initialSize; 
		for (int size : packetSizes) {
			avgSize = (1.0 / 16.0) * size + (15.0 / 16.0) * avgSize;
		}
		return avgSize;
	}
	
	@Test
	public void testOnRtcpReceiveBye() {
		// given
		RtpStatistics stats = new RtpStatistics(rtpClock, SSRC, CNAME);
		double initialAvgSize = stats.getRtcpAvgSize();
		stats.setRtcpPacketType(RtcpPacketType.RTCP_REPORT);
		
		RtpPacket rtp1 = new RtpPacket(172, false);
		RtpPacket rtp2 = new RtpPacket(172, false);
		rtp1.wrap(false, 8, 1, 160 * 1, 123, new byte[160], 0, 160);
		rtp2.wrap(false, 8, 1, 160 * 2, 123, new byte[160], 0, 160);
		
		TimeStamp ntp = new TimeStamp(System.currentTimeMillis());
		RtcpSenderReport sr = new RtcpSenderReport(false, 123, ntp.getSeconds(), ntp.getFraction(), 160 * 1, 5, 5 * 160);
		RtcpSdes sdes = new RtcpSdes(false);
		RtcpSdesChunk sdesChunk = new RtcpSdesChunk(123);
		sdesChunk.addRtcpSdesItem(new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, CNAME));
		sdes.addRtcpSdesChunk(sdesChunk);
		RtcpPacket rtcp1 = new RtcpPacket(sr, sdes);

		RtcpBye bye = new RtcpBye(false);
		RtcpPacket rtcp2 = new RtcpPacket(sr, sdes, bye);
		
		// Need to encode the packets first (as required per sending over the network)
		// so that the RtcpPacket.size variable is initialized!
		rtcp1.encode(new byte[RtpPacket.RTP_PACKET_MAX_SIZE], 0);
		
		// when (1) - receive RTP and RTCP SR
		stats.onRtpReceive(rtp1); // Adds to the senders and members list
		wallClock.tick(20000000L);
		stats.onRtcpReceive(rtcp1);
		wallClock.tick(20000000L);
		stats.onRtpReceive(rtp2);

		// then (1) - sender 123 is registered
		RtpMember member = stats.getMember(123);
		assertTrue(stats.isMember(sr.getSsrc()));
		assertNotNull(member);
		assertEquals(rtp2.getSeqNumber(), member.getExtHighSequence());
		assertEquals(1, member.getReceivedSinceSR());
		
		double avgSize = calculateAvgSize(initialAvgSize, rtcp1.getSize());
		assertEquals(avgSize, stats.getRtcpAvgSize(), 0);
		
		// when (2) - receive RTCP BYE
		wallClock.tick(30000000L);
		stats.onRtcpReceive(rtcp2);
		
		// then (2) - sender is deregistered
		member = stats.getMember(123);
		assertFalse(stats.isMember(sr.getSsrc()));
		assertNull(member);
		assertFalse(stats.isSender(sr.getSsrc()));
		avgSize = calculateAvgSize(avgSize, rtcp2.getSize());
		assertEquals(avgSize, stats.getRtcpAvgSize(), 0);
	}
	
	@Test
	public void testOnRtcpReceiveByeWithByeScheduled() {
		// given
		RtpStatistics stats = new RtpStatistics(rtpClock, SSRC, CNAME);
		double initialAvgSize = stats.getRtcpAvgSize();
		stats.setRtcpPacketType(RtcpPacketType.RTCP_REPORT);
		
		RtpPacket rtp1 = new RtpPacket(172, false);
		RtpPacket rtp2 = new RtpPacket(172, false);
		rtp1.wrap(false, 8, 1, 160 * 1, 123, new byte[160], 0, 160);
		rtp2.wrap(false, 8, 1, 160 * 2, 123, new byte[160], 0, 160);
		
		TimeStamp ntp = new TimeStamp(System.currentTimeMillis());
		RtcpSenderReport sr = new RtcpSenderReport(false, 123, ntp.getSeconds(), ntp.getFraction(), 160 * 1, 5, 5 * 160);
		RtcpSdes sdes = new RtcpSdes(false);
		RtcpSdesChunk sdesChunk = new RtcpSdesChunk(123);
		sdesChunk.addRtcpSdesItem(new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, CNAME));
		sdes.addRtcpSdesChunk(sdesChunk);
		RtcpPacket rtcp1 = new RtcpPacket(sr, sdes);

		RtcpBye bye = new RtcpBye(false);
		RtcpPacket rtcp2 = new RtcpPacket(sr, sdes, bye);
		
		// Need to encode the packets first (as required per sending over the network)
		// so that the RtcpPacket.size variable is initialized!
		rtcp1.encode(new byte[RtpPacket.RTP_PACKET_MAX_SIZE], 0);
		
		// when (1) - receive RTP and RTCP SR
		stats.onRtpReceive(rtp1); // Adds to the senders and members list
		wallClock.tick(20000000L);
		stats.onRtcpReceive(rtcp1);
		wallClock.tick(20000000L);
		stats.onRtpReceive(rtp2);
		
		// then (1) - sender 123 is registered
		int memberCount = stats.getMembers();
		RtpMember member = stats.getMember(123);
		assertTrue(stats.isMember(sr.getSsrc()));
		assertNotNull(member);
		assertEquals(rtp2.getSeqNumber(), member.getExtHighSequence());
		assertEquals(1, member.getReceivedSinceSR());
		
		double avgSize = calculateAvgSize(initialAvgSize, rtcp1.getSize());
		assertEquals(avgSize, stats.getRtcpAvgSize(), 0);
		
		// when (2) - receive RTCP BYE
		// notice RTCP BYE is scheduled in the meanwhile
		stats.setRtcpPacketType(RtcpPacketType.RTCP_BYE);
		wallClock.tick(30000000L);
		stats.onRtcpReceive(rtcp2);
		
		// then (2) - sender is kept but members is updated
		member = stats.getMember(123);
		assertTrue(stats.isMember(sr.getSsrc()));
		assertNotNull(member);
		assertTrue(stats.isSender(sr.getSsrc()));
		assertEquals(memberCount + 1, stats.getMembers());
		avgSize = calculateAvgSize(avgSize, rtcp2.getSize());
		assertEquals(avgSize, stats.getRtcpAvgSize(), 0);
	}
	
}
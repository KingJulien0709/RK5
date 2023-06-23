import fau.cs7.nwemu.*;
import java.util.LinkedList;

public class GoBackN {
    static int nsimmax = 100;
    static double lossprob = 0.2;
    static double corruptprob = 0.2;
    static double lambda = 10;

    public static void main(String[] args) {
        Sender1 sender = new Sender1();
        Receiver1 receiver = new Receiver1();
        NWEmu nwEmu = new NWEmu(sender, receiver);
        nwEmu.emulate(nsimmax, lossprob, corruptprob, lambda, 2);
    }
}

class Sender1 extends AbstractHost {
    private int base;
    private int nextSeqNum;
    private LinkedList<NWEmuPkt> buffer;

    @Override
    public Boolean output(NWEmuMsg message) {
        // Check if the buffer is full
        if (buffer.size() >= 8) {
            return false;
        }

        // Create a new packet with the message and the current sequence number
        NWEmuPkt packet = new NWEmuPkt();
        packet.seqnum = nextSeqNum;
        packet.payload = message.data;

        // Add the packet to the buffer
        buffer.add(packet);

        // If the packet is the first in the window, start the timer
        if (nextSeqNum == base) {
            startTimer(100);
        }

        // Send the packet
        toLayer3(packet);

        // Increment the next sequence number
        nextSeqNum++;

        return true;
    }

    @Override
    public void input(NWEmuPkt packet) {
        // Check if the received packet is the one expected (in-order)
        if (packet.acknum >= base && packet.acknum < base + 8) {
            // Update the base to the next sequence number after the acknowledged packet
            base = packet.acknum + 1;

            // Remove acknowledged packets from the buffer
            while (!buffer.isEmpty() && buffer.peek().seqnum <= packet.acknum) {
                buffer.remove();
            }

            // If there are still packets in the buffer, restart the timer
            if (!buffer.isEmpty()) {
                startTimer(100);
            }
        }
    }

    @Override
    public void timerInterrupt() {
        // Resend all packets in the buffer
        for (NWEmuPkt packet : buffer) {
            toLayer3(packet);
        }

        // Restart the timer
        startTimer(100);
    }

    @Override
    public void init() {
        base = 0;
        nextSeqNum = 0;
        buffer = new LinkedList<>();
    }
}

class Receiver1 extends AbstractHost {
    private int expectedSeqNum;

    @Override
    public void input(NWEmuPkt packet) {
        // Check if the received packet is the one expected
        if (packet.seqnum == expectedSeqNum) {
            // Deliver the message to layer 5
            NWEmuMsg message = new NWEmuMsg();
            message.data = packet.payload;
            toLayer5(message);

            // Send an acknowledgment packet with the received sequence number
            NWEmuPkt ackPacket = new NWEmuPkt();
            ackPacket.acknum = expectedSeqNum;
            toLayer3(ackPacket);

            // Increment the expected sequence number
            expectedSeqNum++;
        } else {
            // Send an acknowledgment packet with the last in-order sequence number
            NWEmuPkt ackPacket = new NWEmuPkt();
            ackPacket.acknum = expectedSeqNum - 1;
            toLayer3(ackPacket);
        }
    }

    @Override
    public void init() {
        expectedSeqNum = 0;
    }
}


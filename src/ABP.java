import fau.cs7.nwemu.*;
public class ABP {
    static int nsimmax = 100;
    static double lossprob = 0;
    static double corruptprob = 0.5;
    static double lambda = 1000;


    public static void main(String[] args) {
        Sender sender = new Sender();
        Receiver receiver = new Receiver();
        NWEmu nwEmu = new NWEmu(sender, receiver);
        nwEmu.emulate(nsimmax, lossprob, corruptprob, lambda,3);
    }
}

class Sender extends AbstractHost {
    private int sequenceNumber = 0;
    private NWEmuPkt lastPacket;

    @Override
    public Boolean output(NWEmuMsg message) {
        // If the last packet has not been acknowledged, return false
        if (lastPacket != null && lastPacket.seqnum == sequenceNumber) {
            return false;
        }

        // Create a packet with the message and the current sequence number
        NWEmuPkt packet = new NWEmuPkt();
        packet.seqnum = sequenceNumber;
        packet.payload = message.data;
        lastPacket = packet;

        // Send the packet
        toLayer3(packet);

        // Start the timer
        startTimer(100);

        return true;
    }

    @Override
    public void input(NWEmuPkt packet) {
        // Check the ACK number in the packet
        if (packet.acknum == sequenceNumber) {
            // If it matches the current sequence number, stop the timer
            stopTimer();

            // Switch the sequence number
            sequenceNumber = 1 - sequenceNumber;
        }
    }

    @Override
    public void timerInterrupt() {
        // Resend the last packet
        toLayer3(lastPacket);

        // Restart the timer
        startTimer(100);
    }

    @Override
    public void init() {
        // Perform any necessary initializations
    }
}

class Receiver extends AbstractHost {
    private int expectedSequenceNumber;
    private int correctPackets;
    @Override
    public void input(NWEmuPkt packet) {

        // Check the sequence number in the packet
        if (packet.seqnum == expectedSequenceNumber){
            // If it matches the expected sequence number, deliver the message to layer 5
            NWEmuMsg message = new NWEmuMsg();
            message.data = packet.payload;
            toLayer5(message);

            NWEmuPkt ackPacket = new NWEmuPkt();
            ackPacket.acknum = expectedSequenceNumber;
            toLayer3(ackPacket);

            // Switch the expected sequence number
            expectedSequenceNumber = 1 - expectedSequenceNumber;

            //correctPackets++;
            //if(correctPackets==10){System.exit(0); }
        }
    }

    @Override
    public void init() {
        correctPackets = 0;
        expectedSequenceNumber = 0;
        // Perform any necessary initializations
    }
}
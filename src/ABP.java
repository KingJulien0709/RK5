import fau.cs7.nwemu.*;

public class ABP {

    public static void main(String[] args) {

        // Declare stuff needed by sender AND receiver in this class
        class CommonHost extends AbstractHost {
            int seqnum;
            int acknum;
        }

        // Class representing the sender
        class SendingHost extends CommonHost {
            public void init() {
                sysLog(0, "Sending Host: init()");
                seqnum = 0;
                acknum = -1;
            }

            public Boolean output(NWEmuMsg message) {
                if (isPacketInTransmission()) {
                    sysLog(0, "Sending Host: output(" + message + ") -> Discarded (Packet in transmission)");
                    return false;
                }

                NWEmuPkt sndpkt = new NWEmuPkt();
                for (int i = 0; i < NWEmu.PAYSIZE; i++) {
                    sndpkt.payload[i] = message.data[i];
                }
                sndpkt.seqnum = seqnum++;
                sndpkt.acknum = acknum;
                sndpkt.checksum = 0;
                sysLog(0, "Sending Host: output(" + message + ") -> toLayer3(" + sndpkt + ")");
                toLayer3(sndpkt);

                return true;
            }

            private boolean isPacketInTransmission() {
                // Check if a packet is still in transmission
                // Return true if a packet is being transmitted, false otherwise
                // Implement your logic here
                return false; // Modify this line with your logic
            }

            public void input(NWEmuPkt pkt) {
                sysLog(0, "Sending Host: input(" + pkt + ")");
            }

            public void timerInterrupt() {
                sysLog(0, "Sending Host: timerInterrupt()");
            }
        }

        // Class representing the receiver
        class ReceivingHost extends CommonHost {
            private int expectedSeqnum; // Variable to track the expected sequence number

            public void init() {
                sysLog(0, "Receiving Host: init()");
                seqnum = -1;
                acknum = 0;
                expectedSeqnum = 0; // Initialize the expected sequence number
            }

            public void input(NWEmuPkt pkt) {
                if (pkt.seqnum == expectedSeqnum) {
                    NWEmuMsg message = new NWEmuMsg();
                    for (int i = 0; i < NWEmu.PAYSIZE; i++) {
                        message.data[i] = pkt.payload[i];
                    }
                    sysLog(0, "Receiving Host: input(" + pkt + ") -> toLayer5(" + message + ")");
                    toLayer5(message);

                    expectedSeqnum = (expectedSeqnum + 1) % 2; // Update the expected sequence number
                } else {
                    sysLog(0, "Receiving Host: input(" + pkt + ") -> Discarded (Out-of-order packet)");
                }
            }

            public void timerInterrupt() {
                sysLog(0, "Receiving Host: timerInterrupt()");
            }
        }

        // Instantiate sender and receiver
        SendingHost HostA = new SendingHost();
        ReceivingHost HostB = new ReceivingHost();

        // Perform emulation
        NWEmu TestEmu = new NWEmu(HostA, HostB);
        TestEmu.randTimer();
        TestEmu.emulate(10, 0.1, 0.3, 1000.0, 2);
        // Send 10 messages, loss probability 0.1, error probability 0.3, lambda 1000, log level 2
    }
}

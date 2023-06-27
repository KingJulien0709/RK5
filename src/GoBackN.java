import fau.cs7.nwemu.*;
import java.util.LinkedList;

public class GoBackN {

    public static void main(String[] args) {

        // declare stuff needed by sender AND receiver in this class
        class CommonHost extends AbstractHost {
            int seqnum;
            int acknum;
        }

        // class representing the sender
        class SendingHost extends CommonHost {
            private LinkedList<NWEmuPkt> buffer;
            private int base;
            private int nextSeqnum;
            private double timeout;

            public void init() {
                sysLog(0, "Sending Host: init()");
                seqnum = 0;
                acknum = -1;
                buffer = new LinkedList<>();
                base = 0;
                nextSeqnum = 0;
                timeout = 100.0; // Set the initial timeout value (adjust as needed)
            }

            public Boolean output(NWEmuMsg message) {
                if (isPacketInTransmission()) {
                    sysLog(0, "Sending Host: output(" + message + ") -> Discarded (Packet in transmission or buffer full)");
                    return false;
                }

                NWEmuPkt sndpkt = new NWEmuPkt();
                for (int i = 0; i < NWEmu.PAYSIZE; i++) {
                    sndpkt.payload[i] = message.data[i];
                }
                sndpkt.seqnum = seqnum++;
                sndpkt.acknum = acknum;
                sndpkt.checksum = 0;
                sysLog(2, "Sending Host: output(" + message + ") failed, will be retried!");

                return true;
            }

            private boolean isPacketInTransmission() {
                return (nextSeqnum != base);
            }

            public void input(NWEmuPkt pkt) {
                sysLog(0, "Sending Host: input(" + pkt + ") -> Discarded (Sender does not process incoming packets)");
            }

            public void timerInterrupt() {
                sysLog(2, "Sending Host: timerInterrupt()");
            }

        }

        // class representing the receiver
        class ReceivingHost extends CommonHost {
            private int expectedSeqnum;

            public void init() {
                sysLog(0, "Receiving Host: init()");
                seqnum = -1;
                acknum = 0;
                expectedSeqnum = 0;
            }

            public void input(NWEmuPkt pkt) {
                sysLog(2, "Receiving Host: input(" + pkt + ")");
                if (pkt.seqnum == expectedSeqnum) {
                    NWEmuMsg message = new NWEmuMsg();
                    for (int i = 0; i < NWEmu.PAYSIZE; i++) {
                        message.data[i] = pkt.payload[i];
                    }
                    sysLog(2, "Receiving Host: input(" + pkt + ") -> toLayer5(" + message + ")");
                    toLayer5(message);

                    expectedSeqnum = (expectedSeqnum + 1) % 2;
                } else {
                    sysLog(0, "Receiving Host: input(" + pkt + ") -> Discarded (Out-of-order packet)");
                }

                NWEmuPkt ackpkt = new NWEmuPkt();
                ackpkt.acknum = pkt.seqnum;
                ackpkt.checksum = 0;
                sysLog(2, "Receiving Host: input(" + pkt + ") -> toLayer3(" + ackpkt + ")");
                toLayer3(ackpkt);

            }
        }

        // instantiate sender and receiver
        SendingHost HostA = new SendingHost();
        ReceivingHost HostB = new ReceivingHost();

        // perform emulation
        NWEmu TestEmu = new NWEmu(HostA, HostB);
        TestEmu.randTimer();
        TestEmu.emulate(20, 0.2, 0.2, 10.0, 2);
    }
}
